package com.willchou.dapenti.presenter

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.*
import com.willchou.dapenti.R
import com.willchou.dapenti.model.DaPenTi
import com.willchou.dapenti.model.Settings
import com.willchou.dapenti.view.EnhancedWebView
import java.util.*

class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"
        const val ACTION_READING_MODE_CHANGED = "com.willchou.NIGHT_MODE_CHANGED"
    }

    private var toolbar: Toolbar? = null

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                DaPenTi.ACTION_CATEGORY_PREPARED -> {
                    val title = intent.getStringExtra(DaPenTi.EXTRA_CATEGORY_TITLE)
                    if (title == null)
                        setupContent()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initiateContent()
        registerReceiver(broadcastReceiver, IntentFilter(DaPenTi.ACTION_CATEGORY_PREPARED))
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume")

        setupListener()
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause")

        EnhancedWebView.enhancedWebViewCallback = null
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")

        unregisterReceiver(broadcastReceiver)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        Log.d(TAG, "onCreateOptionsMenu")
        menuInflater.inflate(R.menu.menu_detail, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        Log.d(TAG, "onPrepareOptionsMenu")
        val nightMode = Settings.settings?.nightMode

        if (nightMode != null) {
            val item = menu?.findItem(R.id.action_mode)
            item?.setTitle(if (nightMode) R.string.action_mode_day else R.string.action_mode_night)
        }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Log.d(TAG, "item clicked: " + item.title)
        when (item.itemId) {
            R.id.action_mode -> {
                val nightMode = !Settings.settings?.nightMode!!
                Settings.settings?.nightMode = nightMode

                sendBroadcast(Intent(ACTION_READING_MODE_CHANGED))
                return true
            }

            R.id.action_favorite -> {
                val intent = Intent(this, FavoriteActivity::class.java)
                startActivity(intent)
                return true
            }

            R.id.action_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setupListener() {
        Log.d(TAG, "setupListener")

        EnhancedWebView.enhancedWebViewCallback = object : EnhancedWebView.EnhancedWebViewCallback {
            override fun fullscreenTriggered(fullscreen: Boolean) {
                if (fullscreen) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LOW_PROFILE
                    //activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
                    //activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                }
            }

            @SuppressLint("WrongViewCast")
            override fun getFullScreenVideoLayout(): ViewGroup {
                return findViewById(R.id.fullscreenVideo)
            }
        }
    }

    private fun initiateContent() {
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar?.visibility = View.GONE

        DaPenTi.storageDir = filesDir.absolutePath

        setupListener()
        Thread { DaPenTi.daPenTi?.prepareCategory(false) }.start()
    }

    private fun setupContent() {
        Log.d(TAG, "setupContent")
        toolbar?.visibility = View.VISIBLE

        val tabLayout = findViewById<TabLayout>(R.id.tabs)
        tabLayout.tabMode = TabLayout.MODE_SCROLLABLE

        val viewPager = findViewById<ViewPager>(R.id.viewpager)
        setupViewPager(viewPager)
        tabLayout.setupWithViewPager(viewPager)
    }

    private fun setupViewPager(viewPager: ViewPager) {
        val adapter = Adapter(supportFragmentManager)

        val daPenTi = DaPenTi.daPenTi
        if (daPenTi == null) {
            Log.e(TAG, "Unable to get data model")
            return
        }

        for (category in daPenTi.daPenTiCategories) {
            adapter.addFragment(category.categoryName,
                    ListFragment().setDaPenTiCategory(category))
        }

        viewPager.adapter = adapter
    }

    internal class Adapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {
        private val mFragments = ArrayList<ListFragment>()
        private val mFragmentTitles = ArrayList<String>()
        private val recycledViewPool = RecyclerView.RecycledViewPool()

        fun addFragment(title: String, fragment: ListFragment) {
            mFragmentTitles.add(title)

            fragment.recycledViewPool = recycledViewPool
            mFragments.add(fragment)

            Log.d(TAG, "title: $title, fragment: $fragment")
        }

        override fun getItem(position: Int): Fragment {
            Log.d(TAG, "Adapter getItem: $position")
            return mFragments[position]
        }

        override fun getPageTitle(position: Int): CharSequence? {
            return mFragmentTitles[position]
        }

        override fun getCount(): Int {
            return mFragmentTitles.size
        }
    }
}
