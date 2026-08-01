package com.example.sms

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.sms.crypto.KeyManager

class SetupActivity : AppCompatActivity() {

    private lateinit var etName: EditText
    private lateinit var spinnerCountryCode: AutoCompleteTextView
    private lateinit var etPhone: EditText
    private lateinit var btnStart: Button
    private lateinit var keyManager: KeyManager

    private val countryCodes = arrayOf(
        "+98  Iran",
        "+1  USA",
        "+44  UK",
        "+49  Germany",
        "+33  France",
        "+39  Italy",
        "+34  Spain",
        "+7  Russia",
        "+86  China",
        "+91  India",
        "+81  Japan",
        "+82  South Korea",
        "+55  Brazil",
        "+61  Australia",
        "+27  South Africa",
        "+90  Turkey",
        "+966  Saudi Arabia",
        "+971  UAE",
        "+20  Egypt",
        "+234  Nigeria"
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

        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, countryCodes)
        spinnerCountryCode.setAdapter(adapter)
        spinnerCountryCode.setText(countryCodes[0], false)

        spinnerCountryCode.setOnItemClickListener { _, _, position, _ ->
            spinnerCountryCode.setText(countryCodes[position], false)
        }

        btnStart.setOnClickListener {
            val name = etName.text.toString().trim()
            val phone = etPhone.text.toString().trim()

            if (name.isEmpty()) {
                etName.error = "Enter your name"
                return@setOnClickListener
            }
            if (phone.isEmpty()) {
                etPhone.error = "Enter your phone number"
                return@setOnClickListener
            }

            val selectedText = spinnerCountryCode.text.toString()
            val codeIndex = countryCodes.indexOf(selectedText)
            val countryCode = if (codeIndex >= 0) countryCodeValues[codeIndex] else "+98"
            val fullPhone = "$countryCode$phone"

            keyManager.getOrCreateIdentity()
            keyManager.saveName(name)
            keyManager.savePhoneNumber(fullPhone)

            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}
