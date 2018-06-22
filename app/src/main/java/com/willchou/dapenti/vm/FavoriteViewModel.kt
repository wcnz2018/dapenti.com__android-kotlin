package com.willchou.dapenti.vm

import android.arch.lifecycle.ViewModel
import android.arch.paging.LivePagedListBuilder
import android.arch.paging.PagedList
import android.support.annotation.WorkerThread
import com.willchou.dapenti.db.DaPenTiData
import com.willchou.dapenti.db.DaPenTiRoomDatabase

class FavoriteViewModel : ViewModel() {
    private val pageDao = DaPenTiRoomDatabase.get().pageDao()
    var noFavorite: Boolean = false
    val allFavoritePages = LivePagedListBuilder(
            pageDao.getFavoritePages(),
            PagedList.Config.Builder()
                    .setPageSize(PAGE_SIZE)
                    .setEnablePlaceholders(ENABLE_PLACEHOLDERS).build())
            .setBoundaryCallback(object : PagedList.BoundaryCallback<DaPenTiData.Page>() {
                override fun onZeroItemsLoaded() { noFavorite = true }
            })
            .build()

    @WorkerThread
    fun reverseChecked() {
        allFavoritePages.value!!.forEach {
            it.checked = if (it.checked == 1) 0 else 1
            pageDao.updateChecked(it.id!!, it.checked)
        }
    }

    fun isCheckedEmpty(): Boolean {
        var b = true
        allFavoritePages.value!!.forEach {
            if (it.checked == 1)
                b = false
                return@forEach
        }

        return b
    }

    @WorkerThread
    fun removeFavoriteFromChecked() {
        allFavoritePages.value!!.forEach {
            if (it.checked == 1) {
                it.checked = 0
                pageDao.updateChecked(it.id!!, it.checked)
                pageDao.updateFavorite(it.id, 0)
            }
        }
    }

    companion object {
        private const val PAGE_SIZE = 30
        private const val ENABLE_PLACEHOLDERS = false
    }
}