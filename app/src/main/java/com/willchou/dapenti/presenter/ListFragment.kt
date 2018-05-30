package com.willchou.dapenti.presenter

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import com.willchou.dapenti.R
import com.willchou.dapenti.model.DaPenTi
import com.willchou.dapenti.model.DaPenTiCategory
import com.willchou.dapenti.view.EnhancedWebView
import com.willchou.dapenti.view.RecyclerViewAdapter

class ListFragment : Fragment() {
    companion object {
        private const val TAG = "ListFragment"
        private const val BSCategoryIndex = "daPenTiCategoryIndex"
    }

    private var daPenTiCategoryIndex = -1
    private var daPenTiCategory: DaPenTiCategory? = null

    private var swipeRefreshLayout: SwipeRefreshLayout? = null
    private var recyclerView: RecyclerView? = null
    private var recyclerViewAdapter: RecyclerViewAdapter? = null

    private var fullScreenViewPair: EnhancedWebView.FullScreenViewPair? = null

    private val fullScreenTriggered = object : EnhancedWebView.onFullScreenTriggered {
        override fun triggered(fullscreen: Boolean) {
            val window = activity!!.window

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
    }

    internal fun setDaPenTiItemIndex(daPenTiCategoryIndex: Int,
                                     fullScreenViewPair: EnhancedWebView.FullScreenViewPair): ListFragment {
        val daPenTi = DaPenTi.daPenTi
        if (daPenTi == null) {
            Log.e(TAG, "Unable to get data model")
            return this
        }

        this.daPenTiCategoryIndex = daPenTiCategoryIndex
        this.daPenTiCategory = daPenTi.daPenTiCategories.get(daPenTiCategoryIndex)
        this.fullScreenViewPair = fullScreenViewPair
        return this
    }

    override fun onSaveInstanceState(outState: Bundle) {
        Log.d(TAG, "onSaveInstanceState: index: $daPenTiCategoryIndex")
        outState.putInt(BSCategoryIndex, daPenTiCategoryIndex)
        super.onSaveInstanceState(outState)
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        if (isVisibleToUser) {
            //Log.d(TAG, "isVisibleToUser: " + daPenTiCategory.getCategoryName());
            //prepareContent();
        }
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        swipeRefreshLayout = inflater.inflate(R.layout.penti_list,
                container, false) as SwipeRefreshLayout
        swipeRefreshLayout!!.setColorSchemeResources(R.color.colorAccent, R.color.colorPrimary)

        recyclerView = swipeRefreshLayout!!.findViewById(R.id.recycler_view)

        Log.d(TAG, "onCreateView：savedInstanceState: $savedInstanceState"
                + ", index: $daPenTiCategoryIndex")
        Log.d(TAG, "onCreateView: adapter: $recyclerViewAdapter")

        if (savedInstanceState != null)
            daPenTiCategoryIndex = savedInstanceState.getInt(BSCategoryIndex)

        prepareContent()
        return swipeRefreshLayout
    }

    private fun setupRecyclerView() {
        Log.d(TAG, "setupRecyclerView with adapter: $recyclerViewAdapter")
        swipeRefreshLayout!!.isRefreshing = false
        if (recyclerViewAdapter == null)
            recyclerViewAdapter = RecyclerViewAdapter(daPenTiCategoryIndex,
                    fullScreenViewPair!!, fullScreenTriggered)
        recyclerView!!.layoutManager = LinearLayoutManager(recyclerView!!.context)
        recyclerView!!.adapter = recyclerViewAdapter

        recyclerViewAdapter!!.notifyDataSetChanged()
    }

    private fun prepareContent() {
        val daPenTi = DaPenTi.daPenTi ?: return

        val dptcs = daPenTi.daPenTiCategories
        Log.d(TAG, "DPTCategories: $dptcs")

        if (daPenTiCategoryIndex < 0 || daPenTiCategoryIndex >= dptcs.size)
            return

        daPenTiCategory = daPenTi.daPenTiCategories[daPenTiCategoryIndex]
        if (daPenTiCategory == null) {
            Log.d(TAG, "Unable to fetch daPenTiCagetory")
            return
        }

        daPenTiCategory?.categoryPrepared = object : DaPenTiCategory.onCategoryPrepared {
            override fun onCategoryPrepared(index: Int) {
                Log.d(TAG, "new page prepared, index: $index")
                activity!!.runOnUiThread { setupRecyclerView(); }
            }
        }

        if (daPenTiCategory!!.initiated()) {
            setupRecyclerView()
        } else
            Thread { daPenTiCategory!!.preparePages(false) }.start()

        swipeRefreshLayout!!.setOnRefreshListener {
            Thread { daPenTiCategory!!.preparePages(true) }.start()
        }
    }
}
