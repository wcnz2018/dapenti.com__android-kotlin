package com.willchou.dapenti.presenter

import android.content.Intent
import android.preference.PreferenceManager
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup

import com.willchou.dapenti.R
import com.willchou.dapenti.model.DaPenTi
import com.willchou.dapenti.model.DaPenTiCategory
import com.willchou.dapenti.model.Database
import com.willchou.dapenti.model.Settings
import com.willchou.dapenti.view.EnhancedWebView

import java.util.ArrayList

class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }

    private var toolbar: Toolbar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.d(TAG, "onCreate")
        initiateContent()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_detail, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Log.d(TAG, "item clicked: " + item.title)
        when (item.itemId) {
            R.id.action_mode -> return true

            R.id.action_collection -> return true

            R.id.action_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun initiateContent() {
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar?.visibility = View.GONE

        DaPenTi.storageDir = filesDir.absolutePath
        DaPenTi.categoryPrepared = object : DaPenTi.onCategoryPrepared {
            override fun onCategoryPrepared() {
                runOnUiThread({ setupContent(); })
            }
        }

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

        val viewPair = EnhancedWebView.FullScreenViewPair()
        viewPair.nonVideoLayout = findViewById(R.id.coordinatorLayout)
        viewPair.videoLayout = findViewById(R.id.fullscreenVideo)

        val daPenTi = DaPenTi.daPenTi
        if (daPenTi == null) {
            Log.e(TAG, "Unable to get data model")
            return
        }

        for (i in daPenTi.daPenTiCategories.indices) {
            val c = daPenTi.daPenTiCategories[i]
            adapter.addFragment(c.categoryName, ListFragment().setDaPenTiItemIndex(i, viewPair))
        }

        viewPager.adapter = adapter
    }

    internal class Adapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {
        private val mFragments = ArrayList<Fragment>()
        private val mFragmentTitles = ArrayList<String>()

        fun addFragment(title: String, fragment: Fragment) {
            mFragmentTitles.add(title)
            mFragments.add(fragment)

            Log.d(TAG, "title: $title, fragment: $fragment")
        }

        override fun getItem(position: Int): Fragment {
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
