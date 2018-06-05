package com.willchou.dapenti.presenter

import android.graphics.Color
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.util.Log
import com.willchou.dapenti.R
import com.willchou.dapenti.model.DaPenTi
import com.willchou.dapenti.model.DaPenTiPage
import com.willchou.dapenti.model.Settings
import com.willchou.dapenti.view.RecyclerViewAdapter

class FavoriteActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "FavoriteActivity"
    }
    private var recyclerView: RecyclerView? = null
    private var favoriteList: List<DaPenTiPage>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorite)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { onBackPressed() }

        recyclerView = findViewById(R.id.recycler_view)

        favoriteList = DaPenTi.daPenTi?.getFavoritePages()
        if (favoriteList == null)
            finish()

        setupRecyclerView()
    }

    override fun onResume() {
        Log.d(TAG, "onResume")
        super.onResume()

        restoreVideoState()
    }

    override fun onBackPressed() {
        super.onBackPressed()

        recyclerView?.adapter = null
    }

    private fun setupRecyclerView() {
        recyclerView?.layoutManager = LinearLayoutManager(this)

        val nightMode = Settings.settings?.nightMode
        if (nightMode != null && nightMode)
            recyclerView?.setBackgroundColor(Color.rgb(48, 48, 48))
        else
            recyclerView?.setBackgroundColor(Color.WHITE)

        recyclerView?.adapter = RecyclerViewAdapter(favoriteList!!)
    }

    private fun restoreVideoState() {
        val lm = recyclerView?.layoutManager as LinearLayoutManager? ?: return

        val first = lm.findFirstVisibleItemPosition()
        val last = lm.findLastVisibleItemPosition()

        recyclerView?.adapter?.notifyItemRangeChanged(first, last - first)
    }
}
