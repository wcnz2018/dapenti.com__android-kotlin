package com.willchou.dapenti.model

import android.util.Log

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.lang.ref.WeakReference

import java.net.URL
import java.util.ArrayList
import java.util.HashMap
import java.util.Properties

class DaPenTiPage internal constructor(val pageTitle: String,
                                       private val pageUrl: URL,
                                       private var favorite: Boolean)
    : Properties() {
    companion object {
        private const val TAG = "DaPenTiPage"

        const val PageTypeUnknown = 0
        const val PageTypeLongReading = 1
        const val PageTypeNote = 2
        const val PageTypePicture = 3
        const val PageTypeVideo = 4
        const val PageTypeOriginal = 5
    }

    var pageType = PageTypeUnknown
    private var originalHtml: String? = null

    // Note: need to set to null in onPause() to prevent memory leak
    //       reassign in onResume() or somewhere
    var pageEventListener: PageEventListener? = null

    var pageLongReading = PageLongReading()
    var pageNotes = PageNotes()
    var pagePicture = PagePicture()
    var pageVideo = PageVideo()

    internal var contentElement: Element? = null
    internal var coverImageUrl: String? = null
    // Properties
    private val mapObject = HashMap<String, Any>()
    fun setObjectProperty(k: String, o: Any) { mapObject[k] = o }
    fun getObjectProperty(k: String): Any? { return mapObject[k] }
    fun removeObjectProperty(k: String) { mapObject.remove(k) }

    interface PageEventListener {
        fun onContentPrepared()
        fun onFavoriteChanged(favorite: Boolean)
    }

    class PageLongReading {
        var contentHtml: String? = null
        var coverImageUrl: String? = null
    }

    class PageNotes {
        var content: String? = null
    }

    class PagePicture {
        var description: String? = null
        var imageUrl: String? = null
    }

    class PageVideo {
        var description: String? = null
        var contentHtml: String? = null
    }

    fun initiated(): Boolean {
        return pageType != PageTypeUnknown
    }

    fun getFavorite():Boolean { return favorite }
    fun setFavorite(f:Boolean) {
        favorite = f
        Database.database?.setPageFavorite(pageTitle, f)

        pageEventListener?.onFavoriteChanged(f)
    }

    private fun getFirstElement(doc: Document, s: String): Element? {
        val es = doc.select(s)
        if (es.size == 0)
            return null

        val e = es.first()
        fixContent(e)
        return e
    }

    private fun preparePageVideo(doc: Document): Boolean {
        val e = getFirstElement(doc, "video") ?: return false

        pageVideo.contentHtml = getContent(e)
        // TODO: get description

        pageType = PageTypeVideo
        return true
    }

    private fun preparePagePicture(doc: Document): Boolean {
        if (!pageUrl.toString().contains("more.asp?name=tupian"))
            return false

        val ss = arrayOf("div>img[src^='http://www.dapenti.com']",
                "div>p>img[src^='http://www.dapenti.com']")

        for (s in ss) {
            val e = getFirstElement(doc, s)
            if (e != null) {
                pagePicture.imageUrl = e.attr("src")

                if (s.contains(">p>"))
                    pagePicture.description = e.parent().parent().text()
                else
                    pagePicture.description = e.parent().text()

                pageType = PageTypePicture
                return true
            }
        }

        return false
    }

    private fun preparePageNote(doc: Document): Boolean {
        val ss = "div.WB_info"
        var e = getFirstElement(doc, ss)
        if (e != null) {
            pageNotes.content = e.text()
            pageType = PageTypeNote
            return true
        }

        val es = doc.select("div.oblog_text")
        Log.d(TAG, "es.size: " + es.size)
        if (es.size != 1)
            return false

        e = es.first()

        val sl = arrayOf("img", "video")
        for (s in sl)
            if (e!!.select(s).size > 0)
                return false

        pageNotes.content = e!!.text()
        pageType = PageTypeNote
        return true
    }

    private fun prepareLongReading(doc: Document): Boolean {
        val e = getFirstElement(doc, "div.oblog_text,span.oblog_text > div") ?: return false

        pageLongReading = PageLongReading()
        pageLongReading.contentHtml = getContent(e)
        pageLongReading.coverImageUrl = getCoverImageUrl(e)

        pageType = PageTypeLongReading
        return true
    }

    fun prepareContent() {
        synchronized(TAG) {
            if (doPrepareContent())
                pageEventListener?.onContentPrepared()
        }
    }

    private fun checkByTitle(doc: Document): Boolean {
        if (pageTitle.contains("【喷嚏图卦") && prepareLongReading(doc))
            return true

        val isShortNotes: Boolean = pageTitle.contains("【最右】") ||
                pageTitle.contains("【喷嚏】") ||
                pageTitle.contains("【段子】") ||
                pageTitle.contains("【喷嚏一下】")
        if (isShortNotes && preparePageNote(doc))
            return true

        return false
    }

    private fun checkByContent(doc: Document): Boolean {
        // Note: order counts
        if (preparePageVideo(doc))
            return true
        if (preparePageNote(doc))
            return true
        if (preparePagePicture(doc))
            return true

        return prepareLongReading(doc)
    }

    private fun doPrepareContent(): Boolean {
        Log.d(TAG, "prepareContent with url: " + pageUrl.toString())
        try {
            val database = Database.database
            var doc: Document? = null

            if (database != null) {
                val content = database.getPageContent(pageTitle)
                if (content != null && !content.isEmpty()) {
                    doc = Jsoup.parse(content, pageUrl.toString())
                }
            }

            if (doc == null) {
                doc = Jsoup.parse(pageUrl, 5000)
                database?.updatePageContent(pageTitle, doc!!.toString())
            }

            if (checkByTitle(doc!!) || checkByContent(doc))
                return true

            pageType = PageTypeOriginal
            originalHtml = doc.toString()

            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }

    }

    private fun getCoverImageUrl(contentElement: Element): String {
        val es = contentElement.select("img:not(.W_img_face)")
        // TODO: check image url valid
        return if (es.size >= 1) es.first().attr("src") else ""

    }

    private fun fixContent(contentElement: Element) {
        contentElement.select("script, ins, hr, iframe").remove()

        val les = ArrayList<Element>()
        for (e in contentElement.select("p, div")) {
            val ec = e.html().replace("\\s+", "")

            if (ec == "&nbsp;" || ec.contains("友情提示")
                    || ec.contains("新浪微博") || ec.contains("来源：")
                    || ec.contains("喷嚏网：")) {
                les.add(e)
                continue
            }

            for (eae in e.select("a")) {
                val href = eae.attr("href")
                if (href.contains("taobao") || href.contains("5463797858")) {
                    les.add(e)
                    break
                }
            }
        }
        for (e in les)
            e.remove()
    }

    private fun getContent(contentElement: Element): String {
        var innerHTML = contentElement.toString()
        innerHTML = innerHTML.replace("广告", "")
        innerHTML = innerHTML.replace("font-size:", "")

        var html: String = "<html><head>" + "<meta name=\"content-type\" content=\"text/html; charset=utf-8\">"
        html += "<style>" +
                "img:not(.W_img_face) {display: block; margin: 0 auto;max-width: 100%;}" +
                "video { width:100% !important; height:auto !important; }" +
                "table { width: 100% !important; }" +
                ".video-rotate {\n" +
                "  position: absolute;\n" +
                "  transform: rotate(90deg);\n" +
                "\n" +
                "  transform-origin: bottom left;\n" +
                "  width: 100vh;\n" +
                "  height: 100vw;\n" +
                "  margin-top: -100vw;\n" +
                "  object-fit: cover;\n" +
                "\n" +
                "  z-index: 4;\n" +
                "  visibility: visible;\n" +
                "}" + Settings.settings?.viewModeCSSStyle +
                "</style>"
        html += "<script type=\"text/javascript\">\n" +
                "  document.addEventListener(\"DOMContentLoaded\", function(event) {\n" +
                "    document.querySelectorAll('img').forEach(function(img){\n" +
                "      img.onerror = function(){this.style.display='none';};\n" +
                "    })\n" +
                "  });\n" +
                "</script>"
        html += "</head><body>$innerHTML</body></html>"
        Log.d(TAG, "html content: \n$html")
        return html
    }
}
