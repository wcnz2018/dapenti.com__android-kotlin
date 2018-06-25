package com.willchou.dapenti.presenter

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.os.Parcelable
import android.support.design.widget.Snackbar
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.willchou.dapenti.R
import com.willchou.dapenti.databinding.ActivityMainBinding
import com.willchou.dapenti.db.DaPenTiData
import com.willchou.dapenti.model.Settings
import com.willchou.dapenti.vm.MainViewModel
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers

class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"

        const val ACTION_READING_MODE_CHANGED = "com.willchou.dapenti.NIGHT_MODE_CHANGED"
        const val ACTION_COLLAPSE_ALL = "com.willchou.dapenti.COLLAPSE_ALL"
    }

    private var binding: ActivityMainBinding? = null

    private var videoOnMobileDataObserver = java.util.Observer { _, p ->
        if (p as Boolean) {
            Snackbar.make(findViewById(android.R.id.content),
                    "正在使用移动网络播放视频,请注意流量", Snackbar.LENGTH_LONG)
                    .setAction(R.string.title_activity_settings) {
                        val i = Intent(this, SettingsActivity::class.java)
                        this.startActivity(i)
                    }.show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        initiateContent()
        Settings.videoOnMobileData.addObserver(videoOnMobileDataObserver)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")

        Settings.videoOnMobileData.deleteObserver(videoOnMobileDataObserver)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        Log.d(TAG, "onCreateOptionsMenu")
        menuInflater.inflate(R.menu.main, menu)
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
            }

            R.id.action_collapse_all -> {
                sendBroadcast(Intent(ACTION_COLLAPSE_ALL))
            }

            R.id.action_favorite -> {
                val intent = Intent(this, FavoriteActivity::class.java)
                startActivity(intent)
            }

            R.id.action_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showWait(failed: Boolean) {
        Log.d(TAG, "showWait with failed: $failed")
        binding!!.waitLayout.visibility = View.VISIBLE
        binding!!.viewpager.visibility = View.GONE
        if (failed) {
            binding!!.waitProgressBar.visibility = View.GONE
            binding!!.waitTextView.visibility = View.VISIBLE
        } else {
            binding!!.waitProgressBar.visibility = View.VISIBLE
            binding!!.waitTextView.visibility = View.GONE
        }
    }

    private fun hideWait() {
        binding!!.waitLayout.visibility = View.GONE
        binding!!.viewpager.visibility = View.VISIBLE
    }

    private fun initiateContent() {
        setSupportActionBar(binding!!.toolbar)

        showWait(false)

        Observable.just(this)
                .subscribeOn(Schedulers.io())
                .subscribe {
                    val mainViewModel = ViewModelProviders.of(this).get(MainViewModel::class.java)
                    mainViewModel.allCategories.observe(this, Observer {
                        val list = it?.toList()
                        if (list != null)
                            runOnUiThread { setupContent(list) }
                    })
                }
    }

    private fun setupContent(categories: List<DaPenTiData.Category>) {
        Log.d(TAG, "setupContent")
        hideWait()

        binding!!.tabLayout.visibility = View.VISIBLE
        binding!!.tabLayout.tabMode = TabLayout.MODE_SCROLLABLE

        val viewPager = findViewById<ViewPager>(R.id.viewpager)

        val adapter = Adapter(supportFragmentManager)
        for (c in categories) {
            if (c.visible == 1)
                adapter.addFragment(c.title, ListFragment().withCategory(c.title))
        }
        viewPager.adapter = adapter

        binding!!.tabLayout.setupWithViewPager(viewPager)
    }

    internal class Adapter(fm: FragmentManager) : FragmentStatePagerAdapter(fm) {
        private val fragmentPairList: MutableList<Pair<String, ListFragment>> = ArrayList()
        private val recycledViewPool = RecyclerView.RecycledViewPool()

        fun addFragment(title: String, fragment: ListFragment) {
            fragment.recycledViewPool = recycledViewPool
            fragmentPairList.add(Pair(title, fragment))
            Log.d(TAG, "title: $title, fragment: $fragment")
        }

        override fun saveState(): Parcelable? {
            return null
        }

        override fun restoreState(state: Parcelable?, loader: ClassLoader?) {

        }

        override fun getItem(position: Int): Fragment {
            return fragmentPairList[position].second
        }

        override fun getPageTitle(position: Int): CharSequence? {
            return fragmentPairList[position].first
        }

        override fun getCount(): Int {
            return fragmentPairList.size
        }
    }
}
