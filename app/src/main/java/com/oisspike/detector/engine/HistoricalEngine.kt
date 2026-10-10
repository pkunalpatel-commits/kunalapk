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
 * Historical scan uses the **same rules as live** [OiEngine]:
 * - Windows from [buildLiveWindows] (primary candle + optional 1m/5m/10m)
 * - Thresholds via [thresholdForInterval]
 * - OI % and |price %| filters
 * - Previous-bar / lookback matching (0.7–1.6× window, min age 0.5× window)
 * - Cooldown between alerts per contract+window
 */
object HistoricalEngine {

    private val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

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
    ): List<HistoricalSpike> {
        val tsArr = candles.optJSONArray("timestamp") ?: return emptyList()
        val closeArr = candles.optJSONArray("close") ?: return emptyList()
        val oiArr = candles.optJSONArray("open_interest") ?: return emptyList()
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
        val cooldownSec = (cooldownMin.coerceAtLeast(1) * 60).toLong()
        val priceTh = pricePct.toDouble()
        val minOiD = minOi.toDouble()

        // last alert time (unix sec) per window label
        val lastAlert = mutableMapOf<String, Long>()
        val alerts = mutableListOf<HistoricalSpike>()

        for (i in 1 until n) {
            val oi = oiArr.optDouble(i, 0.0)
            val ltp = closeArr.optDouble(i, 0.0)
            val ts = tsArr.optLong(i)
            if (oi < minOiD) continue

            for (w in windows) {
                val pastIdx = findPastBarIndex(tsArr, i, ts, w.windowSec) ?: continue
                val pastOi = oiArr.optDouble(pastIdx, 0.0)
                val pastLtp = closeArr.optDouble(pastIdx, 0.0)
                if (pastOi <= 0) continue

                val age = ts - tsArr.optLong(pastIdx)
                if (age < (w.windowSec * 0.5).toLong()) continue

                val oiChange = (oi - pastOi) / pastOi * 100.0
                val priceChange = if (pastLtp != 0.0) (ltp - pastLtp) / pastLtp * 100.0 else 0.0
                if (oiChange < w.oiThreshold || abs(priceChange) < priceTh) continue

                val last = lastAlert[w.label] ?: 0L
                if (ts - last < cooldownSec) continue
                lastAlert[w.label] = ts

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
                        ts = ts,
                        datetime = fmt.format(Date(ts * 1000)),
                        atmDistance = atmDistance,
                        spot = spot,
                    )
                )
            }
        }
        return alerts
    }

    /**
     * Same idea as [OiEngine] findPastForWindow on a candle series:
     * prefer previous bar if age ≈ window; else bar at/before (now - windowSec).
     */
    private fun findPastBarIndex(
        tsArr: JSONArray,
        currentIdx: Int,
        now: Long,
        windowSec: Long,
    ): Int? {
        if (currentIdx < 1) return null
        val prevIdx = currentIdx - 1
        val prevTs = tsArr.optLong(prevIdx)
        val age = now - prevTs
        if (age >= (windowSec * 0.7).toLong() && age <= (windowSec * 1.6).toLong()) {
            return prevIdx
        }
        val target = now - windowSec
        // binary search for last index with ts <= target among 0..currentIdx-1
        var lo = 0
        var hi = currentIdx - 1
        var best = -1
        while (lo <= hi) {
            val mid = (lo + hi) / 2
            if (tsArr.optLong(mid) <= target) {
                best = mid
                lo = mid + 1
            } else {
                hi = mid - 1
            }
        }
        if (best >= 0) return best
        // warm-up: allow previous bar if at least 70% of window old
        return if (age >= (windowSec * 0.7).toLong()) prevIdx else null
    }

    /** Native Dhan interval for a UI candle size; factor for resampling if needed. */
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

    /**
     * Simple resample: take every `factor` bars (last OI/close in bucket).
     * Good enough for 3m from 1m and 30m from 15m.
     */
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
