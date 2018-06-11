package com.willchou.dapenti.view

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import com.willchou.dapenti.DaPenTiApplication
import com.willchou.dapenti.R
import com.willchou.dapenti.model.DaPenTi
import com.willchou.dapenti.model.DaPenTiPage
import com.willchou.dapenti.model.Settings

class RecyclerViewAdapter(var daPenTiPages: List<DaPenTiPage>?)
    : RecyclerView.Adapter<RecyclerViewHolder>() {
    companion object {
        private const val TAG = "RecyclerViewAdapter"
    }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val pageTitle = intent?.getStringExtra(DaPenTi.EXTRA_PAGE_TITLE)

            if (daPenTiPages == null)
                return

            var index = -1
            for (i in daPenTiPages!!.indices) {
                val title = daPenTiPages!![i].pageTitle
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
                    notifyItemChanged(index, RecyclerViewHolder.Bind_PlayVideo)
                }

                DaPenTi.ACTION_PAGE_FAVORITE -> {
                    Log.d(TAG, "onReceive PageFavorite")
                    notifyItemChanged(index, RecyclerViewHolder.Bind_Favorite)
                }
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerViewHolder, position: Int) {
        if (daPenTiPages != null)
            holder.update(daPenTiPages!![position])
    }

    override fun onBindViewHolder(holder: RecyclerViewHolder, position: Int,
                                  payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position)
            return
        }

        for (payLoad in payloads) {
            if (payLoad !is String)
                continue

            val s = payLoad.toString()
            Log.d(TAG, "payLoad: $s")

            when (s) {
                RecyclerViewHolder.Bind_PlayVideo -> holder.setupContent(Settings.settings!!.canPlayVideo())
                RecyclerViewHolder.Bind_Favorite -> holder.checkFavorite()
                RecyclerViewHolder.Bind_Collapse -> holder.hideContent(true)
                RecyclerViewHolder.Bind_SelectModeAnimation -> holder.enterSelectModeAnimation()
                RecyclerViewHolder.Bind_SelectChanged -> holder.checkSelect()
                RecyclerViewHolder.Bind_SelectModeQuit -> holder.quitSelectMode()
            }
        }
    }

    private fun registerReceiver() {
        val filter = IntentFilter()
        filter.addAction(DaPenTi.ACTION_PAGE_PREPARED)
        filter.addAction(DaPenTi.ACTION_PAGE_FAVORITE)
        DaPenTiApplication.getAppContext().registerReceiver(broadcastReceiver, filter)
    }

    fun unregisterReceiver() {
        DaPenTiApplication.getAppContext().unregisterReceiver(broadcastReceiver)
    }

    override fun getItemCount(): Int {
        return if (daPenTiPages == null) 0 else daPenTiPages!!.size
    }

    override fun onViewAttachedToWindow(holder: RecyclerViewHolder) {
        super.onViewAttachedToWindow(holder)
        holder.attachedToWindow()
    }

    override fun onViewDetachedFromWindow(holder: RecyclerViewHolder) {
        super.onViewDetachedFromWindow(holder)
        holder.detachedFromWindow()
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        Log.d(TAG, "onAttachedToRecyclerView")

        registerReceiver()
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        Log.d(TAG, "onDetachedFromRecyclerView")

        LocalBroadcastManager.getInstance(recyclerView.context).unregisterReceiver(broadcastReceiver)
        //unregisterReceiver()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerViewHolder {
        Log.d(TAG, "onCreateViewHolder")
        val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.penti_list_item, parent, false)
        return RecyclerViewHolder(v)
    }
}
