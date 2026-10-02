package com.oisspike.detector.net

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.pow

object TotpUtil {
    /**
     * Generate 6-digit TOTP from Base32 secret (Google Authenticator style).
     */
    fun generate(secretBase32: String, timeSeconds: Long = System.currentTimeMillis() / 1000): String {
        val key = decodeBase32(secretBase32.replace(" ", "").uppercase())
        val counter = timeSeconds / 30
        val data = ByteArray(8)
        var c = counter
        for (i in 7 downTo 0) {
            data[i] = (c and 0xFF).toByte()
            c = c shr 8
        }
        val mac = Mac.getInstance("HmacSHA1")
        mac.init(SecretKeySpec(key, "HmacSHA1"))
        val hash = mac.doFinal(data)
        val offset = hash[hash.size - 1].toInt() and 0x0F
        val binary =
            ((hash[offset].toInt() and 0x7F) shl 24) or
                ((hash[offset + 1].toInt() and 0xFF) shl 16) or
                ((hash[offset + 2].toInt() and 0xFF) shl 8) or
                (hash[offset + 3].toInt() and 0xFF)
        val otp = binary % 1_000_000
        return otp.toString().padStart(6, '0')
    }

    fun secondsRemaining(timeSeconds: Long = System.currentTimeMillis() / 1000): Int =
        (30 - (timeSeconds % 30)).toInt()

    private fun decodeBase32(s: String): ByteArray {
        val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
        val cleaned = s.filter { it != '=' }
        val out = ArrayList<Byte>()
        var buffer = 0
        var bitsLeft = 0
        for (ch in cleaned) {
            val v = alphabet.indexOf(ch)
            if (v < 0) continue
            buffer = (buffer shl 5) or v
            bitsLeft += 5
            if (bitsLeft >= 8) {
                out.add(((buffer shr (bitsLeft - 8)) and 0xFF).toByte())
                bitsLeft -= 8
            }
        }
        return out.toByteArray()
    }
}
