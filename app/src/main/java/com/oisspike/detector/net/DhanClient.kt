package com.oisspike.detector.net

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

class DhanApiException(message: String) : Exception(message)

class DhanClient(var clientId: String, var accessToken: String) {
    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .build()

    private val lastCallMs = AtomicLong(0)
    private val minGapMs = 3100L
    private val histLastCallMs = AtomicLong(0)
    private val histMinGapMs = 1100L

    private fun throttle() {
        val now = System.currentTimeMillis()
        val elapsed = now - lastCallMs.get()
        if (elapsed < minGapMs) Thread.sleep(minGapMs - elapsed)
        lastCallMs.set(System.currentTimeMillis())
    }

    private fun histThrottle() {
        val now = System.currentTimeMillis()
        val elapsed = now - histLastCallMs.get()
        if (elapsed < histMinGapMs) Thread.sleep(histMinGapMs - elapsed)
        histLastCallMs.set(System.currentTimeMillis())
    }

    private fun post(path: String, body: JSONObject): JSONObject {
        throttle()
        val req = Request.Builder()
            .url("https://api.dhan.co/v2$path")
            .addHeader("Content-Type", "application/json")
            .addHeader("access-token", accessToken)
            .addHeader("client-id", clientId)
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()
        http.newCall(req).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw DhanApiException("HTTP ${resp.code}: ${text.take(300)}")
            }
            val json = JSONObject(text)
            if (json.optString("status") != "success") {
                throw DhanApiException("API error: ${text.take(300)}")
            }
            return json.getJSONObject("data")
        }
    }

    fun getExpiryListRaw(scrip: Int, seg: String): List<String> {
        throttle()
        val body = JSONObject()
            .put("UnderlyingScrip", scrip)
            .put("UnderlyingSeg", seg)
        val req = Request.Builder()
            .url("https://api.dhan.co/v2/optionchain/expirylist")
            .addHeader("Content-Type", "application/json")
            .addHeader("access-token", accessToken)
            .addHeader("client-id", clientId)
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()
        http.newCall(req).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) throw DhanApiException("HTTP ${resp.code}: ${text.take(300)}")
            val json = JSONObject(text)
            if (json.optString("status") != "success") {
                throw DhanApiException("API error: ${text.take(300)}")
            }
            val data = json.get("data")
            val out = mutableListOf<String>()
            if (data is JSONArray) {
                for (i in 0 until data.length()) out.add(data.getString(i))
            }
            return out
        }
    }

    fun getOptionChain(scrip: Int, seg: String, expiry: String): JSONObject {
        return post(
            "/optionchain",
            JSONObject()
                .put("UnderlyingScrip", scrip)
                .put("UnderlyingSeg", seg)
                .put("Expiry", expiry),
        )
    }

    fun getIntradayHistorical(
        securityId: String,
        exchangeSegment: String,
        instrument: String,
        interval: String,
        fromDate: String,
        toDate: String,
        oi: Boolean = true,
    ): JSONObject {
        histThrottle()
        val body = JSONObject()
            .put("securityId", securityId)
            .put("exchangeSegment", exchangeSegment)
            .put("instrument", instrument)
            .put("interval", interval)
            .put("oi", oi)
            .put("fromDate", fromDate)
            .put("toDate", toDate)
        val req = Request.Builder()
            .url("https://api.dhan.co/v2/charts/intraday")
            .addHeader("Content-Type", "application/json")
            .addHeader("access-token", accessToken)
            .addHeader("client-id", clientId)
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()
        http.newCall(req).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw DhanApiException("Historical HTTP ${resp.code}: ${text.take(300)}")
            }
            val json = JSONObject(text)
            if (!json.has("timestamp")) {
                throw DhanApiException("Unexpected historical response: ${text.take(300)}")
            }
            return json
        }
    }
}

object TelegramClient {
    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    fun parseChatIds(raw: String): List<String> {
        return raw.replace(";", ",")
            .split(",", " ", "\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
    }

    fun send(botToken: String, chatIdRaw: String, message: String): Pair<Boolean, String> {
        if (botToken.isBlank()) return false to "Bot token missing"
        val ids = parseChatIds(chatIdRaw)
        if (ids.isEmpty()) return false to "Chat ID missing"
        var okCount = 0
        val errors = mutableListOf<String>()
        for (cid in ids) {
            try {
                val body = JSONObject()
                    .put("chat_id", cid)
                    .put("text", message)
                val req = Request.Builder()
                    .url("https://api.telegram.org/bot$botToken/sendMessage")
                    .post(body.toString().toRequestBody("application/json".toMediaType()))
                    .build()
                http.newCall(req).execute().use { resp ->
                    val text = resp.body?.string().orEmpty()
                    val json = JSONObject(text)
                    if (json.optBoolean("ok")) okCount++
                    else errors.add("$cid: ${text.take(120)}")
                }
            } catch (e: Exception) {
                errors.add("$cid: ${e.message}")
            }
        }
        return when {
            okCount > 0 && errors.isEmpty() -> true to "sent to $okCount chat(s)"
            okCount > 0 -> true to "sent to $okCount; failed: ${errors.joinToString()}"
            else -> false to errors.joinToString("; ").ifEmpty { "send failed" }
        }
    }
}
