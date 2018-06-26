package com.willchou.dapenti.model

import android.util.Log
import com.willchou.dapenti.db.DaPenTiData
import com.willchou.dapenti.utils.DObservable
import org.jsoup.Jsoup
import java.net.URL

class DaPenTiWeb {
    companion object {
        const val urlString = "http://www.dapenti.com/blog/index.asp"
        private const val TAG = "DaPenTiWeb"

        private fun getElementsWithQuery(url: String, query: String): List<Pair<String, URL>> {
            try {
                return getElementsWithQuery(URL(url), query)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return ArrayList()
        }

        private fun getElementsWithQuery(url: URL, html: String, query: String):
                MutableList<Pair<String, URL>> {
            val lp = ArrayList<Pair<String, URL>>()

            val us = url.toString()
            val prefix = us.substring(0, us.lastIndexOf("/"))

            try {
                val doc = Jsoup.parse(html, url.toString())
                val titles = doc.select(query)
                Log.d(TAG, "titles: $titles")
                for (e in titles) {
                    val t = e.text().replace(" ", "")
                    var u = e.attr("href")

                    if (t.isEmpty() || u.isEmpty())
                        continue

                    // as we're on Mobile, fetch mobile version
                    //u = u.replace("more.asp", "readforwx.asp")

                    //if (!u.contains(url.protocol))
                    if (!u.contains("http"))
                        u = "$prefix/$u"

                    Log.d(TAG, "title: $t, urlString: $u")
                    lp.add(Pair(t, URL(u)))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return lp
        }

        @Synchronized
        private fun getElementsWithQuery(url: URL, query: String): MutableList<Pair<String, URL>> {
            try {
                val doc = Jsoup.parse(url, 5000)
                return getElementsWithQuery(url, doc.toString(), query)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return ArrayList()
        }

        val categoryFetching: DObservable<Boolean> = DObservable(false)
        val categoryFailed: DObservable<Boolean> = DObservable(false)
        fun getCategories() : List<DaPenTiData.Category> {
            categoryFetching.set(true)
            categoryFailed.set(false)

            val list: MutableList<DaPenTiData.Category> = ArrayList()

            val ss = "div.center_title > a, div.title > a, div.title > p > a"
            val urlPairs = getElementsWithQuery(urlString, ss)

            if (urlPairs.isEmpty())
                categoryFailed.set(true)

            for (p in urlPairs) {
                // "浮世绘"和"本月热读" 中的子页连接是错误的目录内容
                if (p.first == "浮世绘" || p.first == "本月热读")
                    continue

                list.add(DaPenTiData.Category(title = p.first, url = p.second.toString()))
            }

            categoryFetching.set(false)
            return list
        }

        fun getPages(categoryUrl: String) : List<DaPenTiData.Page> {
            val list: MutableList<DaPenTiData.Page> = ArrayList()

            //String ss = "div > ul > li > a";
            val ss = "li>a[href^='more.asp?name='],span>a[href^='more.asp?name=']"
            val subItemPair = getElementsWithQuery(categoryUrl, ss)

            for (p in subItemPair)
                list.add(DaPenTiData.Page(title = p.first, url = p.second.toString()))

            return list
        }
    }
}