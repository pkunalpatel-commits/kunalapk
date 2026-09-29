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

class SpikeAdapter : RecyclerView.Adapter<SpikeAdapter.VH>() {
    private val items = mutableListOf<SpikeAlert>()
    private val fmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    fun submit(list: List<SpikeAlert>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_spike, parent, false)
        return VH(v)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val a = items[position]
        holder.line1.text = "${fmt.format(Date(a.ts * 1000))}  ${a.symbol}  ${a.strike} ${a.type}  ${a.window}"
        holder.line2.text = "OI ${a.oi.toLong()}  (+${a.oiChangePct}%)   LTP ${a.ltp}  (${a.priceChangePct}%)"
        val color = if (a.oiChangePct >= 0) Color.parseColor("#166534") else Color.parseColor("#991B1B")
        holder.line1.setTextColor(color)
    }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val line1: TextView = v.findViewById(R.id.line1)
        val line2: TextView = v.findViewById(R.id.line2)
    }
}
