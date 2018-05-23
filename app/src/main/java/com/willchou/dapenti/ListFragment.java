package com.willchou.dapenti;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.willchou.dapenti.model.DaPenTi;
import com.willchou.dapenti.model.DaPenTiCategory;

import java.util.List;

public class ListFragment extends Fragment {
    static private final String TAG = "ListFragment";
    static private final String BSCategoryIndex = "daPenTiCategoryIndex";

    private int daPenTiCategoryIndex = -1;
    private DaPenTiCategory daPenTiCategory;

    private RecyclerView recyclerView;
    private RecyclerViewAdapter recyclerViewAdapter;

    ListFragment setDaPenTiItemIndex(int daPenTiCategoryIndex) {
        this.daPenTiCategoryIndex = daPenTiCategoryIndex;
        this.daPenTiCategory = DaPenTi.daPenTiCategories.get(daPenTiCategoryIndex);
        return this;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putInt(BSCategoryIndex, daPenTiCategoryIndex);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            Log.d(TAG, "isVisibleToUser: " + daPenTiCategory.getCategoryName());
            //prepareContent();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        recyclerView = (RecyclerView) inflater.inflate(R.layout.penti_list, container, false);

        Log.d(TAG, "onCreateView： " + savedInstanceState + ", index: " + daPenTiCategoryIndex);
        Log.d(TAG, "onCreateView: adapter: " + recyclerViewAdapter);

        if (savedInstanceState != null)
            daPenTiCategoryIndex = savedInstanceState.getInt(BSCategoryIndex);

        prepareContent();

        return recyclerView;
    }

    private void setupRecyclerView() {
        recyclerViewAdapter = new RecyclerViewAdapter(daPenTiCategoryIndex);
        recyclerView.setLayoutManager(new LinearLayoutManager(recyclerView.getContext()));
        recyclerView.setAdapter(recyclerViewAdapter);
    }

    private void prepareContent() {
        List<DaPenTiCategory> dptcs = DaPenTi.daPenTiCategories;

        if (daPenTiCategoryIndex < 0 || daPenTiCategoryIndex >= dptcs.size())
            return;

        daPenTiCategory = DaPenTi.daPenTiCategories.get(daPenTiCategoryIndex);
        daPenTiCategory.categoryPrepared = index -> {
            Log.d(TAG, "new page prepared, index: " + index);
            if (getActivity() != null)
                getActivity().runOnUiThread(this::setupRecyclerView);
        };

        if (daPenTiCategory.initiated()) {
            setupRecyclerView();
        } else
            new Thread(daPenTiCategory::preparePages).start();
    }
}
