package com.oisspike.detector.engine

import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

data class Snapshot(val ts: Long, val oi: Double, val ltp: Double)

data class SpikeAlert(
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
    val atmDistance: Int? = null,
    val spot: Double? = null,
)

fun atmDistanceOf(strike: Double, spot: Double, strikes: List<Double>): Int? {
    if (strikes.isEmpty() || spot.isNaN()) return null
    val sorted = strikes.sorted()
    val atm = sorted.minByOrNull { abs(it - spot) } ?: return null
    val atmIdx = sorted.indexOf(atm)
    val sIdx = sorted.indexOfFirst { abs(it - strike) < 0.01 }
    if (sIdx >= 0) return sIdx - atmIdx
    val step = if (sorted.size >= 2) {
        sorted.zipWithNext { a, b -> b - a }.filter { it > 0 }.minOrNull() ?: return null
    } else return null
    return kotlin.math.round((strike - atm) / step).toInt()
}

data class WindowSpec(val label: String, val windowSec: Long, val oiThreshold: Double)

fun thresholdForInterval(intervalMin: Int, oi1: Float, oi5: Float, oi10: Float): Double {
    return when {
        intervalMin <= 1 -> oi1.toDouble()
        intervalMin <= 5 -> oi5.toDouble()
        else -> oi10.toDouble()
    }
}

fun buildLiveWindows(
    candleIntervalMin: Int,
    oi1: Float,
    oi5: Float,
    oi10: Float,
    includeStandard: Boolean,
): List<WindowSpec> {
    val primary = candleIntervalMin.coerceAtLeast(1)
    val seen = linkedSetOf<Int>()
    fun add(m: Int) {
        seen.add(m)
    }
    add(primary)
    if (includeStandard) {
        listOf(1, 5, 10).forEach { add(it) }
    }
    return seen.sorted().map { m ->
        WindowSpec("${m}m", m * 60L, thresholdForInterval(m, oi1, oi5, oi10))
    }
}

/**
 * Suggested poll interval (seconds) so live sampling is dense enough for the
 * selected candle window — same idea as historical 1m bars.
 */
fun suggestedPollSec(candleIntervalMin: Int, userPollSec: Int): Int {
    val candle = candleIntervalMin.coerceAtLeast(1)
    // Live only: denser polls for short candles (do not change for historical)
    val ideal = when {
        candle <= 1 -> 20
        candle <= 3 -> 30
        candle <= 5 -> 45
        else -> 60
    }
    return minOf(userPollSec.coerceAtLeast(5), ideal).coerceAtLeast(5)
}

class OiEngine(
    private var windows: List<WindowSpec>,
    private var pricePct: Double,
    private var minOi: Double,
    private var cooldownSec: Long,
) {
    private val history = ConcurrentHashMap<String, MutableList<Snapshot>>()
    private val lastAlert = ConcurrentHashMap<String, MutableMap<String, Long>>()
    private val historyWindowSec: Long
        get() {
            val longest = windows.maxOfOrNull { it.windowSec } ?: 600L
            return maxOf(45 * 60L, longest + 5 * 60L)
        }

    fun updateConfig(
        windows: List<WindowSpec>,
        pricePct: Double,
        minOi: Double,
        cooldownSec: Long,
    ) {
        this.windows = windows
        this.pricePct = pricePct
        this.minOi = minOi
        this.cooldownSec = cooldownSec
    }

    private fun key(symbol: String, expiry: String, strike: Double, type: String) =
        "$symbol|$expiry|$strike|$type"

    fun ingest(
        symbol: String,
        expiry: String,
        strike: Double,
        type: String,
        oi: Double,
        ltp: Double,
        ts: Long = System.currentTimeMillis() / 1000,
        atmDistance: Int? = null,
        spot: Double? = null,
    ): List<SpikeAlert> {
        val k = key(symbol, expiry, strike, type)
        val hist = history.getOrPut(k) { mutableListOf() }
        synchronized(hist) {
            // Avoid duplicate same-second samples (multiple legs same sweep already unique by key)
            val last = hist.lastOrNull()
            if (last == null || last.ts != ts || last.oi != oi) {
                hist.add(Snapshot(ts, oi, ltp))
            }
            val cutoff = ts - historyWindowSec
            hist.removeAll { it.ts < cutoff }
        }

        if (oi < minOi) return emptyList()

        val alerts = mutableListOf<SpikeAlert>()
        val snapList = synchronized(hist) { hist.toList() }
        if (snapList.size < 2) return emptyList()

        for (w in windows) {
            val past = findPastForWindow(snapList, ts, w.windowSec) ?: continue
            if (past.oi <= 0) continue
            // Ignore near-identical sample (too close in time)
            if (ts - past.ts < (w.windowSec * 0.5).toLong()) continue

            val oiChange = (oi - past.oi) / past.oi * 100.0
            val priceChange = if (past.ltp != 0.0) (ltp - past.ltp) / past.ltp * 100.0 else 0.0
            if (oiChange >= w.oiThreshold && abs(priceChange) >= pricePct) {
                val map = lastAlert.getOrPut(k) { mutableMapOf() }
                val last = map[w.label] ?: 0L
                if (ts - last >= cooldownSec) {
                    map[w.label] = ts
                    alerts.add(
                        SpikeAlert(
                            symbol, expiry, strike, type, w.label,
                            oi, Math.round(oiChange * 100.0) / 100.0,
                            ltp, Math.round(priceChange * 100.0) / 100.0,
                            ts,
                            atmDistance,
                            spot,
                        )
                    )
                }
            }
        }
        return alerts
    }

    /**
     * Same rules as v1.7.3 / commit 7de1564:
     * - Prefer sample at or before (now - windowSec)  [classic lookback]
     * - Else previous sample if age ≈ 0.7–1.6× window  [sparse / warm-up]
     * Dense live polls (e.g. every 20s for 1m) must use classic lookback first;
     * checking "prev sample age ≈ window" first prevented alerts when poll << window.
     */
    private fun findPastForWindow(hist: List<Snapshot>, now: Long, windowSec: Long): Snapshot? {
        if (hist.size < 2) return null
        val target = now - windowSec
        val minAge = (windowSec * 0.5).toLong()

        // 1) Classic lookback — required for dense live sampling
        if (hist.first().ts <= target) {
            var lo = 0
            var hi = hist.size - 1
            var best = -1
            while (lo <= hi) {
                val mid = (lo + hi) / 2
                if (hist[mid].ts <= target) {
                    best = mid
                    lo = mid + 1
                } else {
                    hi = mid - 1
                }
            }
            if (best >= 0) {
                val s = hist[best]
                if (now - s.ts >= minAge) return s
            }
        }

        // 2) Previous sample as candle proxy (sparse polls / warm-up) — 7de1564
        val prev = hist[hist.size - 2]
        val age = now - prev.ts
        if (age >= (windowSec * 0.7).toLong() && age <= (windowSec * 1.6).toLong()) {
            return prev
        }
        if (hist.first().ts > target && age >= (windowSec * 0.7).toLong()) {
            return prev
        }
        return null
    }
}

