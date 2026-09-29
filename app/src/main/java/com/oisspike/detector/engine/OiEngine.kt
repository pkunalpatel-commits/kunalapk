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
)

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
        if (seen.add(m)) { /* ok */ }
    }
    add(primary)
    if (includeStandard) {
        listOf(1, 5, 10).forEach { add(it) }
    }
    return seen.sorted().map { m ->
        WindowSpec("${m}m", m * 60L, thresholdForInterval(m, oi1, oi5, oi10))
    }
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
    ): List<SpikeAlert> {
        val k = key(symbol, expiry, strike, type)
        val hist = history.getOrPut(k) { mutableListOf() }
        synchronized(hist) {
            hist.add(Snapshot(ts, oi, ltp))
            val cutoff = ts - historyWindowSec
            hist.removeAll { it.ts < cutoff }
        }

        if (oi < minOi) return emptyList()

        val alerts = mutableListOf<SpikeAlert>()
        val snapList = synchronized(hist) { hist.toList() }

        for (w in windows) {
            val past = findPast(snapList, ts - w.windowSec) ?: continue
            if (past.oi <= 0) continue
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
                        )
                    )
                }
            }
        }
        return alerts
    }

    private fun findPast(hist: List<Snapshot>, targetTs: Long): Snapshot? {
        if (hist.isEmpty() || hist.first().ts > targetTs) return null
        var lo = 0
        var hi = hist.size - 1
        var best = -1
        while (lo <= hi) {
            val mid = (lo + hi) / 2
            if (hist[mid].ts <= targetTs) {
                best = mid
                lo = mid + 1
            } else {
                hi = mid - 1
            }
        }
        return if (best >= 0) hist[best] else null
    }
}
