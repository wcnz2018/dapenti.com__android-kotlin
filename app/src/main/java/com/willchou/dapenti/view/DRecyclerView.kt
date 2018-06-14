package com.willchou.dapenti.view

import android.content.Context
import android.content.Intent
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.util.Log
import com.willchou.dapenti.model.DaPenTi
import com.willchou.dapenti.model.DaPenTiPage

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
        val pageTitle = intent?.getStringExtra(DaPenTi.EXTRA_PAGE_TITLE)

        val daPenTiPages = (adapter as RecyclerViewAdapter?)?.daPenTiPages ?: return

        var index = -1
        for (i in daPenTiPages.indices) {
            val title = daPenTiPages[i].pageTitle
            if (title == pageTitle) {
                index = i
                break
            }
        }

        if (index == -1) {
            Log.d(TAG, "Unable to find Page by title $pageTitle")
            return
        }

        when (intent?.action) {
            DaPenTi.ACTION_PAGE_PREPARED -> {
                Log.d(TAG, "onReceive PagePrepared")
                adapter.notifyItemChanged(index, RecyclerViewHolder.Bind_ShowContent)
            }

            DaPenTi.ACTION_PAGE_FAILED -> {
                Log.d(TAG, "onReceive PageFailed")
                adapter.notifyItemChanged(index, RecyclerViewHolder.Bind_PageFailed)
            }

            DaPenTi.ACTION_PAGE_FAVORITE -> {
                Log.d(TAG, "onReceive PageFavorite")
                adapter.notifyItemChanged(index, RecyclerViewHolder.Bind_Favorite)
            }

            VideoWebView.ACTION_VIDEO_LOADFINISHED -> {
                Log.d(TAG, "onReceive PageLoadFinished")
                adapter.notifyItemChanged(index, RecyclerViewHolder.Bind_PageLoadFinished)
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
        DaPenTi.daPenTi?.resetPageSelect()
        updateVisibleState(RecyclerViewHolder.Bind_SelectModeAnimation)
        selectMode = true
    }

    fun exitSelectMode() {
        updateVisibleState(RecyclerViewHolder.Bind_SelectModeQuit)
        selectMode = false
    }

    fun reverseSelect() {
        val adapter = adapter as RecyclerViewAdapter? ?: return
        if (adapter.daPenTiPages == null)
            return

        for (page in adapter.daPenTiPages!!)
            page.isSelected = !page.isSelected
        updateVisibleState(RecyclerViewHolder.Bind_SelectChanged)
    }

    fun getSelectPages(): List<DaPenTiPage>? {
        val adapter = adapter as RecyclerViewAdapter? ?: return null
        if (adapter.daPenTiPages == null)
            return null

        val list: MutableList<DaPenTiPage> = ArrayList()
        for (page in adapter.daPenTiPages!!) {
            if (page.isSelected)
                list.add(page)
        }

        return list
    }

    fun collapseAll() {
        val adapter = adapter as RecyclerViewAdapter? ?: return
        if (adapter.daPenTiPages == null)
            return

        for (page in adapter.daPenTiPages!!)
            page.markExpanded(false)

        updateVisibleState(RecyclerViewHolder.Bind_Collapse)
    }
}
