package com.willchou.dapenti.model

import android.content.Intent
import android.util.Log
import android.util.Pair
import com.willchou.dapenti.DaPenTiApplication
import org.jsoup.Jsoup
import java.net.URL

class DaPenTi {
    companion object {
        const val urlString = "http://www.dapenti.com/blog/index.asp"
        private const val TAG = "DaPenTi"

        const val ACTION_CATEGORY_PREPARED = "com.willchou.dapenti.categoryPrepared"
        const val ACTION_CATEGORY_ERROR = "com.willchou.dapenti.categoryError"
        const val ACTION_PAGE_PREPARED = "com.willchou.dapenti.pagePrepared"
        const val ACTION_PAGE_FAVORITE = "com.willchou.dapenti.pageFavorite"
        const val ACTION_DATABASE_CHANGED = "com.willchou.dapenti.databaseChanged"

        const val EXTRA_PAGE_TITLE = "extra_page_title"
        const val EXTRA_CATEGORY_TITLE = "extra_category_title"

        var storageDir: String? = null
        var daPenTi: DaPenTi? = null

        fun notifyCategoryChanged(categoryTitle: String?) {
            Log.d(TAG, "notifyCategoryChanged: $categoryTitle")

            val intent = Intent(ACTION_CATEGORY_PREPARED)
            intent.putExtra(EXTRA_CATEGORY_TITLE, categoryTitle)
            DaPenTiApplication.getAppContext().sendBroadcast(intent)
        }

        fun notifyCategoryError(categoryTitle: String?) {
            Log.d(TAG, "notifyCategoryError: $categoryTitle")

            val intent = Intent(ACTION_CATEGORY_ERROR)
            intent.putExtra(EXTRA_CATEGORY_TITLE, categoryTitle)
            DaPenTiApplication.getAppContext().sendBroadcast(intent)
        }

        fun notifyPageChanged(pageTitle: String) {
            Log.d(TAG, "notifyPageChanged: $pageTitle")

            val intent = Intent(ACTION_PAGE_PREPARED)
            intent.putExtra(EXTRA_PAGE_TITLE, pageTitle)
            DaPenTiApplication.getAppContext().sendBroadcast(intent)
        }

        fun notifyPageFavorite(pageTitle: String) {
            Log.d(TAG, "notifyFavoriteChanged: $pageTitle")

            val intent = Intent(ACTION_PAGE_FAVORITE)
            intent.putExtra(EXTRA_PAGE_TITLE, pageTitle)
            DaPenTiApplication.getAppContext().sendBroadcast(intent)
        }

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

    var daPenTiCategories: MutableList<DaPenTiCategory> = ArrayList()
    var daPenTiPageMap: MutableMap<String, DaPenTiPage> = HashMap()

    init {
        daPenTi = this
    }

    fun initiated(): Boolean {
        return !daPenTiCategories.isEmpty()
    }

    fun resetPageSelect() {
        for (map in daPenTiPageMap) {
            map.value.isSelected = false
        }
    }

    fun findPageByTitle(pageTitle: String): DaPenTiPage? {
        for (page in daPenTiPageMap) {
            if (page.key == pageTitle)
                return page.value
        }

        return null
    }

    fun getFavoritePages(): List<DaPenTiPage> {
        val list = ArrayList<DaPenTiPage>()
        for (page in daPenTiPageMap) {
            if (page.value.getFavorite())
                list.add(page.value)
        }
        return list
    }

    private fun fetchFromWeb(): Boolean {
        val ss = "div.center_title > a, div.title > a, div.title > p > a"
        val urlPairs = getElementsWithQuery(urlString, ss)
        if (urlPairs.isEmpty()) {
            notifyCategoryError(null)
            return false
        }

        val database = Database.database
        daPenTiCategories = ArrayList()
        for (p in urlPairs) {
            // "浮世绘"和"本月热读" 中的子页连接是错误的目录内容
            if (p.first == "浮世绘" || p.first == "本月热读")
                continue

            daPenTiCategories.add(DaPenTiCategory(p))
            database?.addCategory(p.first, p.second.toString())
        }

        notifyCategoryChanged(null)
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

        notifyCategoryChanged(null)
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

    fun databaseChanged() {
        for (category in daPenTiCategories)
            category.pages.clear()
        daPenTiPageMap.clear()

        prepareCategory(false)

        DaPenTiApplication.getAppContext().sendBroadcast(Intent(ACTION_DATABASE_CHANGED))
    }
}
