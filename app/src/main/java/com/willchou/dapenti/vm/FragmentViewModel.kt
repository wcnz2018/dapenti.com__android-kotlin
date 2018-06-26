package com.willchou.dapenti.vm

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import android.arch.paging.LivePagedListBuilder
import android.arch.paging.PagedList
import android.arch.paging.PagingRequestHelper
import android.util.Log
import com.willchou.dapenti.db.DaPenTiData
import com.willchou.dapenti.db.DaPenTiRoomDatabase
import com.willchou.dapenti.model.DaPenTiWeb
import com.willchou.dapenti.utils.DObservable
import java.util.concurrent.Executors

class FragmentViewModel(private val category: String): ViewModel() {
    companion object {
        private const val TAG = "FragmentViewModel"
        /**
         * A good page size is a value that fills at least a screen worth of content on a large
         * device so the User is unlikely to see a null item.
         * You can play with this constant to observe the paging behavior.
         * <p>
         * It's possible to vary this with list device size, but often unnecessary, unless a user
         * scrolling on a large device is expected to scroll through items more quickly than a small
         * device, such as when the large device uses a grid layout of items.
         */
        private const val PAGE_SIZE = 30

        /**
         * If placeholders are enabled, PagedList will report the full size but some items might
         * be null in onBind method (PagedListAdapter triggers a rebind when data is loaded).
         * <p>
         * If placeholders are disabled, onBind will never receive null but as more pages are
         * loaded, the scrollbars will jitter as new pages are loaded. You should probably disable
         * scrollbars if you disable placeholders.
         */
        private const val ENABLE_PLACEHOLDERS = true
    }

    val isFetchingFromWeb: DObservable<Boolean> = DObservable(false)

    private val helper = PagingRequestHelper(MainViewModel.executor)

    private val categoryDao = DaPenTiRoomDatabase.get().categoryDao()
    private val pageDao = DaPenTiRoomDatabase.get().pageDao()
    private val indexDao = DaPenTiRoomDatabase.get().indexDao()

    private var categoryData: DaPenTiData.Category? = null
    private var allPages : LiveData<PagedList<DaPenTiData.Page>>? = null

    fun fetchPageFromWeb() : Boolean {
        Log.d(TAG, "url: ${categoryData!!.url}")
        isFetchingFromWeb.set(true)
        val list = DaPenTiWeb.getPages(categoryData!!.url)

        for (page in list.reversed()) {
            val id = pageDao.insert(page)
            Log.d(TAG, "insert $page returned id: $id")

            if (id != (-1).toLong()) {
                val index = DaPenTiData.Index(categoryID = categoryData!!.id!!, pageID = id.toInt())
                indexDao.insert(index)
            } else {
                val p = pageDao.getPage(page.title)
                val index = DaPenTiData.Index(categoryID = categoryData!!.id!!, pageID = p.id!!)
                indexDao.insert(index)
            }
        }

        isFetchingFromWeb.set(false)
        return list.isNotEmpty()
    }

    fun preparePagesIfEmpty() {
        if (allPages != null)
            return

        if (categoryData == null)
            categoryData = categoryDao.getCategory(category)

        allPages = LivePagedListBuilder(
                pageDao.getPages(categoryData!!.id!!),
                PagedList.Config.Builder()
                        .setPageSize(PAGE_SIZE)
                        .setEnablePlaceholders(ENABLE_PLACEHOLDERS).build())
                .setBoundaryCallback(object : PagedList.BoundaryCallback<DaPenTiData.Page>() {
                    override fun onZeroItemsLoaded() {
                        helper.runIfNotRunning(PagingRequestHelper.RequestType.INITIAL) {
                            Thread {
                                fetchPageFromWeb()
                                it.recordSuccess()
                            }.start()
                        }
                    }
                })
                .build()
    }

    fun getPages() : LiveData<PagedList<DaPenTiData.Page>>? = allPages

    override fun toString(): String = "FragmentViewModel[$category]"

    class Factor(private val category: String):
            ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return FragmentViewModel(category) as T
        }
    }
}
