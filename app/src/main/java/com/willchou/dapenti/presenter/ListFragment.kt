package com.willchou.dapenti.presenter

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.squareup.haha.perflib.Main
import com.willchou.dapenti.R
import com.willchou.dapenti.model.DaPenTiCategory
import com.willchou.dapenti.model.Settings
import com.willchou.dapenti.view.RecyclerViewAdapter

class ListFragment : Fragment() {
    companion object {
        private const val TAG = "ListFragment"
    }

    private var daPenTiCategory: DaPenTiCategory? = null

    private var swipeRefreshLayout: SwipeRefreshLayout? = null
    private var recyclerView: RecyclerView? = null
    private var recyclerViewAdapter: RecyclerViewAdapter? = null

    private var mContext: Context? = null
    var recycledViewPool: RecyclerView.RecycledViewPool? = null

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                MainActivity.BROADCAST_MODE_CHANGED -> {
                    checkNightMode()
                    recyclerViewAdapter?.notifyDataSetChanged()
                }
            }
        }
    }

    internal fun setDaPenTiCategory(daPenTiCategory: DaPenTiCategory):
            ListFragment {
        this.daPenTiCategory = daPenTiCategory
        return this
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        if (isVisibleToUser) {
            Log.d(TAG, "isVisibleToUser: ${daPenTiCategory?.categoryName}")
            //prepareContent();
            setupListener()
        }
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        mContext = context
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause: ${daPenTiCategory?.categoryName}")
        swipeRefreshLayout!!.isRefreshing = false
        daPenTiCategory?.categoryEventListener = null
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: ${daPenTiCategory?.categoryName}")
        swipeRefreshLayout!!.isRefreshing = false
        setupListener()

        checkNightMode()

        val filter = IntentFilter()
        filter.addAction(MainActivity.BROADCAST_MODE_CHANGED)
        mContext?.registerReceiver(broadcastReceiver, filter)
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

        recyclerView?.adapter = recyclerViewAdapter
    }

    private fun setupRecyclerView() {
        Log.d(TAG, "setupRecyclerView with adapter: $recyclerViewAdapter")
        swipeRefreshLayout!!.isRefreshing = false
        if (recyclerViewAdapter == null)
            recyclerViewAdapter = RecyclerViewAdapter(daPenTiCategory!!.pages)
        recyclerView!!.layoutManager = LinearLayoutManager(recyclerView!!.context)
        recyclerView!!.recycledViewPool = recycledViewPool
        recyclerView!!.adapter = recyclerViewAdapter

        recyclerViewAdapter!!.notifyDataSetChanged()
    }

    private fun setupListener() {
        Log.d(TAG, "setupListener: ${daPenTiCategory?.categoryName}")
        daPenTiCategory?.categoryEventListener =
                object : DaPenTiCategory.CategoryEventListener {
                    override fun onCategoryPrepared(index: Int) {
                        Log.d(TAG, "new page prepared, index: $index")
                        activity!!.runOnUiThread { setupRecyclerView() }
                    }
                }
    }

    private fun prepareContent() {
        setupListener()
        if (daPenTiCategory == null)
            return

        if (daPenTiCategory!!.initiated()) {
            setupRecyclerView()
        } else
            Thread { daPenTiCategory!!.preparePages(false) }.start()

        swipeRefreshLayout!!.setOnRefreshListener {
            Thread { daPenTiCategory!!.preparePages(true) }.start()
        }
    }
}
