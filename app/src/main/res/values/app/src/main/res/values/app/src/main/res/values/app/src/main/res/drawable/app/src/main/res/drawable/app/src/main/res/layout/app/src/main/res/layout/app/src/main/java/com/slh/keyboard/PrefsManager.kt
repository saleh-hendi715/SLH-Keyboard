package com.slh.keyboard

import android.content.Context
import android.graphics.Color

class PrefsManager(context: Context) {
    private val prefs = context.getSharedPreferences("slh_keyboard_prefs", Context.MODE_PRIVATE)

    var accentColor: Int
        get() = prefs.getInt("accent_color", Color.parseColor("#D4AF37"))
        set(value) = prefs.edit().putInt("accent_color", value).apply()

    var isLightMode: Boolean
        get() = prefs.getBoolean("light_mode", false)
        set(value) = prefs.edit().putBoolean("light_mode", value).apply()

    var showNumRow: Boolean
        get() = prefs.getBoolean("show_num_row", false)
        set(value) = prefs.edit().putBoolean("show_num_row", value).apply()

    var soundEnabled: Boolean
        get() = prefs.getBoolean("sound_enabled", true)
        set(value) = prefs.edit().putBoolean("sound_enabled", value).apply()

    var vibrateEnabled: Boolean
        get() = prefs.getBoolean("vibrate_enabled", true)
        set(value) = prefs.edit().putBoolean("vibrate_enabled", value).apply()

    var incognitoMode: Boolean
        get() = prefs.getBoolean("incognito_mode", false)
        set(value) = prefs.edit().putBoolean("incognito_mode", value).apply()

    var userName: String
        get() = prefs.getString("user_name", "SLH Pro") ?: "SLH Pro"
        set(value) = prefs.edit().putString("user_name", value).apply()

    var signature: String
        get() = prefs.getString("signature", "— SLH KEYBOARD ✦") ?: "— SLH KEYBOARD ✦"
        set(value) = prefs.edit().putString("signature", value).apply()
}
