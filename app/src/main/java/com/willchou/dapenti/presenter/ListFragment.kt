package com.willchou.dapenti.presenter

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.Fragment
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.willchou.dapenti.DaPenTiApplication
import com.willchou.dapenti.R
import com.willchou.dapenti.model.DaPenTi
import com.willchou.dapenti.model.DaPenTiCategory
import com.willchou.dapenti.model.Settings
import com.willchou.dapenti.view.DRecyclerView
import com.willchou.dapenti.view.RecyclerViewAdapter

class ListFragment : Fragment() {
    companion object {
        private const val TAG = "ListFragment"
    }

    private var daPenTiCategory: DaPenTiCategory? = null

    private var swipeRefreshLayout: SwipeRefreshLayout? = null
    private var recyclerView: DRecyclerView? = null
    private var recyclerViewAdapter: RecyclerViewAdapter? = null


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
            }
        }
    }

    internal fun setDaPenTiCategory(daPenTiCategory: DaPenTiCategory):
            ListFragment {
        this.daPenTiCategory = daPenTiCategory
        return this
    }

    private fun registerReceiver() {
        val filter = IntentFilter()
        filter.addAction(MainActivity.ACTION_READING_MODE_CHANGED)
        filter.addAction(MainActivity.ACTION_COLLAPSE_ALL)
        filter.addAction(DaPenTi.ACTION_CATEGORY_PREPARED)
        DaPenTiApplication.getAppContext().registerReceiver(broadcastReceiver, filter)
    }

    private fun unregisterReceiver() {
        DaPenTiApplication.getAppContext().unregisterReceiver(broadcastReceiver)
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        Log.d(TAG, "isVisibleToUser: ${daPenTiCategory?.categoryName}")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: ${daPenTiCategory?.categoryName}")

        registerReceiver()
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
        swipeRefreshLayout = inflater.inflate(R.layout.penti_list,
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
        val nightMode = Settings.settings?.nightMode
        if (nightMode != null && nightMode)
            recyclerView?.setBackgroundColor(Color.rgb(48, 48, 48))
        else
            recyclerView?.setBackgroundColor(Color.WHITE)
    }

    private fun setupRecyclerView() {
        swipeRefreshLayout!!.isRefreshing = false
        if (recyclerViewAdapter == null)
            recyclerViewAdapter = RecyclerViewAdapter(daPenTiCategory!!.pages)

        recyclerView!!.layoutManager = LinearLayoutManager(recyclerView!!.context)
        recyclerView!!.recycledViewPool = recycledViewPool
        recyclerView!!.adapter = recyclerViewAdapter

        Log.d(TAG, "setupRecyclerView(${daPenTiCategory?.categoryName})" +
                " with adapter: $recyclerViewAdapter")
        Log.d(TAG, "data.size: ${daPenTiCategory?.pages?.size}")

        recyclerViewAdapter!!.notifyDataSetChanged()
    }

    private fun prepareContent() {
        if (daPenTiCategory == null)
            return

        if (daPenTiCategory!!.initiated()) {
            setupRecyclerView()
        } else {
            Handler().postDelayed({
                if (!daPenTiCategory!!.initiated())
                    swipeRefreshLayout?.isRefreshing = true
            }, 500)
            Thread { daPenTiCategory!!.preparePages(false) }.start()
        }

        swipeRefreshLayout!!.setOnRefreshListener {
            Thread { daPenTiCategory!!.preparePages(true) }.start()
        }
    }
}
