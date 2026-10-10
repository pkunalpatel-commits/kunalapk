package com.oisspike.detector.engine

import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

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
 * Historical **candle** scan (matches desktop + live thresholds).
 *
 * For a chosen candle size (e.g. 1m), each window looks back an exact number
 * of bars:
 *   1m window on 1m candles → previous bar
 *   5m window on 1m candles → 5 bars back
 *   10m window on 1m candles → 10 bars back
 *
 * Thresholds / which windows come from [buildLiveWindows] (same as live Settings).
 */
object HistoricalEngine {

    private val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

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
        val oiArr = resolveOiArray(candles) ?: return emptyList()
        val n = minOf(tsArr.length(), closeArr.length(), oiArr.length())
        if (n < 2) return emptyList()

        val candle = intervalMin.coerceAtLeast(1)
        val windows = buildLiveWindows(
            candleIntervalMin = candle,
            oi1 = oi1mPct,
            oi5 = oi5mPct,
            oi10 = oi10mPct,
            includeStandard = includeStandardWindows,
        )
        // Cooldown in number of candles (~ cooldownMin minutes)
        val cooldownBars = max(1, (cooldownMin.coerceAtLeast(1).toDouble() / candle).roundToInt())
        val priceTh = pricePct.toDouble()
        val minOiD = minOi.toDouble()

        val lastAlertBar = mutableMapOf<String, Int>()
        val alerts = mutableListOf<HistoricalSpike>()

        for (i in 0 until n) {
            val oi = oiArr.optDouble(i, 0.0)
            val ltp = closeArr.optDouble(i, 0.0)
            val tsSec = toUnixSec(tsArr.optLong(i))
            if (oi < minOiD) continue
            if (minLtp > 0f && ltp < minLtp) continue

            for (w in windows) {
                // Exact bar steps: window minutes / candle minutes
                val windowMin = max(1, (w.windowSec / 60L).toInt())
                val step = max(1, (windowMin.toDouble() / candle).roundToInt())
                val pastIdx = i - step
                if (pastIdx < 0) continue

                val pastOi = oiArr.optDouble(pastIdx, 0.0)
                val pastLtp = closeArr.optDouble(pastIdx, 0.0)
                if (pastOi <= 0) continue

                val oiChange = (oi - pastOi) / pastOi * 100.0
                val priceChange = if (pastLtp != 0.0) (ltp - pastLtp) / pastLtp * 100.0 else 0.0
                if (oiChange < w.oiThreshold || abs(priceChange) < priceTh) continue

                val last = lastAlertBar[w.label] ?: -1_000_000
                if (i - last < cooldownBars) continue
                lastAlertBar[w.label] = i

                alerts.add(
                    HistoricalSpike(
                        symbol = symbol,
                        expiry = expiry,
                        strike = strike,
                        type = optType,
                        window = w.label,
                        oi = oi,
                        oiChangePct = Math.round(oiChange * 100.0) / 100.0,
                        ltp = ltp,
                        priceChangePct = Math.round(priceChange * 100.0) / 100.0,
                        ts = tsSec,
                        datetime = fmt.format(Date(tsSec * 1000)),
                        atmDistance = atmDistance,
                        spot = spot,
                    )
                )
            }
        }
        return alerts
    }

    /** Dhan may return open_interest or openInterest. */
    private fun resolveOiArray(candles: JSONObject): JSONArray? {
        candles.optJSONArray("open_interest")?.let { return it }
        candles.optJSONArray("openInterest")?.let { return it }
        candles.optJSONArray("oi")?.let { return it }
        return null
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
        val oi = resolveOiArray(candles) ?: return candles
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
