package com.oisspike.detector.net

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object DhanAuth {
    private const val AUTH_URL = "https://auth.dhan.co/app/generateAccessToken"
    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    data class TokenResult(
        val ok: Boolean,
        val accessToken: String = "",
        val clientId: String = "",
        val expiryTime: String = "",
        val message: String = "",
    )

    /**
     * POST https://auth.dhan.co/app/generateAccessToken?dhanClientId=&pin=&totp=
     * Requires TOTP enabled on Dhan account.
     */
    fun generateAccessToken(clientId: String, pin: String, totp: String): TokenResult {
        val cid = clientId.trim()
        val p = pin.trim()
        val t = totp.trim()
        if (cid.isEmpty() || p.isEmpty() || t.isEmpty()) {
            return TokenResult(false, message = "Client ID, PIN and TOTP are required")
        }
        val url = "$AUTH_URL?dhanClientId=${enc(cid)}&pin=${enc(p)}&totp=${enc(t)}"
        return try {
            val empty = okhttp3.RequestBody.create(okhttp3.MediaType.parse("application/json"), "")
            val req = Request.Builder().url(url).post(empty).build()
            http.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    return TokenResult(false, message = "HTTP ${resp.code}: ${body.take(200)}")
                }
                val json = JSONObject(body)
                val token = json.optString("accessToken", "")
                if (token.isBlank()) {
                    return TokenResult(false, message = "No accessToken in response: ${body.take(200)}")
                }
                TokenResult(
                    ok = true,
                    accessToken = token,
                    clientId = json.optString("dhanClientId", cid),
                    expiryTime = json.optString("expiryTime", ""),
                    message = "Token OK until ${json.optString("expiryTime", "?")}",
                )
            }
        } catch (e: Exception) {
            TokenResult(false, message = e.message ?: "Auth failed")
        }
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
}
