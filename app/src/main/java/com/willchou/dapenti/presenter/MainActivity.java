package com.willchou.dapenti.presenter;

import android.content.pm.ActivityInfo;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

import com.willchou.dapenti.R;
import com.willchou.dapenti.model.DaPenTi;
import com.willchou.dapenti.model.DaPenTiCategory;
import com.willchou.dapenti.view.DWebView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    static private final String TAG = "MainActivity";

    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setVisibility(View.GONE);

        DaPenTi.storageDir = getFilesDir().getAbsolutePath();
        DaPenTi.categoryPrepared = () -> runOnUiThread(this::setupContent);

        new Thread(DaPenTi::prepareCategory).start();
    }

    private void setupContent() {
        toolbar.setVisibility(View.VISIBLE);

        TabLayout tabLayout = findViewById(R.id.tabs);
        tabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);

        ViewPager viewPager = findViewById(R.id.viewpager);
        setupViewPager(viewPager);
        tabLayout.setupWithViewPager(viewPager);
    }

    private void setupViewPager(ViewPager viewPager) {
        Adapter adapter = new Adapter(getSupportFragmentManager());

        DWebView.FullScreenViewPair viewPair = new DWebView.FullScreenViewPair();
        viewPair.nonVideoLayout = findViewById(R.id.coordinatorLayout);
        viewPair.videoLayout = findViewById(R.id.fullscreenVideo);

        for (int i = 0; i < DaPenTi.daPenTiCategories.size(); i ++) {
            DaPenTiCategory c = DaPenTi.daPenTiCategories.get(i);
            adapter.addFragment(c.getCategoryName(),
                    new ListFragment().setDaPenTiItemIndex(i, viewPair));
        }

        viewPager.setAdapter(adapter);
    }

    static class Adapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragments = new ArrayList<>();
        private final List<String> mFragmentTitles = new ArrayList<>();

        Adapter(FragmentManager fm) { super(fm); }

        void addFragment(String title, Fragment fragment) {
            mFragmentTitles.add(title);
            mFragments.add(fragment);

            Log.d(TAG, "title: " + title + ", fragment: " + fragment);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragments.get(position);
        }

        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitles.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentTitles.size();
        }
    }
}
