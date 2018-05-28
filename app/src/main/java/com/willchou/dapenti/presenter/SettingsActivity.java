package com.willchou.dapenti.presenter;

import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;

import com.hannesdorfmann.swipeback.Position;
import com.hannesdorfmann.swipeback.SwipeBack;
import com.willchou.dapenti.R;

public class SettingsActivity extends AppCompatActivity {
    static private final String TAG = "SettingsActivity";
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        SwipeBack.attach(this, Position.LEFT)
                .setContentView(R.layout.activity_settings)
                .setSwipeBackView(R.layout.swipeback_default);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle(R.string.title_activity_settings);
        toolbar.setNavigationOnClickListener((View v) -> onBackPressed());

        getFragmentManager()
                .beginTransaction()
                .replace(R.id.setting_content, new SettingsFragment())
                .commit();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.swipeback_stack_to_front,
                R.anim.swipeback_stack_right_out);
    }

    public static class SettingsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);
        }

        @Override
        public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
                                             Preference preference) {
            Log.d(TAG, "preference clicked: " + preference);
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }
    }
}
