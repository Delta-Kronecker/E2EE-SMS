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
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sms.crypto.CryptoEngine
import com.example.sms.crypto.KeyManager
import com.example.sms.db.AppDatabase
import com.example.sms.model.Message
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChatActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var recyclerView: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: ImageButton
    private lateinit var tvEmpty: TextView
    private val messages = mutableListOf<Message>()
    private lateinit var chatAdapter: ChatMessageAdapter
    private var contactUuid: String = ""
    private var contactName: String = ""
    private lateinit var keyManager: KeyManager
    private lateinit var db: AppDatabase

    companion object {
        private const val PERMISSION_REQUEST_SEND_SMS = 101
        private const val SMS_SENT_ACTION = "SMS_SENT_ACTION"
        private const val SMS_DELIVERED_ACTION = "SMS_DELIVERED_ACTION"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        keyManager = KeyManager(this)
        db = AppDatabase.getDatabase(this)

        toolbar = findViewById(R.id.toolbar)
        recyclerView = findViewById(R.id.chatRecyclerView)
        etMessage = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)
        tvEmpty = findViewById(R.id.tvEmpty)

        contactUuid = intent.getStringExtra("contactUuid") ?: ""
        contactName = intent.getStringExtra("contactName") ?: "Unknown"

        toolbar.title = contactName
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        chatAdapter = ChatMessageAdapter(messages, keyManager.getUuid())
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

            if (contactUuid.isEmpty()) {
                Toast.makeText(this, "No contact selected", Toast.LENGTH_SHORT).show()
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
                sendMessage(messageText)
            }
        }

        if (contactUuid.isNotEmpty()) {
            loadMessages()
        } else {
            tvEmpty.text = "Select a contact to start chatting"
            tvEmpty.visibility = TextView.VISIBLE
            recyclerView.visibility = RecyclerView.GONE
        }
    }

    private fun loadMessages() {
        CoroutineScope(Dispatchers.IO).launch {
            db.messageDao().getMessagesForContact(contactUuid).collectLatest { messageList ->
                withContext(Dispatchers.Main) {
                    messages.clear()
                    messages.addAll(messageList)
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
            }
        }
    }

    private fun sendMessage(plaintext: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val contact = db.contactDao().getContactByUuid(contactUuid)
            if (contact == null) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ChatActivity, "Contact not found", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            val sharedPrefs = getSharedPreferences("shared_secrets", MODE_PRIVATE)
            val encodedSecret = sharedPrefs.getString(contactUuid, null)
            if (encodedSecret == null) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ChatActivity, "No shared key. Re-pair required.", Toast.LENGTH_LONG).show()
                }
                return@launch
            }

            val sharedSecret = android.util.Base64.decode(encodedSecret, android.util.Base64.NO_WRAP)

            val (iv, ciphertext, mac) = CryptoEngine.encrypt(sharedSecret, plaintext)
            val encryptedSms = CryptoEngine.formatEncryptedSms(keyManager.getUuid(), iv, ciphertext, mac)

            val messageId = db.messageDao().insertMessage(
                Message(
                    senderUuid = keyManager.getUuid(),
                    recipientUuid = contactUuid,
                    plaintext = plaintext,
                    isSent = true,
                    deliveryStatus = 0
                )
            )

            withContext(Dispatchers.Main) {
                etMessage.text.clear()
                sendSmsByPhoneNumber(contact.phoneNumber, encryptedSms, messageId)
            }
        }
    }

    private fun sendSmsByPhoneNumber(phoneNumber: String, message: String, messageId: Long) {
        if (phoneNumber.isEmpty()) {
            Toast.makeText(this, "No phone number for this contact", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }

            val sentIntent = PendingIntent.getBroadcast(
                this, messageId.toInt(), Intent(SMS_SENT_ACTION),
                PendingIntent.FLAG_MUTABLE
            )

            val deliveredIntent = PendingIntent.getBroadcast(
                this, messageId.toInt() + 100000, Intent(SMS_DELIVERED_ACTION),
                PendingIntent.FLAG_MUTABLE
            )

            val parts = smsManager.divideMessage(message)
            val sentIntents = ArrayList(parts.mapIndexed { index, _ ->
                PendingIntent.getBroadcast(
                    this, messageId.toInt() + index, Intent(SMS_SENT_ACTION),
                    PendingIntent.FLAG_MUTABLE
                )
            })
            val deliveredIntents = ArrayList(parts.mapIndexed { index, _ ->
                PendingIntent.getBroadcast(
                    this, messageId.toInt() + 100000 + index, Intent(SMS_DELIVERED_ACTION),
                    PendingIntent.FLAG_MUTABLE
                )
            })

            smsManager.sendMultipartTextMessage(phoneNumber, null, parts, sentIntents, deliveredIntents)

            val sentReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    CoroutineScope(Dispatchers.IO).launch {
                        if (resultCode == RESULT_OK) {
                            db.messageDao().updateDeliveryStatus(messageId, 1)
                        } else {
                            db.messageDao().updateDeliveryStatus(messageId, 3)
                        }
                    }
                }
            }

            val deliveredReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    CoroutineScope(Dispatchers.IO).launch {
                        if (resultCode == RESULT_OK) {
                            db.messageDao().updateDeliveryStatus(messageId, 2)
                        }
                    }
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
                if (messageText.isNotEmpty() && contactUuid.isNotEmpty()) {
                    sendMessage(messageText)
                }
            } else {
                Toast.makeText(this, "Send SMS permission required", Toast.LENGTH_LONG).show()
            }
        }
    }
}
