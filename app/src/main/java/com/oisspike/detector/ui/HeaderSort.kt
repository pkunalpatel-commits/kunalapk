package com.oisspike.detector.ui

import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.oisspike.detector.R
import com.oisspike.detector.data.AppSettings

object HeaderSort {
    private data class Col(
        val id: Int,
        val label: String,
        val asc: SpikeSort,
        val desc: SpikeSort,
        val hideKey: String? = null,
    )

    private val cols = listOf(
        Col(R.id.hdrSymbol, "Symbol", SpikeSort.TIME_ASC, SpikeSort.TIME_DESC, null),
        Col(R.id.hdrTime, "Time", SpikeSort.TIME_ASC, SpikeSort.TIME_DESC, "time"),
        Col(R.id.hdrExpiry, "Exp", SpikeSort.EXPIRY_ASC, SpikeSort.EXPIRY_DESC, "expiry"),
        Col(R.id.hdrStrike, "Strike", SpikeSort.STRIKE_ASC, SpikeSort.STRIKE_DESC, null),
        Col(R.id.hdrType, "CE", SpikeSort.TYPE_ASC, SpikeSort.TYPE_DESC, null),
        Col(R.id.hdrWin, "Win", SpikeSort.WIN_ASC, SpikeSort.WIN_DESC, "win"),
        Col(R.id.hdrOi, "OI", SpikeSort.OI_SIZE_ASC, SpikeSort.OI_SIZE_DESC, "oi"),
        Col(R.id.hdrOiPct, "OI%", SpikeSort.OI_PCT_ASC, SpikeSort.OI_PCT_DESC, null),
        Col(R.id.hdrAmt, "Amt", SpikeSort.AMT_ASC, SpikeSort.AMT_DESC, "amt"),
        Col(R.id.hdrLtp, "LTP", SpikeSort.LTP_ASC, SpikeSort.LTP_DESC, "ltp"),
    )

    fun wire(activity: AppCompatActivity, root: View?, adapter: SpikeAdapter) {
        if (root == null) return
        val settings = AppSettings(activity)

        fun applyVis() {
            adapter.applyVisibility(
                settings.isColVisible("time"),
                settings.isColVisible("expiry"),
                settings.isColVisible("win"),
                settings.isColVisible("oi"),
                settings.isColVisible("ltp"),
                settings.isColVisible("amt"),
            )
            for (c in cols) {
                val tv = root.findViewById<TextView>(c.id) ?: continue
                val key = c.hideKey
                tv.visibility = if (key == null || settings.isColVisible(key)) View.VISIBLE else View.GONE
            }
        }

        fun refreshSortLabels() {
            val cur = adapter.currentSort()
            for (c in cols) {
                val tv = root.findViewById<TextView>(c.id) ?: continue
                if (tv.visibility != View.VISIBLE) continue
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
                adapter.setSort(
                    when (cur) {
                        c.desc -> c.asc
                        c.asc -> c.desc
                        else -> c.desc
                    }
                )
                refreshSortLabels()
            }
            val key = c.hideKey
            if (key != null) {
                tv.setOnLongClickListener {
                    AlertDialog.Builder(activity)
                        .setTitle("Hide column?")
                        .setMessage("Hide \"${c.label}\"?\n\nShow again: Settings → Reset result columns")
                        .setPositiveButton("Hide") { _, _ ->
                            settings.setColVisible(key, false)
                            applyVis()
                            refreshSortLabels()
                            Toast.makeText(activity, "${c.label} hidden", Toast.LENGTH_SHORT).show()
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                    true
                }
            }
        }
        applyVis()
        refreshSortLabels()
    }
}
