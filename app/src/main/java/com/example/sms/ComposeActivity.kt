package com.example.sms

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.telephony.SmsManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class ComposeActivity : AppCompatActivity() {

    private lateinit var etAddress: EditText
    private lateinit var etMessage: EditText
    private lateinit var btnSend: Button

    companion object {
        private const val PERMISSION_REQUEST_SEND_SMS = 101
        private const val SMS_SENT_ACTION = "SMS_SENT"
        private const val SMS_DELIVERED_ACTION = "SMS_DELIVERED"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_compose)

        etAddress = findViewById(R.id.etAddress)
        etMessage = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)

        val address = intent.getStringExtra("address")
        if (!address.isNullOrEmpty()) {
            etAddress.setText(address)
        }

        btnSend.setOnClickListener {
            val addressText = etAddress.text.toString().trim()
            val messageText = etMessage.text.toString().trim()

            if (addressText.isEmpty()) {
                etAddress.error = "شماره تماس را وارد کنید"
                return@setOnClickListener
            }

            if (messageText.isEmpty()) {
                etMessage.error = "پیام را وارد کنید"
                return@setOnClickListener
            }

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.SEND_SMS),
                    PERMISSION_REQUEST_SEND_SMS
                )
            } else {
                sendSms(addressText, messageText)
            }
        }
    }

    private fun sendSms(address: String, message: String) {
        try {
            val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }

            val sentIntent = PendingIntent.getBroadcast(
                this, 0, Intent(SMS_SENT_ACTION),
                PendingIntent.FLAG_IMMUTABLE
            )

            val deliveredIntent = PendingIntent.getBroadcast(
                this, 0, Intent(SMS_DELIVERED_ACTION),
                PendingIntent.FLAG_IMMUTABLE
            )

            smsManager.sendTextMessage(address, null, message, sentIntent, deliveredIntent)

            val sentReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    val result = resultCode
                    if (result == RESULT_OK) {
                        Toast.makeText(this@ComposeActivity, "پیام ارسال شد", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this@ComposeActivity, "ارسال پیام ناموفق بود", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            val deliveredReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    Toast.makeText(this@ComposeActivity, "پیام تحویل داده شد", Toast.LENGTH_SHORT).show()
                }
            }

            registerReceiver(sentReceiver, IntentFilter(SMS_SENT_ACTION), RECEIVER_NOT_EXPORTED)
            registerReceiver(deliveredReceiver, IntentFilter(SMS_DELIVERED_ACTION), RECEIVER_NOT_EXPORTED)

        } catch (e: Exception) {
            Toast.makeText(this, "خطا در ارسال پیام: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_SEND_SMS) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                val address = etAddress.text.toString().trim()
                val message = etMessage.text.toString().trim()
                sendSms(address, message)
            } else {
                Toast.makeText(this, "مجوز ارسال SMS لازم است", Toast.LENGTH_LONG).show()
            }
        }
    }
}
