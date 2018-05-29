package com.willchou.dapenti;

import android.app.Application;
import android.preference.PreferenceManager;

import com.squareup.leakcanary.LeakCanary;
import com.willchou.dapenti.model.DaPenTi;
import com.willchou.dapenti.model.Database;
import com.willchou.dapenti.model.Settings;

public class DPTApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        prepareStaticEnv();
        prepareLeakCanary();
    }

    private void prepareStaticEnv() {
        new Settings()
                .initiate(PreferenceManager.getDefaultSharedPreferences(this),
                        getResources());
        new DaPenTi();
        new Database(this);
    }

    private void prepareLeakCanary() {
        if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return;
        }
        LeakCanary.install(this);
    }
}
