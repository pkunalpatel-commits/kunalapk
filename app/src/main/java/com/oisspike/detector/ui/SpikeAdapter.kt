package com.oisspike.detector.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.oisspike.detector.R
import com.oisspike.detector.engine.SpikeAlert
import com.oisspike.detector.engine.formatInr
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class SpikeSort {
    TIME_DESC, TIME_ASC, EXPIRY_DESC, EXPIRY_ASC,
    STRIKE_DESC, STRIKE_ASC, TYPE_ASC, TYPE_DESC,
    WIN_ASC, WIN_DESC, OI_SIZE_DESC, OI_SIZE_ASC,
    OI_PCT_DESC, OI_PCT_ASC, LTP_DESC, LTP_ASC,
    AMT_DESC, AMT_ASC,
}

class SpikeAdapter : RecyclerView.Adapter<SpikeAdapter.VH>() {
    private val raw = mutableListOf<SpikeAlert>()
    private val items = mutableListOf<SpikeAlert>()
    private var sort = SpikeSort.TIME_DESC
    private val fmtTime = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val fmtDate = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
    var showDate: Boolean = false
    var visTime = true
    var visExpiry = true
    var visWin = true
    var visOi = true
    var visLtp = true
    var visAmt = true

    fun applyVisibility(
        time: Boolean, expiry: Boolean, win: Boolean,
        oi: Boolean, ltp: Boolean, amt: Boolean,
    ) {
        visTime = time
        visExpiry = expiry
        visWin = win
        visOi = oi
        visLtp = ltp
        visAmt = amt
        notifyDataSetChanged()
    }

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

    private fun fullExpiry(exp: String): String = exp.trim()
    private fun fullOi(oi: Double): String = String.format(Locale.US, "%,d", oi.toLong())

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
            SpikeSort.AMT_DESC -> raw.sortedByDescending { it.amountIncrease }
            SpikeSort.AMT_ASC -> raw.sortedBy { it.amountIncrease }
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

    private fun setCol(tv: TextView, visible: Boolean, text: String) {
        if (visible) {
            tv.visibility = View.VISIBLE
            tv.text = text
        } else {
            tv.visibility = View.GONE
        }
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val a = items[position]
        holder.colSymbol.text = a.symbol
        setCol(holder.colTime, visTime, if (showDate) fmtDate.format(Date(a.ts * 1000)) else fmtTime.format(Date(a.ts * 1000)))
        setCol(holder.colExpiry, visExpiry, fullExpiry(a.expiry))
        holder.colStrike.text = a.strike.toInt().toString()
        holder.colType.text = if (a.kind == "SQUEEZE") "SQ" else a.type
        setCol(holder.colWin, visWin, a.window)
        setCol(holder.colOi, visOi, fullOi(a.oi))
        holder.colOiPct.text = String.format(Locale.US, "%+.0f%%", a.oiChangePct)
        setCol(holder.colAmt, visAmt, formatInr(a.amountIncrease))
        setCol(holder.colLtp, visLtp, String.format(Locale.US, "%.0f", a.ltp))
        val c = when {
            a.kind == "SQUEEZE" -> Color.parseColor("#FBBF24")
            a.oiChangePct >= 0 -> Color.parseColor("#86EFAC")
            else -> Color.parseColor("#FCA5A5")
        }
        holder.colOiPct.setTextColor(c)
        holder.colTime.setTextColor(c)
        holder.colType.setTextColor(c)
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
        val colOi: TextView = v.findViewById(R.id.colOi)
        val colOiPct: TextView = v.findViewById(R.id.colOiPct)
        val colAmt: TextView = v.findViewById(R.id.colAmt)
        val colLtp: TextView = v.findViewById(R.id.colLtp)
    }
}
