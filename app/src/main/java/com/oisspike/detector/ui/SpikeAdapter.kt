package com.oisspike.detector.ui

import android.graphics.Color
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.oisspike.detector.R
import com.oisspike.detector.engine.SpikeAlert
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class SpikeSort {
    TIME_DESC, TIME_ASC,
    EXPIRY_DESC, EXPIRY_ASC,
    STRIKE_DESC, STRIKE_ASC,
    TYPE_ASC, TYPE_DESC,
    WIN_ASC, WIN_DESC,
    OI_SIZE_DESC, OI_SIZE_ASC,
    OI_PCT_DESC, OI_PCT_ASC,
    LTP_DESC, LTP_ASC,
}

class SpikeAdapter : RecyclerView.Adapter<SpikeAdapter.VH>() {
    private val raw = mutableListOf<SpikeAlert>()
    private val items = mutableListOf<SpikeAlert>()
    private var sort = SpikeSort.TIME_DESC
    private val fmtTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    private val fmtDate = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
    var showDate: Boolean = false

    /** 0.70 … 1.40 — text & column scale for zoom */
    var scale: Float = 1.0f
        set(value) {
            field = value.coerceIn(0.65f, 1.45f)
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

    private fun shortExpiry(exp: String): String {
        val parts = exp.split("-")
        return if (parts.size >= 3) "${parts[1]}-${parts[2]}" else exp
    }

    private fun formatOi(oi: Double): String {
        return when {
            oi >= 1_000_000 -> String.format(Locale.US, "%.1fM", oi / 1_000_000)
            oi >= 1_000 -> String.format(Locale.US, "%.0fK", oi / 1_000)
            else -> oi.toLong().toString()
        }
    }

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

    private fun dp(v: View, d: Float): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, d, v.resources.displayMetrics).toInt()

    private fun applyColWidth(tv: TextView, baseDp: Float) {
        val lp = tv.layoutParams as LinearLayout.LayoutParams
        lp.width = dp(tv, baseDp * scale)
        tv.layoutParams = lp
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f * scale)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val a = items[position]
        val t = if (showDate) fmtDate.format(Date(a.ts * 1000))
        else fmtTime.format(Date(a.ts * 1000))
        holder.colTime.text = t
        holder.colExpiry.text = shortExpiry(a.expiry)
        holder.colStrike.text = a.strike.toInt().toString()
        holder.colType.text = a.type
        holder.colWin.text = a.window
        holder.colOi.text = formatOi(a.oi)
        holder.colOiPct.text = String.format(Locale.US, "%+.1f%%", a.oiChangePct)
        holder.colLtp.text = String.format(Locale.US, "%.1f", a.ltp)

        applyColWidth(holder.colTime, 72f)
        applyColWidth(holder.colExpiry, 52f)
        applyColWidth(holder.colStrike, 56f)
        applyColWidth(holder.colType, 36f)
        applyColWidth(holder.colWin, 36f)
        applyColWidth(holder.colOi, 64f)
        applyColWidth(holder.colOiPct, 56f)
        applyColWidth(holder.colLtp, 52f)

        val green = Color.parseColor("#86EFAC")
        val red = Color.parseColor("#FCA5A5")
        val c = if (a.oiChangePct >= 0) green else red
        holder.colOiPct.setTextColor(c)
        holder.colTime.setTextColor(c)
        val bg = if (position % 2 == 0) Color.parseColor("#1A2332") else Color.parseColor("#141C28")
        holder.itemView.setBackgroundColor(bg)
        (holder.colTime.parent as? View)?.setBackgroundColor(bg)

        val padV = dp(holder.itemView, 7f * scale)
        val row = holder.colTime.parent as? View
        row?.setPadding(dp(holder.itemView, 4f), padV, dp(holder.itemView, 4f), padV)
    }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val colTime: TextView = v.findViewById(R.id.colTime)
        val colExpiry: TextView = v.findViewById(R.id.colExpiry)
        val colStrike: TextView = v.findViewById(R.id.colStrike)
        val colType: TextView = v.findViewById(R.id.colType)
        val colWin: TextView = v.findViewById(R.id.colWin)
        val colOi: TextView = v.findViewById(R.id.colOi)
        val colOiPct: TextView = v.findViewById(R.id.colOiPct)
        val colLtp: TextView = v.findViewById(R.id.colLtp)
    }
}
