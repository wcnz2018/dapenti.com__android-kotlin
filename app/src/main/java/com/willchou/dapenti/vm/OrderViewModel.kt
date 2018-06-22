package com.willchou.dapenti.vm

import android.arch.lifecycle.ViewModel
import android.support.annotation.WorkerThread
import com.willchou.dapenti.db.DaPenTiRoomDatabase

class OrderViewModel : ViewModel() {
    private val dao = DaPenTiRoomDatabase.get().categoryDao()

    @WorkerThread
    fun getCategoryAndVisibility() : List<Pair<String, Boolean>> {
        val list: MutableList<Pair<String, Boolean>> = ArrayList()
        for (c in dao.allCategories())
            list.add(Pair(c.title, c.visible == 1))
        return list
    }

    @WorkerThread
    fun saveCategoryOrderAndVisibility(list : List<Pair<String, Boolean>>) {
        for ((i, p) in list.withIndex())
            dao.updateOrderAndVisible(p.first, i, if (p.second) 1 else 0)
    }
}