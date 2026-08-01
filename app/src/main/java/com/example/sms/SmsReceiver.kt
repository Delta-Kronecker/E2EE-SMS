package com.example.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Base64
import android.widget.Toast
import com.example.sms.crypto.CryptoEngine
import com.example.sms.crypto.KeyManager
import com.example.sms.db.AppDatabase
import com.example.sms.model.Message
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent?.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val keyManager = KeyManager(context)
        val db = AppDatabase.getDatabase(context)

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val senderBodies = mutableMapOf<String, StringBuilder>()

        for (sms in messages) {
            val sender = sms.displayOriginatingAddress ?: continue
            val body = sms.displayMessageBody ?: continue
            senderBodies.getOrPut(sender) { StringBuilder() }.append(body)
        }

        for ((sender, fullBodyBuilder) in senderBodies) {
            val fullBody = fullBodyBuilder.toString()

            val encryptedData = CryptoEngine.parseEncryptedSms(fullBody)
            if (encryptedData != null) {
                CoroutineScope(Dispatchers.IO).launch {
                    val contact = db.contactDao().getContactByUuid(encryptedData.senderUuid)
                    if (contact != null) {
                        val sharedPrefs = context.getSharedPreferences("shared_secrets", Context.MODE_PRIVATE)
                        val encodedSecret = sharedPrefs.getString(encryptedData.senderUuid, null)

                        if (encodedSecret != null) {
                            val sharedSecret = Base64.decode(encodedSecret, Base64.NO_WRAP)

                            val plaintext = CryptoEngine.decrypt(
                                sharedSecret,
                                encryptedData.iv,
                                encryptedData.ciphertext
                            )

                            if (plaintext != null) {
                                db.messageDao().insertMessage(
                                    Message(
                                        senderUuid = encryptedData.senderUuid,
                                        recipientUuid = keyManager.getUuid(),
                                        plaintext = plaintext,
                                        isSent = false
                                    )
                                )

                                CoroutineScope(Dispatchers.Main).launch {
                                    Toast.makeText(
                                        context,
                                        "New encrypted message from ${contact.name}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            } else {
                                CoroutineScope(Dispatchers.Main).launch {
                                    Toast.makeText(
                                        context,
                                        "Failed to decrypt message from ${contact.name}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    } else {
                        CoroutineScope(Dispatchers.Main).launch {
                            Toast.makeText(
                                context,
                                "Encrypted message from unknown sender",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            } else {
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(
                        context,
                        "New message from $sender:\n$fullBody",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}
