package com.willchou.dapenti.presenter

import android.graphics.Point
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.support.annotation.RequiresApi
import android.support.design.widget.AppBarLayout
import android.support.design.widget.CollapsingToolbarLayout
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.v4.content.ContextCompat
import android.support.v4.widget.NestedScrollView
import android.support.v7.widget.Toolbar
import android.view.KeyEvent
import android.webkit.WebView
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.willchou.dapenti.R
import com.willchou.dapenti.model.DaPenTi
import com.willchou.dapenti.model.DaPenTiPage
import com.willchou.dapenti.model.Settings
import me.majiajie.swipeback.SwipeBackActivity

class DetailActivity : SwipeBackActivity() {
    companion object {
        private const val TAG = "DetailActivity"
        const val EXTRA_HTML = "extra_string_html"
        const val EXTRA_COVER_URL = "extra_string_cover"
        const val EXTRA_TITLE = "extra_string_title"
    }

    private var appBarLayout: AppBarLayout? = null
    private var nestedScrollView: NestedScrollView? = null
    private var webView: WebView? = null
    private var coverImageView: ImageView? = null
    private var floatingActionButton: FloatingActionButton? = null

    private var scrollHeight: Int = 0
    private var appBarVisible: Boolean = true

    private var daPenTiPage: DaPenTiPage? = null

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        val toolbar = findViewById(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { onBackPressed() }

        prepareViews()
        prepareScroll()
        prepareContent()
    }

    override fun onBackPressed() {
        if (webView != null && webView!!.canGoBack()) {
            webView?.goBack()
            return
        }

        super.onBackPressed()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val y = nestedScrollView!!.scrollY

        when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                if (appBarVisible) {
                    appBarLayout?.setExpanded(false, true)
                    return true
                }

                nestedScrollView?.scrollTo(0, y + scrollHeight)
                return true
            }

            KeyEvent.KEYCODE_VOLUME_UP -> {
                if (y == 0) {
                    appBarLayout?.setExpanded(true, true)
                    return true
                }

                nestedScrollView?.scrollTo(0, y - scrollHeight)
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun updateFavorite(favorite: Boolean) {
        val drawable = ContextCompat.getDrawable(this,
                if (favorite) R.drawable.favorite else R.drawable.not_favorite
        )
        floatingActionButton?.setImageDrawable(drawable)
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

    private fun prepareScroll() {
        val rect = Rect()
        window.decorView.getWindowVisibleDisplayFrame(rect)
        scrollHeight = rect.height()

        val point = Point()
        windowManager.defaultDisplay.getSize(point)
        // height of status bar
        scrollHeight -= (24 * resources.displayMetrics.density).toInt()
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun prepareViews() {
        appBarLayout = findViewById(R.id.app_bar) as AppBarLayout
        appBarLayout!!.addOnOffsetChangedListener { _, verticalOffset ->
            appBarVisible = Math.abs(verticalOffset) != appBarLayout?.totalScrollRange
        }

        // to prevent error: can't call void android.view.View.setElevation(float) on null object
        appBarLayout!!.stateListAnimator = null

        nestedScrollView = findViewById(R.id.nestedScrollView) as NestedScrollView

        webView = findViewById(R.id.webview) as WebView
        coverImageView = findViewById(R.id.coverImage) as ImageView

        floatingActionButton = findViewById(R.id.fab) as FloatingActionButton
        floatingActionButton?.setOnClickListener { view ->
            val newFavorite = !daPenTiPage!!.getFavorite();
            daPenTiPage!!.setFavorite(newFavorite)
            updateFavorite(newFavorite)

            val s = if (newFavorite) "已加入收藏" else "已从收藏中移除"
            Snackbar.make(view, s, Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun prepareContent() {
        val intent = intent
        val htmlString = intent.getStringExtra(EXTRA_HTML)
        val titleString = intent.getStringExtra(EXTRA_TITLE)
        val coverString = intent.getStringExtra(EXTRA_COVER_URL)

        daPenTiPage = DaPenTi.daPenTi?.findPageByTitle(titleString)
        updateFavorite(daPenTiPage!!.getFavorite())

        val collapsingToolbarLayout = findViewById(R.id.toolbar_layout) as CollapsingToolbarLayout
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
