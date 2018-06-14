package com.willchou.dapenti.presenter

import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import com.willchou.dapenti.R
import com.willchou.dapenti.model.DaPenTi
import com.willchou.dapenti.model.DaPenTiPage
import com.willchou.dapenti.model.Settings
import com.willchou.dapenti.view.ConfirmDialog
import com.willchou.dapenti.view.DRecyclerView
import com.willchou.dapenti.view.RecyclerViewAdapter
import me.majiajie.swipeback.SwipeBackActivity

class FavoriteActivity : SwipeBackActivity() {
    companion object {
        private const val TAG = "FavoriteActivity"
    }
    private var recyclerView: DRecyclerView? = null

    private var linearLayout: LinearLayout? = null
    private var noteLayout: LinearLayout? = null
    private var favoriteList: List<DaPenTiPage>? = null

    private val clickListener = View.OnClickListener { view ->
        when (view.id) {
            R.id.reverse_button -> recyclerView?.reverseSelect()
            R.id.remove_button -> {
                val selectedPageList = recyclerView?.getSelectPages()
                if (selectedPageList == null || selectedPageList.isEmpty()) {
                    recyclerView?.enterSelectMode()
                    return@OnClickListener
                }

                val confirmDialog = ConfirmDialog(this,
                        resources.getString(R.string.title_activity_favorite),
                        resources.getString(R.string.confirm_message_remove_favorite))

                confirmDialog.clickEventListener = object : ConfirmDialog.ClickEventListener {
                    override fun confirmed() {
                        for (page in selectedPageList) {
                            page.setFavorite(false)
                            page.isSelected = false
                        }

                        loadAdapterData()
                        leaveSelectMode()

                        Snackbar.make(recyclerView!!,
                                resources.getString(R.string.snack_removed_from_favorite),
                                Snackbar.LENGTH_LONG).show()
                    }
                }
                confirmDialog.show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorite)

        val toolbar = findViewById(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { onBackPressed() }

        recyclerView = findViewById(R.id.recycler_view) as DRecyclerView
        linearLayout = findViewById(R.id.linearLayout) as LinearLayout
        noteLayout = findViewById(R.id.noteLayout) as LinearLayout

        val reverseButton = findViewById(R.id.reverse_button) as Button
        val removeButton = findViewById(R.id.remove_button) as Button

        reverseButton.setOnClickListener(clickListener)
        removeButton.setOnClickListener(clickListener)

        setupContent()
    }

    override fun onResume() {
        Log.d(TAG, "onResume")
        super.onResume()

        recyclerView?.updateVisibleState(null)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        Log.d(TAG, "onCreateOptionsMenu")
        menuInflater.inflate(R.menu.favorite, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Log.d(TAG, "item clicked: " + item.title)
        when (item.itemId) {
            R.id.action_select_mode -> {
                if (noteLayout!!.visibility == View.GONE) {
                    if (DRecyclerView.isSelectMode())
                        leaveSelectMode()
                    else
                        enterSelectMode()
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        if (DRecyclerView.isSelectMode()) {
            leaveSelectMode()
            return
        }

        recyclerView?.exitSelectMode()
        recyclerView?.adapter = null
        super.onBackPressed()
    }

    private fun enterSelectMode() {
        recyclerView?.enterSelectMode()
        linearLayout?.visibility = View.VISIBLE
    }

    private fun leaveSelectMode() {
        recyclerView?.exitSelectMode()
        linearLayout?.visibility = View.GONE
    }

    private fun loadAdapterData() {
        favoriteList = DaPenTi.daPenTi?.getFavoritePages()
        if (favoriteList!!.isEmpty()) {
            linearLayout!!.visibility = View.GONE
            noteLayout!!.visibility = View.VISIBLE
            return
        }

        val adapter = recyclerView?.getRecyclerViewAdapter()
        adapter?.daPenTiPages = favoriteList!!
        adapter?.notifyDataSetChanged()
    }

    private fun setupContent() {
        recyclerView?.layoutManager = LinearLayoutManager(this)

        val backgroundColor = Settings.settings!!.getBackgroundColor()
        recyclerView?.setBackgroundColor(backgroundColor)
        linearLayout?.setBackgroundColor(backgroundColor)

        recyclerView?.adapter = RecyclerViewAdapter(null)

        loadAdapterData()
    }
}
