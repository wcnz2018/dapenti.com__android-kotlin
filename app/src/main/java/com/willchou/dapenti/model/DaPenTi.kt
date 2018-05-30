package com.willchou.dapenti.model

import android.util.Log
import android.util.Pair
import org.jsoup.Jsoup
import java.net.URL
import java.util.*

class DaPenTi {
    var daPenTiCategories: MutableList<DaPenTiCategory> = ArrayList()

    interface onCategoryPrepared {
        fun onCategoryPrepared()
    }

    init {
        daPenTi = this
    }

    private fun fetchFromWeb(): Boolean {
        val ss = "div.center_title > a, div.title > a, div.title > p > a"
        val urlPairs = getElementsWithQuery(urlString, ss)
        if (urlPairs.isEmpty())
            return false

        val database = Database.database
        daPenTiCategories = ArrayList()
        for (p in urlPairs) {
            // "浮世绘"和"本月热读" 中的子页连接是错误的目录内容
            if (p.first == "浮世绘" || p.first == "本月热读")
                continue

            daPenTiCategories.add(DaPenTiCategory(p))
            database?.addCategory(p.first, p.second.toString())
        }

        if (categoryPrepared != null)
            categoryPrepared!!.onCategoryPrepared()

        return true
    }

    private fun fetchFromDatabase(): Boolean {
        val database = Database.database ?: return false

        val urlPairs = ArrayList<Pair<String, URL>>()
        database.getCategories(urlPairs, true)
        if (urlPairs.isEmpty())
            return false

        daPenTiCategories = ArrayList()
        for (p in urlPairs)
            daPenTiCategories.add(DaPenTiCategory(p))

        if (categoryPrepared != null)
            categoryPrepared!!.onCategoryPrepared()
        return true
    }

    fun prepareCategory(fromWeb: Boolean): Boolean {
        Log.d(TAG, "prepareCategory")

        if (!fromWeb && fetchFromDatabase()) {
            Log.d(TAG, "restore data from database")
            return true
        }

        return fetchFromWeb()
    }

    companion object {
        private val urlString = "http://www.dapenti.com/blog/index.asp"
        private val TAG = "DaPenTi"

        var storageDir: String? = null

        var categoryPrepared: onCategoryPrepared? = null

        var daPenTi: DaPenTi? = null

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

                    if (!u.contains(url.protocol))
                        u = "$prefix/$u"

                    Log.d(TAG, "title: $t, urlString: $u")
                    lp.add(Pair(t, URL(u)))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return lp
        }

        fun getElementsWithQuery(url: URL, query: String): MutableList<Pair<String, URL>> {
            try {
                synchronized(TAG) {
                    val doc = Jsoup.parse(url, 5000)
                    return getElementsWithQuery(url, doc.toString(), query)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return ArrayList()
        }
    }
}
