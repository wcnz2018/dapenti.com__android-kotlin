package com.willchou.dapenti.view

import android.annotation.TargetApi
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Handler
import android.support.design.widget.Snackbar
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.animation.AlphaAnimation
import android.widget.*
import com.bumptech.glide.Glide
import com.willchou.dapenti.DaPenTiApplication
import com.willchou.dapenti.R
import com.willchou.dapenti.model.DaPenTiPage
import com.willchou.dapenti.model.Settings
import com.willchou.dapenti.model.Settings.Companion.settings
import com.willchou.dapenti.presenter.DetailActivity
import android.net.Uri
import android.os.Build
import android.support.annotation.RequiresApi
import com.willchou.dapenti.databinding.PentiListItemBinding
import com.willchou.dapenti.model.DaPenTi

class RecyclerViewHolder internal constructor(private val mView: View,
                                              val binding: PentiListItemBinding)
    : RecyclerView.ViewHolder(mView) {
    companion object {
        private const val TAG = "RecyclerViewHolder"

        const val Bind_ShowContent = "showContent"
        const val Bind_PageFailed = "pageFailed"
        const val Bind_Favorite = "favorite"
        const val Bind_Collapse = "collapse"
        const val Bind_SelectModeAnimation = "selectModeAnimation"
        const val Bind_SelectModeQuit = "selectModeQuit"
        const val Bind_SelectChanged = "selectChanged"
        const val Bind_PageLoadFinished = "pageLoadFinished"
    }

    private var foregroundColor: Int = Color.BLACK
    private var backgroundColor: Int = Color.WHITE

    private var page: DaPenTiPage? = null

    init {
        mView.setOnClickListener { v: View -> itemClicked(v) }
        mView.setOnLongClickListener { v: View ->
            itemLongClicked(v)
            true
        }
    }

    private fun toggleSelect() {
        if (page!!.isSelected) {
            binding.checkImageView.visibility = View.GONE
            page!!.isSelected = false
        } else {
            binding.checkImageView.visibility = View.VISIBLE
            page!!.isSelected = true
        }
    }

    private fun itemClicked(v: View) {
        if (DRecyclerView.isSelectMode()) {
            toggleSelect()
            return
        }

        if (page!!.initiated()) {
            triggerContent(v)
        } else {
            if (page!!.pageExpanded()) {
                setExpand(false, true)
                return
            }

            setExpand(true, true)
            Handler().postDelayed({
                if (!page!!.initiated())
                    showProgressBar()
            }, 500)
            Thread(Runnable { page!!.prepareContent() }).start()
        }
    }

    private fun copyToClipboard(v: View, message: String) {
        val clipboard = DaPenTiApplication.getAppContext()
                .getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("DaPenTi", message)
        clipboard.primaryClip = clip

        Snackbar.make(v, "已复制: $message", Snackbar.LENGTH_LONG).show()
    }

    private fun itemLongClicked(v: View) {
        Log.d(TAG, "long press: $v")

        val popMenu = PopupMenu(DaPenTiApplication.getAppContext(), binding.title, Gravity.END)
        popMenu.inflate(R.menu.item)
        if (page!!.getFavorite())
            popMenu.menu.findItem(R.id.action_favorite).setTitle(R.string.action_remove_favorite)
        else
            popMenu.menu.findItem(R.id.action_favorite).setTitle(R.string.action_add_favorite)

        popMenu.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.action_favorite -> {
                    val newFavorite = !page!!.getFavorite();
                    page!!.setFavorite(newFavorite)

                    var s = "已从收藏中移除"
                    if (newFavorite)
                        s = "已加入收藏"
                    Snackbar.make(v, s, Snackbar.LENGTH_SHORT).show()
                }

                R.id.action_show_original -> {
                    setExpand(false, true)

                    val context = v.context
                    val intent = Intent(context, DetailActivity::class.java)
                    intent.putExtra(DetailActivity.EXTRA_TITLE, page!!.pageTitle)

                    var url = page!!.pageUrl.toString()
                    if (url.contains("more.asp")) {  // fetch page for mobile
                        url = url.replace("more.asp", "readforwx.asp")
                        intent.putExtra(DetailActivity.EXTRA_URL, url)
                    } else {
                        val pageOriginal = page!!.pageOriginal
                        if (pageOriginal.valid) { // html already exists
                            intent.putExtra(DetailActivity.EXTRA_HTML, pageOriginal.contentHtml)
                            intent.putExtra(DetailActivity.EXTRA_COVER_URL, pageOriginal.coverImageUrl)
                        } else // pull it from the site
                            intent.putExtra(DetailActivity.EXTRA_URL, url)
                    }

                    context.startActivity(intent)
                }

                R.id.action_copy_title -> copyToClipboard(v, page!!.pageTitle)

                R.id.action_copy_link -> copyToClipboard(v, page!!.pageUrl.toString())

                R.id.action_open_link -> {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data = Uri.parse(page!!.pageUrl.toString())
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    DaPenTiApplication.getAppContext().startActivity(intent)
                }
            }
            true
        }

        popMenu.show()
    }

    internal fun checkSelect() {
        Log.d(TAG, "checkSelect: isSelectMode: ${DRecyclerView.isSelectMode()}")
        if (!DRecyclerView.isSelectMode()) {
            binding.checkImageView.visibility = View.GONE
            return
        }

        binding.checkImageView.visibility = if (page!!.isSelected) View.VISIBLE else View.GONE
    }

    internal fun checkFavorite() {
        binding.favoriteImageView.visibility = if (page!!.getFavorite()) View.VISIBLE else View.GONE
    }

    internal fun enterSelectModeAnimation() {
        binding.checkImageView.visibility = View.VISIBLE

        val animation = AlphaAnimation(1.0f, 0f)
        animation.duration = 350
        animation.repeatCount = 1
        binding.checkImageView.animation = animation

        Handler().postDelayed({
            binding.checkImageView.visibility = View.GONE
        }, animation.duration + 100)
    }

    internal fun quitSelectMode() {
        binding.checkImageView.visibility = View.GONE
    }

    private fun checkNightMode() {
        backgroundColor = Settings.settings!!.getLighterBackgroundColor()
        foregroundColor = Settings.settings!!.getForegroundColor()

        binding.title.setTextColor(foregroundColor)
        binding.description.setTextColor(foregroundColor)

        binding.cardView.setCardBackgroundColor(backgroundColor)
    }

    internal fun update(page: DaPenTiPage) {
        this.page = page
        Log.d(TAG, "update with page ${page.pageTitle}")

        val animation = AlphaAnimation(0f, 1.0f)
        animation.duration = 200
        binding.cardView.animation = animation
    }

    internal fun setupContent(playVideoIfPossible: Boolean) {
        binding.title.text = page!!.pageTitle

        if (page!!.pageExpanded())
            showContent(mView, playVideoIfPossible)
    }

    internal fun invalidContent() {
        if (!page!!.pageExpanded())
            return

        hideImage()
        hideVideo()
        hideProgressBar()
        showDescription("内容获取失败,请稍后重试...", true)
    }

    internal fun pageLoadFinished() {
        hideProgressBar()
        binding.videoLayout.visibility = View.VISIBLE
    }

    internal fun attachedToWindow() {
        Log.d(TAG, "attachToWindow: ${page!!.pageTitle}," +
                "expanded: ${page?.pageExpanded()}")

        checkSelect()
        checkNightMode()
        checkFavorite()
        setupContent(false)
    }

    internal fun detachedFromWindow() {
        Log.d(TAG, "detachedFromWindow: ${page?.pageTitle}")

        hideContent(false)
    }

    private fun setExpand(expand: Boolean, saveState: Boolean) {
        Log.d(TAG, "setExpand ${page?.pageTitle} to $expand, saveState: $saveState")

        if (expand) {
            binding.title.setTextColor(backgroundColor)
            binding.title.setBackgroundColor(Color.GRAY)

            if (saveState)
                page!!.markExpanded(true)
        } else {
            binding.title.setTextColor(foregroundColor)
            binding.title.setBackgroundColor(0)

            binding.description.visibility = View.GONE
            binding.image.visibility = View.GONE
            binding.progressBar.visibility = View.GONE

            if (saveState)
                page!!.markExpanded(false)
        }
    }

    private fun hideDescription() {
        binding.description.visibility = View.GONE
    }

    private fun showDescription(s: String?, centerAlign: Boolean) {
        when (settings?.fontSize) {
            Settings.FontSizeSmall -> binding.description.textSize = 14f
            Settings.FontSizeMedia -> binding.description.textSize = 15f
            Settings.FontSizeBig -> binding.description.textSize = 17f
            Settings.FontSizeSuperBig -> binding.description.textSize = 20f
        }

        if (s != null) {
            binding.description.text = s
            if (centerAlign)
                binding.description.textAlignment = TextView.TEXT_ALIGNMENT_CENTER
            else
                binding.description.textAlignment = TextView.TEXT_ALIGNMENT_TEXT_START
        } else {
            binding.description.text = "智能识别没有找到内容哦!\n您可以尝试【显示原始内容】"
            binding.description.textAlignment = TextView.TEXT_ALIGNMENT_CENTER
        }
        binding.description.visibility = View.VISIBLE
    }

    private fun hideImage() {
        binding.image.visibility = View.GONE
        binding.image.setImageDrawable(null)
    }

    private fun showImage(v: View, imageUrl: String?) {
        binding.image.setImageDrawable(null)
        if (settings != null && settings!!.isImageEnabled) {
            Glide.with(v.context)
                    .load(imageUrl)
                    .into(binding.image)
            binding.image.visibility = View.VISIBLE
        }
    }

    private fun hideProgressBar() {
        binding.progressBar.visibility = View.GONE
    }

    private fun showProgressBar() {
        binding.progressBar.visibility = View.VISIBLE
    }

    private fun hideVideo() {
        val videoWebView = DaPenTi.daPenTi!!.getCachedVideoWebView(page!!.pageTitle)
        videoWebView?.pauseVideo()

        binding.videoLayout.visibility = View.GONE
        binding.videoLayout.removeAllViews()
    }

    private fun showVideo(v: View, contentHtml: String, autoPlay: Boolean) {
        Log.d(TAG, "showVideo for ${page?.pageTitle}, autoPlay: $autoPlay")

        var videoWebView = DaPenTi.daPenTi!!.getCachedVideoWebView(page!!.pageTitle)

        if (videoWebView == null) {
            videoWebView = VideoWebView(DaPenTiApplication.getAppContext())
            videoWebView.belongPageTitle = page?.pageTitle

            videoWebView.playOnLoadFinished = autoPlay
            videoWebView.loadDataWithBaseURL("", contentHtml,
                    "text/html", "UTF-8", null)

            showProgressBar()
            binding.videoLayout.visibility = View.GONE

            DaPenTi.daPenTi!!.cacheVideoWebView(page!!.pageTitle, videoWebView)
        } else {
            hideProgressBar()
            binding.videoLayout.visibility = View.VISIBLE
        }

        videoWebView.moveTo(binding.videoLayout, false)

        if (autoPlay)
            videoWebView.startVideo()
    }

    internal fun hideContent(saveExpandState: Boolean) {
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
                    if (pageVideo.invalid) {
                        showDescription(pageVideo.invalidReason, true)
                    } else
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

            else -> showDescription(null, true)
        }
    }

    private fun triggerContent(v: View) {
        Log.d(TAG, "update ${page?.pageTitle} with pageType: " + page!!.pageType)

        if (page!!.pageExpanded())
            hideContent(true)
        else
            showContent(v, true)
    }
}
