package com.willchou.dapenti.presenter

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.CardView
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.Toolbar
import android.util.Pair
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView

import com.willchou.dapenti.R
import com.willchou.dapenti.model.DaPenTi
import com.willchou.dapenti.model.Database
import com.willchou.dapenti.model.Settings
import com.woxthebox.draglistview.DragItem
import com.woxthebox.draglistview.DragItemAdapter
import com.woxthebox.draglistview.DragListView
import kotlinx.android.synthetic.main.page_list_item.*

import java.net.URL
import java.util.ArrayList

class PageOrderActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "PageOrderActivity"
    }

    private var applyButton: Button? = null
    private var dragListView: DragListView? = null
    private val itemDetailList = ArrayList<ItemDetail>()

    internal inner class ItemDetail {
        var title: String? = null
        var uniqueID: Long = 0
        var visible: Boolean = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_page_order)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { onBackPressed() }

        if (Settings.settings!!.nightMode) {
            setTheme(R.style.NightModeTheme)

            val color = Settings.settings!!.getBackgroundColor()
            findViewById<View>(R.id.drag_list_view)?.setBackgroundColor(color)
            findViewById<View>(R.id.linearLayout)?.setBackgroundColor(color)
        }

        setupData()
        setupLayout()
    }

    private fun setupData() {
        val database = Database.database ?: return

        val categories = ArrayList<Pair<String, URL>>()
        database.getCategories(categories, false)

        val visibleList = ArrayList<Pair<String, Boolean>>()
        database.getCategoryVisible(visibleList)

        for ((count, p) in categories.withIndex()) {
            val id = ItemDetail()
            id.title = p.first
            id.uniqueID = count.toLong()

            for (p2 in visibleList)
                if (p2.first == id.title) {
                    id.visible = p2.second
                    break
                }

            itemDetailList.add(id)
        }
    }

    private fun setupLayout() {
        applyButton = findViewById(R.id.apply_button)
        applyButton?.visibility = View.GONE
        applyButton?.setOnClickListener { saveChanged() }

        dragListView = findViewById(R.id.drag_list_view)
        if (dragListView == null)
            return

        dragListView?.recyclerView?.isVerticalScrollBarEnabled = true
        dragListView?.setLayoutManager(LinearLayoutManager(this))
        dragListView?.setAdapter(ItemAdapter(), true)
        dragListView?.setCanDragHorizontally(false)
        dragListView?.setCustomDragItem(MyDragItem(this, R.layout.page_list_item))
        dragListView?.setDragListListener(object : DragListView.DragListListenerAdapter() {
            override fun onItemDragEnded(fromPosition: Int, toPosition: Int) {
                if (fromPosition != toPosition && applyButton!!.visibility != View.VISIBLE)
                    applyButton!!.visibility = View.VISIBLE
            }
        })
    }

    private fun saveChanged() {
        applyButton?.visibility = View.GONE

        val list = ArrayList<Pair<String, Boolean>>()
        for (id in itemDetailList)
            list.add(Pair<String, Boolean>(id.title, id.visible))

        if (Database.database != null) {
            Database.database?.updateCategoriesOrderAndVisible(list)
            DaPenTi.daPenTi?.prepareCategory(false)
            Snackbar.make(dragListView!!, "应用成功", Snackbar.LENGTH_SHORT).show()
        } else {
            Snackbar.make(dragListView!!, "数据库操作失败", Snackbar.LENGTH_SHORT)
                    .show()
            applyButton!!.visibility = View.VISIBLE
        }
    }

    internal inner class ItemAdapter : DragItemAdapter<ItemDetail, ViewHolder>() {
        init {
            itemList = itemDetailList
        }

        override fun onBindViewHolder(holder: PageOrderActivity.ViewHolder, position: Int) {
            super.onBindViewHolder(holder, position)

            val id = itemList[position]

            holder.itemDetail = id
            holder.textView.text = id.title
            holder.checkBox.isChecked = id.visible
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int):
                PageOrderActivity.ViewHolder {
            val view = LayoutInflater
                    .from(baseContext)
                    .inflate(R.layout.page_list_item, parent, false)

            if (Settings.settings!!.nightMode) {
                view.findViewById<CardView>(R.id.cardView)
                        ?.setBackgroundColor(Settings.settings!!.getLighterBackgroundColor())
                view.setBackgroundColor(Settings.settings!!.getBackgroundColor())
            }

            return ViewHolder(view, R.id.dragImage, false, applyButton)
        }

        override fun getUniqueItemId(position: Int): Long {
            return itemList[position].uniqueID
        }
    }

    internal inner class ViewHolder(itemView: View, handleResId: Int, dragOnLongPress: Boolean,
                                    private val triggerButton: View?) :
            DragItemAdapter.ViewHolder(itemView, handleResId, dragOnLongPress) {
        var textView: TextView = itemView.findViewById(R.id.pageName)
        var checkBox: CheckBox = itemView.findViewById(R.id.visibleCheckbox)
        var itemDetail: ItemDetail? = null

        override fun onItemClicked(view: View?) {
            super.onItemClicked(view)
            checkBox.toggle()
            itemDetail?.visible = checkBox.isChecked

            if (triggerButton?.visibility != View.VISIBLE)
                triggerButton?.visibility = View.VISIBLE
        }
    }

    internal inner class MyDragItem(context: Context, layoutId: Int) : DragItem(context, layoutId) {
        override fun onBindDragView(clickedView: View, dragView: View) {
            val text = clickedView.findViewById<TextView>(R.id.pageName).text
            val checked = clickedView.findViewById<CheckBox>(R.id.visibleCheckbox).isChecked

            (dragView.findViewById<View>(R.id.pageName) as TextView).text = text
            (dragView.findViewById<View>(R.id.visibleCheckbox) as CheckBox).isChecked = checked

            if (Settings.settings!!.nightMode)
                dragView.setBackgroundColor(Settings.settings!!.getBackgroundColor())
        }
    }
}
