package com.willchou.dapenti.model

import android.content.SharedPreferences
import android.content.res.Resources
import android.util.Log

import com.willchou.dapenti.R

class Settings {
    companion object {
        const val TAG = "Setting"
        const val nightModeStyle =
                "body {color:#d2d2d2 !important;background-color:#424242 !important}" +
                        "a {color:white; !important}"

        const val FontSizeSmall = 0
        const val FontSizeMedia = 1
        const val FontSizeBig = 2
        const val FontSizeSuperBig = 3

        var settings: Settings? = null
            private set
    }

    private var resources: Resources? = null
    private var prefs: SharedPreferences? = null

    init {
        settings = this
    }

    fun initiate(p: SharedPreferences, r: Resources) {
        prefs = p
        resources = r

        settings = this
    }

    val fontSize: Int
        get() {
            val s = prefs!!.getString(resources!!.getString(R.string.pref_key_font_size), "")

            var fontSize = FontSizeMedia
            when (s) {
                "small" -> fontSize = FontSizeSmall
                "big" -> fontSize = FontSizeBig
                "super" -> fontSize = FontSizeSuperBig
            }

            return fontSize
        }

    val isImageEnabled: Boolean
        get() = prefs!!.getBoolean(resources!!.getString(R.string.pref_key_display_image), true)

    var nightMode: Boolean = false
        set(nm) {
            field = nm
            Log.d(TAG, "setNightMode: $nm")
            val editor = prefs!!.edit()
            editor.putBoolean(resources!!.getString(R.string.pref_key_night_mode), nm)
            editor.apply()
        }
        get() {
            return prefs!!.getBoolean(resources!!.getString(R.string.pref_key_night_mode), false)
        }

    val viewModeCSSStyle: String
        get() = if (nightMode) nightModeStyle else ""
}
