package com.willchou.dapenti.model

import android.util.Log
import android.util.Pair
import java.net.URL
import java.util.*

class DaPenTiCategory internal constructor(p: Pair<String, URL>) {
    companion object {
        private const val TAG = "DaPenTiCategory"
    }

    val categoryName: String = p.first
    private val categoryUrl: URL = p.second

    var pages: MutableList<DaPenTiPage> = ArrayList()

    fun initiated(): Boolean {
        return !pages.isEmpty()
    }

    private fun setPages(pageInfoList: List<Database.PageInfo>, fromDatabase: Boolean) {
        pages.clear()
        val database = Database.database

        for (pageInfo in pageInfoList) {
            var page = DaPenTi.daPenTi?.daPenTiPageMap?.get(pageInfo.pageTitle)
            if (page != null)
                pages.add(page)
            else {
                page = DaPenTiPage(pageInfo.pageTitle, pageInfo.pageUrl, pageInfo.isFavorite)
                DaPenTi.daPenTi?.daPenTiPageMap?.put(pageInfo.pageTitle, page)
                pages.add(page)
            }
        }

        if (!fromDatabase && database != null) {
            for (pageInfo in pageInfoList.reversed())
                database.addPage(categoryName, pageInfo.pageTitle, pageInfo.pageUrl.toString())
        }

        DaPenTi.notifyCategoryChanged(categoryName)
    }

    fun preparePages(fromWeb: Boolean) {
        val pageInfoList: MutableList<Database.PageInfo> = ArrayList()

        var fromDatabase = true
        val database = Database.database
        if (!fromWeb && database != null)
            database.getPages(categoryName, pageInfoList)

        if (pageInfoList.isEmpty()) {
            //String ss = "div > ul > li > a";
            val ss = "li>a[href^='more.asp?name='],span>a[href^='more.asp?name=']"
            val subItemPair = DaPenTi.getElementsWithQuery(categoryUrl, ss)

            if (subItemPair.isEmpty()) {
                Log.d(TAG, "Unable to fetch from web: $categoryUrl")
                DaPenTi.notifyCategoryChanged(categoryName)
                return
            }

            for (p in subItemPair) {
                val favorite = Database.database?.getPageFavorite(p.first)
                val pi = Database.PageInfo(p.first, p.second,favorite != null && favorite)
                pageInfoList.add(pi)
            }

            fromDatabase = false
        }

        setPages(pageInfoList, fromDatabase)
    }
}
