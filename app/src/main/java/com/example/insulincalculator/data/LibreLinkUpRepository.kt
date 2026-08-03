package com.example.insulincalculator.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class LibreLinkUpRepository(private val context: Context) {

    companion object {
        val REGIONS = linkedMapOf(
            "EU" to "api-eu.libreview.io",
            "US" to "api-us.libreview.io",
            "AU" to "api-au.libreview.io",
            "CA" to "api-ca.libreview.io",
            "DE" to "api-de.libreview.io",
            "JP" to "api-jp.libreview.io"
        )
        val REGION_LABELS = mapOf(
            "EU" to "Europe (EU)",
            "US" to "United States (US)",
            "AU" to "Australia (AU)",
            "CA" to "Canada (CA)",
            "DE" to "Germany (DE)",
            "JP" to "Japan (JP)"
        )
    }

    private val prefs = context.getSharedPreferences("librelinkup_prefs", Context.MODE_PRIVATE)
    private val client = OkHttpClient()

    fun getStoredRegion(): String = prefs.getString("region", "EU") ?: "EU"
    fun getStoredEmail(): String? = prefs.getString("email", null)
    private fun getToken(): String? = prefs.getString("token", null)
    private fun getTokenExpiry(): Long = prefs.getLong("token_expiry", 0L)

    fun isLinked(): Boolean {
        getToken() ?: return false
        val expiry = getTokenExpiry()
        return expiry == 0L || System.currentTimeMillis() < expiry * 1000L
    }

    private fun baseUrlFor(region: String): String =
        "https://${REGIONS[region] ?: REGIONS["EU"]}"

    private fun defaultHeaders(token: String? = null): Map<String, String> = buildMap {
        put("Content-Type", "application/json")
        put("Accept", "application/json")
        put("product", "llu.android")
        put("version", "4.16.0")
        put("cache-control", "no-cache")
        if (token != null) put("Authorization", "Bearer $token")
    }

    suspend fun login(email: String, password: String, region: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val data = performLogin(email, password, baseUrlFor(region))
                if (data.optBoolean("redirect", false)) {
                    val redirectCode = data.optString("region", "eu").lowercase()
                    // Use the actual redirect region for all future requests
                    val effectiveRegion = redirectCode.uppercase().let {
                        if (REGIONS.containsKey(it)) it else region
                    }
                    val redirectData = performLogin(email, password, "https://api-$redirectCode.libreview.io")
                    saveToken(email, effectiveRegion, redirectData)
                } else {
                    saveToken(email, region, data)
                }
            }
        }

    private fun performLogin(email: String, password: String, baseUrl: String): JSONObject {
        val body = JSONObject().apply {
            put("email", email)
            put("password", password)
        }.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("$baseUrl/llu/auth/login")
            .post(body)
            .apply { defaultHeaders().forEach { (k, v) -> addHeader(k, v) } }
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: throw Exception("Empty response")
        val json = JSONObject(responseBody)
        val data = json.optJSONObject("data")

        if (data?.optBoolean("redirect", false) == true) return data

        val status = json.optInt("status", -1)
        if (status != 0) {
            val msg = json.optJSONObject("error")?.optString("message") ?: "Login failed (status $status)"
            throw Exception(msg)
        }

        return data ?: throw Exception("Unexpected response format")
    }

    private fun saveToken(email: String, region: String, data: JSONObject) {
        val authTicket = data.optJSONObject("authTicket")
            ?: throw Exception("No auth ticket in response")
        val token = authTicket.getString("token")
        val expires = authTicket.getLong("expires")

        prefs.edit().apply {
            putString("token", token)
            putLong("token_expiry", expires)
            putString("email", email)
            putString("region", region)
            apply()
        }
    }

    private fun sha256(input: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        return digest.digest(input.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }

    private fun decodeJwtPayload(token: String): JSONObject? {
        return try {
            val part = token.split(".").getOrNull(1) ?: return null
            val padded = part + "=".repeat((4 - part.length % 4) % 4)
            val bytes = android.util.Base64.decode(padded, android.util.Base64.URL_SAFE)
            JSONObject(String(bytes, Charsets.UTF_8))
        } catch (e: Exception) { null }
    }

    suspend fun fetchCurrentGlucose(): Result<Double> = withContext(Dispatchers.IO) {
        runCatching {
            val token = getToken() ?: throw Exception("Not linked to LibreLinkUp")
            val baseUrl = baseUrlFor(getStoredRegion())

            val jwtPayload = decodeJwtPayload(token)
            val userId = jwtPayload?.optString("id")?.takeIf { it.isNotBlank() }
                ?: throw Exception("Cannot determine user ID — please re-link in Settings")
            val accountId = sha256(userId)
            val request = Request.Builder()
                .url("$baseUrl/llu/connections")
                .get()
                .apply {
                    defaultHeaders(token).forEach { (k, v) -> addHeader(k, v) }
                    addHeader("account-id", accountId)
                }
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: throw Exception("Empty response")

            if (response.code == 401) {
                prefs.edit().remove("token").apply()
                throw Exception("Session expired — please re-link your account in Settings")
            }
            if (!response.isSuccessful) {
                val apiMessage = try {
                    JSONObject(responseBody).optJSONObject("error")?.optString("message")
                } catch (e: Exception) { null }
                throw Exception("HTTP ${response.code}: ${apiMessage ?: responseBody.take(200)}")
            }

            val json = JSONObject(responseBody)
            val dataArray = json.optJSONArray("data")
                ?: throw Exception("No connections found")

            if (dataArray.length() == 0) throw Exception("No LibreLinkUp connections found")

            val measurement = dataArray.getJSONObject(0)
                .optJSONObject("glucoseMeasurement")
                ?: throw Exception("No current glucose reading available")

            val value = measurement.getDouble("Value")
            val units = measurement.optInt("GlucoseUnits", 0)
            // GlucoseUnits: 0 = mmol/L, 1 = mg/dL
            if (units == 1) value / 18.0182 else value
        }
    }

    fun unlink() {
        prefs.edit().clear().apply()
    }
}
