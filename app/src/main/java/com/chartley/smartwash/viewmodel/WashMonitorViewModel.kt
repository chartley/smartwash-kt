package com.chartley.smartwash.viewmodel// WashMonitorViewModel.kt
import com.chartley.smartwash.service.MonitoringService
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class WashMonitorViewModel : ViewModel() {

    private val _isMonitoring = MutableLiveData(false)
    val isMonitoring: LiveData<Boolean> = _isMonitoring

    private val _logMessages = MutableLiveData<List<String>>(emptyList())
    val logMessages: LiveData<List<String>> = _logMessages
    private val log = mutableListOf<String>()

    private val _accelText = MutableLiveData<String>("")
    val accelText: LiveData<String> = _accelText

    private val _accelMagnitude = MutableLiveData<Float>()
    val accelMagnitude: LiveData<Float> = _accelMagnitude

    private var emailAddresses: List<String> = emptyList()


    fun startMonitoring(emailAddressesString: String) {
        _isMonitoring.value = true
        emailAddresses = parseEmailAddresses(emailAddressesString)
        logMessage("Monitoring started.")
    }

    fun stopMonitoring() {
        _isMonitoring.value = false
        logMessage("Monitoring stopped.")
        emailAddresses = emptyList()
    }


    fun onWashComplete() {
        _isMonitoring.postValue(false) // Use postValue for background thread
        logMessage("Wash complete.")
        MonitoringService.brevoEmailHandler?.sendEmails(emailAddresses)
        { addr ->  logMessage("Email sent to: $addr") }
    }



    fun logMessage(message: String) {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
        val timestamp = sdf.format(java.util.Date())
        val entry = "[$timestamp] $message"
        log.add(entry)
        _logMessages.postValue(log) // Use postValue to update from background thread
    }

    fun accelText(message: String) {
        _accelText.postValue(message)
    }

    fun postAccelMagnitude(value: Float) {
        _accelMagnitude.postValue(value)
    }

    private fun parseEmailAddresses(addresses: String): List<String> {
        return addresses.replace("\n", ",").split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }


    override fun onCleared() {
        super.onCleared()
        // Clean up resources if needed
        MonitoringService.viewModel = null
    }

}