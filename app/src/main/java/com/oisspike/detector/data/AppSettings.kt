package com.oisspike.detector.data

import android.content.Context
import android.content.SharedPreferences

data class SymbolInfo(val scrip: Int, val seg: String)

object Defaults {
    val SYMBOLS = mapOf(
        "NIFTY" to SymbolInfo(13, "IDX_I"),
        "BANKNIFTY" to SymbolInfo(25, "IDX_I"),
        "FINNIFTY" to SymbolInfo(27, "IDX_I"),
        "MIDCPNIFTY" to SymbolInfo(442, "IDX_I"),
        "SENSEX" to SymbolInfo(51, "IDX_I"),
    )
}

class AppSettings(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("oi_spike_prefs", Context.MODE_PRIVATE)

    var clientId: String
        get() = prefs.getString("client_id", "") ?: ""
        set(v) = prefs.edit().putString("client_id", v).apply()

    var accessToken: String
        get() = prefs.getString("access_token", "") ?: ""
        set(v) = prefs.edit().putString("access_token", v).apply()

    var telegramBotToken: String
        get() = prefs.getString("telegram_bot_token", "") ?: ""
        set(v) = prefs.edit().putString("telegram_bot_token", v).apply()

    var telegramChatId: String
        get() = prefs.getString("telegram_chat_id", "") ?: ""
        set(v) = prefs.edit().putString("telegram_chat_id", v).apply()

    var pollIntervalSec: Int
        get() = prefs.getInt("poll_interval_sec", 60)
        set(v) = prefs.edit().putInt("poll_interval_sec", v.coerceAtLeast(5)).apply()

    var liveCandleIntervalMin: Int
        get() = prefs.getInt("live_candle_interval_min", 5)
        set(v) = prefs.edit().putInt("live_candle_interval_min", v).apply()

    var includeStandardWindows: Boolean
        get() = prefs.getBoolean("live_include_standard_windows", true)
        set(v) = prefs.edit().putBoolean("live_include_standard_windows", v).apply()

    var oiSpike1mPct: Float
        get() = prefs.getFloat("oi_spike_1m_pct", 8f)
        set(v) = prefs.edit().putFloat("oi_spike_1m_pct", v).apply()

    var oiSpike5mPct: Float
        get() = prefs.getFloat("oi_spike_5m_pct", 12f)
        set(v) = prefs.edit().putFloat("oi_spike_5m_pct", v).apply()

    var oiSpike10mPct: Float
        get() = prefs.getFloat("oi_spike_10m_pct", 25f)
        set(v) = prefs.edit().putFloat("oi_spike_10m_pct", v).apply()

    var priceChangePct: Float
        get() = prefs.getFloat("price_change_pct", 1f)
        set(v) = prefs.edit().putFloat("price_change_pct", v).apply()

    var minOi: Float
        get() = prefs.getFloat("min_oi", 90000f)
        set(v) = prefs.edit().putFloat("min_oi", v).apply()

    var alertCooldownMin: Int
        get() = prefs.getInt("alert_cooldown_min", 10)
        set(v) = prefs.edit().putInt("alert_cooldown_min", v).apply()

    var enableTelegram: Boolean
        get() = prefs.getBoolean("enable_telegram_alert", true)
        set(v) = prefs.edit().putBoolean("enable_telegram_alert", v).apply()

    var atmRangeEnabled: Boolean
        get() = prefs.getBoolean("atm_range_enabled", true)
        set(v) = prefs.edit().putBoolean("atm_range_enabled", v).apply()

    var atmRangeStrikes: Int
        get() = prefs.getInt("atm_range_strikes", 5)
        set(v) = prefs.edit().putInt("atm_range_strikes", v).apply()

    var expiriesPerSymbol: Int
        get() = prefs.getInt("expiries_per_symbol", 2)
        set(v) = prefs.edit().putInt("expiries_per_symbol", v).apply()

    var symbolsEnabled: Set<String>
        get() = prefs.getStringSet("symbols_enabled", setOf("NIFTY")) ?: setOf("NIFTY")
        set(v) = prefs.edit().putStringSet("symbols_enabled", v).apply()
}
