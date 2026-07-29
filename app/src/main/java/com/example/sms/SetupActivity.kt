package com.example.sms

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.sms.crypto.KeyManager

class SetupActivity : AppCompatActivity() {

    private lateinit var etName: EditText
    private lateinit var spinnerCountryCode: Spinner
    private lateinit var etPhone: EditText
    private lateinit var btnStart: Button
    private lateinit var tvMnemonic: TextView
    private lateinit var btnCopyMnemonic: Button
    private lateinit var btnConfirmBackup: Button
    private lateinit var layoutBackup: android.widget.LinearLayout

    private lateinit var keyManager: KeyManager
    private var mnemonic: String = ""

    private val countryCodes = arrayOf(
        "+98 - Iran",
        "+1 - USA",
        "+44 - UK",
        "+49 - Germany",
        "+33 - France",
        "+39 - Italy",
        "+34 - Spain",
        "+7 - Russia",
        "+86 - China",
        "+91 - India",
        "+81 - Japan",
        "+82 - South Korea",
        "+55 - Brazil",
        "+61 - Australia",
        "+27 - South Africa",
        "+90 - Turkey",
        "+966 - Saudi Arabia",
        "+971 - UAE",
        "+20 - Egypt",
        "+234 - Nigeria"
    )

    private val countryCodeValues = arrayOf(
        "+98", "+1", "+44", "+49", "+33", "+39", "+34", "+7", "+86", "+91",
        "+81", "+82", "+55", "+61", "+27", "+90", "+966", "+971", "+20", "+234"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        keyManager = KeyManager(this)

        if (keyManager.hasKeys() && keyManager.getName().isNotEmpty()) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_setup)

        etName = findViewById(R.id.etName)
        spinnerCountryCode = findViewById(R.id.spinnerCountryCode)
        etPhone = findViewById(R.id.etPhone)
        btnStart = findViewById(R.id.btnStart)
        tvMnemonic = findViewById(R.id.tvMnemonic)
        btnCopyMnemonic = findViewById(R.id.btnCopyMnemonic)
        btnConfirmBackup = findViewById(R.id.btnConfirmBackup)
        layoutBackup = findViewById(R.id.layoutBackup)

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, countryCodes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCountryCode.adapter = adapter
        spinnerCountryCode.setSelection(0)

        btnStart.setOnClickListener {
            val name = etName.text.toString().trim()
            val phone = etPhone.text.toString().trim()
            val countryCode = countryCodeValues[spinnerCountryCode.selectedItemPosition]

            if (name.isEmpty()) {
                etName.error = "Enter your name"
                return@setOnClickListener
            }
            if (phone.isEmpty()) {
                etPhone.error = "Enter your phone number"
                return@setOnClickListener
            }

            val fullPhone = "$countryCode$phone"

            val (uuid, _, _) = keyManager.getOrCreateIdentity()
            keyManager.saveName(name)
            keyManager.savePhoneNumber(fullPhone)

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
