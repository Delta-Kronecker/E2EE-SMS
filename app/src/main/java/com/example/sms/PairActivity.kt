package com.example.sms

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.sms.crypto.KeyManager
import com.example.sms.crypto.PairingManager
import com.example.sms.db.AppDatabase
import com.example.sms.model.Contact
import com.google.android.material.tabs.TabLayout
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
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
    private lateinit var ivQrCode: ImageView
    private lateinit var btnCopy: Button
    private lateinit var btnShareQr: Button
    private lateinit var btnScanQr: Button
    private lateinit var etPairingInput: EditText
    private lateinit var btnImport: Button

    private val scanLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            etPairingInput.setText(result.contents)
            importPairing(result.contents)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pair)

        keyManager = KeyManager(this)
        db = AppDatabase.getDatabase(this)

        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        tabLayout = findViewById(R.id.tabLayout)
        layoutShow = findViewById(R.id.layoutShow)
        layoutImport = findViewById(R.layoutImport)
        tvMyPairing = findViewById(R.id.tvMyPairing)
        ivQrCode = findViewById(R.id.ivQrCode)
        btnCopy = findViewById(R.id.btnCopy)
        btnShareQr = findViewById(R.id.btnShareQr)
        btnScanQr = findViewById(R.id.btnScanQr)
        etPairingInput = findViewById(R.id.etPairingInput)
        btnImport = findViewById(R.id.btnImport)

        val uuid = keyManager.getUuid()
        val name = keyManager.getName()
        val publicKey = keyManager.getPublicKey()
        val phoneNumber = keyManager.getPhoneNumber()
        val pairingString = PairingManager.createPairingString(uuid, name, publicKey, phoneNumber)
        tvMyPairing.text = pairingString

        generateQrCode(pairingString)

        btnCopy.setOnClickListener {
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Pairing String", pairingString)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Pairing string copied", Toast.LENGTH_SHORT).show()
        }

        btnShareQr.setOnClickListener {
            val bitmap = generateQrCodeBitmap(pairingString)
            val path = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_PICTURES)
            val file = java.io.File(path, "e2ee_pairing_qr.png")
            java.io.FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            val uri = androidx.core.content.FileProvider.getUriForFile(
                this, "$packageName.provider", file
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, "Share QR Code"))
        }

        btnScanQr.setOnClickListener {
            val options = ScanOptions()
                .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                .setPrompt("Scan pairing QR code")
                .setCameraId(0)
                .setBeepEnabled(false)
                .setOrientationLocked(true)
            scanLauncher.launch(options)
        }

        btnImport.setOnClickListener {
            val input = etPairingInput.text.toString().trim()
            if (input.isEmpty()) {
                etPairingInput.error = "Enter pairing string"
                return@setOnClickListener
            }
            importPairing(input)
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

    private fun importPairing(input: String) {
        val data = PairingManager.parsePairingString(input)
        if (data == null) {
            Toast.makeText(this, "Invalid pairing string", Toast.LENGTH_SHORT).show()
            return
        }

        if (data.uuid == keyManager.getUuid()) {
            Toast.makeText(this, "Cannot pair with yourself", Toast.LENGTH_SHORT).show()
            return
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

    private fun generateQrCode(text: String) {
        try {
            val bitmap = generateQrCodeBitmap(text)
            ivQrCode.setImageBitmap(bitmap)
        } catch (e: Exception) {
            ivQrCode.visibility = android.view.View.GONE
        }
    }

    private fun generateQrCodeBitmap(text: String): Bitmap {
        val size = 512
        val bits = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (bits[x, y]) 0x000000 else 0xFFFFFF)
            }
        }
        return bitmap
    }
}
