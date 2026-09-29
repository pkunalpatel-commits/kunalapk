package com.oisspike.detector.ui

import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.oisspike.detector.R
import com.oisspike.detector.data.AppSettings
import com.oisspike.detector.data.Defaults
import com.oisspike.detector.engine.HistoricalEngine
import com.oisspike.detector.engine.HistoricalSpike
import com.oisspike.detector.engine.SpikeAlert
import com.oisspike.detector.net.DhanApiException
import com.oisspike.detector.net.DhanClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

class HistoricalActivity : AppCompatActivity() {

    private lateinit var settings: AppSettings
    private lateinit var logText: TextView
    private lateinit var statusText: TextView
    private lateinit var adapter: SpikeAdapter
    private var job: Job? = null

    // Reuse SpikeAdapter by mapping HistoricalSpike → SpikeAlert
    private fun toAlert(h: HistoricalSpike) = SpikeAlert(
        h.symbol, h.expiry, h.strike, h.type, h.window,
        h.oi, h.oiChangePct, h.ltp, h.priceChangePct, h.ts,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_historical)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = "Historical Scan"
        settings = AppSettings(this)

        logText = findViewById(R.id.histLog)
        statusText = findViewById(R.id.histStatus)
        val list = findViewById<RecyclerView>(R.id.histList)
        adapter = SpikeAdapter()
        list.layoutManager = LinearLayoutManager(this)
        list.adapter = adapter

        val day = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val today = day.format(Calendar.getInstance().time)
        findViewById<EditText>(R.id.histFrom).setText(today)
        findViewById<EditText>(R.id.histTo).setText(today)
        findViewById<EditText>(R.id.histSymbol).setText("NIFTY")
        findViewById<EditText>(R.id.histInterval).setText("5")
        findViewById<EditText>(R.id.histAtm).setText(settings.atmRangeStrikes.toString())
        findViewById<CheckBox>(R.id.histCe).isChecked = true
        findViewById<CheckBox>(R.id.histPe).isChecked = true

