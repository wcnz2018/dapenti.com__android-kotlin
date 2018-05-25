package com.willchou.dapenti.view;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
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

    private DWebView.onFullScreenTriggered fullScreenTriggered;
    private DWebView.FullScreenViewPair fullScreenViewPair;

    public RecyclerViewAdapter(int daPenTiCategoryIndex,
                               DWebView.FullScreenViewPair fullScreenViewPair,
                               DWebView.onFullScreenTriggered triggered) {
        this.daPenTiCategoryIndex = daPenTiCategoryIndex;
        this.fullScreenViewPair = fullScreenViewPair;
        this.fullScreenTriggered = triggered;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerViewHolder holder, int position) {
        DaPenTiCategory c = DaPenTi.daPenTiCategories.get(daPenTiCategoryIndex);
        holder.update(c.pages.get(position));
        //enterAnimation(holder.itemView);
    }

    @Override
    public int getItemCount() {
        DaPenTiCategory c = DaPenTi.daPenTiCategories.get(daPenTiCategoryIndex);
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

    private void enterAnimation(View view) {
        // TODO: find Y
        view.setTranslationY(1920);
        view.animate()
                .translationY(0)
                .setInterpolator(new DecelerateInterpolator(3.f))
                .setDuration(400)
                .start();
    }
}
