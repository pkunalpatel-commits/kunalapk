package com.oisspike.detector.data

import android.content.Context
import android.content.SharedPreferences

data class SymbolInfo(val scrip: Int, val seg: String)

object Defaults {
    val SYMBOLS = mapOf(
        // NSE / BSE Index F&O
        "NIFTY" to SymbolInfo(13, "IDX_I"),
        "BANKNIFTY" to SymbolInfo(25, "IDX_I"),
        "FINNIFTY" to SymbolInfo(27, "IDX_I"),
        "MIDCPNIFTY" to SymbolInfo(442, "IDX_I"),
        "NIFTYNXT50" to SymbolInfo(38, "IDX_I"),
        "SENSEX" to SymbolInfo(51, "IDX_I"),
        "BANKEX" to SymbolInfo(69, "IDX_I"),
        // MCX Commodity F&O (options underlyings)
        "CRUDEOIL" to SymbolInfo(11, "MCX_COMM"),
        "CRUDEOILM" to SymbolInfo(556, "MCX_COMM"),
        "NATURALGAS" to SymbolInfo(14, "MCX_COMM"),
        "NATGASMINI" to SymbolInfo(596, "MCX_COMM"),
        "GOLD" to SymbolInfo(1, "MCX_COMM"),
        "GOLDM" to SymbolInfo(2, "MCX_COMM"),
        "SILVER" to SymbolInfo(115, "MCX_COMM"),
        "SILVERM" to SymbolInfo(10, "MCX_COMM"),
        "COPPER" to SymbolInfo(8, "MCX_COMM"),
        "ZINC" to SymbolInfo(27, "MCX_COMM"),
        // NSE Equity F&O stocks
        "360ONE" to SymbolInfo(13061, "NSE_EQ"),
        "ABB" to SymbolInfo(13, "NSE_EQ"),
        "ABCAPITAL" to SymbolInfo(21614, "NSE_EQ"),
        "ADANIENSOL" to SymbolInfo(10217, "NSE_EQ"),
        "ADANIENT" to SymbolInfo(25, "NSE_EQ"),
        "ADANIGREEN" to SymbolInfo(3563, "NSE_EQ"),
        "ADANIPORTS" to SymbolInfo(15083, "NSE_EQ"),
        "ADANIPOWER" to SymbolInfo(17388, "NSE_EQ"),
        "ALKEM" to SymbolInfo(11703, "NSE_EQ"),
        "AMBER" to SymbolInfo(1185, "NSE_EQ"),
        "AMBUJACEM" to SymbolInfo(1270, "NSE_EQ"),
        "ANANDRATHI" to SymbolInfo(7145, "NSE_EQ"),
        "ANGELONE" to SymbolInfo(324, "NSE_EQ"),
        "APLAPOLLO" to SymbolInfo(25780, "NSE_EQ"),
        "APOLLOHOSP" to SymbolInfo(157, "NSE_EQ"),
        "ASHOKLEY" to SymbolInfo(212, "NSE_EQ"),
        "ASIANPAINT" to SymbolInfo(236, "NSE_EQ"),
        "ASTRAL" to SymbolInfo(14418, "NSE_EQ"),
        "ATHERENERG" to SymbolInfo(757645, "NSE_EQ"),
        "AUBANK" to SymbolInfo(21238, "NSE_EQ"),
        "AUROPHARMA" to SymbolInfo(275, "NSE_EQ"),
        "AXISBANK" to SymbolInfo(5900, "NSE_EQ"),
        "BAJAJ-AUTO" to SymbolInfo(16669, "NSE_EQ"),
        "BAJAJFINSV" to SymbolInfo(16675, "NSE_EQ"),
        "BAJAJHLDNG" to SymbolInfo(305, "NSE_EQ"),
        "BAJFINANCE" to SymbolInfo(317, "NSE_EQ"),
        "BANDHANBNK" to SymbolInfo(2263, "NSE_EQ"),
        "BANKBARODA" to SymbolInfo(4668, "NSE_EQ"),
        "BANKINDIA" to SymbolInfo(4745, "NSE_EQ"),
        "BDL" to SymbolInfo(2144, "NSE_EQ"),
        "BEL" to SymbolInfo(383, "NSE_EQ"),
        "BHARATFORG" to SymbolInfo(422, "NSE_EQ"),
        "BHARTIARTL" to SymbolInfo(10604, "NSE_EQ"),
        "BHEL" to SymbolInfo(438, "NSE_EQ"),
        "BIOCON" to SymbolInfo(11373, "NSE_EQ"),
        "BLUESTARCO" to SymbolInfo(8311, "NSE_EQ"),
        "BOSCHLTD" to SymbolInfo(2181, "NSE_EQ"),
        "BPCL" to SymbolInfo(526, "NSE_EQ"),
        "BRITANNIA" to SymbolInfo(547, "NSE_EQ"),
        "BSE" to SymbolInfo(19585, "NSE_EQ"),
        "CAMS" to SymbolInfo(342, "NSE_EQ"),
        "CANBK" to SymbolInfo(10794, "NSE_EQ"),
        "CDSL" to SymbolInfo(21174, "NSE_EQ"),
        "CGPOWER" to SymbolInfo(760, "NSE_EQ"),
        "CHOLAFIN" to SymbolInfo(685, "NSE_EQ"),
        "CIPLA" to SymbolInfo(694, "NSE_EQ"),
        "COALINDIA" to SymbolInfo(20374, "NSE_EQ"),
        "COCHINSHIP" to SymbolInfo(21508, "NSE_EQ"),
        "COFORGE" to SymbolInfo(11543, "NSE_EQ"),
        "COLPAL" to SymbolInfo(15141, "NSE_EQ"),
        "CONCOR" to SymbolInfo(4749, "NSE_EQ"),
        "CROMPTON" to SymbolInfo(17094, "NSE_EQ"),
        "CUMMINSIND" to SymbolInfo(1901, "NSE_EQ"),
        "DABUR" to SymbolInfo(772, "NSE_EQ"),
        "DELHIVERY" to SymbolInfo(9599, "NSE_EQ"),
        "DIVISLAB" to SymbolInfo(10940, "NSE_EQ"),
        "DIXON" to SymbolInfo(21690, "NSE_EQ"),
        "DLF" to SymbolInfo(14732, "NSE_EQ"),
        "DMART" to SymbolInfo(19913, "NSE_EQ"),
        "DRREDDY" to SymbolInfo(881, "NSE_EQ"),
        "EICHERMOT" to SymbolInfo(910, "NSE_EQ"),
        "ENRIN" to SymbolInfo(756871, "NSE_EQ"),
        "ETERNAL" to SymbolInfo(5097, "NSE_EQ"),
        "FEDERALBNK" to SymbolInfo(1023, "NSE_EQ"),
        "FORCEMOT" to SymbolInfo(11573, "NSE_EQ"),
        "FORTIS" to SymbolInfo(14592, "NSE_EQ"),
        "GAIL" to SymbolInfo(4717, "NSE_EQ"),
        "GLENMARK" to SymbolInfo(7406, "NSE_EQ"),
        "GMRAIRPORT" to SymbolInfo(13528, "NSE_EQ"),
        "GODFRYPHLP" to SymbolInfo(1181, "NSE_EQ"),
        "GODREJCP" to SymbolInfo(10099, "NSE_EQ"),
        "GODREJPROP" to SymbolInfo(17875, "NSE_EQ"),
        "GRASIM" to SymbolInfo(1232, "NSE_EQ"),
        "GVT&D" to SymbolInfo(16783, "NSE_EQ"),
        "HAL" to SymbolInfo(2303, "NSE_EQ"),
        "HAVELLS" to SymbolInfo(9819, "NSE_EQ"),
        "HCLTECH" to SymbolInfo(7229, "NSE_EQ"),
        "HDFCAMC" to SymbolInfo(4244, "NSE_EQ"),
        "HDFCBANK" to SymbolInfo(1333, "NSE_EQ"),
        "HDFCLIFE" to SymbolInfo(467, "NSE_EQ"),
        "HEROMOTOCO" to SymbolInfo(1348, "NSE_EQ"),
        "HINDALCO" to SymbolInfo(1363, "NSE_EQ"),
        "HINDPETRO" to SymbolInfo(1406, "NSE_EQ"),
        "HINDUNILVR" to SymbolInfo(1394, "NSE_EQ"),
        "HINDZINC" to SymbolInfo(1424, "NSE_EQ"),
        "HYUNDAI" to SymbolInfo(25844, "NSE_EQ"),
        "ICICIBANK" to SymbolInfo(4963, "NSE_EQ"),
        "ICICIGI" to SymbolInfo(21770, "NSE_EQ"),
        "ICICIPRULI" to SymbolInfo(18652, "NSE_EQ"),
        "IDEA" to SymbolInfo(14366, "NSE_EQ"),
        "IDFCFIRSTB" to SymbolInfo(11184, "NSE_EQ"),
        "IEX" to SymbolInfo(220, "NSE_EQ"),
        "INDHOTEL" to SymbolInfo(1512, "NSE_EQ"),
        "INDIANB" to SymbolInfo(14309, "NSE_EQ"),
        "INDIGO" to SymbolInfo(11195, "NSE_EQ"),
        "INDUSINDBK" to SymbolInfo(5258, "NSE_EQ"),
        "INDUSTOWER" to SymbolInfo(29135, "NSE_EQ"),
        "INFY" to SymbolInfo(1594, "NSE_EQ"),
        "INOXWIND" to SymbolInfo(7852, "NSE_EQ"),
        "IOC" to SymbolInfo(1624, "NSE_EQ"),
        "IREDA" to SymbolInfo(20261, "NSE_EQ"),
        "IRFC" to SymbolInfo(2029, "NSE_EQ"),
        "ITC" to SymbolInfo(1660, "NSE_EQ"),
        "JINDALSTEL" to SymbolInfo(6733, "NSE_EQ"),
        "JIOFIN" to SymbolInfo(18143, "NSE_EQ"),
        "JSWENERGY" to SymbolInfo(17869, "NSE_EQ"),
        "JSWSTEEL" to SymbolInfo(11723, "NSE_EQ"),
        "JUBLFOOD" to SymbolInfo(18096, "NSE_EQ"),
        "KALYANKJIL" to SymbolInfo(2955, "NSE_EQ"),
        "KAYNES" to SymbolInfo(12092, "NSE_EQ"),
        "KEI" to SymbolInfo(13310, "NSE_EQ"),
        "KFINTECH" to SymbolInfo(13359, "NSE_EQ"),
        "KOTAKBANK" to SymbolInfo(1922, "NSE_EQ"),
        "KPITTECH" to SymbolInfo(9683, "NSE_EQ"),
        "LAURUSLABS" to SymbolInfo(19234, "NSE_EQ"),
        "LICHSGFIN" to SymbolInfo(1997, "NSE_EQ"),
        "LICI" to SymbolInfo(9480, "NSE_EQ"),
        "LODHA" to SymbolInfo(3220, "NSE_EQ"),
        "LT" to SymbolInfo(11483, "NSE_EQ"),
        "LTF" to SymbolInfo(24948, "NSE_EQ"),
        "LTM" to SymbolInfo(17818, "NSE_EQ"),
        "LUPIN" to SymbolInfo(10440, "NSE_EQ"),
        "M&M" to SymbolInfo(2031, "NSE_EQ"),
        "MAHABANK" to SymbolInfo(11377, "NSE_EQ"),
        "MANAPPURAM" to SymbolInfo(19061, "NSE_EQ"),
        "MANKIND" to SymbolInfo(15380, "NSE_EQ"),
        "MARICO" to SymbolInfo(4067, "NSE_EQ"),
        "MARUTI" to SymbolInfo(10999, "NSE_EQ"),
        "MAXHEALTH" to SymbolInfo(22377, "NSE_EQ"),
        "MAZDOCK" to SymbolInfo(509, "NSE_EQ"),
        "MCX" to SymbolInfo(31181, "NSE_EQ"),
        "MFSL" to SymbolInfo(2142, "NSE_EQ"),
        "MOTHERSON" to SymbolInfo(4204, "NSE_EQ"),
        "MOTILALOFS" to SymbolInfo(14947, "NSE_EQ"),
        "MPHASIS" to SymbolInfo(4503, "NSE_EQ"),
        "MUTHOOTFIN" to SymbolInfo(23650, "NSE_EQ"),
        "NAM-INDIA" to SymbolInfo(357, "NSE_EQ"),
        "NATIONALUM" to SymbolInfo(6364, "NSE_EQ"),
        "NAUKRI" to SymbolInfo(13751, "NSE_EQ"),
        "NBCC" to SymbolInfo(31415, "NSE_EQ"),
        "NESTLEIND" to SymbolInfo(17963, "NSE_EQ"),
        "NHPC" to SymbolInfo(17400, "NSE_EQ"),
        "NMDC" to SymbolInfo(15332, "NSE_EQ"),
        "NTPC" to SymbolInfo(11630, "NSE_EQ"),
        "NYKAA" to SymbolInfo(6545, "NSE_EQ"),
        "OBEROIRLTY" to SymbolInfo(20242, "NSE_EQ"),
        "OFSS" to SymbolInfo(10738, "NSE_EQ"),
        "OIL" to SymbolInfo(17438, "NSE_EQ"),
        "ONGC" to SymbolInfo(2475, "NSE_EQ"),
        "PAGEIND" to SymbolInfo(14413, "NSE_EQ"),
        "PATANJALI" to SymbolInfo(17029, "NSE_EQ"),
        "PAYTM" to SymbolInfo(6705, "NSE_EQ"),
        "PERSISTENT" to SymbolInfo(18365, "NSE_EQ"),
        "PETRONET" to SymbolInfo(11351, "NSE_EQ"),
        "PFC" to SymbolInfo(14299, "NSE_EQ"),
        "PGEL" to SymbolInfo(25358, "NSE_EQ"),
        "PHOENIXLTD" to SymbolInfo(14552, "NSE_EQ"),
        "PIDILITIND" to SymbolInfo(2664, "NSE_EQ"),
        "PIIND" to SymbolInfo(24184, "NSE_EQ"),
        "PNB" to SymbolInfo(10666, "NSE_EQ"),
        "PNBHOUSING" to SymbolInfo(18908, "NSE_EQ"),
        "POLICYBZR" to SymbolInfo(6656, "NSE_EQ"),
        "POLYCAB" to SymbolInfo(9590, "NSE_EQ"),
        "POWERGRID" to SymbolInfo(14977, "NSE_EQ"),
        "POWERINDIA" to SymbolInfo(18457, "NSE_EQ"),
        "PREMIERENE" to SymbolInfo(25049, "NSE_EQ"),
        "PRESTIGE" to SymbolInfo(20302, "NSE_EQ"),
        "RADICO" to SymbolInfo(10990, "NSE_EQ"),
        "RBLBANK" to SymbolInfo(18391, "NSE_EQ"),
        "RECLTD" to SymbolInfo(15355, "NSE_EQ"),
        "RELIANCE" to SymbolInfo(2885, "NSE_EQ"),
        "RVNL" to SymbolInfo(9552, "NSE_EQ"),
        "SAGILITY" to SymbolInfo(27052, "NSE_EQ"),
        "SAIL" to SymbolInfo(2963, "NSE_EQ"),
        "SBICARD" to SymbolInfo(17971, "NSE_EQ"),
        "SBILIFE" to SymbolInfo(21808, "NSE_EQ"),
        "SBIN" to SymbolInfo(3045, "NSE_EQ"),
        "SHREECEM" to SymbolInfo(3103, "NSE_EQ"),
        "SHRIRAMFIN" to SymbolInfo(4306, "NSE_EQ"),
        "SIEMENS" to SymbolInfo(3150, "NSE_EQ"),
        "SOLARINDS" to SymbolInfo(13332, "NSE_EQ"),
        "SONACOMS" to SymbolInfo(4684, "NSE_EQ"),
        "SRF" to SymbolInfo(3273, "NSE_EQ"),
        "SUNPHARMA" to SymbolInfo(3351, "NSE_EQ"),
        "SUPREMEIND" to SymbolInfo(3363, "NSE_EQ"),
        "SUZLON" to SymbolInfo(12018, "NSE_EQ"),
        "SWIGGY" to SymbolInfo(27066, "NSE_EQ"),
        "TATACONSUM" to SymbolInfo(3432, "NSE_EQ"),
        "TATAELXSI" to SymbolInfo(3411, "NSE_EQ"),
        "TATAPOWER" to SymbolInfo(3426, "NSE_EQ"),
        "TATASTEEL" to SymbolInfo(3499, "NSE_EQ"),
        "TCS" to SymbolInfo(11536, "NSE_EQ"),
        "TECHM" to SymbolInfo(13538, "NSE_EQ"),
        "TIINDIA" to SymbolInfo(312, "NSE_EQ"),
        "TITAN" to SymbolInfo(3506, "NSE_EQ"),
        "TMPV" to SymbolInfo(3456, "NSE_EQ"),
        "TORNTPHARM" to SymbolInfo(3518, "NSE_EQ"),
        "TRENT" to SymbolInfo(1964, "NSE_EQ"),
        "TVSMOTOR" to SymbolInfo(8479, "NSE_EQ"),
        "UJJIVANSFB" to SymbolInfo(15228, "NSE_EQ"),
        "ULTRACEMCO" to SymbolInfo(11532, "NSE_EQ"),
        "UNIONBANK" to SymbolInfo(10753, "NSE_EQ"),
        "UNITDSPR" to SymbolInfo(10447, "NSE_EQ"),
        "UNOMINDA" to SymbolInfo(14154, "NSE_EQ"),
        "UPL" to SymbolInfo(11287, "NSE_EQ"),
        "VBL" to SymbolInfo(18921, "NSE_EQ"),
        "VEDL" to SymbolInfo(3063, "NSE_EQ"),
        "VMM" to SymbolInfo(27969, "NSE_EQ"),
        "VOLTAS" to SymbolInfo(3718, "NSE_EQ"),
        "WAAREEENER" to SymbolInfo(25907, "NSE_EQ"),
        "WIPRO" to SymbolInfo(3787, "NSE_EQ"),
        "YESBANK" to SymbolInfo(11915, "NSE_EQ"),
        "ZYDUSLIFE" to SymbolInfo(7929, "NSE_EQ"),
    )

