package com.example.sms.crypto

import android.util.Base64

object PairingManager {

    private const val PREFIX = "E2EE-SMS"
    private const val VERSION = "2"

    fun createPairingString(uuid: String, name: String, publicKey: String, phoneNumber: String): String {
        return "$PREFIX:$VERSION:$uuid:$name:$phoneNumber:$publicKey"
    }

    fun parsePairingString(pairing: String): PairingData? {
        try {
            val parts = pairing.split(":")
            if (parts.size != 6) return null
            if (parts[0] != PREFIX) return null
            if (parts[1] != VERSION) return null

            return PairingData(
                uuid = parts[2],
                name = parts[3],
                phoneNumber = parts[4],
                publicKey = parts[5]
            )
        } catch (e: Exception) {
            return null
        }
    }

    fun isValidPairingString(pairing: String): Boolean {
        return parsePairingString(pairing) != null
    }

    data class PairingData(
        val uuid: String,
        val name: String,
        val phoneNumber: String,
        val publicKey: String
    )
}
