package com.willchou.dapenti.view

import android.arch.lifecycle.ViewModelProviders
import android.arch.paging.PagedListAdapter
import android.databinding.DataBindingUtil
import android.support.v4.app.FragmentActivity
import android.support.v7.util.DiffUtil
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import com.willchou.dapenti.R
import com.willchou.dapenti.databinding.PentiListItemBinding
import com.willchou.dapenti.db.DaPenTiData
import com.willchou.dapenti.model.Settings
import com.willchou.dapenti.vm.HolderViewModel

class RecyclerViewAdapter : PagedListAdapter<DaPenTiData.Page, RecyclerViewHolder>(diffCallback) {
    companion object {
        private const val TAG = "RecyclerViewAdapter"

        private val diffCallback = object : DiffUtil.ItemCallback<DaPenTiData.Page>() {
            override fun areItemsTheSame(oldItem: DaPenTiData.Page?,
                                         newItem: DaPenTiData.Page?): Boolean =
                    oldItem?.title == newItem?.title

            override fun areContentsTheSame(oldItem: DaPenTiData.Page?,
                                            newItem: DaPenTiData.Page?): Boolean =
                    oldItem == newItem
        }
    }

    override fun onBindViewHolder(holder: RecyclerViewHolder, position: Int) {
        val title = getItem(position)!!.title
        Log.d(TAG, "position: $position title: $title")
        val model = ViewModelProviders
                .of(holder.itemView.context as FragmentActivity, HolderViewModel.Factor(title))
                .get(title, HolderViewModel::class.java)
        holder.bindTo(model)
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
                //RecyclerViewHolder.Bind_PageFailed -> holder.invalidContent()
                RecyclerViewHolder.Bind_Favorite -> holder.checkFavorite()
                RecyclerViewHolder.Bind_Collapse -> holder.hideContent(true)
                RecyclerViewHolder.Bind_SelectModeAnimation -> holder.enterSelectModeAnimation()
                RecyclerViewHolder.Bind_SelectChanged -> holder.checkSelect()
                RecyclerViewHolder.Bind_SelectToggle -> holder.toggleSelect()
                RecyclerViewHolder.Bind_SelectModeQuit -> holder.quitSelectMode()
                RecyclerViewHolder.Bind_PageLoadFinished -> holder.pageLoadFinished()
            }
        }
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
