package com.willchou.dapenti.view

import android.content.Context
import android.content.Intent
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.util.Log
import com.willchou.dapenti.model.DaPenTi
import com.willchou.dapenti.model.DaPenTiPage
import com.willchou.dapenti.utils.DObservable

class DRecyclerView : RecyclerView {
    companion object {
        private const val TAG = "DRecyclerView"

        private var selectMode = false
        fun isSelectMode(): Boolean { return selectMode }
    }
    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context) : super(context)

    fun broadcastAction(intent: Intent?) {
        when (intent?.action) {
            VideoWebView.ACTION_VIDEO_LOADFINISHED -> {
                Log.d(TAG, "onReceive PageLoadFinished")
                updateVisibleState(RecyclerViewHolder.Bind_PageLoadFinished)
            }
        }
    }

    fun getRecyclerViewAdapter(): RecyclerViewAdapter? {
        return adapter as RecyclerViewAdapter? ?: return null
    }

    fun updateVisibleState(payload: Any?) {
        val lm = layoutManager as LinearLayoutManager? ?: return

        val first = lm.findFirstVisibleItemPosition()
        val last = lm.findLastVisibleItemPosition()

        adapter?.notifyItemRangeChanged(first, last - first + 1, payload)
    }

    fun enterSelectMode() {
        Log.d(TAG, "enterSelectModeAnimation")
        //DaPenTi.daPenTi?.resetPageSelect()
        updateVisibleState(RecyclerViewHolder.Bind_SelectModeAnimation)
        selectMode = true
    }

    fun exitSelectMode() {
        updateVisibleState(RecyclerViewHolder.Bind_SelectModeQuit)
        selectMode = false
    }

    fun collapseAll() {
        /*
        val adapter = adapter as RecyclerViewAdapter? ?: return
        if (adapter.daPenTiPages == null)
            return

        for (page in adapter.daPenTiPages!!)
            page.markExpanded(false)

        updateVisibleState(RecyclerViewHolder.Bind_Collapse)
        */
    }
}
