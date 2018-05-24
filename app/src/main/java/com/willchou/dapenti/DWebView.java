package com.willchou.dapenti;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.http.SslError;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class DWebView extends WebView {
    static private final String TAG = "DWebView";

    private boolean loadFinished;
    public boolean playOnLoadFinished = false;

    public DWebView(Context context) {
        super(context);
        setup();
    }

    public DWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setup();
    }

    public DWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setup();
    }

    private void setup() {
        loadFinished = false;

        WebSettings settings = getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);

        setWebChromeClient(new WebChromeClient() {
            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                //super.onShowCustomView(view, callback);
            }

            @Override
            public void onHideCustomView() {
                //super.onHideCustomView();
            }

            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                Log.d("webview console: ", consoleMessage.message());
                return super.onConsoleMessage(consoleMessage);
            }
        });

        setWebViewClient(new WebViewClient(){
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                Log.d(TAG, "onPageStarted");
                // TODO: 显示等待
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
