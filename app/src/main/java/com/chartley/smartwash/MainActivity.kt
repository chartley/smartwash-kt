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

        viewModel.isMonitoring.observe(this, Observer { isMonitoring ->
            startStopButton.text = if (isMonitoring) "Monitoring..." else "Start Wash Monitor"
        })

        viewModel.logMessages.observe(this, Observer { logs ->
            logTextView.text = logs.joinToString("\n")
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
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.SEND_SMS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestSmsPermission()
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(Intent(this, MonitoringService::class.java))
        } else {
            startService(Intent(this, MonitoringService::class.java))
        }
    }

    private fun stopMonitoringService() {
        stopService(Intent(this, MonitoringService::class.java))
    }

    private fun requestSmsPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.SEND_SMS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (shouldShowRequestPermissionRationale(Manifest.permission.SEND_SMS)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                // You might show a dialog or a snackbar.  For simplicity, we'll just log.
                logTextView.append("\n[${currentTime()}] SMS permission required to send notifications.")
            }
            requestPermissionLauncher.launch(Manifest.permission.SEND_SMS)
        }
    }


    private fun currentTime(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }

}


//import android.os.Bundle
//import androidx.activity.enableEdgeToEdge
//import androidx.appcompat.app.AppCompatActivity
//import androidx.core.view.ViewCompat
//import androidx.core.view.WindowInsetsCompat
//
//class MainActivity : AppCompatActivity() {
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
//        setContentView(R.layout.activity_main)
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
//            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
//            insets
//        }
//    }
//}