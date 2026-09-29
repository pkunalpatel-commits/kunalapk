package com.oisspike.detector.ui

import com.oisspike.detector.R
import android.util.TypedValue
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/** Soft column-header buttons: tap toggles asc ↔ desc and updates label arrow. */
object HeaderSort {
    private data class Col(
        val viewId: Int,
        val label: String,
        val asc: SpikeSort,
        val desc: SpikeSort,
        val baseDp: Float,
    )

    private val cols = listOf(
        Col(R.id.hdrTime, "Time", SpikeSort.TIME_ASC, SpikeSort.TIME_DESC, 72f),
        Col(R.id.hdrExpiry, "Exp", SpikeSort.EXPIRY_ASC, SpikeSort.EXPIRY_DESC, 52f),
        Col(R.id.hdrStrike, "Strike", SpikeSort.STRIKE_ASC, SpikeSort.STRIKE_DESC, 56f),
        Col(R.id.hdrType, "CE", SpikeSort.TYPE_ASC, SpikeSort.TYPE_DESC, 36f),
        Col(R.id.hdrWin, "Win", SpikeSort.WIN_ASC, SpikeSort.WIN_DESC, 36f),
        Col(R.id.hdrOi, "OI", SpikeSort.OI_SIZE_ASC, SpikeSort.OI_SIZE_DESC, 64f),
        Col(R.id.hdrOiPct, "OI%", SpikeSort.OI_PCT_ASC, SpikeSort.OI_PCT_DESC, 56f),
        Col(R.id.hdrLtp, "LTP", SpikeSort.LTP_ASC, SpikeSort.LTP_DESC, 52f),
    )

    fun wire(activity: AppCompatActivity, root: View, adapter: SpikeAdapter) {
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
                tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f * scale)
                val lp = tv.layoutParams as? LinearLayout.LayoutParams
                if (lp != null) {
                    val d = TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, c.baseDp * scale, tv.resources.displayMetrics
                    ).toInt()
                    lp.width = d
                    tv.layoutParams = lp
                }
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
        // store refresher for zoom
        root.setTag(0x70A1, Runnable { refreshLabels() })
        refreshLabels()
    }

    fun refresh(root: View?) {
        val r = root?.getTag(0x70A1) as? Runnable
        r?.run()
    }
}
