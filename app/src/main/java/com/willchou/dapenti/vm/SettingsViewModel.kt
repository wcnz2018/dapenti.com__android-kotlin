package com.willchou.dapenti.vm

import android.arch.lifecycle.ViewModel
import android.support.annotation.WorkerThread
import com.willchou.dapenti.db.DaPenTiRoomDatabase
import java.util.*
import kotlin.collections.ArrayList

class SettingsViewModel : ViewModel() {
    private val pageDao = DaPenTiRoomDatabase.get().pageDao()
    private val indexDao = DaPenTiRoomDatabase.get().indexDao()

    @WorkerThread
    fun removeDateBefore(date: Date) {
        val indices = indexDao.getIndecesBefore(date)

        val pageIDs: MutableList<Int> = ArrayList()
        val indexIDs: MutableList<Int> = ArrayList()
        for (index in indices) {
            pageIDs.add(index.pageID)
            indexIDs.add(index.id!!)
        }

        pageDao.deleteWithIDs(pageIDs)
        indexDao.deleteWithIDs(indexIDs)
    }
}