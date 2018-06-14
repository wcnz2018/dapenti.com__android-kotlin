package com.willchou.dapenti.view

import android.databinding.DataBindingUtil
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import com.willchou.dapenti.R
import com.willchou.dapenti.databinding.PentiListItemBinding
import com.willchou.dapenti.model.DaPenTiPage
import com.willchou.dapenti.model.Settings

class RecyclerViewAdapter(var daPenTiPages: List<DaPenTiPage>?)
    : RecyclerView.Adapter<RecyclerViewHolder>() {
    companion object {
        private const val TAG = "RecyclerViewAdapter"
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
                RecyclerViewHolder.Bind_ShowContent -> holder.setupContent(Settings.settings!!.canPlayVideo())
                RecyclerViewHolder.Bind_PageFailed -> holder.invalidContent()
                RecyclerViewHolder.Bind_Favorite -> holder.checkFavorite()
                RecyclerViewHolder.Bind_Collapse -> holder.hideContent(true)
                RecyclerViewHolder.Bind_SelectModeAnimation -> holder.enterSelectModeAnimation()
                RecyclerViewHolder.Bind_SelectChanged -> holder.checkSelect()
                RecyclerViewHolder.Bind_SelectModeQuit -> holder.quitSelectMode()
                RecyclerViewHolder.Bind_PageLoadFinished -> holder.pageLoadFinished()
            }
        }
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerViewHolder {
        Log.d(TAG, "onCreateViewHolder")
        val binding: PentiListItemBinding  = DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                R.layout.penti_list_item, parent, false)

        return RecyclerViewHolder(binding.root, binding)
    }
}
