package com.willchou.dapenti.presenter

import android.annotation.SuppressLint
import android.graphics.Color
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import com.willchou.dapenti.R
import com.willchou.dapenti.model.DaPenTi
import com.willchou.dapenti.model.Settings
import com.willchou.dapenti.view.EnhancedWebView
import com.willchou.dapenti.view.RecyclerViewAdapter

class FavoriteActivity : AppCompatActivity() {
    private var recyclerView: RecyclerView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorite)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { onBackPressed() }

        recyclerView = findViewById(R.id.recycler_view)

        setupRecyclerView()
        setupEnhancedWebView()
    }

    override fun onResume() {
        super.onResume()
        setupEnhancedWebView()
    }

    override fun onPause() {
        super.onPause()

        // release those listeners to prevent memory leak
        recyclerView?.adapter = null
        EnhancedWebView.enhancedWebViewCallback = null
    }

    private fun setupRecyclerView() {
        val pageList = DaPenTi.daPenTi?.getFavoritePages()
        if (pageList == null)
            finish()

        recyclerView?.layoutManager = LinearLayoutManager(this)

        val nightMode = Settings.settings?.nightMode
        if (nightMode != null && nightMode)
            recyclerView?.setBackgroundColor(Color.rgb(48, 48, 48))
        else
            recyclerView?.setBackgroundColor(Color.WHITE)

        recyclerView?.adapter = RecyclerViewAdapter(pageList!!)
    }

    private fun setupEnhancedWebView() {
        EnhancedWebView.enhancedWebViewCallback = object : EnhancedWebView.EnhancedWebViewCallback {
            override fun fullscreenTriggered(fullscreen: Boolean) {
                if (fullscreen) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LOW_PROFILE
                    //activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
                    //activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                }
            }

            @SuppressLint("WrongViewCast")
            override fun getFullScreenVideoLayout(): ViewGroup {
                return findViewById(R.id.fullscreenVideo)
            }
        }
    }
}
