package com.slh.keyboard

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: PrefsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefs = PrefsManager(this)

        findViewById<Button>(R.id.btnEnableKb).setOnClickListener {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        }

        findViewById<Button>(R.id.btnSelectKb).setOnClickListener {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showInputMethodPicker()
        }

        val etName = findViewById<EditText>(R.id.etUserName)
        val etSig = findViewById<EditText>(R.id.etSignature)
        etName.setText(prefs.userName)
        etSig.setText(prefs.signature)

        findViewById<Button>(R.id.btnSaveIdentity).setOnClickListener {
            prefs.userName = etName.text.toString().ifBlank { "SLH Pro" }
            prefs.signature = etSig.text.toString().ifBlank { "— SLH KEYBOARD ✦" }
            Toast.makeText(this, "تم حفظ البصمة الشخصية 🪪", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnAccentGold).setOnClickListener { setAccent("#D4AF37", "الذهبي المعدني") }
        findViewById<Button>(R.id.btnAccentCyan).setOnClickListener { setAccent("#06B6D4", "الأزرق السيان") }
        findViewById<Button>(R.id.btnAccentPurple).setOnClickListener { setAccent("#A855F7", "البنفسجي الملكي") }
        findViewById<Button>(R.id.btnAccentSilver).setOnClickListener { setAccent("#CBD5E1", "الفضي المعدني") }

        val swLight = findViewById<Switch>(R.id.swLightMode)
        val swNumRow = findViewById<Switch>(R.id.swNumRow)
        val swSound = findViewById<Switch>(R.id.swSound)
        val swVibrate = findViewById<Switch>(R.id.swVibrate)
        val swIncognito = findViewById<Switch>(R.id.swIncognito)

        swLight.isChecked = prefs.isLightMode
        swNumRow.isChecked = prefs.showNumRow
        swSound.isChecked = prefs.soundEnabled
        swVibrate.isChecked = prefs.vibrateEnabled
        swIncognito.isChecked = prefs.incognitoMode

        swLight.setOnCheckedChangeListener { _, checked -> prefs.isLightMode = checked }
        swNumRow.setOnCheckedChangeListener { _, checked -> prefs.showNumRow = checked }
        swSound.setOnCheckedChangeListener { _, checked -> prefs.soundEnabled = checked }
        swVibrate.setOnCheckedChangeListener { _, checked -> prefs.vibrateEnabled = checked }
        swIncognito.setOnCheckedChangeListener { _, checked -> prefs.incognitoMode = checked }
    }

    private fun setAccent(hex: String, name: String) {
        prefs.accentColor = Color.parseColor(hex)
        Toast.makeText(this, "تم تفعيل لون الهوية: $name", Toast.LENGTH_SHORT).show()
    }
}
