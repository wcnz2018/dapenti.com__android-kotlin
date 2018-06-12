package com.willchou.dapenti.model

import android.util.Log
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.net.URL
import java.util.*

class DaPenTiPage internal constructor(val pageTitle: String,
                                       val pageUrl: URL,
                                       private var favorite: Boolean)
    : Properties() {
    companion object {
        private const val TAG = "DaPenTiPage"

        const val PageTypeUnknown = 0
        const val PageTypeLongReading = 1
        const val PageTypeNote = 2
        const val PageTypePicture = 3
        const val PageTypeVideo = 4
    }

    var pageType = PageTypeUnknown

    var isSelected = false
    private var isExpanded = false

    private var doc: Document? = null

    var pageLongReading = PageLongReading()
    var pageNotes = PageNotes()
    var pagePicture = PagePicture()
    var pageVideo = PageVideo()
    var pageOriginal = PageOriginal()

    fun pageExpanded(): Boolean {
        return isExpanded
    }

    fun markExpanded(e: Boolean) {
        isExpanded = e
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

        var invalid: Boolean = false
        var invalidReason: String = ""
    }

    class PageOriginal {
        var valid: Boolean = false
        var contentHtml: String? = null
        var coverImageUrl: String? = null
    }

    var isPageInitiated: Boolean = false
    fun initiated(): Boolean { return isPageInitiated }

    fun getFavorite():Boolean { return favorite }
    fun setFavorite(f:Boolean) {
        favorite = f
        Database.database?.setPageFavorite(pageTitle, f)

        DaPenTi.notifyPageFavorite(pageTitle)
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
        val src = e.attr("src")

        pageVideo.contentHtml = getContent(e)
        if (src.isEmpty()) {
            pageVideo.invalid = true
            pageVideo.invalidReason = "没有检测到视频..."
        } else if (src.contains("f.us.sinaimg.cn")) {
            pageVideo.invalid = true
            pageVideo.invalidReason = "检测到防外链的新浪视频, 使用浏览器打开吧..."
        }

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

        pageLongReading.contentHtml = getContent(e)
        pageLongReading.coverImageUrl = getCoverImageUrl(e)

        pageType = PageTypeLongReading
        return true
    }

    private fun prepareOriginal(doc: Document) {
        pageOriginal.valid = false
        val e = getFirstElement(doc, "body") ?: return

        pageOriginal.contentHtml = getContent(e)
        pageOriginal.coverImageUrl = getCoverImageUrl(e)
        pageOriginal.valid = true
    }

    fun prepareContent() {
        synchronized(TAG) {
            isPageInitiated = false
            if (doPrepareContent())
                DaPenTi.notifyPageChanged(pageTitle)
            else
                DaPenTi.notifyPageError(pageTitle)
            isPageInitiated = true
        }
    }

    private fun smartContent(): Boolean {
        if (Settings.settings!!.smartContentEnabled)
            if (checkByTitle(doc!!) || checkByContent(doc!!))
                return true

        return false
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

            if (doc != null) {
                prepareOriginal(doc!!)
                smartContent()
                return true
            }

            return false
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    private fun getCoverImageUrl(contentElement: Element): String {
        val es = contentElement.select("img:not(.W_img_face)")
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
                "table { width: 100% !important; }" + Settings.settings?.viewModeCSSStyle +
                "</style>"
        html += "<script type=\"text/javascript\">\n" +
                "  document.addEventListener(\"DOMContentLoaded\", function(event) {\n" +
                "    document.querySelectorAll('img').forEach(function(img){\n" +
                "      img.onerror = function(){this.style.display='none';};\n" +
                "    })\n" +
                "  });\n" +
                "</script>"

        html += if (innerHTML.startsWith("<body>"))
            "</head>$innerHTML</html>"
        else
            "</head><body>$innerHTML</body></html>"

        Log.d(TAG, "html content: \n$html")
        return html
    }
}
