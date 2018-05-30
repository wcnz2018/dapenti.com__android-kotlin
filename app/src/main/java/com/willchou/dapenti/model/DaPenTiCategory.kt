package com.willchou.dapenti.model

import android.util.Pair

import java.net.URL
import java.util.ArrayList

class DaPenTiCategory internal constructor(p: Pair<String, URL>) {
    companion object {
        private const val TAG = "DaPenTiCategory"
    }

    val categoryName: String = p.first
    private val categoryUrl: URL = p.second

    var pages: MutableList<DaPenTiPage> = ArrayList()

    var categoryPrepared: onCategoryPrepared? = null

    interface onCategoryPrepared {
        fun onCategoryPrepared(index: Int)
    }

    fun initiated(): Boolean {
        return !pages.isEmpty()
    }

    private fun setPages(pair: List<Pair<String, URL>>, fromDatabase: Boolean) {
        pages.clear()
        val database = Database.database
        for (p in pair)
            pages.add(DaPenTiPage(p))

        if (!fromDatabase && database != null) {
            for (i in pair.indices.reversed()) {
                val p = pair[i]
                database.addPage(categoryName, p.first, p.second.toString())
            }
        }

        categoryPrepared?.onCategoryPrepared(pages.size - 1);
    }

    fun preparePages(fromWeb: Boolean) {
        var subItemPair: MutableList<Pair<String, URL>> = ArrayList()

        var fromDatabase = true
        val database = Database.database
        if (!fromWeb && database != null)
            database.getPages(categoryName, subItemPair)

        if (subItemPair.isEmpty()) {
            //String ss = "div > ul > li > a";
            val ss = "li>a[href^='more.asp?name='],span>a[href^='more.asp?name=']"
            subItemPair = DaPenTi.getElementsWithQuery(categoryUrl, ss)

            fromDatabase = false
        }

        setPages(subItemPair, fromDatabase)
    }
}
