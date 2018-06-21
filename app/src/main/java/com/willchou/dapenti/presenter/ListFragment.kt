package com.willchou.dapenti.presenter

import android.arch.lifecycle.Observer
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.willchou.dapenti.R
import com.willchou.dapenti.model.DaPenTi
import com.willchou.dapenti.model.DaPenTiCategory
import com.willchou.dapenti.model.Settings
import android.arch.lifecycle.ViewModelProviders
import com.willchou.dapenti.view.*
import com.willchou.dapenti.vm.FragmentViewModel
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

class ListFragment : Fragment() {
    companion object {
        private const val TAG = "ListFragment"
        private const val EXTRA_CATEGORY = "category"
    }

    private var daPenTiCategory: DaPenTiCategory? = null

    private var swipeRefreshLayout: SwipeRefreshLayout? = null
    private var recyclerView: DRecyclerView? = null
    private var recyclerViewAdapter: RecyclerViewAdapter? = null


    private var categoryName: String? = null
    private var fragmentViewModel: FragmentViewModel? = null
    var recycledViewPool: RecyclerView.RecycledViewPool? = null

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                MainActivity.ACTION_READING_MODE_CHANGED -> {
                    checkNightMode()
                    recyclerViewAdapter?.notifyDataSetChanged()
                }

                MainActivity.ACTION_COLLAPSE_ALL -> recyclerView?.collapseAll()

                DaPenTi.ACTION_CATEGORY_PREPARED -> {
                    val categoryTitle = intent.getStringExtra(DaPenTi.EXTRA_CATEGORY_TITLE)
                    Log.d(TAG, "onReceive: $categoryTitle")
                    if (categoryTitle == daPenTiCategory?.categoryName)
                        activity!!.runOnUiThread { setupRecyclerView() }
                }

                /*
                DaPenTi.ACTION_DATABASE_CHANGED -> setupRecyclerView()

                DaPenTi.ACTION_PAGE_PREPARED,
                DaPenTi.ACTION_PAGE_FAILED,
                DaPenTi.ACTION_PAGE_FAVORITE,
                */
                VideoWebView.ACTION_VIDEO_LOADFINISHED -> recyclerView?.broadcastAction(intent)
            }
        }
    }

    internal fun withCategory(name: String) : ListFragment {
        categoryName = name
        return this
    }

    private fun registerReceiver() {
        val filter = IntentFilter()
        filter.addAction(MainActivity.ACTION_READING_MODE_CHANGED)
        filter.addAction(MainActivity.ACTION_COLLAPSE_ALL)

        /*
        filter.addAction(DaPenTi.ACTION_CATEGORY_PREPARED)
        filter.addAction(DaPenTi.ACTION_DATABASE_CHANGED)

        filter.addAction(DaPenTi.ACTION_PAGE_PREPARED)
        filter.addAction(DaPenTi.ACTION_PAGE_FAILED)
        filter.addAction(DaPenTi.ACTION_PAGE_FAVORITE)
        */

        filter.addAction(VideoWebView.ACTION_VIDEO_LOADFINISHED)
        context!!.registerReceiver(broadcastReceiver, filter)
    }

    private fun unregisterReceiver() {
        context!!.unregisterReceiver(broadcastReceiver)
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        Log.d(TAG, "isVisibleToUser: ${daPenTiCategory?.categoryName}")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState != null) {
            val n = savedInstanceState.getString(EXTRA_CATEGORY)
            if (!n.isNullOrEmpty())
                categoryName = n
        }

        fragmentViewModel = ViewModelProviders
                .of(this, FragmentViewModel.Factor(categoryName!!))
                .get(categoryName!!, FragmentViewModel::class.java)

        //swipeRefreshLayout!!.isRefreshing = true
        Observable.just(fragmentViewModel)
                .subscribeOn(Schedulers.io())
                .subscribe { fragmentViewModel?.preparePagesIfEmpty() }

        registerReceiver()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        Log.d(TAG, "onSaveInstanceState")
        outState.putString(EXTRA_CATEGORY, categoryName)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: ${daPenTiCategory?.categoryName}")

        unregisterReceiver()
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause: ${daPenTiCategory?.categoryName}")
        swipeRefreshLayout!!.isRefreshing = false
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: ${daPenTiCategory?.categoryName}")
        swipeRefreshLayout!!.isRefreshing = false

        checkNightMode()
        recyclerView?.updateVisibleState(null)
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        swipeRefreshLayout = inflater.inflate(R.layout.penti_fragment,
                container, false) as SwipeRefreshLayout
        swipeRefreshLayout!!.setColorSchemeResources(R.color.colorAccent, R.color.colorPrimary)

        recyclerView = swipeRefreshLayout!!.findViewById(R.id.recycler_view)

        Log.d(TAG, "onCreateView(${daPenTiCategory?.categoryName}): " +
                "adapter: $recyclerViewAdapter," +
                "Bundle: $savedInstanceState")

        prepareContent()
        return swipeRefreshLayout
    }

    private fun checkNightMode() {
        val bgColor = Settings.settings!!.getBackgroundColor()
        recyclerView?.setBackgroundColor(bgColor)
    }

    private fun setupRecyclerView() {
        recyclerViewAdapter = RecyclerViewAdapter()

        recyclerView!!.layoutManager = LinearLayoutManager(recyclerView!!.context)
        recyclerView!!.recycledViewPool = recycledViewPool
        recyclerView!!.adapter = recyclerViewAdapter
    }

    private fun prepareContent() {
        setupRecyclerView()

        Observable.just(fragmentViewModel)
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    it!!.getPages()?.observe(this,
                            Observer(recyclerViewAdapter!!::submitList))
                }
    }
}
