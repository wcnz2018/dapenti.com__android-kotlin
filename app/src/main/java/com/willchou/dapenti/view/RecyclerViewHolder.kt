package com.willchou.dapenti.view

import android.content.Intent
import android.graphics.Color
import android.os.Handler
import android.support.design.widget.Snackbar
import android.support.v7.widget.CardView
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.bumptech.glide.Glide
import com.willchou.dapenti.DaPenTiApplication
import com.willchou.dapenti.R
import com.willchou.dapenti.model.DaPenTiPage
import com.willchou.dapenti.model.DaPenTiPage.Companion.PageProperty_Expanded
import com.willchou.dapenti.model.DaPenTiPage.Companion.PageProperty_WebView
import com.willchou.dapenti.model.Settings
import com.willchou.dapenti.model.Settings.Companion.settings
import com.willchou.dapenti.presenter.DetailActivity

class RecyclerViewHolder internal constructor(private val mView: View)
    : RecyclerView.ViewHolder(mView) {
    companion object {
        private const val TAG = "RecyclerViewHolder"

        const val Bind_PlayVideo = "playVideo"
        const val Bind_Favorite = "favorite"
    }

    private val cardView: CardView = mView.findViewById(R.id.cardView)

    private val titleTextView: TextView = mView.findViewById(R.id.title)
    private val progressBar: ProgressBar = mView.findViewById(R.id.progressBar)
    private val descriptionTextView: TextView = mView.findViewById(R.id.description)
    private val imageView: ImageView = mView.findViewById(R.id.image)
    private val favoriteImage: ImageView = mView.findViewById(R.id.page_favorite_iv)
    private val videoLayout: LinearLayout = mView.findViewById(R.id.webViewLayout)

    private var foregroundColor: Int = Color.BLACK
    private var backgroundColor: Int = Color.WHITE

    private var page: DaPenTiPage? = null

    init {
        mView.setOnClickListener { v: View ->
            if (page!!.initiated()) {
                Log.d(TAG, "on clicked: ${page?.pageTitle}" +
                        " page initiated: ${page?.initiated()}")
                // TODO: finish me
                //page!!.checkSmartContent()
                triggerContent(v)
            } else {
                setExpand(true, true)
                Handler().postDelayed({
                    if (!page!!.initiated())
                        showProgressBar()
                }, 500)
                Thread(Runnable { page!!.prepareContent() }).start()
            }
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

    fun checkFavorite() {
        favoriteImage.visibility = if (page!!.getFavorite()) View.VISIBLE else View.GONE
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
        Log.d(TAG, "update with page ${page.pageTitle}")
    }

    fun setupContent(playVideoIfPossible: Boolean) {
        titleTextView.text = page!!.pageTitle

        if (pageExpanded())
            showContent(mView, playVideoIfPossible)
    }

    internal fun attachedToWindow() {
        Log.d(TAG, "attachToWindow: ${page!!.pageTitle}," +
                "expanded: ${pageExpanded()}")

        checkNightMode()
        checkFavorite()
        setupContent(false)
    }

    internal fun detachedFromWindow() {
        Log.d(TAG, "detachedFromWindow: ${page?.pageTitle}")

        hideContent(false)
    }

    private fun pageExpanded(): Boolean {
        return page?.getProperty(PageProperty_Expanded) != null
    }

    private fun setExpand(expand: Boolean, saveState: Boolean) {
        Log.d(TAG, "setExpand ${page?.pageTitle} to $expand, saveState: $saveState")

        if (page?.pageTitle!!.contains("现实中见义勇为") && !saveState)
            Exception().printStackTrace()

        if (expand) {
            titleTextView.setTextColor(backgroundColor)
            titleTextView.setBackgroundColor(Color.GRAY)

            if (saveState)
                page!!.setProperty(PageProperty_Expanded, "yes")
        } else {
            titleTextView.setTextColor(foregroundColor)
            titleTextView.setBackgroundColor(0)

            if (saveState)
                page!!.remove(PageProperty_Expanded)
        }
    }

    private fun hideDescription() {
        descriptionTextView.visibility = View.GONE
    }

    private fun showDescription(s: String?, centerAlign: Boolean) {
        when (settings?.fontSize) {
            Settings.FontSizeSmall -> descriptionTextView.textSize = 14f
            Settings.FontSizeMedia -> descriptionTextView.textSize = 15f
            Settings.FontSizeBig -> descriptionTextView.textSize = 17f
            Settings.FontSizeSuperBig -> descriptionTextView.textSize = 20f
        }

        if (s != null) {
            descriptionTextView.text = s
            if (centerAlign)
                descriptionTextView.textAlignment = TextView.TEXT_ALIGNMENT_CENTER
            else
                descriptionTextView.textAlignment = TextView.TEXT_ALIGNMENT_TEXT_START
        } else {
            descriptionTextView.text = "没有识别到内容哦!"
            descriptionTextView.textAlignment = TextView.TEXT_ALIGNMENT_CENTER
        }
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

    private fun hideProgressBar() {
        progressBar.visibility = View.GONE
    }

    private fun showProgressBar() {
        progressBar.visibility = View.VISIBLE
    }

    private fun initVideoWebView(videoWebView: VideoWebView?) {
        videoWebView?.belongPageTitle = page?.pageTitle
        videoWebView?.videoWebViewContentEventListener =
                object : VideoWebView.VideoWebViewContentEventListener {
                    override fun onLoadFinished() {
                        Log.d(TAG, "onLoadFailed")
                        hideProgressBar()
                        videoLayout.visibility = View.VISIBLE
                    }

                    override fun onLoadFailed() {
                        Log.d(TAG, "onLoadFailed")
                    }
                }
    }

    private fun unInitVideoWebView(videoWebView: VideoWebView?) {
        videoWebView?.videoWebViewContentEventListener = null
    }

    private fun hideVideo() {
        val videoWebView = page?.getObjectProperty(DaPenTiPage.PageProperty_WebView) as VideoWebView?
        videoWebView?.pauseVideo()
        unInitVideoWebView(videoWebView)

        videoLayout.visibility = View.GONE
        videoLayout.removeAllViews()
    }

    private fun showVideo(v: View, contentHtml: String, autoPlay: Boolean) {
        Log.d(TAG, "showVideo for ${page?.pageTitle}, autoPlay: $autoPlay")

        var videoWebView = page?.getObjectProperty(PageProperty_WebView) as VideoWebView?
        if (videoWebView == null) {
            videoWebView = VideoWebView(DaPenTiApplication.getAppContext())
            initVideoWebView(videoWebView)

            videoWebView.playOnLoadFinished = autoPlay
            videoWebView.loadDataWithBaseURL("", contentHtml,
                    "text/html", "UTF-8", null)

            showProgressBar()
            videoLayout.visibility = View.GONE

            page!!.setObjectProperty(PageProperty_WebView, videoWebView)
        } else {
            initVideoWebView(videoWebView)

            hideProgressBar()
            videoLayout.visibility = View.VISIBLE
        }

        videoWebView.moveTo(videoLayout, false)

        if (autoPlay)
            videoWebView.startVideo()
    }

    private fun hideContent(saveExpandState: Boolean) {
        setExpand(false, saveExpandState)
        hideDescription()
        hideImage()
        hideVideo()
        hideProgressBar()
    }

    private fun showContent(v: View, playVideoIfPossible: Boolean) {
        setExpand(true, true)
        hideProgressBar()

        when (page!!.pageType) {
            DaPenTiPage.PageTypeNote ->
                showDescription(page!!.pageNotes.content, false)

            DaPenTiPage.PageTypePicture -> {
                val picture = page!!.pagePicture
                showDescription(picture.description, false)
                showImage(v, picture.imageUrl)
            }

            DaPenTiPage.PageTypeVideo -> {
                val canPlayVideo = Settings.settings!!.canPlayVideo()
                if (canPlayVideo) {
                    val pageVideo = page!!.pageVideo
                    showVideo(v, pageVideo.contentHtml!!, playVideoIfPossible)
                } else
                    showDescription("已设置当前条件下不播放视频...", true)
            }

            DaPenTiPage.PageTypeLongReading -> {
                setExpand(false, true)

                val pageLongReading = page!!.pageLongReading

                val context = v.context
                val intent = Intent(context, DetailActivity::class.java)
                intent.putExtra(DetailActivity.EXTRA_HTML, pageLongReading.contentHtml)
                intent.putExtra(DetailActivity.EXTRA_COVER_URL, pageLongReading.coverImageUrl)
                intent.putExtra(DetailActivity.EXTRA_TITLE, page!!.pageTitle)

                context.startActivity(intent)
            }

            else -> {
                showDescription(null, true)
            }
        }
    }

    private fun triggerContent(v: View) {
        Log.d(TAG, "update ${page?.pageTitle} with pageType: " + page!!.pageType)

        if (pageExpanded())
            hideContent(true)
        else
            showContent(v, true)
    }
}
