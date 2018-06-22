package com.willchou.dapenti.db

import android.arch.persistence.room.*
import java.util.*


class DaPenTiData {
    companion object {
        const val DATABASE_NAME = "daPenTi.db"
        const val DATABASE_VERSION = 1

        const val TABLE_CATEGORIES = "categories"
        const val COLUMN_CATEGORY__ID = "id"
        const val COLUMN_CATEGORY__TITLE = "title"
        const val COLUMN_CATEGORY__URL = "url"
        const val COLUMN_CATEGORY__VISIBLE = "visible"
        const val COLUMN_CATEGORY__DISPLAY_ORDER = "display_order"

        const val TABLE_PAGES = "pages"
        const val COLUMN_PAGE__ID = "id"
        const val COLUMN_PAGE__TITLE = "title"
        const val COLUMN_PAGE__URL = "url"
        const val COLUMN_PAGE__FAVORITE = "favorite"
        const val COLUMN_PAGE__FAVORITE_AT = "favorite_at"
        const val COLUMN_PAGE__CONTENT = "html_content"
        const val COLUMN_PAGE__CHECKED = "checked"

        const val TABLE_PAGE_INDEX = "page_index"
        const val COLUMN_PAGE_INDEX__ID = "id"
        const val COLUMN_PAGE_INDEX__CATEGORY_ID = "category_id"
        const val COLUMN_PAGE_INDEX__PAGE_ID = "page_id"
        const val COLUMN_PAGE_INDEX__CREATE_AT = "create_at"
    }

    @Entity(tableName = TABLE_CATEGORIES,
            indices = [(Index(value = [COLUMN_CATEGORY__TITLE], unique = true))])
    data class Category(@PrimaryKey(autoGenerate = true)
                        @ColumnInfo(name = COLUMN_CATEGORY__ID)
                        val id: Int? = null,

                        @ColumnInfo(name = COLUMN_CATEGORY__TITLE)
                        val title: String,

                        @ColumnInfo(name = COLUMN_CATEGORY__URL)
                        val url: String,

                        @ColumnInfo(name = COLUMN_CATEGORY__VISIBLE)
                        val visible: Int = 1,

                        @ColumnInfo(name = COLUMN_CATEGORY__DISPLAY_ORDER)
                        val displayOrder: Int = 0)

    @Entity(tableName = TABLE_PAGES,
            indices = [(Index(value = [COLUMN_PAGE__TITLE], unique = true))])
    data class Page(@PrimaryKey(autoGenerate = true)
                    @ColumnInfo(name = COLUMN_PAGE__ID)
                    val id: Int? = null,

                    @ColumnInfo(name = COLUMN_PAGE__TITLE)
                    val title: String,

                    @ColumnInfo(name = COLUMN_PAGE__URL)
                    val url: String,

                    @ColumnInfo(name = COLUMN_PAGE__FAVORITE)
                    var favorite: Int = 0,

                    @ColumnInfo(name = COLUMN_PAGE__FAVORITE_AT)
                    var favoriteAt: Date? = null,

                    @ColumnInfo(name = COLUMN_PAGE__CONTENT)
                    var content: String = "",

                    @ColumnInfo(name = COLUMN_PAGE__CHECKED)
                    var checked: Int = 0)

    @Entity(tableName = TABLE_PAGE_INDEX,
            indices = [(Index(value = [COLUMN_PAGE_INDEX__PAGE_ID, COLUMN_PAGE_INDEX__CATEGORY_ID], unique = true))])
    data class Index(@PrimaryKey(autoGenerate = true)
                     @ColumnInfo(name = COLUMN_PAGE_INDEX__ID)
                     val id: Int? = null,

                     @ColumnInfo(name = COLUMN_PAGE_INDEX__CATEGORY_ID)
                     val categoryID: Int,

                     @ColumnInfo(name = COLUMN_PAGE_INDEX__PAGE_ID)
                     val pageID: Int,

                     @ColumnInfo(name = COLUMN_PAGE_INDEX__CREATE_AT)
                     val createAt: Date = Calendar.getInstance().time)
}

