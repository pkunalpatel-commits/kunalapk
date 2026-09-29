package com.oisspike.detector.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.oisspike.detector.R
import com.oisspike.detector.engine.SpikeAlert
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class SpikeSort {
    TIME_DESC, TIME_ASC,
    OI_DESC, OI_ASC,
    LTP_DESC, LTP_ASC,
    STRIKE_ASC, STRIKE_DESC,
}

class SpikeAdapter : RecyclerView.Adapter<SpikeAdapter.VH>() {
    private val raw = mutableListOf<SpikeAlert>()
    private val items = mutableListOf<SpikeAlert>()
    private var sort = SpikeSort.TIME_DESC
    private val fmtTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    private val fmtDate = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
    var showDate: Boolean = false

    fun submit(list: List<SpikeAlert>) {
        raw.clear()
        raw.addAll(list)
        applySort()
    }

    fun setSort(s: SpikeSort) {
        sort = s
        applySort()
    }

    fun currentSort(): SpikeSort = sort

    private fun applySort() {
        val sorted = when (sort) {
            SpikeSort.TIME_DESC -> raw.sortedByDescending { it.ts }
            SpikeSort.TIME_ASC -> raw.sortedBy { it.ts }
            SpikeSort.OI_DESC -> raw.sortedByDescending { it.oiChangePct }
            SpikeSort.OI_ASC -> raw.sortedBy { it.oiChangePct }
            SpikeSort.LTP_DESC -> raw.sortedByDescending { it.ltp }
            SpikeSort.LTP_ASC -> raw.sortedBy { it.ltp }
            SpikeSort.STRIKE_ASC -> raw.sortedBy { it.strike }
            SpikeSort.STRIKE_DESC -> raw.sortedByDescending { it.strike }
        }
        items.clear()
        items.addAll(sorted)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_spike, parent, false)
        return VH(v)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val a = items[position]
        val t = if (showDate) fmtDate.format(Date(a.ts * 1000))
        else fmtTime.format(Date(a.ts * 1000))
        holder.colTime.text = t
        holder.colStrike.text = "${a.strike.toInt()}"
        holder.colType.text = a.type
        holder.colWin.text = a.window
        holder.colOiPct.text = String.format(Locale.US, "%+.1f%%", a.oiChangePct)
        holder.colLtp.text = String.format(Locale.US, "%.1f", a.ltp)
        val green = Color.parseColor("#86EFAC")
        val red = Color.parseColor("#FCA5A5")
        val c = if (a.oiChangePct >= 0) green else red
        holder.colOiPct.setTextColor(c)
        holder.colTime.setTextColor(c)
        // zebra
        holder.itemView.setBackgroundColor(
            if (position % 2 == 0) Color.parseColor("#1A2332") else Color.parseColor("#141C28")
        )
    }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val colTime: TextView = v.findViewById(R.id.colTime)
        val colStrike: TextView = v.findViewById(R.id.colStrike)
        val colType: TextView = v.findViewById(R.id.colType)
        val colWin: TextView = v.findViewById(R.id.colWin)
        val colOiPct: TextView = v.findViewById(R.id.colOiPct)
        val colLtp: TextView = v.findViewById(R.id.colLtp)
    }
}