        findViewById<Button>(R.id.btnHistScan).setOnClickListener { startScan() }
        findViewById<Button>(R.id.btnHistStop).setOnClickListener {
            job?.cancel()
            statusText.text = "Stopped"
            appendLog("Scan cancelled")
        }
    }

    private fun startScan() {
        if (settings.clientId.isBlank() || settings.accessToken.isBlank()) {
            Toast.makeText(this, "Set Dhan Client ID + Token in Settings first", Toast.LENGTH_LONG).show()
            return
        }
        val symbol = findViewById<EditText>(R.id.histSymbol).text.toString().trim().uppercase()
        val from = findViewById<EditText>(R.id.histFrom).text.toString().trim()
        val to = findViewById<EditText>(R.id.histTo).text.toString().trim()
        val intervalMin = findViewById<EditText>(R.id.histInterval).text.toString().toIntOrNull() ?: 5
        val atmN = findViewById<EditText>(R.id.histAtm).text.toString().toIntOrNull() ?: 5
        val wantCe = findViewById<CheckBox>(R.id.histCe).isChecked
        val wantPe = findViewById<CheckBox>(R.id.histPe).isChecked
        if (!wantCe && !wantPe) {
            Toast.makeText(this, "Tick CE and/or PE", Toast.LENGTH_SHORT).show()
            return
        }
        val info = Defaults.SYMBOLS[symbol]
        if (info == null) {
            Toast.makeText(this, "Symbol not supported: $symbol", Toast.LENGTH_SHORT).show()
            return
        }

        job?.cancel()
        adapter.submit(emptyList())
        statusText.text = "Scanning…"
        appendLog("Starting historical scan $symbol $from → $to  interval=${intervalMin}m")

        job = CoroutineScope(Dispatchers.Main).launch {
            val results = mutableListOf<SpikeAlert>()
            try {
                withContext(Dispatchers.IO) {
                    val client = DhanClient(settings.clientId, settings.accessToken)
                    val expiries = client.getExpiryListRaw(info.scrip, info.seg)
                        .take(settings.expiriesPerSymbol.coerceAtLeast(1))
                    if (expiries.isEmpty()) {
                        withContext(Dispatchers.Main) { appendLog("No expiries found") }
                        return@withContext
                    }
                    withContext(Dispatchers.Main) {
                        appendLog("Expiries: ${expiries.joinToString()}")
                    }

                    val (native, factor) = HistoricalEngine.nativeInterval(intervalMin)
                    val instrument = if (symbol in listOf("NIFTY", "BANKNIFTY", "FINNIFTY", "MIDCPNIFTY", "SENSEX"))
                        "OPTIDX" else "OPTSTK"

                    for (expiry in expiries) {
                        if (job?.isCancelled == true) break
                        withContext(Dispatchers.Main) { appendLog("Chain $symbol $expiry…") }
                        val chain = try {
                            client.getOptionChain(info.scrip, info.seg, expiry)
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) { appendLog("ERROR chain: ${e.message}") }
                            continue
                        }
                        val contracts = pickContracts(chain, atmN, wantCe, wantPe)
                        withContext(Dispatchers.Main) {
                            appendLog("  ${contracts.size} contracts near ATM")
                        }

                        for (c in contracts) {
                            if (job?.isCancelled == true) break
                            try {
                                var candles = client.getIntradayHistorical(
                                    securityId = c.securityId,
                                    exchangeSegment = "NSE_FNO",
                                    instrument = instrument,
                                    interval = native,
                                    fromDate = from,
                                    toDate = to,
                                    oi = true,
                                )
                                if (factor > 1) {
                                    candles = HistoricalEngine.resample(candles, factor)
                                }
                                val found = HistoricalEngine.findHistoricalSpikes(
                                    candles, symbol, expiry, c.strike, c.type,
                                    intervalMin,
                                    settings.oiSpike1mPct,
                                    settings.oiSpike5mPct,
                                    settings.oiSpike10mPct,
                                    settings.priceChangePct,
                                    settings.minOi,
                                )
                                if (found.isNotEmpty()) {
                                    withContext(Dispatchers.Main) {
                                        appendLog("  ${c.strike} ${c.type}: ${found.size} spike(s)")
                                    }
                                    results.addAll(found.map { toAlert(it) })
                                }
                            } catch (e: DhanApiException) {
                                withContext(Dispatchers.Main) {
                                    appendLog("  ERROR ${c.strike} ${c.type}: ${e.message}")
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    appendLog("  ERROR ${c.strike} ${c.type}: ${e.message}")
                                }
                            }
                        }
                    }
                }
                // newest first
                results.sortByDescending { it.ts }
                adapter.submit(results)
                statusText.text = "Done — ${results.size} spike(s)"
                appendLog("Finished. Total spikes: ${results.size}")
            } catch (e: Exception) {
                statusText.text = "Failed"
                appendLog("Scan failed: ${e.message}")
            }
        }
    }

    private data class Contract(val securityId: String, val strike: Double, val type: String)

    private fun pickContracts(
        chain: JSONObject,
        atmN: Int,
        wantCe: Boolean,
        wantPe: Boolean,
    ): List<Contract> {
        val oc = chain.optJSONObject("oc") ?: return emptyList()
        val lastPrice = chain.optDouble("last_price", Double.NaN)
        val keys = oc.keys().asSequence().toList()
        val strikes = keys.mapNotNull { it.toDoubleOrNull() }.sorted()
        if (strikes.isEmpty()) return emptyList()
        val atm = if (!lastPrice.isNaN()) {
            strikes.minByOrNull { abs(it - lastPrice) } ?: strikes[strikes.size / 2]
        } else strikes[strikes.size / 2]
        val idx = strikes.indexOf(atm)
        val lo = (idx - atmN).coerceAtLeast(0)
        val hi = (idx + atmN + 1).coerceAtMost(strikes.size)
        val selected = strikes.subList(lo, hi)
        val out = mutableListOf<Contract>()
        for (key in keys) {
            val strike = key.toDoubleOrNull() ?: continue
            if (selected.none { abs(it - strike) < 0.01 }) continue
            val legs = oc.optJSONObject(key) ?: continue
            if (wantCe) {
                val ce = legs.optJSONObject("ce")
                val sid = ce?.opt("security_id")?.toString()
                if (!sid.isNullOrBlank() && sid != "null") {
                    out.add(Contract(sid, strike, "CE"))
                }
            }
            if (wantPe) {
                val pe = legs.optJSONObject("pe")
                val sid = pe?.opt("security_id")?.toString()
                if (!sid.isNullOrBlank() && sid != "null") {
                    out.add(Contract(sid, strike, "PE"))
                }
            }
        }
        return out
    }

    private fun appendLog(msg: String) {
        val prev = logText.text?.toString().orEmpty()
        logText.text = if (prev.isBlank()) msg else "$msg\n$prev"
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onDestroy() {
        job?.cancel()
        super.onDestroy()
    }
}
