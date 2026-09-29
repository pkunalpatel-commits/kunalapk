package com.oisspike.detector.ui

import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
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
    private lateinit var expiryBox: LinearLayout
    private lateinit var expiryHint: TextView
    private lateinit var adapter: SpikeAdapter
    private var job: Job? = null
    private val expiryChecks = linkedMapOf<String, CheckBox>()
    private var filtersPanelView: View? = null
    private var filtersToggleBtn: Button? = null

    private fun hideOptionsForResults() {
        filtersPanelView?.visibility = View.GONE
        filtersToggleBtn?.text = "Show options"
    }

    private fun showOptions() {
        filtersPanelView?.visibility = View.VISIBLE
        filtersToggleBtn?.text = "Hide options"
    }

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
        expiryBox = findViewById(R.id.histExpiryBox)
        expiryHint = findViewById(R.id.histExpiryHint)
        val list = findViewById<RecyclerView>(R.id.histList)
        adapter = SpikeAdapter().also { it.showDate = true }
        list.layoutManager = LinearLayoutManager(this)
        list.adapter = adapter
        fun bindSort(id: Int, sort: SpikeSort) {
            findViewById<Button>(id).setOnClickListener { adapter.setSort(sort) }
        }
        bindSort(R.id.btnHistSortTimeDesc, SpikeSort.TIME_DESC)
        bindSort(R.id.btnHistSortTimeAsc, SpikeSort.TIME_ASC)
        bindSort(R.id.btnHistSortOiDesc, SpikeSort.OI_DESC)
        bindSort(R.id.btnHistSortOiAsc, SpikeSort.OI_ASC)
        bindSort(R.id.btnHistSortStrikeAsc, SpikeSort.STRIKE_ASC)
        bindSort(R.id.btnHistSortLtpDesc, SpikeSort.LTP_DESC)
        bindSort(R.id.btnHistSortExpAsc, SpikeSort.EXPIRY_ASC)

        val day = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val today = day.format(Calendar.getInstance().time)
        findViewById<EditText>(R.id.histFrom).setText(today)
        findViewById<EditText>(R.id.histTo).setText(today)
        findViewById<EditText>(R.id.histSymbol).setText("NIFTY")
        findViewById<EditText>(R.id.histInterval).setText("5")
        findViewById<EditText>(R.id.histAtm).setText(settings.atmRangeStrikes.toString())
        findViewById<CheckBox>(R.id.histCe).isChecked = true
        findViewById<CheckBox>(R.id.histPe).isChecked = true
        findViewById<EditText>(R.id.histMinLtp).setText(settings.minLtp.toString())

        val filtersPanel = findViewById<View>(R.id.histFiltersPanel)
        val btnToggle = findViewById<Button>(R.id.btnToggleFilters)
        fun setFiltersVisible(show: Boolean) {
            filtersPanel.visibility = if (show) View.VISIBLE else View.GONE
            btnToggle.text = if (show) "Hide options" else "Show options"
        }
        btnToggle.setOnClickListener {
            setFiltersVisible(filtersPanel.visibility != View.VISIBLE)
        }
        // Keep reference for auto-hide on scan
        this.filtersPanelView = filtersPanel
        this.filtersToggleBtn = btnToggle

        findViewById<Button>(R.id.btnFetchExpiries).setOnClickListener { fetchExpiries() }
        findViewById<Button>(R.id.btnHistScan).setOnClickListener { startScan() }
        findViewById<Button>(R.id.btnHistStop).setOnClickListener {
            job?.cancel()
            statusText.text = "Stopped"
            appendLog("Scan cancelled")
        }
    }

    private fun selectedExpiries(): List<String> {
        return expiryChecks.filter { it.value.isChecked }.map { it.key }
    }

    private fun rebuildExpiryChecks(expiries: List<String>) {
        expiryBox.removeAllViews()
        expiryChecks.clear()
        if (expiries.isEmpty()) {
            expiryHint.text = "No expiries found."
            return
        }
        expiries.forEachIndexed { index, exp ->
            val cb = CheckBox(this).apply {
                text = exp
                setTextColor(0xFFE7ECF3.toInt())
                // Tick first expiry by default (same as desktop)
                isChecked = index == 0
            }
            expiryBox.addView(cb)
            expiryChecks[exp] = cb
        }
        expiryHint.text = "${expiries.size} expiries loaded — tick the ones to scan, then Scan Historical."
    }

    private fun fetchExpiries() {
        if (settings.clientId.isBlank() || settings.accessToken.isBlank()) {
            Toast.makeText(this, "Set Dhan Client ID + Token in Settings first", Toast.LENGTH_LONG).show()
            return
        }
        val symbol = findViewById<EditText>(R.id.histSymbol).text.toString().trim().uppercase()
        val info = Defaults.SYMBOLS[symbol]
        if (info == null) {
            Toast.makeText(this, "Symbol not supported: $symbol", Toast.LENGTH_SHORT).show()
            return
        }
        statusText.text = "Fetching expiries…"
        appendLog("Fetching expiries for $symbol…")
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val list = withContext(Dispatchers.IO) {
                    DhanClient(settings.clientId, settings.accessToken)
                        .getExpiryListRaw(info.scrip, info.seg)
                }
                rebuildExpiryChecks(list)
                statusText.text = "Expiries loaded (${list.size})"
                appendLog("Found ${list.size} expiries")
            } catch (e: Exception) {
                statusText.text = "Fetch failed"
                appendLog("ERROR fetch expiries: ${e.message}")
                Toast.makeText(this@HistoricalActivity, e.message, Toast.LENGTH_LONG).show()
            }
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
        val minLtpUi = findViewById<EditText>(R.id.histMinLtp).text.toString().toFloatOrNull()
        if (minLtpUi != null) settings.minLtp = minLtpUi
        if (!wantCe && !wantPe) {
            Toast.makeText(this, "Tick CE and/or PE", Toast.LENGTH_SHORT).show()
            return
        }
        val info = Defaults.SYMBOLS[symbol]
        if (info == null) {
            Toast.makeText(this, "Symbol not supported: $symbol", Toast.LENGTH_SHORT).show()
            return
        }

        // Require Fetch Expiries + tick marks (same as desktop)
        if (expiryChecks.isEmpty()) {
            Toast.makeText(this, "Click Fetch Expiries first, then tick expiries to scan", Toast.LENGTH_LONG).show()
            return
        }
        val expiries = selectedExpiries()
        if (expiries.isEmpty()) {
            Toast.makeText(this, "Tick at least one expiry", Toast.LENGTH_SHORT).show()
            return
        }

        job?.cancel()
        adapter.submit(emptyList())
        hideOptionsForResults()  // free screen for results
        statusText.text = "Scanning…"
        appendLog("Historical scan $symbol  expiries=${expiries.joinToString()}  $from → $to  ${intervalMin}m")

        job = CoroutineScope(Dispatchers.Main).launch {
            val results = mutableListOf<SpikeAlert>()
            try {
                withContext(Dispatchers.IO) {
                    val client = DhanClient(settings.clientId, settings.accessToken)
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
                        val contracts = pickContracts(chain, atmN, wantCe, wantPe, settings.minLtp.toDouble())
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
        minLtp: Double,
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
                val ltp = ce?.optDouble("last_price", 0.0) ?: 0.0
                if (!sid.isNullOrBlank() && sid != "null" && (minLtp <= 0 || ltp >= minLtp)) {
                    out.add(Contract(sid, strike, "CE"))
                }
            }
            if (wantPe) {
                val pe = legs.optJSONObject("pe")
                val sid = pe?.opt("security_id")?.toString()
                val ltp = pe?.optDouble("last_price", 0.0) ?: 0.0
                if (!sid.isNullOrBlank() && sid != "null" && (minLtp <= 0 || ltp >= minLtp)) {
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
