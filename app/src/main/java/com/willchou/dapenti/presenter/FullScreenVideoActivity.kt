package com.willchou.dapenti.presenter

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Window
import android.view.WindowManager
import android.widget.LinearLayout
import com.willchou.dapenti.R
import com.willchou.dapenti.model.DaPenTi
import com.willchou.dapenti.view.VideoWebView

class FullScreenVideoActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "FullScreenVideoActivity"
    }
    private var videoLayout: LinearLayout? = null
    private var videoWebView: VideoWebView? = null

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                VideoWebView.ACTION_QUIT_FULLSCREEN -> onBackPressed()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate")
        super.onCreate(savedInstanceState)

        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN)

        setContentView(R.layout.activity_full_screen_video)

        videoLayout = findViewById(R.id.videoLayout)

        val intentFilter = IntentFilter(VideoWebView.ACTION_QUIT_FULLSCREEN)
        registerReceiver(broadcastReceiver, intentFilter)

        setupContent()
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        super.onDestroy()

        unregisterReceiver(broadcastReceiver)
    }

    override fun onBackPressed() {
        Log.d(TAG, "onBackPressed")

        videoWebView?.pauseVideo()
        videoWebView?.detachFromParent()

        super.onBackPressed()
    }

    private fun setupContent() {
        val pageTitle = intent.getStringExtra(DaPenTi.EXTRA_PAGE_TITLE)
        if (pageTitle == null) {
            Log.d(TAG, "unable to get page title from intent")
            onBackPressed()
        }

        videoWebView = VideoWebView.instanceCacheMap[pageTitle]
        if (videoWebView == null) {
            Log.d(TAG, "unable to get videoWebView from page $pageTitle")
            onBackPressed()
        }

        videoWebView!!.moveTo(videoLayout!!, true)
        videoWebView!!.startVideo()
    }
}
