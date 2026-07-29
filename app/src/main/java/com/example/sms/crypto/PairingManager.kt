package com.example.sms.crypto

import android.util.Base64

object PairingManager {

    private const val PREFIX = "E2EE-SMS"
    private const val VERSION = "1"

    fun createPairingString(uuid: String, name: String, publicKey: String): String {
        return "$PREFIX:$VERSION:$uuid:$name:$publicKey"
    }

    fun parsePairingString(pairing: String): PairingData? {
        try {
            val parts = pairing.split(":")
            if (parts.size != 5) return null
            if (parts[0] != PREFIX) return null
            if (parts[1] != VERSION) return null

            return PairingData(
                uuid = parts[2],
                name = parts[3],
                publicKey = parts[4]
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
        val publicKey: String
    )
}
