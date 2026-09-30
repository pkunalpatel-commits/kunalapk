package com.oisspike.detector.ui

import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.oisspike.detector.R
import com.oisspike.detector.data.AppSettings
import com.oisspike.detector.data.Defaults
import com.oisspike.detector.net.TelegramClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsActivity : AppCompatActivity() {
    private lateinit var settings: AppSettings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = "Settings"
        settings = AppSettings(this)

        val clientId = findViewById<EditText>(R.id.inputClientId)
        val token = findViewById<EditText>(R.id.inputToken)
        val bot = findViewById<EditText>(R.id.inputBotToken)
        val chat = findViewById<EditText>(R.id.inputChatId)
        val poll = findViewById<EditText>(R.id.inputPoll)
        val candle = findViewById<EditText>(R.id.inputCandle)
        val oi1 = findViewById<EditText>(R.id.inputOi1)
        val oi5 = findViewById<EditText>(R.id.inputOi5)
        val oi10 = findViewById<EditText>(R.id.inputOi10)
        val price = findViewById<EditText>(R.id.inputPrice)
        val minOi = findViewById<EditText>(R.id.inputMinOi)
        val minLtp = findViewById<EditText>(R.id.inputMinLtp)
        val cooldown = findViewById<EditText>(R.id.inputCooldown)
        val atm = findViewById<EditText>(R.id.inputAtm)
        val chkTelegram = findViewById<CheckBox>(R.id.chkTelegram)
        val chkStandard = findViewById<CheckBox>(R.id.chkStandard)
        val chkAtm = findViewById<CheckBox>(R.id.chkAtm)
        val chkNifty = findViewById<CheckBox>(R.id.chkNifty)
        val chkBank = findViewById<CheckBox>(R.id.chkBank)
        val chkFin = findViewById<CheckBox>(R.id.chkFin)
        val extraSym = findViewById<EditText>(R.id.inputExtraSymbols)

        clientId.setText(settings.clientId)
        token.setText(settings.accessToken)
        bot.setText(settings.telegramBotToken)
        chat.setText(settings.telegramChatId)
        poll.setText(settings.pollIntervalSec.toString())
        candle.setText(settings.liveCandleIntervalMin.toString())
        oi1.setText(settings.oiSpike1mPct.toString())
        oi5.setText(settings.oiSpike5mPct.toString())
        oi10.setText(settings.oiSpike10mPct.toString())
        price.setText(settings.priceChangePct.toString())
        minOi.setText(settings.minOi.toLong().toString())
        minLtp.setText(settings.minLtp.toString())
        cooldown.setText(settings.alertCooldownMin.toString())
        atm.setText(settings.atmRangeStrikes.toString())
        chkTelegram.isChecked = settings.enableTelegram
        chkStandard.isChecked = settings.includeStandardWindows
        chkAtm.isChecked = settings.atmRangeEnabled
        val syms = settings.symbolsEnabled
        chkNifty.isChecked = "NIFTY" in syms
        chkBank.isChecked = "BANKNIFTY" in syms
        chkFin.isChecked = "FINNIFTY" in syms
        extraSym.setText(settings.extraSymbols)
        // Optional: pick symbol into extra list
        findViewById<Button?>(R.id.btnSearchExtra)?.setOnClickListener {
            SymbolPicker.show(this, "") { picked ->
                val cur = extraSym.text.toString().trim()
                extraSym.setText(if (cur.isEmpty()) picked else "$cur,$picked")
            }
        }

        findViewById<Button>(R.id.btnSave).setOnClickListener {
            settings.clientId = clientId.text.toString().trim()
            settings.accessToken = token.text.toString().trim()
            settings.telegramBotToken = bot.text.toString().trim()
            settings.telegramChatId = chat.text.toString().trim()
            settings.pollIntervalSec = poll.text.toString().toIntOrNull() ?: 60
            settings.liveCandleIntervalMin = candle.text.toString().toIntOrNull() ?: 5
            settings.oiSpike1mPct = oi1.text.toString().toFloatOrNull() ?: 8f
            settings.oiSpike5mPct = oi5.text.toString().toFloatOrNull() ?: 12f
            settings.oiSpike10mPct = oi10.text.toString().toFloatOrNull() ?: 25f
            settings.priceChangePct = price.text.toString().toFloatOrNull() ?: 1f
            settings.minOi = minOi.text.toString().toFloatOrNull() ?: 90000f
            settings.minLtp = minLtp.text.toString().toFloatOrNull() ?: 0f
            settings.alertCooldownMin = cooldown.text.toString().toIntOrNull() ?: 10
            settings.atmRangeStrikes = atm.text.toString().toIntOrNull() ?: 5
            settings.enableTelegram = chkTelegram.isChecked
            settings.includeStandardWindows = chkStandard.isChecked
            settings.atmRangeEnabled = chkAtm.isChecked
            val set = mutableSetOf<String>()
            if (chkNifty.isChecked) set.add("NIFTY")
            if (chkBank.isChecked) set.add("BANKNIFTY")
            if (chkFin.isChecked) set.add("FINNIFTY")
            if (set.isEmpty()) set.add("NIFTY")
            settings.symbolsEnabled = set
            settings.extraSymbols = extraSym.text.toString().trim()
            Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnTestTelegram).setOnClickListener {
            settings.telegramBotToken = bot.text.toString().trim()
            settings.telegramChatId = chat.text.toString().trim()
            CoroutineScope(Dispatchers.Main).launch {
                val (ok, info) = withContext(Dispatchers.IO) {
                    TelegramClient.send(
                        settings.telegramBotToken,
                        settings.telegramChatId,
                        "✅ OI Spike Detector (Android): Telegram test successful.",
                    )
                }
                Toast.makeText(
                    this@SettingsActivity,
                    if (ok) info else "Failed: $info",
                    Toast.LENGTH_LONG,
                ).show()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
