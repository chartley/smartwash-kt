package com.chartley.smartwash.service// MonitoringService.kt
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.chartley.smartwash.MainActivity
import com.chartley.smartwash.R
import com.chartley.smartwash.sms.TwilioSMSHandler
import com.chartley.smartwash.viewmodel.WashMonitorViewModel
import kotlin.math.sqrt

class MonitoringService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var lastMovementTime: Long = 0
    private val threshold = 2f // Adjust this threshold based on your needs (experimentally)
    private val noMovementDuration = 60_000L // 60 seconds
    private var isWashDone = false

    companion object {
        var viewModel: WashMonitorViewModel? = null
        var twilioSMSHandler: TwilioSMSHandler? = null
    }


    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        createNotificationChannel()
        startForegroundNotification()
        twilioSMSHandler = TwilioSMSHandler(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startMonitoring()
        return START_STICKY
    }

    private fun startMonitoring() {
        viewModel?.logMessage("Monitoring service started.")
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
            lastMovementTime = System.currentTimeMillis()
            viewModel?.logMessage("Accelerometer started.")
        } ?: run {
            viewModel?.logMessage("Accelerometer not available.")
        }
    }


    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor == accelerometer && !isWashDone) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            val accelMagnitude = sqrt(x * x + y * y + z * z) - 9.8f // Subtract gravity

            viewModel?.accelText("accelMagnitude $accelMagnitude")

            if (accelMagnitude > threshold) {
                lastMovementTime = System.currentTimeMillis()
            } else {
                if (System.currentTimeMillis() - lastMovementTime > noMovementDuration) {
                    onWashComplete()
                }
            }
        }
    }

    private fun onWashComplete() {
        isWashDone = true
        viewModel?.onWashComplete()
        stopSelf()
    }


    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Handle sensor accuracy changes if needed.
    }


    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "WashMonitorChannel",
                "Wash Monitor Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }


    private fun startForegroundNotification() {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, "WashMonitorChannel")
            .setContentTitle("Wash Monitor")
            .setContentText("Monitoring wash cycle...")
            // .setSmallIcon(R.drawable.ic_notification) // Replace with your notification icon
            .setContentIntent(pendingIntent)
            .build()

        startForeground(1, notification)
    }


    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
        viewModel?.logMessage("Monitoring service stopped.")
        viewModel = null // Release ViewModel reference
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null // No binding for this service
    }
}