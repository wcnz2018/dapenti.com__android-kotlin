package com.willchou.dapenti.presenter

import android.content.Context
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
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
import com.woxthebox.draglistview.DragItem
import com.woxthebox.draglistview.DragItemAdapter
import com.woxthebox.draglistview.DragListView

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
        toolbar.setNavigationOnClickListener { v: View -> onBackPressed() }

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

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageOrderActivity.ViewHolder {
            val view = LayoutInflater.from(baseContext).inflate(R.layout.page_list_item, parent, false)
            return PageOrderActivity().ViewHolder(view, R.id.drag_image, false)
        }

        override fun getUniqueItemId(position: Int): Long {
            return itemList[position].uniqueID
        }
    }

    internal inner class ViewHolder(itemView: View, handleResId: Int, dragOnLongPress: Boolean) :
            DragItemAdapter.ViewHolder(itemView, handleResId, dragOnLongPress) {
        var textView: TextView = itemView.findViewById(R.id.drag_page_name)
        var checkBox: CheckBox = itemView.findViewById(R.id.visible_checkbox)
        var itemDetail: ItemDetail? = null

        override fun onItemClicked(view: View?) {
            super.onItemClicked(view)
            checkBox.toggle()

            itemDetail!!.visible = checkBox.isChecked

            if (applyButton!!.visibility != View.VISIBLE)
                applyButton!!.visibility = View.VISIBLE
        }
    }

    internal inner class MyDragItem(context: Context, layoutId: Int) : DragItem(context, layoutId) {
        override fun onBindDragView(clickedView: View, dragView: View) {
            val text = (clickedView.findViewById<View>(R.id.drag_page_name) as TextView).text
            (dragView.findViewById<View>(R.id.drag_page_name) as TextView).text = text

            val checked = (clickedView.findViewById<View>(R.id.visible_checkbox) as CheckBox).isChecked
            (dragView.findViewById<View>(R.id.visible_checkbox) as CheckBox).isChecked = checked
        }
    }
}
