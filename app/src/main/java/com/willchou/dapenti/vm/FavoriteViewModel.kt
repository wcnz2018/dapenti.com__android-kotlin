package com.willchou.dapenti.vm

import android.arch.lifecycle.ViewModel
import android.arch.paging.LivePagedListBuilder
import android.arch.paging.PagedList
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

    companion object {
        private const val PAGE_SIZE = 30
        private const val ENABLE_PLACEHOLDERS = false
    }
}