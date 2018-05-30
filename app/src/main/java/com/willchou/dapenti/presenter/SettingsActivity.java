package com.willchou.dapenti.presenter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
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
import com.willchou.dapenti.model.Settings;

public class SettingsActivity extends AppCompatActivity {
    static private final String TAG = "SettingsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        SwipeBack.attach(this, Position.LEFT)
                .setContentView(R.layout.activity_settings)
                .setSwipeBackView(R.layout.swipeback_default);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener((View v) -> onBackPressed());

        getFragmentManager()
                .beginTransaction()
                .replace(R.id.setting_content, new SettingsFragment(this))
                .commit();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.swipeback_stack_to_front,
                R.anim.swipeback_stack_right_out);
    }

    @SuppressLint("ValidFragment")
    public static class SettingsFragment extends PreferenceFragment {
        private Context context;

        SettingsFragment (Context c) {
            this.context = c;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);
        }

        @Override
        public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
                                             Preference preference) {
            Log.d(TAG, "preference clicked: " + preference);

            String aboutTitle = getResources().getString(R.string.setting_other_about);
            String title = preference.getTitle().toString();
            if (title.equals(aboutTitle)) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(preference.getSummary().toString()));
                startActivity(intent);
                return true;
            }

            String key = preference.getKey();
            if (key == null)
                return super.onPreferenceTreeClick(preferenceScreen, preference);

            if (key.equals(getResources().getString(R.string.pref_key_order_page))) {
                Intent intent = new Intent(context, PageOrderActivity.class);
                startActivity(intent);
            }

            return true;
        }
    }
}
