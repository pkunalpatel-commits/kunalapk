package com.oisspike.detector.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.oisspike.detector.R
import com.oisspike.detector.data.AppSettings
import com.oisspike.detector.data.Defaults
import com.oisspike.detector.engine.OiEngine
import com.oisspike.detector.engine.SpikeAlert
import com.oisspike.detector.engine.atmDistanceOf
import com.oisspike.detector.engine.buildLiveWindows
import com.oisspike.detector.engine.suggestedPollSec
import com.oisspike.detector.net.DhanApiException
import com.oisspike.detector.net.DhanAuth
import com.oisspike.detector.net.DhanClient
import com.oisspike.detector.net.TotpUtil
import com.oisspike.detector.net.TelegramClient
import com.oisspike.detector.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList

class ScanService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var loopJob: Job? = null
    private lateinit var settings: AppSettings

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        settings = AppSettings(this)
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopScan()
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                startForeground(NOTIF_ID, buildNotification("Scanning…"))
                startScanLoop()
            }
        }
        return START_STICKY
    }

    private fun startScanLoop() {
        if (loopJob?.isActive == true) return
        isRunning = true
        notifyState()
        loopJob = scope.launch {
            // Refresh access token via TOTP if PIN+secret configured
                if (settings.dhanPin.isNotBlank() && settings.totpSecret.isNotBlank()) {
                    try {
                        val code = TotpUtil.generate(settings.totpSecret)
                        val tr = DhanAuth.generateAccessToken(settings.clientId, settings.dhanPin, code)
                        if (tr.ok) {
                            settings.accessToken = tr.accessToken
                            settings.tokenExpiryHint = tr.expiryTime
                            appendLog("Token refreshed via TOTP until ${tr.expiryTime}")
                        } else {
                            appendLog("TOTP token refresh failed: ${tr.message}")
                        }
                    } catch (e: Exception) {
                        appendLog("TOTP error: ${e.message}")
                    }
                }
                val client = DhanClient(settings.clientId, settings.accessToken)
            val windows = buildLiveWindows(
                settings.liveCandleIntervalMin,
                settings.oiSpike1mPct,
                settings.oiSpike5mPct,
                settings.oiSpike10mPct,
                settings.includeStandardWindows,
            )
            val engine = OiEngine(
                windows,
                settings.priceChangePct.toDouble(),
                settings.minOi.toDouble(),
                settings.alertCooldownMin * 60L,
            )
            val pollSec = suggestedPollSec(settings.liveCandleIntervalMin, settings.pollIntervalSec)
            appendLog(
                "Live mode: candle=${settings.liveCandleIntervalMin}m  poll≈${pollSec}s  " +
                    "windows=" + windows.joinToString { "${it.label}≥${it.oiThreshold}%" } +
                    "  price≥${settings.priceChangePct}%  minOI=${settings.minOi.toLong()}"
            )
            appendLog("Warm-up: need ~1 full candle interval of samples before 1m alerts (same as historical bars).")
            appendLog("Scanner started. Windows: ${windows.joinToString { it.label }}")

            while (isActive && isRunning) {
                try {
                    client.clientId = settings.clientId
                    client.accessToken = settings.accessToken
                    engine.updateConfig(
                        buildLiveWindows(
                            settings.liveCandleIntervalMin,
                            settings.oiSpike1mPct,
                            settings.oiSpike5mPct,
                            settings.oiSpike10mPct,
                            settings.includeStandardWindows,
                        ),
                        settings.priceChangePct.toDouble(),
                        settings.minOi.toDouble(),
                        settings.alertCooldownMin * 60L,
                    )

                    if (client.clientId.isBlank() || client.accessToken.isBlank()) {
                        appendLog("Missing Dhan Client ID / Access Token in Settings")
                        delay(10_000)
                        continue
                    }

                    val symbols = settings.allEnabledSymbols()
                    for (symbol in symbols) {
                        if (!isRunning) break
                        val info = Defaults.SYMBOLS[symbol] ?: continue
                        try {
                            val custom = settings.getCustomExpiries(symbol)
                            val expiries = if (custom.isNotEmpty()) {
                                custom
                            } else {
                                client.getExpiryListRaw(info.scrip, info.seg)
                                    .take(settings.expiriesPerSymbol)
                            }
                            for (expiry in expiries) {
                                if (!isRunning) break
                                try {
                                    val chain = client.getOptionChain(info.scrip, info.seg, expiry)
                                    processChain(engine, symbol, expiry, chain)
                                    appendLog("Scanned $symbol $expiry")
                                } catch (e: DhanApiException) {
                                    appendLog("ERROR $symbol $expiry: ${e.message}")
                                }
                            }
                        } catch (e: Exception) {
                            appendLog("ERROR $symbol: ${e.message}")
                        }
                    }
                    lastSweepTs = System.currentTimeMillis()
                    notifyState()
                    updateNotification("Idle — next sweep soon")
                } catch (e: Exception) {
                    appendLog("Sweep error: ${e.message}")
                }
                val waitSec = suggestedPollSec(settings.liveCandleIntervalMin, settings.pollIntervalSec)
                appendLog("Next sweep in ${waitSec}s (candle=${settings.liveCandleIntervalMin}m, poll setting=${settings.pollIntervalSec}s)")
                delay(waitSec * 1000L)
            }
            appendLog("Scanner stopped")
            isRunning = false
            notifyState()
        }
    }

    private fun processChain(engine: OiEngine, symbol: String, expiry: String, chain: JSONObject) {
        val oc = chain.optJSONObject("oc") ?: return
        val lastPrice = chain.optDouble("last_price", Double.NaN)
        var keys = oc.keys().asSequence().toList()
        if (settings.atmRangeEnabled && !lastPrice.isNaN()) {
            val strikes = keys.mapNotNull { it.toDoubleOrNull() }.sorted()
            if (strikes.isNotEmpty()) {
                val atm = strikes.minByOrNull { kotlin.math.abs(it - lastPrice) } ?: return
                val idx = strikes.indexOf(atm)
                val n = settings.atmRangeStrikes
                val lo = (idx - n).coerceAtLeast(0)
                val hi = (idx + n + 1).coerceAtMost(strikes.size)
                val keep = strikes.subList(lo, hi).map { String.format(Locale.US, "%.6f", it) }.toSet()
                // keys in JSON may be "24500.000000"
                keys = keys.filter { k ->
                    val s = k.toDoubleOrNull() ?: return@filter false
                    strikes.subList(lo, hi).any { kotlin.math.abs(it - s) < 0.01 }
                }
            }
        }
        val ts = System.currentTimeMillis() / 1000
        val allStrikes = oc.keys().asSequence().mapNotNull { it.toDoubleOrNull() }.sorted().toList()
        for (strikeKey in keys) {
            val legs = oc.optJSONObject(strikeKey) ?: continue
            val strike = strikeKey.toDoubleOrNull() ?: continue
            val dist = if (!lastPrice.isNaN()) atmDistanceOf(strike, lastPrice, allStrikes) else null
            for (side in listOf("ce" to "CE", "pe" to "PE")) {
                val leg = legs.optJSONObject(side.first) ?: continue
                val oi = leg.optDouble("oi", 0.0)
                val ltp = leg.optDouble("last_price", 0.0)
                if (oi <= 0) continue
                // Only scan contracts at/above min LTP
                if (settings.minLtp > 0f && ltp < settings.minLtp) continue
                val spotVal = if (!lastPrice.isNaN()) lastPrice else null
                val alerts = engine.ingest(symbol, expiry, strike, side.second, oi, ltp, ts, dist, spotVal)
                for (a in alerts) {
                    onSpike(a)
                }
            }
        }
    }

    private fun onSpike(a: SpikeAlert) {
        recentSpikes.add(0, a)
        while (recentSpikes.size > 200) recentSpikes.removeAt(recentSpikes.size - 1)
        val msg = formatAlert(a)
        appendLog("SPIKE ${a.window} ${a.symbol} ${a.strike} ${a.type} OI +${a.oiChangePct}%")
        notifyState()
        showSpikeNotification(a)
        if (settings.enableTelegram) {
            scope.launch {
                TelegramClient.send(settings.telegramBotToken, settings.telegramChatId, msg)
            }
        }
    }

    private fun formatAlert(a: SpikeAlert): String {
        val arrow = if (a.oiChangePct >= 0) "🔼" else "🔽"
        val atmLabel = when (val d = a.atmDistance) {
            null -> ""
            0 -> "  ATM"
            else -> "  ATM${if (d > 0) "+" else ""}$d"
        }
        val spotLabel = a.spot?.let { "Spot: ${String.format(java.util.Locale.US, "%.2f", it)}\n" } ?: ""
        return "$arrow OI SPIKE (${a.window}) — ${a.symbol} ${a.strike} ${a.type}$atmLabel\n" +
            "Expiry: ${a.expiry}\n" +
            spotLabel +
            "OI: ${a.oi.toLong()}  (+${a.oiChangePct}% in ${a.window})\n" +
            "LTP: ${a.ltp}  (${if (a.priceChangePct >= 0) "+" else ""}${a.priceChangePct}%)"
    }

    private fun stopScan() {
        isRunning = false
        loopJob?.cancel()
        loopJob = null
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    override fun onDestroy() {
        stopScan()
        scope.cancel()
        super.onDestroy()
    }

    private fun createChannel() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "OI Scanner", NotificationManager.IMPORTANCE_LOW)
        )
        nm.createNotificationChannel(
            NotificationChannel(SPIKE_CHANNEL, "OI Spikes", NotificationManager.IMPORTANCE_HIGH)
        )
    }

    private fun buildNotification(text: String): Notification {
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stop = PendingIntent.getService(
            this, 1,
            Intent(this, ScanService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("OI Spike Detector")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentIntent(open)
            .addAction(0, "Stop", stop)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIF_ID, buildNotification(text))
    }

    private fun showSpikeNotification(a: SpikeAlert) {
        val nm = getSystemService(NotificationManager::class.java)
        val n = NotificationCompat.Builder(this, SPIKE_CHANNEL)
            .setContentTitle("OI Spike ${a.window}: ${a.symbol} ${a.strike} ${a.type}")
            .setContentText("OI +${a.oiChangePct}%  LTP ${a.priceChangePct}%")
            .setSmallIcon(R.drawable.ic_launcher)
            .setAutoCancel(true)
            .build()
        nm.notify((System.currentTimeMillis() % 100000).toInt(), n)
    }

    companion object {
        const val ACTION_STOP = "com.oisspike.detector.STOP"
        private const val CHANNEL_ID = "oi_scan"
        private const val SPIKE_CHANNEL = "oi_spikes"
        private const val NOTIF_ID = 1001
        private const val TAG = "ScanService"

        @Volatile var isRunning: Boolean = false
            private set
        @Volatile var lastSweepTs: Long = 0L
            private set

        val recentSpikes = CopyOnWriteArrayList<SpikeAlert>()
        val logs = CopyOnWriteArrayList<String>()

        private val listeners = CopyOnWriteArrayList<() -> Unit>()

        fun addListener(l: () -> Unit) { listeners.add(l) }
        fun removeListener(l: () -> Unit) { listeners.remove(l) }
        private fun notifyState() { listeners.forEach { runCatching { it() } } }

        fun appendLog(msg: String) {
            val line = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date()) + "  " + msg
            logs.add(0, line)
            while (logs.size > 150) logs.removeAt(logs.size - 1)
            Log.d(TAG, msg)
            notifyState()
        }

        fun start(ctx: Context) {
            val i = Intent(ctx, ScanService::class.java)
            ctx.startForegroundService(i)
        }

        fun stop(ctx: Context) {
            ctx.startService(Intent(ctx, ScanService::class.java).setAction(ACTION_STOP))
        }
    }
}
