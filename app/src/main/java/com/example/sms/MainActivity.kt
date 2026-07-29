package com.example.sms

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.Telephony
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var fab: FloatingActionButton
    private val conversations = mutableListOf<Conversation>()
    private lateinit var adapter: ConversationAdapter

    companion object {
        private const val PERMISSION_REQUEST_SMS = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerView)
        fab = findViewById(R.id.fabCompose)

        adapter = ConversationAdapter(conversations) { conversation ->
            val intent = Intent(this, ChatActivity::class.java)
            intent.putExtra("address", conversation.address)
            startActivity(intent)
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        fab.setOnClickListener {
            val intent = Intent(this, ChatActivity::class.java)
            startActivity(intent)
        }

        checkPermissions()
    }

    override fun onResume() {
        super.onResume()
        if (hasPermissions()) {
            loadConversations()
        }
    }

    private fun checkPermissions() {
        val permissions = arrayOf(
            Manifest.permission.READ_SMS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_CONTACTS
        )

        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), PERMISSION_REQUEST_SMS)
        } else {
            loadConversations()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_SMS) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                loadConversations()
            } else {
                Toast.makeText(this, "SMS permission is required", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun hasPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) ==
                PackageManager.PERMISSION_GRANTED
    }

    private fun loadConversations() {
        conversations.clear()

        val uri: Uri = Telephony.Sms.CONTENT_URI
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.TYPE
        )

        val cursor: Cursor? = contentResolver.query(
            uri, projection, null, null,
            Telephony.Sms.DATE + " DESC"
        )

        val conversationMap = mutableMapOf<String, MutableList<SmsMessage>>()

        cursor?.use {
            val idIndex = it.getColumnIndex(Telephony.Sms._ID)
            val addressIndex = it.getColumnIndex(Telephony.Sms.ADDRESS)
            val bodyIndex = it.getColumnIndex(Telephony.Sms.BODY)
            val dateIndex = it.getColumnIndex(Telephony.Sms.DATE)
            val typeIndex = it.getColumnIndex(Telephony.Sms.TYPE)

            while (it.moveToNext()) {
                val id = it.getLong(idIndex)
                val address = it.getString(addressIndex) ?: "Unknown"
                val body = it.getString(bodyIndex) ?: ""
                val date = it.getLong(dateIndex)
                val type = it.getInt(typeIndex)

                val isSent = type == Telephony.Sms.MESSAGE_TYPE_SENT ||
                        type == Telephony.Sms.MESSAGE_TYPE_OUTBOX

                val message = SmsMessage(id, address, body, date, isSent)
                conversationMap.getOrPut(address) { mutableListOf() }.add(message)
            }
        }

        for ((address, messages) in conversationMap) {
            val lastMessage = messages.first()
            conversations.add(
                Conversation(
                    address = address,
                    lastMessage = lastMessage.body,
                    lastTimestamp = lastMessage.timestamp,
                    messageCount = messages.size
                )
            )
        }

        conversations.sortByDescending { it.lastTimestamp }
        adapter.notifyDataSetChanged()
    }
}
