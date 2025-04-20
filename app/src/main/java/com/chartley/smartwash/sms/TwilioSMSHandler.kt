// TwilioSMSHandler.kt
package com.chartley.smartwash.sms

import android.content.Context
import android.util.Log
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import java.util.HashMap

class TwilioSMSHandler(private val context: Context) {
    private val accountSid = "ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" // Replace with your Account SID
    private val authToken = "your_auth_token"  // Replace with your Auth Token
    private val twilioNumber = "+1234567890" // Replace with your Twilio phone number

    private var requestQueue: RequestQueue? = null

    init {
        requestQueue = Volley.newRequestQueue(context)
    }

    fun sendSms(phoneNumbers: List<String>, onSent: (String) -> Unit) {
        phoneNumbers.forEach { number ->
            sendSingleSms(number, onSent)
        }
    }


    private fun sendSingleSms(phoneNumber: String, onSent: (String) -> Unit) {
        val url = "https://api.twilio.com/2010-04-01/Accounts/$accountSid/Messages.json"
        val requestBody: MutableMap<String, String> = HashMap()
        requestBody["To"] = phoneNumber
        requestBody["From"] = twilioNumber
        requestBody["Body"] = "Your wash cycle is complete!"

        val stringRequest: StringRequest = object : StringRequest(
            Request.Method.POST, url,
            { response ->
                Log.d("Twilio", "SMS sent successfully to $phoneNumber: $response")
                onSent(phoneNumber)
            },
            { error ->
                Log.e("Twilio", "Error sending SMS to $phoneNumber: ${error.message}")
            }) {
            override fun getHeaders(): Map<String, String> {
                val headers: MutableMap<String, String> = HashMap()
                val credentials = "$accountSid:$authToken"
                val encodedCredentials =
                    "Basic " + android.util.Base64.encodeToString(credentials.toByteArray(), android.util.Base64.NO_WRAP)
                headers["Authorization"] = encodedCredentials
                return headers
            }

            override fun getParams(): Map<String, String> = requestBody
        }

        requestQueue?.add(stringRequest)
    }
}