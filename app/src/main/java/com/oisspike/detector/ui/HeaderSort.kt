package com.oisspike.detector.ui

import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.oisspike.detector.R

object HeaderSort {
    private data class Col(val id: Int, val label: String, val asc: SpikeSort, val desc: SpikeSort)

    private val cols = listOf(
        Col(R.id.hdrTime, "Time", SpikeSort.TIME_ASC, SpikeSort.TIME_DESC),
        Col(R.id.hdrExpiry, "Exp", SpikeSort.EXPIRY_ASC, SpikeSort.EXPIRY_DESC),
        Col(R.id.hdrStrike, "Strike", SpikeSort.STRIKE_ASC, SpikeSort.STRIKE_DESC),
        Col(R.id.hdrType, "CE", SpikeSort.TYPE_ASC, SpikeSort.TYPE_DESC),
        Col(R.id.hdrWin, "Win", SpikeSort.WIN_ASC, SpikeSort.WIN_DESC),        Col(R.id.hdrOi, "OI", SpikeSort.OI_SIZE_ASC, SpikeSort.OI_SIZE_DESC),
        Col(R.id.hdrOiPct, "OI%", SpikeSort.OI_PCT_ASC, SpikeSort.OI_PCT_DESC),
        Col(R.id.hdrLtp, "LTP", SpikeSort.LTP_ASC, SpikeSort.LTP_DESC),
    )

    fun wire(activity: AppCompatActivity, root: View?, adapter: SpikeAdapter) {
        if (root == null) return
        fun refresh() {
            val cur = adapter.currentSort()
            for (c in cols) {
                val tv = root.findViewById<TextView>(c.id) ?: continue
                tv.text = c.label + when (cur) {
                    c.asc -> "↑"
                    c.desc -> "↓"
                    else -> ""
                }
            }
        }
        for (c in cols) {
            val tv = root.findViewById<TextView>(c.id) ?: continue
            tv.setOnClickListener {
                val cur = adapter.currentSort()
                adapter.setSort(when (cur) {
                    c.desc -> c.asc
                    c.asc -> c.desc
                    else -> c.desc
                })
                refresh()
            }
        }
        refresh()
    }

    fun refresh(root: View?) { /* no-op for simple mode */ }
}
