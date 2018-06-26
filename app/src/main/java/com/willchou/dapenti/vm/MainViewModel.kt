package com.willchou.dapenti.vm

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.ViewModel
import android.arch.paging.LivePagedListBuilder
import android.arch.paging.PagedList
import android.arch.paging.PagingRequestHelper
import android.support.annotation.WorkerThread
import com.willchou.dapenti.db.DaPenTiData
import com.willchou.dapenti.db.DaPenTiRoomDatabase
import com.willchou.dapenti.model.DaPenTiWeb
import java.util.concurrent.Executors

class MainViewModel : ViewModel() {
    private val dao = DaPenTiRoomDatabase.get().categoryDao()

    private val helper = PagingRequestHelper(executor)
    var allCategories : LiveData<PagedList<DaPenTiData.Category>>? = null

    @WorkerThread
    fun prepareCategoriesIfEmpty() {
        if (allCategories != null)
            return

        allCategories = LivePagedListBuilder(
                dao.allCategoriesDataSourceFactor(),
                PagedList.Config.Builder()
                        .setPageSize(PAGE_SIZE)
                        .setEnablePlaceholders(ENABLE_PLACEHOLDERS).build())
                .setBoundaryCallback(object : PagedList.BoundaryCallback<DaPenTiData.Category>() {
                    override fun onZeroItemsLoaded() {
                        helper.runIfNotRunning(PagingRequestHelper.RequestType.INITIAL) {
                            Thread {
                                val list = DaPenTiWeb.getCategories()
                                if (list.isNotEmpty())
                                    dao.insert(categories = list)
                                it.recordSuccess()
                            }.start()
                        }
                    }
                })
                .build()
    }

    companion object {
        private const val PAGE_SIZE = 30
        private const val ENABLE_PLACEHOLDERS = false

        val executor = Executors.newSingleThreadExecutor()!!
    }
}