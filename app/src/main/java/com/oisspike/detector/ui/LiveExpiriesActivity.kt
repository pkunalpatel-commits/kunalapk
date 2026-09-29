package com.oisspike.detector.ui

import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.oisspike.detector.R
import com.oisspike.detector.data.AppSettings
import com.oisspike.detector.data.Defaults
import com.oisspike.detector.net.DhanClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Same as desktop Live tab: Fetch Expiries → tick marks → Apply to live scan.
 */
class LiveExpiriesActivity : AppCompatActivity() {

    private lateinit var settings: AppSettings
    private lateinit var expiryBox: LinearLayout
    private lateinit var statusText: TextView
    private val expiryChecks = linkedMapOf<String, CheckBox>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live_expiries)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = "Live Expiries"
        settings = AppSettings(this)

        expiryBox = findViewById(R.id.liveExpiryBox)
        statusText = findViewById(R.id.liveExpiryStatus)
        findViewById<EditText>(R.id.liveExpirySymbol).setText("NIFTY")

        findViewById<Button>(R.id.btnLiveFetchExpiries).setOnClickListener { fetchExpiries() }
        findViewById<Button>(R.id.btnLiveApplyExpiries).setOnClickListener { applyExpiries() }
        findViewById<Button>(R.id.btnLiveClearExpiries).setOnClickListener { clearExpiries() }

        refreshStatus()
    }

    private fun currentSymbol(): String =
        findViewById<EditText>(R.id.liveExpirySymbol).text.toString().trim().uppercase()

    private fun refreshStatus() {
        val symbol = currentSymbol()
        val custom = settings.getCustomExpiries(symbol)
        statusText.text = if (custom.isEmpty()) {
            "Active override for $symbol: none (uses nearest ${settings.expiriesPerSymbol})"
        } else {
            "Active override for $symbol: ${custom.joinToString()}"
        }
    }

    private fun rebuildChecks(expiries: List<String>, preselect: Set<String>) {
        expiryBox.removeAllViews()
        expiryChecks.clear()
        expiries.forEachIndexed { index, exp ->
            val cb = CheckBox(this).apply {
                text = exp
                setTextColor(0xFFE7ECF3.toInt())
                isChecked = if (preselect.isNotEmpty()) exp in preselect else index == 0
            }
            expiryBox.addView(cb)
            expiryChecks[exp] = cb
        }
    }

    private fun fetchExpiries() {
        if (settings.clientId.isBlank() || settings.accessToken.isBlank()) {
            Toast.makeText(this, "Set Dhan credentials in Settings first", Toast.LENGTH_LONG).show()
            return
        }
        val symbol = currentSymbol()
        val info = Defaults.SYMBOLS[symbol]
        if (info == null) {
            Toast.makeText(this, "Unsupported symbol: $symbol", Toast.LENGTH_SHORT).show()
            return
        }
        statusText.text = "Fetching…"
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val list = withContext(Dispatchers.IO) {
                    DhanClient(settings.clientId, settings.accessToken)
                        .getExpiryListRaw(info.scrip, info.seg)
                }
                val pre = settings.getCustomExpiries(symbol).toSet()
                rebuildChecks(list, pre)
                statusText.text = "${list.size} expiries — tick and Apply"
                Toast.makeText(this@LiveExpiriesActivity, "Loaded ${list.size} expiries", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                statusText.text = "Fetch failed: ${e.message}"
                Toast.makeText(this@LiveExpiriesActivity, e.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun applyExpiries() {
        val symbol = currentSymbol()
        if (expiryChecks.isEmpty()) {
            Toast.makeText(this, "Fetch Expiries first", Toast.LENGTH_SHORT).show()
            return
        }
        val ticked = expiryChecks.filter { it.value.isChecked }.map { it.key }
        if (ticked.isEmpty()) {
            Toast.makeText(this, "Tick at least one expiry", Toast.LENGTH_SHORT).show()
            return
        }
        settings.setCustomExpiries(symbol, ticked)
        refreshStatus()
        Toast.makeText(this, "Live scan will use: ${ticked.joinToString()}", Toast.LENGTH_LONG).show()
    }

    private fun clearExpiries() {
        val symbol = currentSymbol()
        settings.clearCustomExpiries(symbol)
        refreshStatus()
        Toast.makeText(this, "Cleared override for $symbol — back to nearest N", Toast.LENGTH_SHORT).show()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
