package com.example.sms.crypto

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object PairingManager {

    private const val PREFIX = "E2EE-SMS"
    private const val VERSION = "3"
    private const val GCM_TAG_LENGTH = 128
    private const val GCM_IV_LENGTH = 12

    fun createPairingString(uuid: String, name: String, publicKey: String, phoneNumber: String): String {
        val payload = "$uuid:$name:$phoneNumber"
        val encryptedPayload = encryptPayload(payload, publicKey)
        return "$PREFIX:$VERSION:$encryptedPayload:$publicKey"
    }

    fun parsePairingString(pairing: String): PairingData? {
        try {
            val parts = pairing.split(":")
            if (parts.size != 4) return null
            if (parts[0] != PREFIX) return null
            if (parts[1] != VERSION) return null

            val encryptedPayload = parts[2]
            val publicKey = parts[3]

            val decryptedPayload = decryptPayload(encryptedPayload, publicKey) ?: return null
            val payloadParts = decryptedPayload.split(":")
            if (payloadParts.size != 3) return null

            return PairingData(
                uuid = payloadParts[0],
                name = payloadParts[1],
                phoneNumber = payloadParts[2],
                publicKey = publicKey
            )
        } catch (e: Exception) {
            return null
        }
    }

    fun isValidPairingString(pairing: String): Boolean {
        return parsePairingString(pairing) != null
    }

    private fun deriveKey(publicKey: String): ByteArray {
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(publicKey.toByteArray(Charsets.UTF_8))
    }

    private fun encryptPayload(payload: String, publicKey: String): String {
        val key = deriveKey(publicKey)
        val iv = ByteArray(GCM_IV_LENGTH)
        SecureRandom().nextBytes(iv)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val keySpec = SecretKeySpec(key, "AES")
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)

        val ciphertext = cipher.doFinal(payload.toByteArray(Charsets.UTF_8))
        val combined = iv + ciphertext
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    private fun decryptPayload(encryptedBase64: String, publicKey: String): String? {
        return try {
            val key = deriveKey(publicKey)
            val combined = Base64.decode(encryptedBase64, Base64.NO_WRAP)

            val iv = combined.sliceArray(0 until GCM_IV_LENGTH)
            val ciphertext = combined.sliceArray(GCM_IV_LENGTH until combined.size)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val keySpec = SecretKeySpec(key, "AES")
            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec)

            val plaintext = cipher.doFinal(ciphertext)
            String(plaintext, Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }

    data class PairingData(
        val uuid: String,
        val name: String,
        val phoneNumber: String,
        val publicKey: String
    )
}
