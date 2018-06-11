package com.willchou.dapenti.model

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import android.util.Pair
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class Database(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    companion object {
        private const val TAG = "DaPenTiDatabase"

        private const val DATABASE_NAME = "daPenTi.db"
        private const val DATABASE_VERSION = 1

        private const val TABLE_CATEGORIES = "categories"
        private const val COLUMN_CATEGORY__ID = "id"
        private const val COLUMN_CATEGORY__TITLE = "title"
        private const val COLUMN_CATEGORY__URL = "url"
        private const val COLUMN_CATEGORY__VISIBLE = "visible"
        private const val COLUMN_CATEGORY__DISPLAY_ORDER = "display_order"

        private const val TABLE_PAGES = "pages"
        private const val COLUMN_PAGE__ID = "id"
        private const val COLUMN_PAGE__TITLE = "title"
        private const val COLUMN_PAGE__URL = "url"
        private const val COLUMN_PAGE__FAVORITE = "favorite"
        private const val COLUMN_PAGE__CONTENT = "html_content"

        private const val TABLE_PAGE_INDEX = "page_index"
        private const val COLUMN_PAGE_INDEX__ID = "id"
        private const val COLUMN_PAGE_INDEX__CATEGORY_ID = "category_id"
        private const val COLUMN_PAGE_INDEX__PAGE_ID = "page_id"
        private const val COLUMN_PAGE_INDEX__CREATE_AT = "create_at"

        var database: Database? = null
    }

    init {
        database = this
    }

    class PageInfo(val pageTitle: String, val pageUrl: URL, val isFavorite: Boolean)

    override fun onCreate(sqLiteDatabase: SQLiteDatabase) {
        Log.d(TAG, "onCreate")

        var sql: String = "CREATE TABLE IF NOT EXISTS $TABLE_CATEGORIES (" +
                "$COLUMN_CATEGORY__ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                "$COLUMN_CATEGORY__TITLE TEXT UNIQUE NOT NULL," +
                "$COLUMN_CATEGORY__VISIBLE INTEGER NOT NULL DEFAULT 1," +
                "$COLUMN_CATEGORY__URL TEXT NOT NULL," +
                "$COLUMN_CATEGORY__DISPLAY_ORDER INTEGER DEFAULT 0);"
        Log.d(TAG, "sql: $sql")
        sqLiteDatabase.execSQL(sql)

        sql = "CREATE TABLE IF NOT EXISTS $TABLE_PAGES (" +
                "$COLUMN_PAGE__ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                "$COLUMN_PAGE__TITLE TEXT UNIQUE NOT NULL," +
                "$COLUMN_PAGE__URL TEXT NOT NULL," +
                "$COLUMN_PAGE__FAVORITE INTEGER DEFAULT 0," +
                "$COLUMN_PAGE__CONTENT TEXT);"
        Log.d(TAG, "sql: $sql")
        sqLiteDatabase.execSQL(sql)

        sql = "CREATE TABLE IF NOT EXISTS $TABLE_PAGE_INDEX (" +
                "$COLUMN_PAGE_INDEX__ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                "$COLUMN_PAGE_INDEX__CATEGORY_ID INTEGER NOT NULL," +
                "$COLUMN_PAGE_INDEX__PAGE_ID INTEGER NOT NULL," +
                "$COLUMN_PAGE_INDEX__CREATE_AT TIMESTAMP NOT NULL DEFAULT(STRFTIME('%Y-%m-%d %H:%M:%f', 'NOW')));"
        Log.d(TAG, "sql: $sql")
        sqLiteDatabase.execSQL(sql)
    }

    override fun onUpgrade(sqLiteDatabase: SQLiteDatabase, i: Int, i1: Int) { }

    private fun getCategoryID(db: SQLiteDatabase, title: String): Int {
        val sql = "SELECT $COLUMN_CATEGORY__ID FROM $TABLE_CATEGORIES " +
                "WHERE $COLUMN_CATEGORY__TITLE =? "
        val cursor = db.rawQuery(sql, arrayOf(title))

        var categoryID = -1
        while (cursor.moveToNext()) {
            categoryID = cursor.getInt(0)
            if (categoryID != -1)
                break
        }

        cursor.close()
        return categoryID
    }

    private fun getPageID(db: SQLiteDatabase, pageTitle: String): Int {
        val sql = "SELECT $COLUMN_PAGE__ID FROM $TABLE_PAGES " +
                "WHERE $COLUMN_PAGE__TITLE =? "
        val cursor = db.rawQuery(sql, arrayOf(pageTitle))

        var pageID = -1
        while (cursor.moveToNext()) {
            pageID = cursor.getInt(0)
            if (pageID != -1)
                break
        }

        cursor.close()
        return pageID
    }

    fun addCategory(category: String, url: String) {
        Log.d(TAG, "addCategory: $category, url: $url")

        val values = ContentValues()
        values.put(COLUMN_CATEGORY__TITLE, category)
        values.put(COLUMN_CATEGORY__URL, url)

        synchronized(this) {
            val db = writableDatabase
            if (getCategoryID(db, category) != -1) {
                Log.d(TAG, "addCategory with $category, already exists")
                db.close()
                return
            }

            try {
                db.insertOrThrow(TABLE_CATEGORIES, null, values)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                db.close()
            }
        }
    }

    fun removePageBefore(before: Date) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.US)
        val beforeString = dateFormat.format(before)

        Log.d(TAG, "remove before $beforeString")

        synchronized(this) {
            val db = writableDatabase
            val cursor = db.query(TABLE_PAGE_INDEX, arrayOf(COLUMN_PAGE_INDEX__PAGE_ID),
                    "$COLUMN_PAGE_INDEX__CREATE_AT<=?", arrayOf(beforeString),
                    null, null, null)

            val pageIdList: MutableList<Int> = ArrayList()
            while (cursor.moveToNext()) {
                pageIdList.add(cursor.getInt(0))
            }
            cursor.close()

            if (pageIdList.isEmpty()) {
                Log.d(TAG, "no index found before $beforeString")
                db.close()
                return
            }

            val joinString = pageIdList.joinToString()
            db.delete(TABLE_PAGE_INDEX,
                    "$COLUMN_PAGE_INDEX__PAGE_ID IN ($joinString)", null)

            db.delete(TABLE_PAGES,
                    "$COLUMN_PAGE__ID IN ($joinString)", null)

            db.close()
        }

        DaPenTi.daPenTi!!.databaseChanged()
    }

    fun updateCategoriesOrderAndVisible(pairs: List<Pair<String, Boolean>>) {
        synchronized(this) {
            val db = writableDatabase
            for (i in pairs.indices) {
                val p = pairs[i]

                val categoryID = getCategoryID(db, p.first)
                if (categoryID == -1) {
                    Log.d(TAG, "Unable to get categoryID: " + p.first)
                    continue
                }

                val values = ContentValues()
                values.put(COLUMN_CATEGORY__DISPLAY_ORDER, i)
                values.put(COLUMN_CATEGORY__VISIBLE, if (p.second) "1" else "0")

                db.update(TABLE_CATEGORIES, values,
                        "$COLUMN_CATEGORY__ID=?", arrayOf(categoryID.toString()))
            }
            db.close()
        }
    }

    fun getCategories(pairs: MutableList<Pair<String, URL>>, visibleOnly: Boolean) {
        pairs.clear()

        var selection: String? = null
        var selectionArg: Array<String>? = null

        if (visibleOnly) {
            selection = "$COLUMN_CATEGORY__VISIBLE=?"
            selectionArg = arrayOf("1")
        }

        synchronized(this) {
            val db = readableDatabase
            val cursor = db.query(TABLE_CATEGORIES, arrayOf(COLUMN_CATEGORY__TITLE, COLUMN_CATEGORY__URL),
                    selection, selectionArg, null, null,
                    COLUMN_CATEGORY__DISPLAY_ORDER)

            while (cursor.moveToNext()) {
                try {
                    pairs.add(Pair(cursor.getString(0),
                            URL(cursor.getString(1))))
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }

            Log.d(TAG, "getCategories get size: " + pairs.size)

            cursor.close()
            db.close()
        }
    }

    fun getCategoryVisible(pairs: MutableList<Pair<String, Boolean>>) {
        pairs.clear()

        synchronized(this) {
            val db = readableDatabase
            val cursor = db.query(TABLE_CATEGORIES,
                    arrayOf(COLUMN_CATEGORY__TITLE, COLUMN_CATEGORY__VISIBLE),
                    null, null, null, null, null)

            while (cursor.moveToNext())
                pairs.add(Pair(cursor.getString(0), cursor.getInt(1) == 1))

            cursor.close()
            db.close()
        }
    }

    private fun insertNewPage(db: SQLiteDatabase, pageTitle: String, pageUrl: String): Int {
        val values = ContentValues()
        values.put(COLUMN_PAGE__TITLE, pageTitle)
        values.put(COLUMN_PAGE__URL, pageUrl)

        try {
            db.insertOrThrow(TABLE_PAGES, null, values)
        } catch (e : Exception) { e.printStackTrace() }
        return getPageID(db, pageTitle)
    }

    fun addPage(category: String, page: String, url: String) {
        synchronized(this) {
            val db = writableDatabase
            val categoryID = getCategoryID(db, category)
            if (categoryID == -1) {
                Log.d(TAG, "Unable to find categoryID with $category")
                db.close()
                return
            }

            var pageID = getPageID(db, page)
            if (pageID == -1)
                pageID = insertNewPage(db, page, url)

            if (pageID == -1) {
                Log.d(TAG, "Unable to insert new page")
                return
            }

            val cursor = db.query(TABLE_PAGE_INDEX, arrayOf(COLUMN_PAGE_INDEX__ID),
                    "$COLUMN_PAGE_INDEX__PAGE_ID=? AND $COLUMN_PAGE_INDEX__CATEGORY_ID=?",
                    arrayOf(pageID.toString(), categoryID.toString()),
                    null, null, null)
            if (cursor.count != 0) {
                Log.d(TAG, "Index already exist($category -> $page)")
                cursor.close()
                return
            }
            cursor.close()

            val values = ContentValues()
            values.put(COLUMN_PAGE_INDEX__CATEGORY_ID, categoryID)
            values.put(COLUMN_PAGE_INDEX__PAGE_ID, pageID)

            try {
                db.insertOrThrow(TABLE_PAGE_INDEX, null, values)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                db.close()
            }
        }
    }

    fun setPageFavorite(pageTitle: String, favorite: Boolean) {
        synchronized(this) {
            val db = writableDatabase
            val values = ContentValues()
            values.put(COLUMN_PAGE__FAVORITE, if (favorite) "1" else "0")
            db.update(TABLE_PAGES, values, "$COLUMN_PAGE__TITLE=?", arrayOf(pageTitle))
            db.close()
        }
    }

    fun getPageFavorite(pageTitle: String): Boolean {
        synchronized(this) {
            val db = readableDatabase
            val cursor = db.query(TABLE_PAGES, arrayOf(COLUMN_PAGE__FAVORITE), "$COLUMN_PAGE__TITLE=?",
                    arrayOf(pageTitle), null, null, null)

            var favorite = false
            while (cursor.moveToNext()) {
                val b = cursor.getInt(0)
                if (b == 0 || b == 1) {
                    favorite = b == 1
                    break
                }
            }
            cursor.close()
            db.close()

            return favorite
        }
    }

    fun getPages(category: String, pairs: MutableList<PageInfo>) {
        pairs.clear()

        val dataBundleMap : HashMap<Int, PageInfo> = HashMap()

        synchronized(this) {
            val db = readableDatabase
            val categoryID = getCategoryID(db, category)
            if (categoryID == -1) {
                Log.d(TAG, "Unable to find categoryID with $category")
                db.close()
                return
            }

            var cursor = db.query(TABLE_PAGE_INDEX, arrayOf(COLUMN_PAGE_INDEX__PAGE_ID),
                    "$COLUMN_PAGE_INDEX__CATEGORY_ID=?", arrayOf(categoryID.toString()),
                    null, null,
                    "$COLUMN_PAGE_INDEX__CREATE_AT DESC")

            val pageIDs: MutableList<Int> = ArrayList()
            while (cursor.moveToNext())
                pageIDs.add(cursor.getInt(0))
            cursor.close()

            if (pageIDs.size == 0) {
                db.close()
                return
            }

            cursor = db.query(TABLE_PAGES,
                    arrayOf(COLUMN_PAGE__ID, COLUMN_PAGE__TITLE, COLUMN_PAGE__URL, COLUMN_PAGE__FAVORITE),
                    "$COLUMN_PAGE__ID IN (${pageIDs.joinToString()})",
                    null, null, null, null)

            try {
                while (cursor.moveToNext())
                    dataBundleMap[cursor.getInt(0)] =
                            PageInfo(cursor.getString(1), URL(cursor.getString(2)), cursor.getInt(3) == 1)
            } catch (e: Exception) { e.printStackTrace() } finally {
                cursor.close()
                db.close()
            }

            for (i in pageIDs)
                pairs.add(dataBundleMap[i]!!)
        }
    }

    fun updatePageContent(page: String, content: String) {
        synchronized(this) {
            val db = writableDatabase
            val pageID = getPageID(db, page)
            if (pageID == -1) {
                Log.d(TAG, "Unable to find page with $page")
                db.close()
                return
            }

            val values = ContentValues()
            values.put(COLUMN_PAGE__CONTENT, content)

            try {
                db.update(TABLE_PAGES, values,
                        "$COLUMN_PAGE__ID=?", arrayOf("" + pageID))
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                db.close()
            }
        }
    }

    fun getPageContent(page: String): String? {
        var content: String? = ""
        synchronized(this) {
            val db = readableDatabase
            val cursor = db.query(TABLE_PAGES, arrayOf(COLUMN_PAGE__CONTENT),
                    "$COLUMN_PAGE__TITLE=?", arrayOf(page), null, null, null)

            while (cursor.moveToNext()) {
                content = cursor.getString(0)
                if (content != null)
                    break
            }

            cursor.close()
            db.close()
        }
        return content
    }
}
