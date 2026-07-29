package com.example.sms.crypto

import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.SecretKeySpec
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.security.SecureRandom

object CryptoEngine {

    private const val GCM_TAG_LENGTH = 128
    private const val GCM_IV_LENGTH = 12

    fun encrypt(sharedSecret: ByteArray, plaintext: String): Triple<String, String, String> {
        // Derive message key using HKDF-like construction
        val messageKey = deriveKey(sharedSecret)

        // Generate random IV
        val iv = ByteArray(GCM_IV_LENGTH)
        SecureRandom().nextBytes(iv)

        // Encrypt with AES-256-GCM
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val keySpec = SecretKeySpec(messageKey, "AES")
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)

        val ciphertextBytes = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

        return Triple(
            Base64.encodeToString(iv, Base64.NO_WRAP),
            Base64.encodeToString(ciphertextBytes, Base64.NO_WRAP),
            deriveMac(sharedSecret, ciphertextBytes)
        )
    }

    fun decrypt(sharedSecret: ByteArray, ivBase64: String, ciphertextBase64: String): String? {
        return try {
            val messageKey = deriveKey(sharedSecret)
            val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
            val ciphertext = Base64.decode(ciphertextBase64, Base64.NO_WRAP)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val keySpec = SecretKeySpec(messageKey, "AES")
            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec)

            val plaintextBytes = cipher.doFinal(ciphertext)
            String(plaintextBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }

    private fun deriveKey(sharedSecret: ByteArray): ByteArray {
        // HMAC-SHA256 based key derivation
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec("E2EE-SMS-KEY-DERIVATION".toByteArray(), "HmacSHA256"))
        return mac.doFinal(sharedSecret)
    }

    private fun deriveMac(sharedSecret: ByteArray, ciphertext: ByteArray): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(sharedSecret, "HmacSHA256"))
        val hash = mac.doFinal(ciphertext)
        return Base64.encodeToString(hash, Base64.NO_WRAP)
    }

    fun formatEncryptedSms(
        senderUuid: String,
        iv: String,
        ciphertext: String,
        mac: String
    ): String {
        return "E2EE:$senderUuid:$iv:$ciphertext:$mac"
    }

    fun parseEncryptedSms(body: String): EncryptedSmsData? {
        if (!body.startsWith("E2EE:")) return null
        val parts = body.removePrefix("E2EE:").split(":")
        if (parts.size != 4) return null
        return EncryptedSmsData(
            senderUuid = parts[0],
            iv = parts[1],
            ciphertext = parts[2],
            mac = parts[3]
        )
    }

    data class EncryptedSmsData(
        val senderUuid: String,
        val iv: String,
        val ciphertext: String,
        val mac: String
    )
}
