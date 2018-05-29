package com.willchou.dapenti.view;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;

import com.willchou.dapenti.R;
import com.willchou.dapenti.model.DaPenTi;
import com.willchou.dapenti.model.DaPenTiCategory;

public class RecyclerViewAdapter
        extends RecyclerView.Adapter<RecyclerViewHolder> {
    static private final String TAG = "RecyclerViewAdapter";
    private int daPenTiCategoryIndex;

    private EnhancedWebView.onFullScreenTriggered fullScreenTriggered;
    private EnhancedWebView.FullScreenViewPair fullScreenViewPair;

    public RecyclerViewAdapter(int daPenTiCategoryIndex,
                               EnhancedWebView.FullScreenViewPair fullScreenViewPair,
                               EnhancedWebView.onFullScreenTriggered triggered) {
        this.daPenTiCategoryIndex = daPenTiCategoryIndex;
        this.fullScreenViewPair = fullScreenViewPair;
        this.fullScreenTriggered = triggered;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerViewHolder holder, int position) {
        DaPenTiCategory c = DaPenTi.daPenTiCategories.get(daPenTiCategoryIndex);
        holder.update(c.pages.get(position));
    }

    @Override
    public int getItemCount() {
        DaPenTiCategory c = DaPenTi.daPenTiCategories.get(daPenTiCategoryIndex);
        Log.d(TAG, "page.pages.size: " + c.pages.size());
        return c.pages.size();
    }

    @Override
    public void onViewAttachedToWindow(@NonNull RecyclerViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        holder.attachedToWindow();
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull RecyclerViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        holder.detachedFromWindow();
    }

    @NonNull
    @Override
    public RecyclerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.penti_list_item, parent, false);
        return new RecyclerViewHolder(v, fullScreenTriggered, fullScreenViewPair);
    }
}
