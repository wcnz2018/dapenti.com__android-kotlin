package com.willchou.dapenti.presenter

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Window
import android.view.WindowManager
import android.widget.LinearLayout
import com.willchou.dapenti.R
import com.willchou.dapenti.view.VideoWebView
import java.util.*

class FullScreenVideoActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "FullScreenVideoActivity"
    }
    private var videoLayout: LinearLayout? = null
    private var videoWebView: VideoWebView? = null

    private var videoFullscreenObserver = Observer { _, pFullScreen ->
        val fullscreen = pFullScreen as Boolean
        if (!fullscreen) onBackPressed()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate")
        super.onCreate(savedInstanceState)

        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN)

        setContentView(R.layout.activity_full_screen_video)

        videoLayout = findViewById(R.id.videoLayout)

        setupContent()
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        super.onDestroy()

        videoWebView?.fullScreen?.deleteObserver(videoFullscreenObserver)
    }

    override fun onBackPressed() {
        Log.d(TAG, "onBackPressed")

        videoWebView?.pauseVideo()
        videoWebView?.detachFromParent()

        super.onBackPressed()
    }

    private fun setupContent() {
        val pageTitle = intent.getStringExtra(VideoWebView.EXTRA_PAGE_TITLE)
        Log.d(TAG, "play full screen video with title $pageTitle")
        if (pageTitle == null) {
            Log.d(TAG, "unable to get page title from intent")
            onBackPressed()
        }

        videoWebView = VideoWebView.instanceCacheMap[pageTitle]
        if (videoWebView == null) {
            Log.d(TAG, "unable to get videoWebView from page $pageTitle")
            onBackPressed()
        }

        videoWebView!!.fullScreen.addObserver(videoFullscreenObserver)

        videoWebView!!.moveTo(videoLayout!!, true)
        videoWebView!!.startVideo()
    }
}
