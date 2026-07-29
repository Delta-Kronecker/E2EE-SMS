package com.example.sms

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.sms.crypto.KeyManager
import com.example.sms.crypto.PairingManager
import com.example.sms.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SetupActivity : AppCompatActivity() {

    private lateinit var etName: EditText
    private lateinit var btnStart: Button
    private lateinit var tvMnemonic: TextView
    private lateinit var btnCopyMnemonic: Button
    private lateinit var btnConfirmBackup: Button
    private lateinit var layoutBackup: android.widget.LinearLayout

    private lateinit var keyManager: KeyManager
    private var mnemonic: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        keyManager = KeyManager(this)

        // If already set up, go to MainActivity
        if (keyManager.hasKeys() && keyManager.getName().isNotEmpty()) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_setup)

        etName = findViewById(R.id.etName)
        btnStart = findViewById(R.id.btnStart)
        tvMnemonic = findViewById(R.id.tvMnemonic)
        btnCopyMnemonic = findViewById(R.id.btnCopyMnemonic)
        btnConfirmBackup = findViewById(R.id.btnConfirmBackup)
        layoutBackup = findViewById(R.id.layoutBackup)

        btnStart.setOnClickListener {
            val name = etName.text.toString().trim()
            if (name.isEmpty()) {
                etName.error = "Enter your name"
                return@setOnClickListener
            }

            // Generate keys
            val (uuid, _, _) = keyManager.getOrCreateIdentity()
            keyManager.saveName(name)

            // Show backup mnemonic
            mnemonic = generateMnemonic(uuid)
            tvMnemonic.text = mnemonic
            layoutBackup.visibility = android.view.View.VISIBLE
            btnStart.visibility = android.view.View.GONE
        }

        btnCopyMnemonic.setOnClickListener {
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("E2EE Backup", mnemonic)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Backup phrase copied", Toast.LENGTH_SHORT).show()
        }

        btnConfirmBackup.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun generateMnemonic(uuid: String): String {
        // Simple mnemonic generation from UUID for backup
        val words = listOf(
            "alpha", "bravo", "charlie", "delta", "echo", "foxtrot",
            "golf", "hotel", "india", "juliet", "kilo", "lima",
            "mike", "november", "oscar", "papa", "quebec", "romeo",
            "sierra", "tango", "uniform", "victor", "whiskey", "xray",
            "yankee", "zulu", "apple", "banana", "cherry", "dragon",
            "eagle", "falcon", "griffin", "hawk", "ivory", "jade",
            "karma", "lemon", "mango", "nectar", "ocean", "pearl",
            "quartz", "river", "storm", "tiger", "ultra", "viper",
            "winter", "xenon", "yellow", "zenith"
        )
        val hash = uuid.hashCode()
        val selectedWords = mutableListOf<String>()
        for (i in 0 until 12) {
            val index = ((hash shr (i * 2)) and 0x7FFFFFFF) % words.size
            selectedWords.add(words[index])
        }
        return selectedWords.joinToString(" ")
    }
}
