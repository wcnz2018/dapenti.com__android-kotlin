package com.willchou.dapenti.vm

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import android.util.Log
import com.willchou.dapenti.DaPenTiApplication
import com.willchou.dapenti.db.DaPenTiData
import com.willchou.dapenti.db.DaPenTiRoomDatabase
import com.willchou.dapenti.model.Settings
import com.willchou.dapenti.utils.DObservable
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.net.URL
import java.util.*

class HolderViewModel(val pageTitle: String,
                      /* for ViewHolder use */
                      var selected: Boolean = false,
                      var expanded: Boolean = false,
                      /* for content/data flags */
                      private var doc: Document? = null,
                      var pageType: Int = PageTypeUnknown,
                      var pageMayChanged: Boolean = false,
                      /* content types */
                      var pageLongReading: PageLongReading = PageLongReading(),
                      var pageNotes: PageNotes = PageNotes(),
                      var pagePicture: PagePicture = PagePicture(),
                      var pageVideo: PageVideo = PageVideo(),
                      var pageOriginal: PageOriginal = PageOriginal()
) : ViewModel() {
    var pageDao: DaPenTiRoomDatabase.PageDao? = null
    var pageData: DaPenTiData.Page? = null

    fun initDB() {
        if (pageDao == null)
            pageDao = DaPenTiRoomDatabase.get(DaPenTiApplication.getAppContext()).pageDao()

        if (pageMayChanged || pageData == null) {
            pageData = pageDao!!.getPage(pageTitle)
            pageMayChanged = false
        }
    }

    fun getUrlString(): String = pageData!!.url

    fun getFavorite(): Boolean = pageData!!.favorite == 1
    fun setFavorite(f: Boolean) {
        pageData!!.favorite = if (f) 1 else 0
        Thread { pageDao!!.updateFavorite(pageData!!.id!!, pageData!!.favorite) }.start()
    }

    fun contentPrepared(): Boolean = (doc != null)

    @Synchronized
    fun prepareContent(): Boolean {
        Log.d(TAG, "prepareContent with url: ${pageData!!.url}")
        try {
            val html = pageData!!.content
            if (html.isEmpty()) {
                doc = Jsoup.parse(URL(pageData!!.url), 5000)
                if (doc != null && doc.toString().isEmpty()) {
                    pageData!!.content = doc.toString()
                    pageDao!!.updateContent(pageData!!.id!!, pageData!!.content)
                }
            } else
                doc = Jsoup.parse(html, pageData!!.url)

            if (doc != null) {
                prepareOriginal(doc!!.clone())
                smartContent(doc!!.clone())
                return true
            }

            return false
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
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

        e.attr("style", "width:100% !important; height:auto !important;")

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
        if (!pageData!!.url.contains("more.asp?name=tupian"))
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

    private fun smartContent(doc: Document): Boolean {
        //if (Settings.settings!!.smartContentEnabled)
        if (checkByTitle(doc.clone()) || checkByContent(doc))
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
                "table { width: 100% !important; }" + Settings.settings?.viewModeCSSStyle +
                "</style>"
        html += "<script type=\"text/javascript\">" +
                "  document.addEventListener(\"DOMContentLoaded\", function(event) {" +
                "    document.querySelectorAll('img').forEach(function(img){" +
                "      img.onerror = function(){this.style.display='none';};" +
                "    })" +
                "  });" +
                "</script>"

        html += if (innerHTML.startsWith("<body>"))
            "</head>$innerHTML</html>"
        else
            "</head><body>$innerHTML</body></html>"

        Log.d(TAG, "html content: \n$html")
        return html
    }

    override fun toString(): String = "HolderViewModel[$pageData]"

    class Factor(private val title: String):
            ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return HolderViewModel(title) as T
        }
    }

    companion object {
        private const val TAG = "HolderViewModel"

        const val PageTypeUnknown = 0
        const val PageTypeLongReading = 1
        const val PageTypeNote = 2
        const val PageTypePicture = 3
        const val PageTypeVideo = 4
    }
}