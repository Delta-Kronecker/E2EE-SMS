package com.example.sms

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sms.crypto.KeyManager
import com.example.sms.db.AppDatabase
import com.example.sms.model.Contact
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var fab: FloatingActionButton
    private lateinit var toolbar: MaterialToolbar
    private val contacts = mutableListOf<Contact>()
    private lateinit var adapter: ContactAdapter
    private lateinit var keyManager: KeyManager
    private lateinit var db: AppDatabase

    companion object {
        private const val PERMISSION_REQUEST_SMS = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        keyManager = KeyManager(this)

        // Check if setup is complete
        if (!keyManager.hasKeys() || keyManager.getName().isEmpty()) {
            startActivity(Intent(this, SetupActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)

        db = AppDatabase.getDatabase(this)

        toolbar = findViewById(R.id.toolbar)
        toolbar.title = "E2EE-SMS"
        setSupportActionBar(toolbar)

        recyclerView = findViewById(R.id.recyclerView)
        fab = findViewById(R.id.fabCompose)

        adapter = ContactAdapter(contacts) { contact ->
            val intent = Intent(this, ChatActivity::class.java)
            intent.putExtra("contactUuid", contact.uuid)
            intent.putExtra("contactName", contact.name)
            startActivity(intent)
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        fab.setOnClickListener {
            val intent = Intent(this, PairActivity::class.java)
            startActivity(intent)
        }

        checkPermissions()
    }

    override fun onResume() {
        super.onResume()
        if (hasPermissions()) {
            loadContacts()
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
            loadContacts()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_SMS) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                loadContacts()
            } else {
                Toast.makeText(this, "SMS permissions are required", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun hasPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) ==
                PackageManager.PERMISSION_GRANTED
    }

    private fun loadContacts() {
        CoroutineScope(Dispatchers.IO).launch {
            db.contactDao().getAllContacts().collectLatest { contactList ->
                withContext(Dispatchers.Main) {
                    contacts.clear()
                    contacts.addAll(contactList)
                    adapter.notifyDataSetChanged()
                }
            }
        }
    }
}
