package com.willchou.dapenti.vm

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import com.willchou.dapenti.db.DaPenTiData
import com.willchou.dapenti.db.DaPenTiRoomDatabase


open class DetailViewModel(val pageTitle: String): ViewModel() {
    var pageDao: DaPenTiRoomDatabase.PageDao? = null
    var pageData: LiveData<DaPenTiData.Page>? = null

    open fun initDB() {
        if (pageDao == null)
            pageDao = DaPenTiRoomDatabase.get().pageDao()

        if (pageData == null)
            pageData = pageDao!!.getPageLiveData(pageTitle)
    }

    fun getUrlString(): String = pageData!!.value!!.url

    fun getFavorite(): Boolean = pageData!!.value!!.favorite == 1
    fun setFavorite(f: Boolean) {
        val to = if (f) 1 else 0
        if (pageData!!.value!!.favorite == to)
            return

        pageData!!.value!!.favorite = to
        Thread { pageDao!!.updateFavorite(pageData!!.value!!.id!!, to) }.start()
    }

    class Factor(private val title: String):
            ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return DetailViewModel(title) as T
        }
    }
}