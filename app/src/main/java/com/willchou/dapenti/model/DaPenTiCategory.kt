package com.willchou.dapenti.model

import android.util.Log
import android.util.Pair
import java.lang.ref.WeakReference

import java.net.URL
import java.util.ArrayList

class DaPenTiCategory internal constructor(p: Pair<String, URL>) {
    companion object {
        private const val TAG = "DaPenTiCategory"
    }

    val categoryName: String = p.first
    private val categoryUrl: URL = p.second

    var pages: MutableList<DaPenTiPage> = ArrayList()

    interface CategoryEventListener {
        fun onCategoryPrepared(index: Int)
    }
    // Note: need to set to null in onPause() to prevent memory leak
    //       reassign in onResume() or somewhere
    var categoryEventListener: CategoryEventListener? = null
    fun resetEventListener() { categoryEventListener = null }
    fun resetAllPageEventListener() {
        for (p in pages)
            p.resetEventListener()
    }

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

        Log.d(TAG, "categoryEventListener: $categoryEventListener")
        categoryEventListener?.onCategoryPrepared(pages.size - 1)
    }

    fun preparePages(fromWeb: Boolean) {
        val pageInfoList: MutableList<Database.PageInfo> = ArrayList()

        var fromDatabase = true
        val database = Database.database
        if (!fromWeb && database != null) {
            database.getPages(categoryName, pageInfoList)
        }

        if (pageInfoList.isEmpty()) {
            //String ss = "div > ul > li > a";
            val ss = "li>a[href^='more.asp?name='],span>a[href^='more.asp?name=']"
            val subItemPair = DaPenTi.getElementsWithQuery(categoryUrl, ss)

            if (subItemPair.isEmpty()) {
                Log.d(TAG, "categoryEventListener: $categoryEventListener")
                categoryEventListener?.onCategoryPrepared(pages.size - 1)
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
