package com.example.sms

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.sms.crypto.KeyManager
import com.example.sms.crypto.PairingManager
import com.example.sms.db.AppDatabase
import com.example.sms.model.Contact
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PairActivity : AppCompatActivity() {

    private lateinit var keyManager: KeyManager
    private lateinit var db: AppDatabase
    private lateinit var tabLayout: TabLayout
    private lateinit var layoutShow: android.widget.LinearLayout
    private lateinit var layoutImport: android.widget.LinearLayout
    private lateinit var tvMyPairing: TextView
    private lateinit var btnCopy: Button
    private lateinit var etPairingInput: EditText
    private lateinit var btnImport: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pair)

        keyManager = KeyManager(this)
        db = AppDatabase.getDatabase(this)

        tabLayout = findViewById(R.id.tabLayout)
        layoutShow = findViewById(R.id.layoutShow)
        layoutImport = findViewById(R.id.layoutImport)
        tvMyPairing = findViewById(R.id.tvMyPairing)
        btnCopy = findViewById(R.id.btnCopy)
        etPairingInput = findViewById(R.id.etPairingInput)
        btnImport = findViewById(R.id.btnImport)

        val uuid = keyManager.getUuid()
        val name = keyManager.getName()
        val publicKey = keyManager.getPublicKey()
        val phoneNumber = keyManager.getPhoneNumber()
        val pairingString = PairingManager.createPairingString(uuid, name, publicKey, phoneNumber)
        tvMyPairing.text = pairingString

        btnCopy.setOnClickListener {
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Pairing String", pairingString)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Pairing string copied", Toast.LENGTH_SHORT).show()
        }

        btnImport.setOnClickListener {
            val input = etPairingInput.text.toString().trim()
            if (input.isEmpty()) {
                etPairingInput.error = "Enter pairing string"
                return@setOnClickListener
            }

            val data = PairingManager.parsePairingString(input)
            if (data == null) {
                Toast.makeText(this, "Invalid pairing string", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (data.uuid == keyManager.getUuid()) {
                Toast.makeText(this, "Cannot pair with yourself", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val sharedSecret = keyManager.computeSharedSecret(data.publicKey)

            CoroutineScope(Dispatchers.IO).launch {
                db.contactDao().insertContact(
                    Contact(
                        uuid = data.uuid,
                        name = data.name,
                        publicKey = data.publicKey,
                        phoneNumber = data.phoneNumber
                    )
                )

                val sharedPrefs = getSharedPreferences("shared_secrets", MODE_PRIVATE)
                sharedPrefs.edit().putString(
                    data.uuid,
                    android.util.Base64.encodeToString(sharedSecret, android.util.Base64.NO_WRAP)
                ).apply()

                runOnUiThread {
                    Toast.makeText(this@PairActivity, "Paired with ${data.name}!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> {
                        layoutShow.visibility = android.view.View.VISIBLE
                        layoutImport.visibility = android.view.View.GONE
                    }
                    1 -> {
                        layoutShow.visibility = android.view.View.GONE
                        layoutImport.visibility = android.view.View.VISIBLE
                    }
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    fun getSharedSecret(contactUuid: String): ByteArray? {
        val sharedPrefs = getSharedPreferences("shared_secrets", MODE_PRIVATE)
        val encoded = sharedPrefs.getString(contactUuid, null) ?: return null
        return android.util.Base64.decode(encoded, android.util.Base64.NO_WRAP)
    }
}
