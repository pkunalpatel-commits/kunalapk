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
)

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
    ): List<HistoricalSpike> {
        val tsArr = candles.optJSONArray("timestamp") ?: return emptyList()
        val closeArr = candles.optJSONArray("close") ?: return emptyList()
        val oiArr = candles.optJSONArray("open_interest") ?: return emptyList()
        val n = minOf(tsArr.length(), closeArr.length(), oiArr.length())
        if (n == 0) return emptyList()

        val step1 = max(1, (1.0 / intervalMin).roundToInt())
        val step5 = max(1, (5.0 / intervalMin).roundToInt())
        val step10 = max(1, (10.0 / intervalMin).roundToInt())
        val cooldown = max(1, (10.0 / intervalMin).roundToInt())

        val lastIdx = mutableMapOf("1m" to -10000, "5m" to -10000, "10m" to -10000)
        val alerts = mutableListOf<HistoricalSpike>()

        for (i in 0 until n) {
            val oi = oiArr.optDouble(i, 0.0)
            val ltp = closeArr.optDouble(i, 0.0)
            if (oi < minOi) continue

            for ((label, step, threshold) in listOf(
                Triple("1m", step1, oi1mPct.toDouble()),
                Triple("5m", step5, oi5mPct.toDouble()),
                Triple("10m", step10, oi10mPct.toDouble()),
            )) {
                val pastIdx = i - step
                if (pastIdx < 0) continue
                val pastOi = oiArr.optDouble(pastIdx, 0.0)
                val pastLtp = closeArr.optDouble(pastIdx, 0.0)
                if (pastOi <= 0) continue

                val oiChange = (oi - pastOi) / pastOi * 100.0
                val priceChange = if (pastLtp != 0.0) (ltp - pastLtp) / pastLtp * 100.0 else 0.0
                if (oiChange >= threshold && abs(priceChange) >= pricePct) {
                    val last = lastIdx[label] ?: -10000
                    if (i - last < cooldown) continue
                    lastIdx[label] = i
                    val ts = tsArr.optLong(i)
                    alerts.add(
                        HistoricalSpike(
                            symbol = symbol,
                            expiry = expiry,
                            strike = strike,
                            type = optType,
                            window = label,
                            oi = oi,
                            oiChangePct = Math.round(oiChange * 100.0) / 100.0,
                            ltp = ltp,
                            priceChangePct = Math.round(priceChange * 100.0) / 100.0,
                            ts = ts,
                            datetime = fmt.format(Date(ts * 1000)),
                        )
                    )
                }
            }
        }
        return alerts
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
