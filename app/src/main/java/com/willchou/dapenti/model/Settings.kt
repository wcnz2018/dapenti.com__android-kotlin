package com.willchou.dapenti.model

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Resources
import android.graphics.Color
import android.net.ConnectivityManager
import android.util.Log
import com.willchou.dapenti.DaPenTiApplication

import com.willchou.dapenti.R
import java.io.File

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

    private val packageDataDir = DaPenTiApplication.getAppContext().applicationInfo.dataDir

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

    val smartContentEnabled: Boolean
        get() = prefs!!.getBoolean(resources!!.getString(R.string.pref_key_smart_content), true)

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

    fun getBackgroundColor(): Int {
        return if (nightMode) Color.rgb(48, 48, 48) else Color.WHITE
    }

    fun getLighterBackgroundColor(): Int {
        return if (nightMode) Color.rgb(66, 66, 66) else Color.WHITE
    }

    fun getForegroundColor(): Int {
        return if(nightMode) Color.rgb(213, 213, 213) else Color.BLACK
    }

    private fun getDataType(): Int {
        val context = DaPenTiApplication.getAppContext()
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val info = manager.activeNetworkInfo

        if (info.type == ConnectivityManager.TYPE_WIFI)
            return DataTypeWifi
        if (info.type == ConnectivityManager.TYPE_MOBILE)
            return DataTypeMobile

        return DataTypeNone
    }

    private fun notifyPlayOnMobileData() {
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

    private fun deleteRecursive(fileOrDirectory: File) {
        if (fileOrDirectory.isDirectory)
            for (child in fileOrDirectory.listFiles()!!)
                deleteRecursive(child)

        fileOrDirectory.delete()
    }

    private fun getFolderSize(directory: File): Long {
        var length: Long = 0
        for (file in directory.listFiles()) {
            length += if (file.isFile)
                file.length()
            else
                getFolderSize(file)
        }
        return length
    }

    private fun getDirSizeInsidePackageData(dirEndWith: String): Long {
        var size: Long = 0

        for (file in File(packageDataDir).listFiles()) {
            if (!file.isDirectory)
                continue
            if (file.absoluteFile.endsWith(dirEndWith))
                size += getFolderSize(file)
        }

        return size
    }

    fun getCacheSize(): Long {
        return getDirSizeInsidePackageData("cache")
    }

    fun clearCache() {
        for (file in File(packageDataDir).listFiles()) {
            if (file.absoluteFile.endsWith("cache"))
                deleteRecursive(file)
        }
    }

    fun getDatabaseSize(): Long {
        return getDirSizeInsidePackageData("databases")
    }

    fun getSizeString(size: Long): String {
        val K = 1024
        val M = K * 1024
        val G = M * 1024

        if (size < K)
            return "${size}B"
        if (size < M)
            return "${size / K}K"
        if (size < G)
            return "${size / M}M"
        return "${size / G}G"
    }
}
