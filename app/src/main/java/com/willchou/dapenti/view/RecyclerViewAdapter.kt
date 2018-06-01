package com.willchou.dapenti.view

import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import com.willchou.dapenti.R
import com.willchou.dapenti.model.DaPenTiPage

class RecyclerViewAdapter(private val daPenTiPages: List<DaPenTiPage>)
    : RecyclerView.Adapter<RecyclerViewHolder>() {
    companion object {
        private const val TAG = "RecyclerViewAdapter"
    }

    override fun onBindViewHolder(holder: RecyclerViewHolder, position: Int) {
        holder.update(daPenTiPages[position])
    }

    override fun getItemCount(): Int {
        return daPenTiPages.size
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
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        Log.d(TAG, "onDetachedFromRecyclerView")
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerViewHolder {
        Log.d(TAG, "onCreateViewHolder")
        val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.penti_list_item, parent, false)
        return RecyclerViewHolder(v)
    }
}
