package com.willchou.dapenti.view

import android.content.Context
import android.graphics.Bitmap
import android.net.http.SslError
import android.util.AttributeSet
import android.util.Log
import android.view.ViewGroup
import android.webkit.SslErrorHandler
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient

class EnhancedWebView : VideoEnabledWebView {
    companion object {
        private const val TAG = "DWebView"
        private const val nightModeStyle =
                "body {color:#d2d2d2 !important;background-color:#424242}" +
                "a {color:white;}"


        // Note: need to set to null in onPause() to prevent memory leak
        //       reassign in onResume() or somewhere
        var enhancedWebViewCallback: EnhancedWebViewCallback? = null
    }

    private var loadFinished: Boolean = false
    var playOnLoadFinished = false

    interface EnhancedWebViewCallback {
        fun fullscreenTriggered(fullscreen: Boolean)
        fun getFullScreenVideoLayout(): ViewGroup
    }

    var smallScreenVideoLayout: ViewGroup? = null

    var nightMode: Boolean = false

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

    fun prepareFullScreen() {
        val client = VideoEnabledWebChromeClient(smallScreenVideoLayout!!,
                enhancedWebViewCallback!!.getFullScreenVideoLayout(),
                null, this)
        client.setOnToggledFullscreen(object : VideoEnabledWebChromeClient.ToggledFullscreenCallback {
            override fun toggledFullscreen(fullscreen: Boolean) {
                enhancedWebViewCallback!!.fullscreenTriggered(fullscreen)
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

            private fun injectStyleSheet(view: WebView, style: String) {
                view.loadUrl("javascript:" +
                        "var style = document.createElement('style');" +
                        "style.innerHTML=\"$style\";" +
                        "document.body.appendChild(style);")
            }

            override fun onPageFinished(view: WebView, url: String) {
                Log.d(TAG, "onPageFinished")
                if (nightMode) {
                    injectStyleSheet(view, nightModeStyle)
                }

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
}
