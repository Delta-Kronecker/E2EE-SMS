package com.example.sms.crypto

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.spec.ECGenParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.UUID
import javax.crypto.KeyAgreement

class KeyManager(private val context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "e2ee_keys",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun hasKeys(): Boolean = prefs.contains("uuid")

    fun getOrCreateIdentity(): Triple<String, String, String> {
        val existingUuid = prefs.getString("uuid", null)
        if (existingUuid != null) {
            return Triple(
                existingUuid,
                prefs.getString("name", "")!!,
                prefs.getString("ec_public", "")!!
            )
        }

        val uuid = UUID.randomUUID().toString()

        val kpg = KeyPairGenerator.getInstance("EC")
        kpg.initialize(ECGenParameterSpec("secp256r1"), SecureRandom())
        val keyPair = kpg.generateKeyPair()

        val privateBytes = Base64.encodeToString(keyPair.private.encoded, Base64.NO_WRAP)
        val publicBytes = Base64.encodeToString(keyPair.public.encoded, Base64.NO_WRAP)

        prefs.edit()
            .putString("uuid", uuid)
            .putString("ec_private", privateBytes)
            .putString("ec_public", publicBytes)
            .apply()

        return Triple(uuid, "", publicBytes)
    }

    fun saveName(name: String) {
        prefs.edit().putString("name", name).apply()
    }

    fun savePhoneNumber(phone: String) {
        prefs.edit().putString("phone", phone).apply()
    }

    fun getUuid(): String = prefs.getString("uuid", "") ?: ""

    fun getName(): String = prefs.getString("name", "") ?: ""

    fun getPhoneNumber(): String = prefs.getString("phone", "") ?: ""

    fun getPublicKey(): String = prefs.getString("ec_public", "") ?: ""

    fun computeSharedSecret(otherPublicKeyBase64: String): ByteArray {
        val kf = KeyFactory.getInstance("EC")

        val myPrivateBytes = Base64.decode(prefs.getString("ec_private", "")!!, Base64.NO_WRAP)
        val myPrivateKey = kf.generatePrivate(PKCS8EncodedKeySpec(myPrivateBytes))

        val otherPublicBytes = Base64.decode(otherPublicKeyBase64, Base64.NO_WRAP)
        val otherPublicKey = kf.generatePublic(X509EncodedKeySpec(otherPublicBytes))

        val ka = KeyAgreement.getInstance("ECDH")
        ka.init(myPrivateKey)
        ka.doPhase(otherPublicKey, true)
        return ka.generateSecret()
    }
}
