package com.willchou.dapenti.presenter

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.databinding.DataBindingUtil
import android.graphics.Point
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.support.annotation.Nullable
import android.support.annotation.RequiresApi
import android.support.design.widget.Snackbar
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.KeyEvent
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.willchou.dapenti.R
import com.willchou.dapenti.databinding.ActivityDetailBinding
import com.willchou.dapenti.db.DaPenTiData
import com.willchou.dapenti.model.Settings
import com.willchou.dapenti.vm.DetailViewModel
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import me.majiajie.swipeback.SwipeBackActivity

class DetailActivity : SwipeBackActivity() {
    companion object {
        private const val TAG = "DetailActivity"
        const val EXTRA_URL = "extra_url"
        const val EXTRA_HTML = "extra_string_html"
        const val EXTRA_COVER_URL = "extra_string_cover"
        const val EXTRA_TITLE = "extra_string_title"
    }

    private var binding: ActivityDetailBinding? = null

    private var scrollHeight: Int = 0
    private var appBarVisible: Boolean = true

    private var detailViewModel: DetailViewModel? = null
    private var observer = Observer<DaPenTiData.Page> { prepareContent() }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_detail)

        setSupportActionBar(binding!!.toolbar)
        binding!!.toolbar.setNavigationOnClickListener { onBackPressed() }

        val title = intent.getStringExtra(EXTRA_TITLE)
        detailViewModel = ViewModelProviders.of(this, DetailViewModel.Factor(title))
                .get(title, DetailViewModel::class.java)

        prepareViews()
        prepareScroll()
        prepareCover()

        Observable.just(detailViewModel)
                .subscribeOn(Schedulers.io())
                .doOnNext { it!!.initDB() }
                .subscribe { detailViewModel!!.pageData?.observeForever(observer) }
    }

    override fun onDestroy() {
        detailViewModel!!.pageData?.removeObserver(observer)
        super.onDestroy()
    }

    override fun onBackPressed() {
        val webView = binding!!.contentDetail?.webview
        if (webView != null && webView.canGoBack()) {
            webView.goBack()
            return
        }

        super.onBackPressed()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val y = binding!!.contentDetail!!.nestedScrollView.scrollY
        val appBar = binding!!.appBarLayout
        val nestedScrollView = binding!!.contentDetail!!.nestedScrollView

        Log.d(TAG, "appBarLayout visible: $appBarVisible")

        when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                if (appBarVisible)
                    appBar.setExpanded(false, true)
                else
                    nestedScrollView.scrollTo(0, y + scrollHeight)

                return true
            }

            KeyEvent.KEYCODE_VOLUME_UP -> {
                if (y == 0)
                    appBar.setExpanded(true, true)
                else
                    nestedScrollView.scrollTo(0, y - scrollHeight)

                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun updateFavorite(favorite: Boolean) {
        val drawable = ContextCompat.getDrawable(this,
                if (favorite) R.drawable.favorite else R.drawable.not_favorite
        )
        binding!!.fabActionButton.setImageDrawable(drawable)
    }

    private fun applyUserSettings() {
        val webSettings = binding!!.contentDetail!!.webview.settings ?: return
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
        binding!!.toolbarLayout.title = detailViewModel!!.pageTitle

        val appBarLayout = binding!!.appBarLayout
        appBarLayout.addOnOffsetChangedListener { _, verticalOffset ->
            appBarVisible = Math.abs(verticalOffset) != appBarLayout.totalScrollRange
        }

        // to prevent error: can't call void android.view.View.setElevation(float) on null object
        appBarLayout.stateListAnimator = null

        binding!!.contentDetail!!.webview.enableZoomGesture = true
        binding!!.contentDetail!!.webview.settings?.userAgentString = "Android"

        binding!!.fabActionButton.setOnClickListener { view ->
            val newFavorite = !detailViewModel!!.getFavorite()
            detailViewModel!!.setFavorite(newFavorite)

            updateFavorite(newFavorite)

            val s = if (newFavorite) "已加入收藏" else "已从收藏中移除"
            Snackbar.make(view, s, Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun prepareCover() {
        val coverString = intent.getStringExtra(EXTRA_COVER_URL)

        val options = RequestOptions()
                .centerCrop()
                .placeholder(R.drawable.cat)
                .error(R.drawable.cat)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .priority(Priority.HIGH)

        Glide.with(this)
                .load(coverString)
                .apply(options)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(binding!!.coverImageView)
    }

    private fun prepareContent() {
        updateFavorite(detailViewModel!!.getFavorite())

        val urlString = intent.getStringExtra(EXTRA_URL)
        val htmlString = intent.getStringExtra(EXTRA_HTML)

        applyUserSettings()

        if (urlString.isNullOrEmpty())
            binding!!.contentDetail!!.webview.loadDataWithBaseURL(null, htmlString,
                    "text/html", "UTF-8", null)
        else
            binding!!.contentDetail!!.webview.loadUrl(urlString)
    }
}
