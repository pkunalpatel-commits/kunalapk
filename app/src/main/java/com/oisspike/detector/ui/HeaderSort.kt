package com.oisspike.detector.ui

import com.oisspike.detector.R
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/** Soft column-header buttons: tap toggles asc ↔ desc and updates label arrow. */
object HeaderSort {
    private data class Col(
        val viewId: Int,
        val label: String,
        val asc: SpikeSort,
        val desc: SpikeSort,
    )

    private val cols = listOf(
        Col(R.id.hdrTime, "Time", SpikeSort.TIME_ASC, SpikeSort.TIME_DESC),
        Col(R.id.hdrExpiry, "Exp", SpikeSort.EXPIRY_ASC, SpikeSort.EXPIRY_DESC),
        Col(R.id.hdrStrike, "Strike", SpikeSort.STRIKE_ASC, SpikeSort.STRIKE_DESC),
        Col(R.id.hdrType, "CE", SpikeSort.TYPE_ASC, SpikeSort.TYPE_DESC),
        Col(R.id.hdrWin, "Win", SpikeSort.WIN_ASC, SpikeSort.WIN_DESC),
        Col(R.id.hdrOi, "OI", SpikeSort.OI_SIZE_ASC, SpikeSort.OI_SIZE_DESC),
        Col(R.id.hdrOiPct, "OI%", SpikeSort.OI_PCT_ASC, SpikeSort.OI_PCT_DESC),
        Col(R.id.hdrLtp, "LTP", SpikeSort.LTP_ASC, SpikeSort.LTP_DESC),
    )

    fun wire(activity: AppCompatActivity, root: View, adapter: SpikeAdapter) {
        fun refreshLabels() {
            val cur = adapter.currentSort()
            for (c in cols) {
                val tv = root.findViewById<TextView>(c.viewId) ?: continue
                val arrow = when (cur) {
                    c.asc -> " ↑"
                    c.desc -> " ↓"
                    else -> " ↕"
                }
                tv.text = c.label + arrow
            }
        }
        for (c in cols) {
            val tv = root.findViewById<TextView>(c.viewId) ?: continue
            tv.isClickable = true
            tv.isFocusable = true
            tv.setOnClickListener {
                val cur = adapter.currentSort()
                val next = when (cur) {
                    c.desc -> c.asc
                    c.asc -> c.desc
                    else -> c.desc // default first tap = highest first
                }
                adapter.setSort(next)
                refreshLabels()
            }
        }
        refreshLabels()
    }
}
