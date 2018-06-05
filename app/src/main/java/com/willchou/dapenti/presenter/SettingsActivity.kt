package com.willchou.dapenti.presenter

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.preference.Preference
import android.preference.PreferenceFragment
import android.preference.PreferenceManager
import android.preference.PreferenceScreen
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.View
import com.hannesdorfmann.swipeback.Position
import com.hannesdorfmann.swipeback.SwipeBack
import com.willchou.dapenti.DaPenTiApplication
import com.willchou.dapenti.R

class SettingsActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "SettingsActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        SwipeBack.attach(this, Position.LEFT)
                .setContentView(R.layout.activity_settings)
                .setSwipeBackView(R.layout.swipeback_default)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { onBackPressed() }

        fragmentManager
                .beginTransaction()
                .replace(R.id.setting_content, SettingsFragment())
                .commit()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.swipeback_stack_to_front,
                R.anim.swipeback_stack_right_out)
    }

    internal class SettingsFragment: PreferenceFragment() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            addPreferencesFromResource(R.xml.preferences)
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
                preference.onPreferenceChangeListener = object : Preference.OnPreferenceChangeListener {
                    override fun onPreferenceChange(preference: Preference?, value: Any?): Boolean {
                        if (preference == null)
                            return false

                        when (value) {
                            "weekAgo" -> {
                                Log.d(TAG, "clear cache [weekAgo]")
                            }

                            "monthAgo" -> {
                                Log.d(TAG, "clear cache [monthAgo]")
                            }

                            "all" -> {
                                Log.d(TAG, "clear cache [all]")
                            }
                        }

                        return value.toString() != "none"
                    }
                }
            }

            return true
        }
    }
}
