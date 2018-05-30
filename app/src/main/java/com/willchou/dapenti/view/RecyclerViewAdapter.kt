package com.willchou.dapenti.view

import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator

import com.willchou.dapenti.R
import com.willchou.dapenti.model.DaPenTi
import com.willchou.dapenti.model.DaPenTiCategory

class RecyclerViewAdapter(private val daPenTiCategoryIndex: Int,
                          private val fullScreenViewPair: EnhancedWebView.FullScreenViewPair,
                          private val fullScreenTriggered: EnhancedWebView.onFullScreenTriggered)
    : RecyclerView.Adapter<RecyclerViewHolder>() {
    companion object {
        private const val TAG = "RecyclerViewAdapter"
    }

    override fun onBindViewHolder(holder: RecyclerViewHolder, position: Int) {
        val daPenTi = DaPenTi.daPenTi
        if (daPenTi != null) {
            val c = daPenTi.daPenTiCategories.get(daPenTiCategoryIndex)
            holder.update(c.pages.get(position))
        }
    }

    override fun getItemCount(): Int {
        val daPenTi = DaPenTi.daPenTi ?: return 0

        val c = daPenTi.daPenTiCategories.get(daPenTiCategoryIndex)
        Log.d(TAG, "page.pages.size: " + c.pages.size)
        return c.pages.size
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
        val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.penti_list_item, parent, false)
        return RecyclerViewHolder(v, fullScreenTriggered, fullScreenViewPair)
    }
}
