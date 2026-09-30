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
    TIME_DESC, TIME_ASC, EXPIRY_DESC, EXPIRY_ASC,
    STRIKE_DESC, STRIKE_ASC, TYPE_ASC, TYPE_DESC,
    WIN_ASC, WIN_DESC, OI_SIZE_DESC, OI_SIZE_ASC,
    OI_PCT_DESC, OI_PCT_ASC, LTP_DESC, LTP_ASC,
    ATM_DESC, ATM_ASC,
}

class SpikeAdapter : RecyclerView.Adapter<SpikeAdapter.VH>() {
    private val raw = mutableListOf<SpikeAlert>()
    private val items = mutableListOf<SpikeAlert>()
    private var sort = SpikeSort.TIME_DESC
    private val fmtTime = SimpleDateFormat("HH:mm", Locale.getDefault())
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

    /** Full expiry e.g. 2026-10-19 (no short form). */
    private fun fullExpiry(exp: String): String = exp.trim()

    /** Full OI integer with thousand separators, e.g. 694,460 */
    private fun fullOi(oi: Double): String =
        String.format(Locale.US, "%,d", oi.toLong())

    private fun applySort() {
        val sorted = when (sort) {
            SpikeSort.TIME_DESC -> raw.sortedByDescending { it.ts }
            SpikeSort.TIME_ASC -> raw.sortedBy { it.ts }
            SpikeSort.EXPIRY_DESC -> raw.sortedByDescending { it.expiry }
            SpikeSort.EXPIRY_ASC -> raw.sortedBy { it.expiry }
            SpikeSort.STRIKE_DESC -> raw.sortedByDescending { it.strike }
            SpikeSort.STRIKE_ASC -> raw.sortedBy { it.strike }
            SpikeSort.TYPE_ASC -> raw.sortedBy { it.type }
            SpikeSort.TYPE_DESC -> raw.sortedByDescending { it.type }
            SpikeSort.WIN_ASC -> raw.sortedBy { it.window }
            SpikeSort.WIN_DESC -> raw.sortedByDescending { it.window }
            SpikeSort.OI_SIZE_DESC -> raw.sortedByDescending { it.oi }
            SpikeSort.OI_SIZE_ASC -> raw.sortedBy { it.oi }
            SpikeSort.OI_PCT_DESC -> raw.sortedByDescending { it.oiChangePct }
            SpikeSort.OI_PCT_ASC -> raw.sortedBy { it.oiChangePct }
            SpikeSort.LTP_DESC -> raw.sortedByDescending { it.ltp }
            SpikeSort.LTP_ASC -> raw.sortedBy { it.ltp }
            SpikeSort.ATM_DESC -> raw.sortedByDescending { it.atmDistance ?: Int.MIN_VALUE }
            SpikeSort.ATM_ASC -> raw.sortedBy { it.atmDistance ?: Int.MAX_VALUE }
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
        holder.colSymbol.text = a.symbol
        holder.colTime.text = if (showDate) fmtDate.format(Date(a.ts * 1000))
        else fmtTime.format(Date(a.ts * 1000))
        holder.colExpiry.text = fullExpiry(a.expiry)
        holder.colStrike.text = a.strike.toInt().toString()
        holder.colType.text = a.type
        holder.colWin.text = a.window
        holder.colAtm.text = when (val d = a.atmDistance) {
            null -> "—"
            0 -> "0"
            else -> if (d > 0) "+$d" else "$d"
        }
        holder.colOi.text = fullOi(a.oi)
        holder.colOiPct.text = String.format(Locale.US, "%+.0f%%", a.oiChangePct)
        holder.colLtp.text = String.format(Locale.US, "%.0f", a.ltp)
        val c = if (a.oiChangePct >= 0) Color.parseColor("#86EFAC") else Color.parseColor("#FCA5A5")
        holder.colOiPct.setTextColor(c)
        holder.colTime.setTextColor(c)
        holder.itemView.setBackgroundColor(
            if (position % 2 == 0) Color.parseColor("#1A2332") else Color.parseColor("#141C28")
        )
    }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val colSymbol: TextView = v.findViewById(R.id.colSymbol)
        val colTime: TextView = v.findViewById(R.id.colTime)
        val colExpiry: TextView = v.findViewById(R.id.colExpiry)
        val colStrike: TextView = v.findViewById(R.id.colStrike)
        val colType: TextView = v.findViewById(R.id.colType)
        val colWin: TextView = v.findViewById(R.id.colWin)
        val colAtm: TextView = v.findViewById(R.id.colAtm)
        val colOi: TextView = v.findViewById(R.id.colOi)
        val colOiPct: TextView = v.findViewById(R.id.colOiPct)
        val colLtp: TextView = v.findViewById(R.id.colLtp)
    }
}
