package com.willchou.dapenti

import android.app.Application
import android.content.Context
import android.preference.PreferenceManager
import com.squareup.leakcanary.LeakCanary
import com.willchou.dapenti.model.DaPenTi
import com.willchou.dapenti.model.Database
import com.willchou.dapenti.model.Settings
import me.majiajie.swipeback.utils.ActivityStack


class DaPenTiApplication : Application() {
    companion object {
        private var daPenTiApplication: DaPenTiApplication? = null

        fun getAppContext():Context {
            return daPenTiApplication!!.applicationContext
        }
    }

    override fun onCreate() {
        super.onCreate()

        daPenTiApplication = this

        prepareStaticEnv()
        prepareLeakCanary()

        registerActivityLifecycleCallbacks(ActivityStack.getInstance())
    }

    private fun prepareStaticEnv() {
        Settings()
                .initiate(PreferenceManager.getDefaultSharedPreferences(this),
                        resources)
        DaPenTi()
        Database(this)
    }

    private fun prepareLeakCanary() {
        if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return
        }
        LeakCanary.install(this)
    }
}
