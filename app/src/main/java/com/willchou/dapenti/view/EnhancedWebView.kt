package com.willchou.dapenti.view

import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.net.http.SslError
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.ConsoleMessage
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient

class EnhancedWebView : VideoEnabledWebView {
    var fullScreenTriggered: onFullScreenTriggered? = null

    private var loadFinished: Boolean = false
    var playOnLoadFinished = false

    interface onFullScreenTriggered {
        fun triggered(fullscreen: Boolean)
    }

    class FullScreenViewPair {
        var nonVideoLayout: ViewGroup? = null
        var videoLayout: ViewGroup? = null
    }

    constructor(context: Context) : super(context) {
        setup()
    }

    constructor(context: Context, attrs: AttributeSet) :
            super(context, attrs) {
        setup()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) :
            super(context, attrs, defStyleAttr) {
        setup()
    }

    fun prepareFullScreen(p: FullScreenViewPair) {
        val client = VideoEnabledWebChromeClient(p.nonVideoLayout!!,
                p.videoLayout!!, null, this)
        client.setOnToggledFullscreen(object : VideoEnabledWebChromeClient.ToggledFullscreenCallback {
            override fun toggledFullscreen(fullscreen: Boolean) {
                fullScreenTriggered!!.triggered(fullscreen)
            }
        })
        webChromeClient = client
    }

    private fun setup() {
        loadFinished = false

        val settings = settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true

        webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                Log.d(TAG, "onPageStarted")
                // TODO: 显示等待
            }

            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                view.loadUrl(url)
                return true
            }

            override fun onReceivedHttpError(view: WebView, request: WebResourceRequest,
                                             errorResponse: WebResourceResponse) {
                Log.d(TAG, "onReceivedHttpError")
                // TODO:
            }

            override fun onPageFinished(view: WebView, url: String) {
                Log.d(TAG, "onPageFinished")
                loadFinished = true
                if (playOnLoadFinished) {
                    playOnLoadFinished = false
                    startVideo()
                }
                //view.loadUrl("javascript:(function() { document.getElementsByTagName('video')[0].play(); })()");
                // TODO: 退出等待
            }

            override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
                handler.proceed()
            }
        }
    }

    fun startVideo() {
        if (loadFinished)
            loadUrl("javascript:(function() { document.getElementsByTagName('video')[0].play(); })()")
    }

    fun pauseVideo() {
        playOnLoadFinished = false
        if (loadFinished)
            loadUrl("javascript:(function() { document.getElementsByTagName('video')[0].pause(); })()")
    }

    companion object {
        private val TAG = "DWebView"
    }
}