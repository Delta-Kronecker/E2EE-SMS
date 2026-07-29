package com.example.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.widget.Toast

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)

            for (sms in messages) {
                val sender = sms.displayOriginatingAddress
                val body = sms.displayMessageBody
                val timestamp = sms.timestampMillis

                Toast.makeText(
                    context,
                    "پیام جدید از $sender:\n$body",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
