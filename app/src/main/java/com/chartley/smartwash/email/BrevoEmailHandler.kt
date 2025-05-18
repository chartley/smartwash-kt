// BrevoEmailHandler.kt
package com.chartley.smartwash.email

import android.content.Context
import android.util.Log
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.chartley.smartwash.AppSecrets
import org.json.JSONArray
import org.json.JSONObject

class BrevoEmailHandler(private val context: Context) {
    private val apiKey = AppSecrets.brevoApiKey
    private var requestQueue: RequestQueue? = null

    init {
        requestQueue = Volley.newRequestQueue(context)
    }

    fun sendEmails(addresses: List<String>, onSent: (String) -> Unit) {
        addresses.forEach { email ->
            sendSingleEmail(email, onSent)
        }
    }

    private fun sendSingleEmail(email: String, onSent: (String) -> Unit) {
        val url = "https://api.brevo.com/v3/smtp/email"
        val jsonBody = JSONObject().apply {
            put("sender", JSONObject().apply {
                put("name", "SmartWash")
                put("email", "no-reply@smartwash.local")
            })
            put("to", JSONArray().apply {
                put(JSONObject().apply { put("email", email) })
            })
            put("subject", "Wash cycle complete")
            put("htmlContent", "<p>Your wash cycle is complete!</p>")
        }

        val request = object : JsonObjectRequest(Method.POST, url, jsonBody,
            Response.Listener { _ ->
                Log.d("Brevo", "Email sent successfully to $email")
                onSent(email)
            },
            Response.ErrorListener { error ->
                Log.e("Brevo", "Error sending email to $email: ${'$'}{error.message}")
            }) {
            override fun getHeaders(): Map<String, String> {
                val headers = HashMap<String, String>()
                headers["api-key"] = apiKey
                headers["Content-Type"] = "application/json"
                return headers
            }
        }

        requestQueue?.add(request)
    }
}
