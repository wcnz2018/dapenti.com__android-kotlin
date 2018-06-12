package com.willchou.dapenti.presenter

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.preference.ListPreference
import android.preference.Preference
import android.preference.PreferenceFragment
import android.preference.PreferenceScreen
import android.support.design.widget.Snackbar
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.View
import com.willchou.dapenti.DaPenTiApplication
import com.willchou.dapenti.R
import com.willchou.dapenti.model.Database
import com.willchou.dapenti.model.Settings
import com.willchou.dapenti.view.ConfirmDialog
import me.majiajie.swipeback.SwipeBackActivity
import java.util.*

class SettingsActivity : SwipeBackActivity() {
    companion object {
        private const val TAG = "SettingsActivity"
    }

    private var settingContent: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val toolbar = findViewById(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { onBackPressed() }

        fragmentManager
                .beginTransaction()
                .replace(R.id.setting_content, SettingsFragment().setContext(this))
                .commit()

        settingContent = findViewById(R.id.setting_content)

        if (Settings.settings!!.nightMode) {
            setTheme(R.style.NightModeTheme)
            settingContent?.setBackgroundColor(Settings.settings!!.getLighterBackgroundColor())
        }
    }

    internal class SettingsFragment : PreferenceFragment() {
        private var mContext: Context? = null
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            addPreferencesFromResource(R.xml.preferences)

            setup()
        }

        fun setContext(context: Context): SettingsFragment {
            mContext = context
            return this
        }

        private fun setup() {
            val s = Settings.settings!!
            val cacheSize = s.getSizeString(s.getCacheSize())
            val databaseSize = s.getSizeString(s.getDatabaseSize())

            val pref = findPreference(resources.getString(R.string.pref_key_clear_cache))
            pref.summary = "页面呈现缓存: $cacheSize, 条目数据库: $databaseSize"
        }

        override fun onPreferenceTreeClick(preferenceScreen: PreferenceScreen,
                                           preference: Preference): Boolean {
            Log.d(TAG, "preference clicked: $preference")

            val aboutTitle = resources.getString(R.string.setting_other_about)
            val title = preference.title.toString()
            if (title == aboutTitle) {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse(preference.summary.toString())
                startActivity(intent)
                return true
            }

            val key = preference.key
                    ?: return super.onPreferenceTreeClick(preferenceScreen, preference)

            if (key == resources.getString(R.string.pref_key_order_page)) {
                val intent = Intent(DaPenTiApplication.getAppContext(),
                        PageOrderActivity::class.java)
                startActivity(intent)
            }

            if (key == resources.getString(R.string.pref_key_clear_cache)) {
                preference.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { pref, value ->
                    if (pref == null)
                        return@OnPreferenceChangeListener false
                    if (value.toString() == "none")
                        return@OnPreferenceChangeListener true

                    val listPref = pref as ListPreference
                    val index = listPref.findIndexOfValue(value.toString())

                    val confirmDialog = ConfirmDialog(mContext!!, pref.title.toString(),
                            "确定清除${listPref.entries[index]}?")
                    confirmDialog.clickEventListener = object : ConfirmDialog.ClickEventListener {
                        override fun confirmed() {
                            val settings = Settings.settings!!
                            val database = Database.database!!

                            val calendar = Calendar.getInstance()

                            when (index) {
                                1 -> settings.clearCache() // cacheOnly
                                2 -> { // weekAgo
                                    calendar.add(Calendar.DAY_OF_YEAR, -7)
                                    database.removePageBefore(calendar.time)
                                }
                                3 -> { // monthAgo
                                    calendar.add(Calendar.MONTH, -1)
                                    database.removePageBefore(calendar.time)
                                }
                                4 -> { // all
                                    database.removePageBefore(calendar.time)
                                }
                            }

                            setup()
                            //Snackbar.make(view, "清理成功", Snackbar.LENGTH_LONG).show()
                        }
                    }
                    confirmDialog.show()

                    false
                }
            }

            return true
        }
    }
}
