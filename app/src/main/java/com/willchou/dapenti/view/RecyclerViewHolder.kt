package com.willchou.dapenti.view

import android.content.Intent
import android.graphics.Color
import android.os.Handler
import android.os.Looper
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
import com.willchou.dapenti.presenter.DetailActivity

class RecyclerViewHolder internal constructor(private val mView: View,
                                              private val fullScreenTriggered: EnhancedWebView.onFullScreenTriggered,
                                              private val fullScreenViewPair: EnhancedWebView.FullScreenViewPair)
    : RecyclerView.ViewHolder(mView) {
    companion object {
        private const val TAG = "RecyclerViewHolder"

        private const val PageProperty_Expanded = "pp_expand"
        private const val PageProperty_WebView = "pp_webview"
    }

    private val titleTextView: TextView = mView.findViewById(R.id.title)
    private val descriptionTextView: TextView = mView.findViewById(R.id.description)
    private val imageView: ImageView = mView.findViewById(R.id.image)
    private val videoLayout: LinearLayout = mView.findViewById(R.id.webViewLayout)

    private var page: DaPenTiPage? = null
    private var enhancedWebView: EnhancedWebView? = null

    internal fun update(page: DaPenTiPage) {
        this.page = page
        this.enhancedWebView = page.getObjectProperty(PageProperty_WebView) as EnhancedWebView?

        titleTextView.text = page.pageTitle
        Log.d(TAG, "update(reuse view): " + page.pageTitle)

        hideContent()
        if (page.getProperty(PageProperty_Expanded) != null)
            showContent(mView, false)

        mView.setOnClickListener { v: View ->
            if (page.initiated())
                triggerContent(v)
            else {
                page.contentPrepared = object : DaPenTiPage.onContentPrepared {
                    override fun onContentPrepared() {
                        Handler(Looper.getMainLooper()).post { triggerContent(v) }
                    }
                }
                Thread(Runnable { page.prepareContent() }).start()
            }
        }
    }

    private fun detachWebView() {
        val d = page?.getObjectProperty(PageProperty_WebView) as EnhancedWebView?
        (d?.parent as ViewGroup?)?.removeView(d);
    }

    private fun setupFullScreenWebView() {
        enhancedWebView?.fullScreenTriggered = fullScreenTriggered
        fullScreenViewPair.nonVideoLayout = enhancedWebView?.parent as ViewGroup?
        enhancedWebView?.prepareFullScreen(fullScreenViewPair)
    }

    internal fun attachedToWindow() {
        Log.d(TAG, "attachToWindow: " + page!!.pageTitle)
        if (page?.getProperty(PageProperty_Expanded) != null)
            showContent(mView, false)
    }

    internal fun detachedFromWindow() {
        Log.d(TAG, "detachedFromWindow: ${page?.pageTitle}")
        hideContent()
    }

    private fun hideContent() {
        descriptionTextView.visibility = View.GONE
        descriptionTextView.text = ""

        imageView.visibility = View.GONE
        imageView.setImageDrawable(null)

        videoLayout.visibility = View.GONE

        detachWebView()
        enhancedWebView?.pauseVideo()

        titleTextView.setBackgroundColor(Color.WHITE)
        mView.requestLayout()
    }

    private fun showContent(v: View, playVideo: Boolean?) {
        val settings = Settings.settings

        titleTextView.setBackgroundColor(Color.LTGRAY)
        when (page!!.pageType) {
            DaPenTiPage.PageTypeNote -> {
                val notes = page!!.pageNotes

                descriptionTextView.visibility = View.VISIBLE
                descriptionTextView.text = notes.content
            }

            DaPenTiPage.PageTypePicture -> {
                val picture = page!!.pagePicture

                descriptionTextView.visibility = View.VISIBLE
                descriptionTextView.text = picture.description

                if (settings != null && settings.isImageEnabled) {
                    imageView.setImageDrawable(null)
                    imageView.visibility = View.VISIBLE
                    Glide.with(v.context)
                            .load(picture.imageUrl)
                            .into(imageView)
                }
            }

            DaPenTiPage.PageTypeVideo -> {
                if (enhancedWebView == null) {
                    enhancedWebView = EnhancedWebView(v.context)

                    val pageVideo = page!!.pageVideo
                    page!!.setObjectProperty(PageProperty_WebView, enhancedWebView!!)

                    enhancedWebView!!.playOnLoadFinished = playVideo!!
                    enhancedWebView!!.loadDataWithBaseURL("", pageVideo.contentHtml!!,
                            "text/html", "UTF-8", null)
                } else if (playVideo!!)
                    enhancedWebView!!.startVideo()

                detachWebView()
                videoLayout.addView(enhancedWebView)
                videoLayout.visibility = View.VISIBLE

                setupFullScreenWebView()
            }

            DaPenTiPage.PageTypeLongReading -> {
                // show content with next click
                page!!.remove(PageProperty_Expanded)
                titleTextView.setBackgroundColor(Color.WHITE)
                val pageLongReading = page!!.pageLongReading

                val context = v.context

                val intent = Intent(context, DetailActivity::class.java)
                intent.putExtra(DetailActivity.EXTRA_HTML, pageLongReading.contentHtml)
                intent.putExtra(DetailActivity.EXTRA_COVER_URL, pageLongReading.coverImageUrl)
                intent.putExtra(DetailActivity.EXTRA_TITLE, page!!.pageTitle)

                context.startActivity(intent)
            }

            else -> {
                descriptionTextView.visibility = View.VISIBLE
                descriptionTextView.text = "没有识别到内容哦!"
            }
        }

        if (settings != null) {
            when (settings.fontSize) {
                Settings.FontSizeSmall -> descriptionTextView.textSize = 14f
                Settings.FontSizeMedia -> descriptionTextView.textSize = 15f
                Settings.FontSizeBig -> descriptionTextView.textSize = 17f
                Settings.FontSizeSuperBig -> descriptionTextView.textSize = 20f
            }
        }

        mView.requestLayout()
    }

    private fun triggerContent(v: View) {
        Log.d(TAG, "update with pageType: " + page!!.pageType)

        if (page!!.getProperty(PageProperty_Expanded) != null) {
            page!!.remove(PageProperty_Expanded)
            hideContent()
            return
        }

        page!!.setProperty(PageProperty_Expanded, "yes")
        showContent(v, true)
    }
}
