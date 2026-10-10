package com.oisspike.detector.engine

import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class HistoricalSpike(
    val symbol: String,
    val expiry: String,
    val strike: Double,
    val type: String,
    val window: String,
    val oi: Double,
    val oiChangePct: Double,
    val ltp: Double,
    val priceChangePct: Double,
    val ts: Long,
    val datetime: String,
    val atmDistance: Int? = null,
    val spot: Double? = null,
)

/**
 * Historical spikes by **replaying candles through the same [OiEngine] as live**.
 * Same windows, thresholds, lookback, price filter, min OI, and cooldown.
 */
object HistoricalEngine {

    private val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    /** Normalize Dhan chart timestamps to unix seconds. */
    fun toUnixSec(ts: Long): Long =
        if (ts > 10_000_000_000L) ts / 1000L else ts

    fun findHistoricalSpikes(
        candles: JSONObject,
        symbol: String,
        expiry: String,
        strike: Double,
        optType: String,
        intervalMin: Int,
        oi1mPct: Float,
        oi5mPct: Float,
        oi10mPct: Float,
        pricePct: Float,
        minOi: Float,
        atmDistance: Int? = null,
        spot: Double? = null,
        includeStandardWindows: Boolean = true,
        cooldownMin: Int = 10,
        minLtp: Float = 0f,
    ): List<HistoricalSpike> {
        val tsArr = candles.optJSONArray("timestamp") ?: return emptyList()
        val closeArr = candles.optJSONArray("close") ?: return emptyList()
        val oiArr = candles.optJSONArray("open_interest") ?: return emptyList()
        val n = minOf(tsArr.length(), closeArr.length(), oiArr.length())
        if (n < 2) return emptyList()

        val windows = buildLiveWindows(
            candleIntervalMin = intervalMin.coerceAtLeast(1),
            oi1 = oi1mPct,
            oi5 = oi5mPct,
            oi10 = oi10mPct,
            includeStandard = includeStandardWindows,
        )
        val engine = OiEngine(
            windows = windows,
            pricePct = pricePct.toDouble(),
            minOi = minOi.toDouble(),
            cooldownSec = cooldownMin.coerceAtLeast(1) * 60L,
        )

        val out = mutableListOf<HistoricalSpike>()
        for (i in 0 until n) {
            val oi = oiArr.optDouble(i, 0.0)
            val ltp = closeArr.optDouble(i, 0.0)
            val tsSec = toUnixSec(tsArr.optLong(i))
            if (oi <= 0) continue
            if (minLtp > 0f && ltp < minLtp) continue

            val alerts = engine.ingest(
                symbol = symbol,
                expiry = expiry,
                strike = strike,
                type = optType,
                oi = oi,
                ltp = ltp,
                ts = tsSec,
                atmDistance = atmDistance,
                spot = spot,
            )
            for (a in alerts) {
                out.add(
                    HistoricalSpike(
                        symbol = a.symbol,
                        expiry = a.expiry,
                        strike = a.strike,
                        type = a.type,
                        window = a.window,
                        oi = a.oi,
                        oiChangePct = a.oiChangePct,
                        ltp = a.ltp,
                        priceChangePct = a.priceChangePct,
                        ts = a.ts,
                        datetime = fmt.format(Date(a.ts * 1000)),
                        atmDistance = a.atmDistance,
                        spot = a.spot,
                    )
                )
            }
        }
        return out
    }

    fun nativeInterval(uiMinutes: Int): Pair<String, Int> {
        return when (uiMinutes) {
            1 -> "1" to 1
            3 -> "1" to 3
            5 -> "5" to 1
            15 -> "15" to 1
            30 -> "15" to 2
            else -> "5" to 1
        }
    }

    fun resample(candles: JSONObject, factor: Int): JSONObject {
        if (factor <= 1) return candles
        val ts = candles.optJSONArray("timestamp") ?: return candles
        val close = candles.optJSONArray("close") ?: return candles
        val oi = candles.optJSONArray("open_interest") ?: return candles
        val n = minOf(ts.length(), close.length(), oi.length())
        val outTs = JSONArray()
        val outClose = JSONArray()
        val outOi = JSONArray()
        var i = 0
        while (i < n) {
            val end = minOf(i + factor - 1, n - 1)
            outTs.put(ts.getLong(end))
            outClose.put(close.getDouble(end))
            outOi.put(oi.getDouble(end))
            i += factor
        }
        return JSONObject()
            .put("timestamp", outTs)
            .put("close", outClose)
            .put("open_interest", outOi)
    }
}
