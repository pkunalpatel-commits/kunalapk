package com.oisspike.detector.ui

import android.app.AlertDialog
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.Toast
import com.oisspike.detector.data.Defaults

object SymbolPicker {
    private val allSymbols: List<String> by lazy {
        Defaults.SYMBOLS.keys.sorted()
    }

    fun show(context: Context, current: String, onPicked: (String) -> Unit) {
        val pad = (16 * context.resources.displayMetrics.density).toInt()
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }
        val search = EditText(context).apply {
            hint = "Search underlying (NIFTY, RELIANCE, CRUDEOIL…)"
            setSingleLine()
            setText(current)
            setSelection(text.length)
        }
        val list = ListView(context)
        var filtered = allSymbols
        val adapter = ArrayAdapter(context, android.R.layout.simple_list_item_1, filtered.toMutableList())
        list.adapter = adapter
        list.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            (320 * context.resources.displayMetrics.density).toInt()
        )

        fun applyFilter(q: String) {
            val u = q.trim().uppercase()
            filtered = if (u.isEmpty()) allSymbols
            else allSymbols.filter { it.contains(u) }
            adapter.clear()
            adapter.addAll(filtered)
            adapter.notifyDataSetChanged()
        }

        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                applyFilter(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        layout.addView(search)
        layout.addView(list)

        val dialog = AlertDialog.Builder(context)
            .setTitle("Select underlying (${allSymbols.size})")
            .setView(layout)
            .setNegativeButton("Cancel", null)
            .create()

        list.setOnItemClickListener { _, _, position, _ ->
            if (position in filtered.indices) {
                onPicked(filtered[position])
                dialog.dismiss()
            }
        }
        dialog.show()
        search.requestFocus()
    }

    fun validateOrToast(context: Context, symbol: String): Boolean {
        val s = symbol.trim().uppercase()
        if (s.isEmpty()) {
            Toast.makeText(context, "Enter or pick a symbol", Toast.LENGTH_SHORT).show()
            return false
        }
        if (!Defaults.SYMBOLS.containsKey(s)) {
            Toast.makeText(
                context,
                "Unknown underlying: $s — use Search symbol",
                Toast.LENGTH_LONG
            ).show()
            return false
        }
        return true
    }
}
