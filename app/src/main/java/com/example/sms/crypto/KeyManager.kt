package com.example.sms.crypto

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.bouncycastle.crypto.agreement.X25519Agreement
import org.bouncycastle.crypto.generators.X25519KeyPairGenerator
import org.bouncycastle.crypto.params.X25519KeyGenerationParameters
import org.bouncycastle.crypto.params.X25519PrivateKeyParameters
import org.bouncycastle.crypto.params.X25519PublicKeyParameters
import java.security.SecureRandom
import java.util.UUID

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

        // Generate X25519 key pair using BC low-level API
        val generator = X25519KeyPairGenerator()
        generator.init(X25519KeyGenerationParameters(SecureRandom()))
        val keyPair = generator.generateKeyPair()

        val privateKey = keyPair.private as X25519PrivateKeyParameters
        val publicKey = keyPair.public as X25519PublicKeyParameters

        val privateBytes = Base64.encodeToString(privateKey.encoded, Base64.NO_WRAP)
        val publicBytes = Base64.encodeToString(publicKey.encoded, Base64.NO_WRAP)

        prefs.edit()
            .putString("uuid", uuid)
            .putString("x25519_private", privateBytes)
            .putString("x25519_public", publicBytes)
            .apply()

        return Triple(uuid, "", publicBytes)
    }

    fun saveName(name: String) {
        prefs.edit().putString("name", name).apply()
    }

    fun getUuid(): String = prefs.getString("uuid", "") ?: ""

    fun getName(): String = prefs.getString("name", "") ?: ""

    fun getX25519PublicKey(): String = prefs.getString("x25519_public", "") ?: ""

    fun getX25519PrivateKey(): String = prefs.getString("x25519_private", "") ?: ""

    fun computeSharedSecret(otherPublicKeyBase64: String): ByteArray {
        val myPrivateBytes = Base64.decode(getX25519PrivateKey(), Base64.NO_WRAP)
        val otherPublicBytes = Base64.decode(otherPublicKeyBase64, Base64.NO_WRAP)

        val privateKey = X25519PrivateKeyParameters(myPrivateBytes)
        val otherPublicKey = X25519PublicKeyParameters(otherPublicBytes)

        val agreement = X25519Agreement()
        agreement.init(privateKey)
        val secretBigInt = agreement.calculateAgreement(otherPublicKey)
        val bytes = secretBigInt.toByteArray()
        return if (bytes.size >= 32) bytes.copyOfRange(bytes.size - 32, bytes.size)
               else ByteArray(32 - bytes.size) + bytes
    }
}