    val INDEX_SYMBOLS = listOf("NIFTY", "BANKNIFTY", "FINNIFTY", "MIDCPNIFTY", "NIFTYNXT50", "SENSEX", "BANKEX")
    val MCX_SYMBOLS = listOf("CRUDEOIL", "CRUDEOILM", "NATURALGAS", "NATGASMINI", "GOLD", "GOLDM", "SILVER", "SILVERM", "COPPER", "ZINC")
    val STOCK_SYMBOLS = listOf("360ONE", "ABB", "ABCAPITAL", "ADANIENSOL", "ADANIENT", "ADANIGREEN", "ADANIPORTS", "ADANIPOWER", "ALKEM", "AMBER", "AMBUJACEM", "ANANDRATHI", "ANGELONE", "APLAPOLLO", "APOLLOHOSP", "ASHOKLEY", "ASIANPAINT", "ASTRAL", "ATHERENERG", "AUBANK", "AUROPHARMA", "AXISBANK", "BAJAJ-AUTO", "BAJAJFINSV", "BAJAJHLDNG", "BAJFINANCE", "BANDHANBNK", "BANKBARODA", "BANKINDIA", "BDL", "BEL", "BHARATFORG", "BHARTIARTL", "BHEL", "BIOCON", "BLUESTARCO", "BOSCHLTD", "BPCL", "BRITANNIA", "BSE", "CAMS", "CANBK", "CDSL", "CGPOWER", "CHOLAFIN", "CIPLA", "COALINDIA", "COCHINSHIP", "COFORGE", "COLPAL", "CONCOR", "CROMPTON", "CUMMINSIND", "DABUR", "DELHIVERY", "DIVISLAB", "DIXON", "DLF", "DMART", "DRREDDY", "EICHERMOT", "ENRIN", "ETERNAL", "FEDERALBNK", "FORCEMOT", "FORTIS", "GAIL", "GLENMARK", "GMRAIRPORT", "GODFRYPHLP", "GODREJCP", "GODREJPROP", "GRASIM", "GVT&D", "HAL", "HAVELLS", "HCLTECH", "HDFCAMC", "HDFCBANK", "HDFCLIFE", "HEROMOTOCO", "HINDALCO", "HINDPETRO", "HINDUNILVR", "HINDZINC", "HYUNDAI", "ICICIBANK", "ICICIGI", "ICICIPRULI", "IDEA", "IDFCFIRSTB", "IEX", "INDHOTEL", "INDIANB", "INDIGO", "INDUSINDBK", "INDUSTOWER", "INFY", "INOXWIND", "IOC", "IREDA", "IRFC", "ITC", "JINDALSTEL", "JIOFIN", "JSWENERGY", "JSWSTEEL", "JUBLFOOD", "KALYANKJIL", "KAYNES", "KEI", "KFINTECH", "KOTAKBANK", "KPITTECH", "LAURUSLABS", "LICHSGFIN", "LICI", "LODHA", "LT", "LTF", "LTM", "LUPIN", "M&M", "MAHABANK", "MANAPPURAM", "MANKIND", "MARICO", "MARUTI", "MAXHEALTH", "MAZDOCK", "MCX", "MFSL", "MOTHERSON", "MOTILALOFS", "MPHASIS", "MUTHOOTFIN", "NAM-INDIA", "NATIONALUM", "NAUKRI", "NBCC", "NESTLEIND", "NHPC", "NMDC", "NTPC", "NYKAA", "OBEROIRLTY", "OFSS", "OIL", "ONGC", "PAGEIND", "PATANJALI", "PAYTM", "PERSISTENT", "PETRONET", "PFC", "PGEL", "PHOENIXLTD", "PIDILITIND", "PIIND", "PNB", "PNBHOUSING", "POLICYBZR", "POLYCAB", "POWERGRID", "POWERINDIA", "PREMIERENE", "PRESTIGE", "RADICO", "RBLBANK", "RECLTD", "RELIANCE", "RVNL", "SAGILITY", "SAIL", "SBICARD", "SBILIFE", "SBIN", "SHREECEM", "SHRIRAMFIN", "SIEMENS", "SOLARINDS", "SONACOMS", "SRF", "SUNPHARMA", "SUPREMEIND", "SUZLON", "SWIGGY", "TATACONSUM", "TATAELXSI", "TATAPOWER", "TATASTEEL", "TCS", "TECHM", "TIINDIA", "TITAN", "TMPV", "TORNTPHARM", "TRENT", "TVSMOTOR", "UJJIVANSFB", "ULTRACEMCO", "UNIONBANK", "UNITDSPR", "UNOMINDA", "UPL", "VBL", "VEDL", "VMM", "VOLTAS", "WAAREEENER", "WIPRO", "YESBANK", "ZYDUSLIFE")
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

    var minLtp: Float
        get() = prefs.getFloat("min_ltp", 5f)
        set(v) = prefs.edit().putFloat("min_ltp", v).apply()

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

    /** Extra symbols: stocks / MCX / more indices (comma or space separated). */
    var extraSymbols: String
        get() = prefs.getString("extra_symbols", "") ?: ""
        set(v) = prefs.edit().putString("extra_symbols", v).apply()

    fun allEnabledSymbols(): Set<String> {
        val out = symbolsEnabled.toMutableSet()
        for (p in extraSymbols.split(',', ' ', '\n', ';', '\t')) {
            val s = p.trim().uppercase()
            if (s.isNotEmpty() && Defaults.SYMBOLS.containsKey(s)) out.add(s)
        }
        return out
    }

    fun getCustomExpiries(symbol: String): List<String> {
        val raw = prefs.getString("custom_expiries_$symbol", "") ?: ""
        return raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    fun setCustomExpiries(symbol: String, expiries: List<String>) {
        prefs.edit().putString("custom_expiries_$symbol", expiries.joinToString(",")).apply()
    }

    fun clearCustomExpiries(symbol: String) {
        prefs.edit().remove("custom_expiries_$symbol").apply()
    }
}
