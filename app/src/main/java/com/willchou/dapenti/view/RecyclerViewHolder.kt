package com.willchou.dapenti.view

import android.content.Intent
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.support.design.widget.Snackbar
import android.support.v7.widget.CardView
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.bumptech.glide.Glide
import com.willchou.dapenti.R
import com.willchou.dapenti.model.DaPenTiPage
import com.willchou.dapenti.model.Settings
import com.willchou.dapenti.model.Settings.Companion.settings
import com.willchou.dapenti.presenter.DetailActivity

class RecyclerViewHolder internal constructor(private val mView: View)
    : RecyclerView.ViewHolder(mView) {
    companion object {
        private const val TAG = "RecyclerViewHolder"

        private const val PageProperty_Expanded = "pp_expand"
        private const val PageProperty_WebView = "pp_webView"
    }

    private val cardView: CardView = mView.findViewById(R.id.cardView)

    private val titleTextView: TextView = mView.findViewById(R.id.title)
    private val descriptionTextView: TextView = mView.findViewById(R.id.description)
    private val imageView: ImageView = mView.findViewById(R.id.image)
    private val favoriteImage: ImageView = mView.findViewById(R.id.page_favorite_iv)
    private val videoLayout: LinearLayout = mView.findViewById(R.id.webViewLayout)

    private var foregroundColor: Int = Color.BLACK
    private var backgroundColor: Int = Color.WHITE

    private var page: DaPenTiPage? = null

    init {
        mView.setOnClickListener { v: View ->
            if (page!!.initiated())
                triggerContent(v)
            else
                Thread(Runnable { page!!.prepareContent() }).start()
        }

        mView.setOnLongClickListener { v: View ->
            Log.d(TAG, "long press: $v")
            val newFavorite = !page!!.getFavorite();
            page!!.setFavorite(newFavorite)

            var s = "已从收藏中移除"
            if (newFavorite)
                s = "已加入收藏"
            Snackbar.make(v, s, Snackbar.LENGTH_SHORT).show()

            true
        }
    }

    private val pageEventListener = object : DaPenTiPage.PageEventListener {
        override fun onContentPrepared() {
            Handler(Looper.getMainLooper()).post { triggerContent(mView) }
        }

        override fun onFavoriteChanged(favorite: Boolean) {
            Log.d(TAG, "onFavoriteChanged: $favorite")
            favoriteImage.visibility = if (favorite) View.VISIBLE else View.GONE
        }
    }

    private fun checkNightMode() {
        val nightMode = Settings.settings?.nightMode
        backgroundColor = Color.WHITE
        foregroundColor = Color.BLACK
        if (nightMode != null && nightMode) {
            backgroundColor = Color.rgb(66, 66, 66)
            foregroundColor = Color.rgb(213, 213, 213)
        }

        titleTextView.setTextColor(foregroundColor)
        descriptionTextView.setTextColor(foregroundColor)

        cardView.setCardBackgroundColor(backgroundColor)
        Log.d(TAG, "change cardView background to ${String.format("#%06X", backgroundColor)}")
    }

    internal fun update(page: DaPenTiPage) {
        this.page = page
    }

    internal fun attachedToWindow() {
        Log.d(TAG, "attachToWindow: " + page!!.pageTitle)

        checkNightMode()
        titleTextView.text = page!!.pageTitle

        if (page?.getProperty(PageProperty_Expanded) != null)
            showContent(mView, false)

        page?.pageEventListener = pageEventListener

        favoriteImage.visibility = if (page!!.getFavorite()) View.VISIBLE else View.GONE
    }

    internal fun detachedFromWindow() {
        Log.d(TAG, "detachedFromWindow: ${page?.pageTitle}")
        page?.pageEventListener = null
        hideContent()
    }

    private fun hideContent() {
        markExpanded(false)
        hideDescription()
        hideImage()
        hideVideo()
    }

    private fun markExpanded(expanded: Boolean) {
        if (expanded) {
            titleTextView.setTextColor(backgroundColor)
            titleTextView.setBackgroundColor(Color.GRAY)
        } else {
            titleTextView.setTextColor(foregroundColor)
            titleTextView.setBackgroundColor(0)
        }
    }

    private fun hideDescription() {
        descriptionTextView.visibility = View.GONE
    }

    private fun showDescription(s: String?) {
        when (settings?.fontSize) {
            Settings.FontSizeSmall -> descriptionTextView.textSize = 14f
            Settings.FontSizeMedia -> descriptionTextView.textSize = 15f
            Settings.FontSizeBig -> descriptionTextView.textSize = 17f
            Settings.FontSizeSuperBig -> descriptionTextView.textSize = 20f
        }

        if (s != null)
            descriptionTextView.text = s
        else
            descriptionTextView.text = "没有识别到内容哦!"
        descriptionTextView.visibility = View.VISIBLE
    }

    private fun hideImage() {
        imageView.visibility = View.GONE
        imageView.setImageDrawable(null)
    }

    private fun showImage(v: View, imageUrl: String?) {
        imageView.setImageDrawable(null)
        if (settings != null && settings!!.isImageEnabled) {
            Glide.with(v.context)
                    .load(imageUrl)
                    .into(imageView)
            imageView.visibility = View.VISIBLE
        }
    }

    private fun hideVideo() {
        val enhancedWebView = page?.getObjectProperty(PageProperty_WebView) as EnhancedWebView?
        enhancedWebView?.pauseVideo()

        videoLayout.visibility = View.GONE
        videoLayout.removeAllViews()
    }

    private fun showVideo(v: View, contentHtml: String, autoPlay: Boolean) {
        var enhancedWebView = page?.getObjectProperty(PageProperty_WebView) as EnhancedWebView?
        if (enhancedWebView == null) {
            enhancedWebView = EnhancedWebView(v.context)
            page!!.setObjectProperty(PageProperty_WebView, enhancedWebView)

            enhancedWebView.playOnLoadFinished = autoPlay
            enhancedWebView.loadDataWithBaseURL("", contentHtml,
                    "text/html", "UTF-8", null)
        } else {
            // the enhancedWebView may be possessed by another videoLayout
            // when we jump to favorite activity, detach it from parent anyway
            val p = enhancedWebView.parent as ViewGroup?
            p?.removeView(enhancedWebView)
        }

        videoLayout.addView(enhancedWebView)

        enhancedWebView.smallScreenVideoLayout = videoLayout
        enhancedWebView.prepareFullScreen()

        videoLayout.visibility = View.VISIBLE

        if (autoPlay)
            enhancedWebView.startVideo()
    }

    private fun showContent(v: View, playVideo: Boolean?) {
        markExpanded(true)
        when (page!!.pageType) {
            DaPenTiPage.PageTypeNote ->
                showDescription(page!!.pageNotes.content)

            DaPenTiPage.PageTypePicture -> {
                val picture = page!!.pagePicture
                showDescription(picture.description)
                showImage(v, picture.imageUrl)
            }

            DaPenTiPage.PageTypeVideo -> {
                val pageVideo = page!!.pageVideo
                showVideo(v, pageVideo.contentHtml!!, playVideo != null && playVideo)
            }

            DaPenTiPage.PageTypeLongReading -> {
                markExpanded(false)

                // show content in next click
                page!!.remove(PageProperty_Expanded)
                val pageLongReading = page!!.pageLongReading

                val context = v.context
                val intent = Intent(context, DetailActivity::class.java)
                intent.putExtra(DetailActivity.EXTRA_HTML, pageLongReading.contentHtml)
                intent.putExtra(DetailActivity.EXTRA_COVER_URL, pageLongReading.coverImageUrl)
                intent.putExtra(DetailActivity.EXTRA_TITLE, page!!.pageTitle)

                context.startActivity(intent)
            }

            else -> {
                showDescription(null)
            }
        }
    }

    private fun triggerContent(v: View) {
        Log.d(TAG, "update with pageType: " + page!!.pageType)

        val expanded = page!!.getProperty(PageProperty_Expanded) != null

        if (expanded) {
            page!!.remove(PageProperty_Expanded)
            hideContent()
        } else {
            page!!.setProperty(PageProperty_Expanded, "yes")
            showContent(v, true)
        }
    }
}
