package com.example.sms

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Telephony
import android.telephony.SmsManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar

class ChatActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var recyclerView: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: ImageButton
    private lateinit var tvEmpty: TextView
    private val messages = mutableListOf<SmsMessage>()
    private lateinit var chatAdapter: ChatAdapter
    private var address: String = ""

    companion object {
        private const val PERMISSION_REQUEST_SEND_SMS = 101
        private const val SMS_SENT_ACTION = "SMS_SENT"
        private const val SMS_DELIVERED_ACTION = "SMS_DELIVERED"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        toolbar = findViewById(R.id.toolbar)
        recyclerView = findViewById(R.id.chatRecyclerView)
        etMessage = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)
        tvEmpty = findViewById(R.id.tvEmpty)

        address = intent.getStringExtra("address") ?: ""

        toolbar.title = address
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        chatAdapter = ChatAdapter(messages)
        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = chatAdapter

        btnSend.setOnClickListener {
            val messageText = etMessage.text.toString().trim()
            if (messageText.isEmpty()) {
                etMessage.error = "Enter a message"
                return@setOnClickListener
            }

            val targetAddress = address.ifEmpty {
                etMessage.text.toString().trim()
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
                sendSms(targetAddress, messageText)
            }
        }

        if (address.isNotEmpty()) {
            loadMessages()
        } else {
            tvEmpty.text = "Enter a phone number to start chatting"
            tvEmpty.visibility = TextView.VISIBLE
            recyclerView.visibility = RecyclerView.GONE
        }
    }

    private fun loadMessages() {
        messages.clear()

        val uri: Uri = Telephony.Sms.CONTENT_URI
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.TYPE
        )
        val selection = "${Telephony.Sms.ADDRESS} = ?"
        val selectionArgs = arrayOf(address)

        val cursor: Cursor? = contentResolver.query(
            uri, projection, selection, selectionArgs,
            Telephony.Sms.DATE + " ASC"
        )

        cursor?.use {
            val idIndex = it.getColumnIndex(Telephony.Sms._ID)
            val bodyIndex = it.getColumnIndex(Telephony.Sms.BODY)
            val dateIndex = it.getColumnIndex(Telephony.Sms.DATE)
            val typeIndex = it.getColumnIndex(Telephony.Sms.TYPE)

            while (it.moveToNext()) {
                val id = it.getLong(idIndex)
                val body = it.getString(bodyIndex) ?: ""
                val date = it.getLong(dateIndex)
                val type = it.getInt(typeIndex)

                val isSent = type == Telephony.Sms.MESSAGE_TYPE_SENT ||
                        type == Telephony.Sms.MESSAGE_TYPE_OUTBOX

                messages.add(SmsMessage(id, address, body, date, isSent))
            }
        }

        chatAdapter.notifyDataSetChanged()

        if (messages.isNotEmpty()) {
            tvEmpty.visibility = TextView.GONE
            recyclerView.visibility = RecyclerView.VISIBLE
            recyclerView.scrollToPosition(messages.size - 1)
        } else {
            tvEmpty.text = "No messages yet"
            tvEmpty.visibility = TextView.VISIBLE
            recyclerView.visibility = RecyclerView.GONE
        }
    }

    private fun sendSms(targetAddress: String, message: String) {
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

            smsManager.sendTextMessage(targetAddress, null, message, sentIntent, deliveredIntent)

            val sentReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    if (resultCode == RESULT_OK) {
                        etMessage.text.clear()
                        loadMessages()
                    } else {
                        Toast.makeText(this@ChatActivity, "Failed to send message", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            val deliveredReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    Toast.makeText(this@ChatActivity, "Message delivered", Toast.LENGTH_SHORT).show()
                }
            }

            registerReceiver(sentReceiver, IntentFilter(SMS_SENT_ACTION), RECEIVER_NOT_EXPORTED)
            registerReceiver(deliveredReceiver, IntentFilter(SMS_DELIVERED_ACTION), RECEIVER_NOT_EXPORTED)

        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_SEND_SMS) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                val messageText = etMessage.text.toString().trim()
                if (messageText.isNotEmpty() && address.isNotEmpty()) {
                    sendSms(address, messageText)
                }
            } else {
                Toast.makeText(this, "Send SMS permission is required", Toast.LENGTH_LONG).show()
            }
        }
    }
}
