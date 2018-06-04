package com.willchou.dapenti.model

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Resources
import android.net.ConnectivityManager
import android.util.Log
import com.willchou.dapenti.DaPenTiApplication

import com.willchou.dapenti.R

class Settings {
    companion object {
        const val TAG = "Setting"
        const val nightModeStyle =
                "body {color:#d2d2d2 !important;background-color:#424242 !important}" +
                        "a {color:white; !important}"
        const val dayModeStyle =
                "body {color:black !important;background-color:white !important}" +
                        "a {color:blue; !important}"

        const val ACTION_PLAY_ON_MOBILE_DATA = "com.willchou.dapenti.videoOnMobileData"

        const val FontSizeSmall = 0
        const val FontSizeMedia = 1
        const val FontSizeBig = 2
        const val FontSizeSuperBig = 3

        const val DataTypeNone = 0
        const val DataTypeWifi = 1
        const val DataTypeMobile = 2

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
        get() = if (nightMode) nightModeStyle else dayModeStyle

    fun getDataType(): Int {
        val context = DaPenTiApplication.getAppContext()
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val info = manager.activeNetworkInfo

        if (info.type == ConnectivityManager.TYPE_WIFI)
            return DataTypeWifi
        if (info.type == ConnectivityManager.TYPE_MOBILE)
            return DataTypeMobile

        return DataTypeNone
    }

    fun notifyPlayOnMobileData() {
        val intent = Intent(ACTION_PLAY_ON_MOBILE_DATA)
        DaPenTiApplication.getAppContext().sendBroadcast(intent)
    }

    fun canPlayVideo():Boolean {
        val dataType = getDataType()
        val s = prefs!!.getString(resources!!.getString(R.string.pref_key_auto_play), "")
        Log.d(TAG, "auto_play: $s")
        if (s.isEmpty() || s == "none" || dataType == DataTypeNone)
            return false

        when (s) {
            "mobileAndWiFi" -> {
                if (dataType == DataTypeMobile)
                    notifyPlayOnMobileData()
                return dataType == DataTypeMobile || dataType == DataTypeWifi
            }

            "WiFi" -> {
                return dataType == DataTypeWifi
            }
        }

        return false
    }
}
