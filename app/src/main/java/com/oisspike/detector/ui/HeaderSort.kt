package com.oisspike.detector.ui

import com.oisspike.detector.R
import android.util.TypedValue
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

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

    fun wire(activity: AppCompatActivity, root: View?, adapter: SpikeAdapter) {
        if (root == null) return
        fun refreshLabels() {
            val cur = adapter.currentSort()
            val scale = adapter.scale
            for (c in cols) {
                val tv = root.findViewById<TextView>(c.viewId) ?: continue
                val arrow = when (cur) {
                    c.asc -> " ↑"
                    c.desc -> " ↓"
                    else -> " ↕"
                }
                tv.text = c.label + arrow
                try {
                    tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f * scale)
                } catch (_: Exception) { }
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
                    else -> c.desc
                }
                adapter.setSort(next)
                refreshLabels()
            }
        }
        root.setTag(0x70A1, Runnable { refreshLabels() })
        refreshLabels()
    }

    fun refresh(root: View?) {
        try {
            (root?.getTag(0x70A1) as? Runnable)?.run()
        } catch (_: Exception) { }
    }
}
