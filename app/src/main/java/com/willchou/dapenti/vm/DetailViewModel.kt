package com.willchou.dapenti.vm

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import com.willchou.dapenti.DaPenTiApplication
import com.willchou.dapenti.db.DaPenTiData
import com.willchou.dapenti.db.DaPenTiRoomDatabase

open class DetailViewModel(val pageTitle: String): ViewModel() {
    var pageDao: DaPenTiRoomDatabase.PageDao? = null
    var pageData: DaPenTiData.Page? = null

    fun initDB(forceReloadData: Boolean = true) {
        if (pageDao == null)
            pageDao = DaPenTiRoomDatabase.get(DaPenTiApplication.getAppContext()).pageDao()

        if (forceReloadData || pageData == null)
            pageData = pageDao!!.getPage(pageTitle)
    }

    fun getUrlString(): String = pageData!!.url

    fun getFavorite(): Boolean = pageData!!.favorite == 1
    fun setFavorite(f: Boolean) {
        pageData!!.favorite = if (f) 1 else 0
        Thread { pageDao!!.updateFavorite(pageData!!.id!!, pageData!!.favorite) }.start()
    }

    class Factor(private val title: String):
            ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return DetailViewModel(title) as T
        }
    }
}