package com.willchou.dapenti.presenter

import android.os.Bundle
import android.support.design.widget.CollapsingToolbarLayout
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.View
import android.widget.ImageView

import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.hannesdorfmann.swipeback.Position
import com.hannesdorfmann.swipeback.SwipeBack
import com.willchou.dapenti.R
import com.willchou.dapenti.model.Settings
import com.willchou.dapenti.view.EnhancedWebView

class DetailActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "DetailActivity"
        const val EXTRA_HTML = "extra_string_html"
        const val EXTRA_COVER_URL = "extra_string_cover"
        const val EXTRA_TITLE = "extra_string_title"
    }

    //private DaPenTi.DaPenTiContent daPenTiContent;
    private var webView: EnhancedWebView? = null
    private var coverImageView: ImageView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        SwipeBack.attach(this, Position.LEFT)
                .setContentView(R.layout.activity_detail)
                .setSwipeBackView(R.layout.swipeback_default)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { v: View -> onBackPressed() }

        coverImageView = findViewById(R.id.coverImage)
        webView = findViewById(R.id.webview)

        val fab = findViewById<FloatingActionButton>(R.id.fab)
        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
        }

        prepareContent()
    }

    override fun onBackPressed() {
        if (webView != null && webView!!.canGoBack()) {
            webView?.goBack()
            return
        }

        super.onBackPressed()
        overridePendingTransition(R.anim.swipeback_stack_to_front,
                R.anim.swipeback_stack_right_out)
    }

    private fun applyUserSettings() {
        val webSettings = webView?.settings ?: return
        val settings = Settings.settings ?: return

        when (settings.fontSize) {
            Settings.FontSizeSmall -> webSettings.defaultFontSize = 15
            Settings.FontSizeMedia -> webSettings.defaultFontSize = 17
            Settings.FontSizeBig -> webSettings.defaultFontSize = 19
            Settings.FontSizeSuperBig -> webSettings.defaultFontSize = 21
        }
    }

    private fun prepareContent() {
        val intent = intent
        val htmlString = intent.getStringExtra(EXTRA_HTML)
        val titleString = intent.getStringExtra(EXTRA_TITLE)
        val coverString = intent.getStringExtra(EXTRA_COVER_URL)

        val collapsingToolbarLayout = findViewById<CollapsingToolbarLayout>(R.id.toolbar_layout)
        collapsingToolbarLayout.title = titleString

        applyUserSettings()

        val options = RequestOptions()
                .centerCrop()
                .placeholder(R.drawable.cat)
                .error(R.drawable.cat)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .priority(Priority.HIGH)

        val settings = Settings.settings
        if (settings != null && settings.isImageEnabled) {
            Glide.with(this)
                    .load(coverString)
                    .apply(options)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(coverImageView!!)
        }

        webView?.loadDataWithBaseURL(null, htmlString,
                "text/html", "UTF-8", null)
    }
}