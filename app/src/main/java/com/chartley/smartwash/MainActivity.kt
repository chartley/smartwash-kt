package com.chartley.smartwash

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.graphics.Color
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
// import androidx.lifecycle.observe
import com.chartley.smartwash.service.MonitoringService
import com.chartley.smartwash.viewmodel.WashMonitorViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// MainActivity.kt
class MainActivity : AppCompatActivity() {

    private val viewModel: WashMonitorViewModel by viewModels()
    private lateinit var startStopButton: Button
    private lateinit var phoneNumbersEditText: EditText
    private lateinit var logTextView: TextView
    private lateinit var accelChart: LineChart
    private lateinit var lineDataSet: LineDataSet
    private var sampleIndex = 0f
    private val maxPoints = 30

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                // Permission granted. Start service if monitoring.
                if (viewModel.isMonitoring.value == true) {
                    startMonitoringService()
                }
            } else {
                // Explain to the user that the feature is unavailable because the
                // features requires a permission that the user has denied.
                logTextView.append("\n[${currentTime()}] SMS permission denied.")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        startStopButton = findViewById(R.id.startStopButton)
        phoneNumbersEditText = findViewById(R.id.phoneNumbersEditText)
        logTextView = findViewById(R.id.logTextView)
        logTextView.movementMethod = ScrollingMovementMethod()
        accelChart = findViewById(R.id.accelChart)
        setupChart()

        viewModel.isMonitoring.observe(this, Observer { isMonitoring ->
            startStopButton.text = if (isMonitoring) "Monitoring..." else "Start Wash Monitor"
        })

        viewModel.logMessages.observe(this, Observer { logs ->
            logTextView.text = logs.joinToString("\n")
        })

        viewModel.accelMagnitude.observe(this, Observer { value ->
            addEntry(value)
        })

        startStopButton.setOnClickListener {
            if (viewModel.isMonitoring.value == true) {
                viewModel.stopMonitoring()
                stopMonitoringService()
            } else {
                val phoneNumbers = phoneNumbersEditText.text.toString()
                viewModel.startMonitoring(phoneNumbers)
                startMonitoringService()
            }
        }
    }

    private fun startMonitoringService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(Intent(this, MonitoringService::class.java))
            MonitoringService.viewModel = viewModel
        } else {
            startService(Intent(this, MonitoringService::class.java))
        }
    }

    private fun stopMonitoringService() {
        stopService(Intent(this, MonitoringService::class.java))
    }

    private fun currentTime(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun setupChart() {
        lineDataSet = LineDataSet(mutableListOf(), "accel")
        lineDataSet.setDrawCircles(false)
        lineDataSet.color = Color.BLUE
        lineDataSet.lineWidth = 2f
        val data = LineData(lineDataSet)
        data.setDrawValues(false)
        accelChart.data = data
        accelChart.description.isEnabled = false
        accelChart.setTouchEnabled(false)
        accelChart.axisRight.isEnabled = false
    }

    private fun addEntry(value: Float) {
        val data = accelChart.data ?: return
        data.addEntry(Entry(sampleIndex, value), 0)
        if (lineDataSet.entryCount > maxPoints) {
            lineDataSet.removeFirst()
        }
        data.notifyDataChanged()
        accelChart.notifyDataSetChanged()
        accelChart.setVisibleXRangeMaximum(maxPoints.toFloat())
        accelChart.moveViewToX(sampleIndex - maxPoints)
        sampleIndex += 1f
    }

}