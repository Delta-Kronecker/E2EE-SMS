package com.example.sms.crypto

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.Security
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.UUID
import javax.crypto.KeyAgreement

class KeyManager(private val context: Context) {

    init {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(BouncyCastleProvider())
        }
    }

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
                prefs.getString("x25519_public", "")!!
            )
        }

        val uuid = UUID.randomUUID().toString()

        // Generate X25519 key pair
        val x25519Kpg = KeyPairGenerator.getInstance("X25519", "BC")
        val x25519Kp = x25519Kpg.generateKeyPair()
        val x25519Private = Base64.encodeToString(
            x25519Kp.private.encoded, Base64.NO_WRAP
        )
        val x25519Public = Base64.encodeToString(
            x25519Kp.public.encoded, Base64.NO_WRAP
        )

        // Generate Ed25519 key pair
        val ed25519Kpg = KeyPairGenerator.getInstance("Ed25519", "BC")
        val ed25519Kp = ed25519Kpg.generateKeyPair()
        val ed25519Private = Base64.encodeToString(
            ed25519Kp.private.encoded, Base64.NO_WRAP
        )
        val ed25519Public = Base64.encodeToString(
            ed25519Kp.public.encoded, Base64.NO_WRAP
        )

        prefs.edit()
            .putString("uuid", uuid)
            .putString("x25519_private", x25519Private)
            .putString("x25519_public", x25519Public)
            .putString("ed25519_private", ed25519Private)
            .putString("ed25519_public", ed25519Public)
            .apply()

        return Triple(uuid, "", x25519Public)
    }

    fun saveName(name: String) {
        prefs.edit().putString("name", name).apply()
    }

    fun getUuid(): String = prefs.getString("uuid", "") ?: ""

    fun getName(): String = prefs.getString("name", "") ?: ""

    fun getX25519PublicKey(): String = prefs.getString("x25519_public", "") ?: ""

    fun getX25519PrivateKey(): String = prefs.getString("x25519_private", "") ?: ""

    fun computeSharedSecret(otherPublicKeyBase64: String): ByteArray {
        val kf = KeyFactory.getInstance("X25519", "BC")

        val myPrivateBytes = Base64.decode(getX25519PrivateKey(), Base64.NO_WRAP)
        val myPrivateKey = kf.generatePrivate(PKCS8EncodedKeySpec(myPrivateBytes))

        val otherPublicBytes = Base64.decode(otherPublicKeyBase64, Base64.NO_WRAP)
        val otherPublicKey = kf.generatePublic(X509EncodedKeySpec(otherPublicBytes))

        val ka = KeyAgreement.getInstance("X25519", "BC")
        ka.init(myPrivateKey)
        ka.doPhase(otherPublicKey, true)
        return ka.generateSecret()
    }
}
