package com.willchou.dapenti.view

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.http.SslError
import android.util.AttributeSet
import android.util.Base64
import android.util.Log
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import android.widget.FrameLayout
import android.widget.Toast
import com.willchou.dapenti.DaPenTiApplication
import com.willchou.dapenti.model.DaPenTi
import com.willchou.dapenti.model.Settings
import android.webkit.WebView




class VideoWebView : WebView {
    companion object {
        private const val TAG = "VideoWebView"

        const val ACTION_ENTER_FULLSCREEN = "com.willchou.dapenti.EnhancedWebViewEnterFullScreen"
        const val ACTION_QUIT_FULLSCREEN = "com.willchou.dapenti.EnhancedWebViewQuitFullScreen"
    }

    constructor(context: Context) : super(context) {
        setup()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        setup()
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        setup()
    }

    var enableZoomGesture: Boolean = false
    private var defaultFontSize: Int = 0
    private var scaleGestureDetector: ScaleGestureDetector? = null

    interface VideoWebViewContentEventListener {
        fun onLoadFinished()
        fun onLoadFailed()
    }
    var videoWebViewContentEventListener: VideoWebViewContentEventListener? = null

    private var loadFinished: Boolean = false

    var belongPageTitle: String? = null
    var playOnLoadFinished = false

    private var videoViewContainer: FrameLayout? = null

    private var webViewClient = object : WebViewClient() {
        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            Log.d(TAG, "onPageStarted")
        }

        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
            view.loadUrl(url)
            return true
        }

        override fun onReceivedHttpError(view: WebView, request: WebResourceRequest,
                                         errorResponse: WebResourceResponse) {
            Log.d(TAG, "onReceivedHttpError")
            videoWebViewContentEventListener?.onLoadFailed()
        }

        override fun onPageFinished(view: WebView, url: String) {
            Log.d(TAG, "onPageFinished, playOnLoadFinished: $playOnLoadFinished")
            injectCSSOrJavaScript()

            loadFinished = true
            if (playOnLoadFinished) {
                playOnLoadFinished = false
                startVideo()
            }

            defaultFontSize = settings.defaultFontSize
            videoWebViewContentEventListener?.onLoadFinished()
        }

        override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
            handler.proceed()
        }
    }

    private val webChromeClient = object : WebChromeClient() {
        override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
            Log.d(TAG, "consoleMessage: " + consoleMessage?.message())
            return super.onConsoleMessage(consoleMessage)
        }

        override fun getDefaultVideoPoster(): Bitmap {
            return Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888);
        }

        override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
            if (view is FrameLayout) {
                notifyFullScreen(true)
                videoViewContainer = view
            }
            super.onShowCustomView(view, callback)
        }

        override fun onHideCustomView() {
            notifyFullScreen(false)
            super.onHideCustomView()
        }
    }

    private fun injectStyleSheet(style: String?) {
        if (style == null)
            return

        loadUrl("javascript:" +
                "var style = document.createElement('style');" +
                "style.innerHTML=\"$style\";" +
                "document.body.appendChild(style);")
    }

    private fun injectStyleSheetFromAsset(cssName: String) {
        Log.d(TAG, "injectStyleSheetFromAsset: $cssName")
        try {
            val inputStream = DaPenTiApplication.getAppContext().assets.open(cssName)
            val buffer = ByteArray(inputStream.available())
            inputStream.read(buffer)
            inputStream.close()
            val encoded = Base64.encodeToString(buffer, Base64.NO_WRAP)
            loadUrl("javascript:(function() {" +
                    "var parent = document.getElementsByTagName('head').item(0);" +
                    "var style = document.createElement('style');" +
                    "style.type = 'text/css';" +
                    // Tell the browser to BASE64-decode the string into your script !!!
                    "style.innerHTML = window.atob('" + encoded + "');" +
                    "parent.appendChild(style)" +
                    "})()")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun injectJavaScriptFromAsset(scriptName: String) {
        Log.d(TAG, "injectJavaScriptFromAsset: $scriptName")
        try {
            val input = DaPenTiApplication.getAppContext().assets.open(scriptName)
            val buffer = ByteArray(input.available())
            input.read(buffer)
            input.close()

            // String-ify the script byte-array using BASE64 encoding !!!
            val encoded = Base64.encodeToString(buffer, Base64.NO_WRAP)
            loadUrl("javascript:(function() {" +
                    "var parent = document.getElementsByTagName('head').item(0);" +
                    "var script = document.createElement('script');" +
                    "script.type = 'text/javascript';" +
                    // Tell the browser to BASE64-decode the string into your script !!!
                    "script.innerHTML = window.atob('" + encoded + "');" +
                    "parent.appendChild(script)" +
                    "})()")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun injectCSSOrJavaScript() {
        injectStyleSheetFromAsset("css/mediaelementplayer.min.css")
        injectStyleSheetFromAsset("css/custom.css")
        injectJavaScriptFromAsset("js/jquery.min.js")
        injectJavaScriptFromAsset("js/mediaelement-and-player.min.js")

        injectJavaScriptFromAsset("js/custom.js")

        // webView may use it's cache to present content,
        // day/night mode from new html which belongs to the same url
        // does not have effect any more, we change it manually after load finished
        injectStyleSheet(Settings.settings?.viewModeCSSStyle)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (enableZoomGesture)
            scaleGestureDetector?.onTouchEvent(event)
        return super.onTouchEvent(event)
    }

    private fun notifyFullScreen(enter: Boolean) = if (enter) {
        val intent = Intent(ACTION_ENTER_FULLSCREEN)
        intent.putExtra(DaPenTi.EXTRA_PAGE_TITLE, belongPageTitle)
        context.sendBroadcast(intent)
    } else {
        val intent = Intent(ACTION_QUIT_FULLSCREEN)
        context.sendBroadcast(intent)
    }

    fun detachFromParent() {
        (videoViewContainer?.parent as ViewGroup?)?.removeView(videoViewContainer)
        (parent as ViewGroup?)?.removeView(this)
    }

    fun moveTo(v: ViewGroup, fullScreen: Boolean) {
        detachFromParent()
        if (fullScreen) {
            val layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT)
            v.addView(videoViewContainer, layoutParams)
        } else
            v.addView(this)
    }

    private fun setup() {
        loadFinished = false

        val settings = settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true

        setWebViewClient(webViewClient)
        setWebChromeClient(webChromeClient)

        scaleGestureDetector = ScaleGestureDetector(context, OnScaleGestureListener())
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

    private inner class OnScaleGestureListener
        : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        private var toast: Toast? = null

        override fun onScale(detector: ScaleGestureDetector?): Boolean {
            Log.d(TAG, "onScale.detector: $detector, factor: ${detector?.scaleFactor}")
            if (detector == null)
                return super.onScale(detector)

            if (detector.scaleFactor >= 1f)
                settings.defaultFontSize += 1
            else
                settings.defaultFontSize -= 1

            val s = "${settings.defaultFontSize * 100 / defaultFontSize}%"

            if (toast != null) toast?.cancel()
            toast = Toast.makeText(context, s, Toast.LENGTH_SHORT)
            toast?.show()
            return true
        }

        override fun onScaleBegin(detector: ScaleGestureDetector?): Boolean {
            return true
        }

        override fun onScaleEnd(detector: ScaleGestureDetector?) {

        }
    }
}
