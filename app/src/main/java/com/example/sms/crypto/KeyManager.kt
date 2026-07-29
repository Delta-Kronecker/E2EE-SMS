package com.example.sms.crypto

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.bouncycastle.crypto.generators.Ed25519KeyPairGenerator
import org.bouncycastle.crypto.params.Ed25519KeyGenerationParameters
import org.bouncycastle.crypto.signers.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.signers.Ed25519PublicKeyParameters
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.SecureRandom
import java.util.UUID
import javax.crypto.KeyAgreement
import java.security.KeyPairGenerator
import java.security.spec.ECGenParameterSpec
import java.util.Base64

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
                prefs.getString("x25519_public", "")!!
            )
        }

        val uuid = UUID.randomUUID().toString()

        // Generate X25519 key pair for key agreement
        val x25519Kpg = KeyPairGenerator.getInstance("X25519", "BC")
        val x25519Kp = x25519Kpg.generateKeyPair()
        val x25519Private = Base64.getEncoder().encodeToString(x25519Kp.private.encoded)
        val x25519Public = Base64.getEncoder().encodeToString(x25519Kp.public.encoded)

        // Generate Ed25519 key pair for identity signing
        val ed25519Generator = Ed25519KeyPairGenerator()
        ed25519Generator.init(Ed25519KeyGenerationParameters(SecureRandom()))
        val ed25519Kp = ed25519Generator.generateKeyPair()
        val ed25519Private = Base64.getEncoder().encodeToString(
            (ed25519Kp.private as Ed25519PrivateKeyParameters).encoded
        )
        val ed25519Public = Base64.getEncoder().encodeToString(
            (ed25519Kp.public as Ed25519PublicKeyParameters).encoded
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
        val myPrivateBytes = Base64.getDecoder().decode(getX25519PrivateKey())
        val otherPublicBytes = Base64.getDecoder().decode(otherPublicKeyBase64)

        val myPrivateKey = java.security.spec.XECPrivateKeySpec(
            java.security.spec.NamedParameterSpec.X25519,
            myPrivateBytes
        )
        val myKeyFactory = java.security.KeyFactory.getInstance("X25519")
        val myPrivateKeyObj = myKeyFactory.generatePrivate(myPrivateKey)

        val otherPublicKeySpec = java.security.spec.XECPublicKeySpec(
            java.security.spec.NamedParameterSpec.X25519,
            otherPublicBytes
        )
        val otherPublicKeyObj = myKeyFactory.generatePublic(otherPublicKeySpec)

        val ka = KeyAgreement.getInstance("X25519")
        ka.init(myPrivateKeyObj)
        ka.doPhase(otherPublicKeyObj, true)
        return ka.generateSecret()
    }
}
