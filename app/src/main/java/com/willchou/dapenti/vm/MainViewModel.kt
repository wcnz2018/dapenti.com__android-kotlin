package com.willchou.dapenti.vm

import android.arch.lifecycle.ViewModel
import com.willchou.dapenti.DaPenTiApplication
import com.willchou.dapenti.db.DaPenTiData
import com.willchou.dapenti.db.DaPenTiRoomDatabase

class MainViewModel : ViewModel() {
    private val dao = DaPenTiRoomDatabase.get(DaPenTiApplication.getAppContext()).categoryDao()

    fun getCategories(visibleOnly: Boolean = true) : List<DaPenTiData.Category> {
        return if (visibleOnly)
            dao.visibleCategories()
        else
            dao.allCategories()
    }

    fun insertCategories(categories: List<DaPenTiData.Category>) {
        dao.insert(categories)
    }
}