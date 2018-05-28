package com.willchou.dapenti.presenter;

import android.app.Activity;
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
import android.view.Window;
import android.view.WindowManager;

import com.willchou.dapenti.R;
import com.willchou.dapenti.model.DaPenTi;
import com.willchou.dapenti.model.DaPenTiCategory;
import com.willchou.dapenti.view.EnhancedWebView;
import com.willchou.dapenti.view.RecyclerViewAdapter;

import java.util.List;

public class ListFragment extends Fragment {
    static private final String TAG = "ListFragment";
    static private final String BSCategoryIndex = "daPenTiCategoryIndex";

    private int daPenTiCategoryIndex = -1;
    private DaPenTiCategory daPenTiCategory;

    private RecyclerView recyclerView;
    private RecyclerViewAdapter recyclerViewAdapter;

    private EnhancedWebView.FullScreenViewPair fullScreenViewPair;

    private EnhancedWebView.onFullScreenTriggered fullScreenTriggered = fullscreen -> {
        Activity activity = getActivity();
        if (activity == null) {
            Log.d(TAG, "Unable to get activity");
            return;
        }

        Window window = activity.getWindow();

        if (fullscreen) {
            //noinspection all
            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
            //activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else {
            //noinspection all
            window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
            //activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    };

    ListFragment setDaPenTiItemIndex(int daPenTiCategoryIndex,
                                     EnhancedWebView.FullScreenViewPair fullScreenViewPair) {
        this.daPenTiCategoryIndex = daPenTiCategoryIndex;
        this.daPenTiCategory = DaPenTi.daPenTiCategories.get(daPenTiCategoryIndex);
        this.fullScreenViewPair = fullScreenViewPair;
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
        recyclerView = (RecyclerView) inflater.inflate(R.layout.penti_list,
                container, false);

        Log.d(TAG, "onCreateView：savedInstanceState: " + savedInstanceState
                + ", index: " + daPenTiCategoryIndex);
        Log.d(TAG, "onCreateView: adapter: " + recyclerViewAdapter);

        if (savedInstanceState != null)
            daPenTiCategoryIndex = savedInstanceState.getInt(BSCategoryIndex);

        prepareContent();

        return recyclerView;
    }

    private void setupRecyclerView() {
        Log.d(TAG, "setupRecyclerView with adapter: " + recyclerViewAdapter);
        if (recyclerViewAdapter == null)
            recyclerViewAdapter = new RecyclerViewAdapter(daPenTiCategoryIndex,
                    fullScreenViewPair,
                    fullScreenTriggered);
        recyclerView.setLayoutManager(new LinearLayoutManager(recyclerView.getContext()));
        recyclerView.setAdapter(recyclerViewAdapter);
    }

    private void prepareContent() {
        List<DaPenTiCategory> dptcs = DaPenTi.daPenTiCategories;
        Log.d(TAG, "DPTCategoryies: " + dptcs);
        if (dptcs == null)
            return;

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
