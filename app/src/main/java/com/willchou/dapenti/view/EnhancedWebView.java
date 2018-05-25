package com.willchou.dapenti.view;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.net.http.SslError;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.ConsoleMessage;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class EnhancedWebView extends VideoEnabledWebView {
    static private final String TAG = "DWebView";

    public interface onFullScreenTriggered {
        void triggered(boolean fullscreen);
    }
    public onFullScreenTriggered fullScreenTriggered = null;

    private boolean loadFinished;
    public boolean playOnLoadFinished = false;

    static public class FullScreenViewPair {
        public ViewGroup nonVideoLayout;
        public ViewGroup videoLayout;
    }

    public EnhancedWebView(Context context) {
        super(context);
        setup();
    }

    public EnhancedWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setup();
    }

    public EnhancedWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setup();
    }

    public void prepareFullScreen(FullScreenViewPair p) {
        VideoEnabledWebChromeClient client = new VideoEnabledWebChromeClient(p.nonVideoLayout,
                p.videoLayout, null, this);
        client.setOnToggledFullscreen(fullscreen -> {
            // Your code to handle the full-screen change, for example showing and hiding the title bar. Example:
            if (fullScreenTriggered != null)
                fullScreenTriggered.triggered(fullscreen);
        });
        setWebChromeClient(client);
    }

    private void setup() {
        loadFinished = false;

        WebSettings settings = getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);

        setWebViewClient(new WebViewClient(){
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                Log.d(TAG, "onPageStarted");
                // TODO: 显示等待
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }

            @Override
            public void onReceivedHttpError(WebView view, WebResourceRequest request,
                                            WebResourceResponse errorResponse) {
                Log.d(TAG, "onReceivedHttpError");
                // TODO:
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                Log.d(TAG, "onPageFinished");
                loadFinished = true;
                if (playOnLoadFinished) {
                    playOnLoadFinished = false;
                    startVideo();
                }
                //view.loadUrl("javascript:(function() { document.getElementsByTagName('video')[0].play(); })()");
                // TODO: 退出等待
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                handler.proceed();
            }
        });
    }

    public void startVideo() {
        if (loadFinished)
            loadUrl("javascript:(function() { document.getElementsByTagName('video')[0].play(); })()");
    }

    public void pauseVideo() {
        playOnLoadFinished = false;
        if (loadFinished)
            loadUrl("javascript:(function() { document.getElementsByTagName('video')[0].pause(); })()");
    }
}
