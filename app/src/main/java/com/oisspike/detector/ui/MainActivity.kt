package com.oisspike.detector.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.oisspike.detector.R
import com.oisspike.detector.service.ScanService
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var logText: TextView
    private lateinit var btnStart: Button
    private lateinit var btnStop: Button
    private lateinit var adapter: SpikeAdapter
    private val handler = Handler(Looper.getMainLooper())
    private val listener: () -> Unit = { handler.post { refreshUi() }; Unit }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        logText = findViewById(R.id.logText)
        btnStart = findViewById(R.id.btnStart)
        btnStop = findViewById(R.id.btnStop)
        val list = findViewById<RecyclerView>(R.id.spikeList)
        adapter = SpikeAdapter()
        list.layoutManager = LinearLayoutManager(this)
        list.adapter = adapter

        btnStart.setOnClickListener {
            ensureNotifPermission()
            ScanService.start(this)
            refreshUi()
        }
        btnStop.setOnClickListener {
            ScanService.stop(this)
            handler.postDelayed({ refreshUi() }, 500)
        }
        findViewById<Button>(R.id.btnSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        findViewById<Button>(R.id.btnHistorical).setOnClickListener {
            startActivity(Intent(this, HistoricalActivity::class.java))
        }
        findViewById<Button>(R.id.btnLiveExpiries).setOnClickListener {
            startActivity(Intent(this, LiveExpiriesActivity::class.java))
        }

        val extraPanel = findViewById<LinearLayout>(R.id.mainExtraPanel)
        val btnToggleExtra = findViewById<Button>(R.id.btnToggleMainExtra)
        btnToggleExtra.setOnClickListener {
            if (extraPanel.visibility == View.VISIBLE) {
                extraPanel.visibility = View.GONE
                btnToggleExtra.text = "Show menu"
            } else {
                extraPanel.visibility = View.VISIBLE
                btnToggleExtra.text = "Min menu"
            }
        }

        refreshUi()
    }

    override fun onResume() {
        super.onResume()
        ScanService.addListener(listener)
        refreshUi()
        handler.post(tick)
    }

    override fun onPause() {
        handler.removeCallbacks(tick)
        ScanService.removeListener(listener)
        super.onPause()
    }

    private val tick = object : Runnable {
        override fun run() {
            refreshUi()
            handler.postDelayed(this, 2000)
        }
    }

    private fun refreshUi() {
        val running = ScanService.isRunning
        statusText.text = if (running) {
            val sweep = if (ScanService.lastSweepTs > 0)
                SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(ScanService.lastSweepTs))
            else "—"
            "Status: RUNNING · last sweep $sweep"
        } else {
            "Status: stopped"
        }
        statusText.setTextColor(
            ContextCompat.getColor(this, if (running) R.color.green else R.color.red)
        )
        btnStart.isEnabled = !running
        btnStop.isEnabled = running
        adapter.submit(ScanService.recentSpikes.toList())
        logText.text = ScanService.logs.take(40).joinToString("\n")
    }

    private fun ensureNotifPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100
                )
            }
        }
    }
}
