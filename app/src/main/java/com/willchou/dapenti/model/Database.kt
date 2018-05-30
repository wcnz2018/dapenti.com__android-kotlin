package com.willchou.dapenti.model

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import android.util.Pair

import java.net.URL

class Database(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    companion object {
        private const val TAG = "DaPenTiDatabase"
        private const val DATABASE_NAME = "daPenTi.db"
        private const val DATABASE_VERSION = 1

        private const val TABLE_CATEGORIES = "categories"
        private const val TABLE_PAGES = "pages"

        private const val COLUMN_CATEGORY__ID = "id"
        private const val COLUMN_CATEGORY__TITLE = "title"
        private const val COLUMN_CATEGORY__URL = "url"
        private const val COLUMN_CATEGORY__VISIBLE = "visible"
        private const val COLUMN_CATEGORY__DISPLAY_ORDER = "display_order"

        private const val COLUMN_PAGE__ID = "id"
        private const val COLUMN_PAGE__TITLE = "title"
        private const val COLUMN_PAGE__URL = "url"
        private const val COLUMN_PAGE__BELONG = "belong_category_id"
        private const val COLUMN_PAGE__CREATE_TIME = "create_at"
        private const val COLUMN_PAGE__CONTENT = "html_content"

        var database: Database? = null
    }

    init {
        database = this
    }

    override fun onCreate(sqLiteDatabase: SQLiteDatabase) {
        var sql: String = "CREATE TABLE IF NOT EXISTS " + TABLE_CATEGORIES + "(" +
                COLUMN_CATEGORY__ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                COLUMN_CATEGORY__TITLE + " TEXT UNIQUE NOT NULL," +
                COLUMN_CATEGORY__VISIBLE + " INTERGER NOT NULL DEFAULT 1," +
                COLUMN_CATEGORY__URL + " TEXT NOT NULL," +
                COLUMN_CATEGORY__DISPLAY_ORDER + " INTEGER DEFAULT 0);"
        Log.d(TAG, "sql: $sql")
        sqLiteDatabase.execSQL(sql)

        sql = "CREATE TABLE IF NOT EXISTS " + TABLE_PAGES + "(" +
                COLUMN_PAGE__ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                COLUMN_PAGE__TITLE + " TEXT UNIQUE NOT NULL," +
                COLUMN_PAGE__URL + " TEXT NOT NULL," +
                COLUMN_PAGE__BELONG + " TEXT NOT NULL," +
                COLUMN_PAGE__CREATE_TIME + " TIMESTAMP NOT NULL DEFAULT(STRFTIME('%Y-%m-%d %H:%M:%f', 'NOW'))," +
                COLUMN_PAGE__CONTENT + " TEXT);"
        Log.d(TAG, "sql: $sql")
        sqLiteDatabase.execSQL(sql)

        Log.d(TAG, "onCreate")
    }

    override fun onUpgrade(sqLiteDatabase: SQLiteDatabase, i: Int, i1: Int) {

    }

    private fun toDatabaseCategoryID(categoryID: Int): String {
        return "[$categoryID]"
    }

    private fun getCategoryID(db: SQLiteDatabase, title: String): Int {
        val sql = ("SELECT " + COLUMN_CATEGORY__ID + " FROM " + TABLE_CATEGORIES
                + " WHERE " + COLUMN_CATEGORY__TITLE + " =? ")
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

    private fun getPageID(db: SQLiteDatabase, page: String): Int {
        val sql = ("SELECT " + COLUMN_PAGE__ID + " FROM " + TABLE_PAGES
                + " WHERE " + COLUMN_PAGE__TITLE + " =? ")
        val cursor = db.rawQuery(sql, arrayOf(page))

        var pageID = -1
        while (cursor.moveToNext()) {
            pageID = cursor.getInt(0)
            if (pageID != -1)
                break
        }

        cursor.close()
        return pageID
    }

    private fun getPageBelong(db: SQLiteDatabase, pageID: Int): String? {
        val sql = ("SELECT " + COLUMN_PAGE__BELONG + " FROM " + TABLE_PAGES
                + " WHERE " + COLUMN_PAGE__ID + " =? ")
        val cursor = db.rawQuery(sql, arrayOf("" + pageID))

        var pageBelong: String? = ""
        while (cursor.moveToNext()) {
            pageBelong = cursor.getString(0)
            if (pageBelong != null && !pageBelong.isEmpty())
                break
        }
        cursor.close()
        return pageBelong
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

    fun updateCategoriesOrderAndVisible(pairs: List<Pair<String, Boolean>>) {
        synchronized(this) {
            val db = writableDatabase
            for (i in pairs.indices) {
                val p = pairs[i]

                val CategoryID = getCategoryID(db, p.first)
                if (CategoryID == -1) {
                    Log.d(TAG, "Unable to get categoryID: " + p.first)
                    continue
                }

                val values = ContentValues()
                values.put(COLUMN_CATEGORY__DISPLAY_ORDER, i)
                values.put(COLUMN_CATEGORY__VISIBLE, if (p.second) "1" else "0")

                db.update(TABLE_CATEGORIES, values,
                        "$COLUMN_CATEGORY__ID=?", arrayOf(CategoryID.toString() + ""))
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

    private fun addPageCategory(db: SQLiteDatabase, pageID: Int, categoryID: Int) {
        var pageBelong: String? = getPageBelong(db, pageID) ?: return

        for (s in pageBelong!!.split(",".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()) {
            if (s.contains(toDatabaseCategoryID(categoryID))) {
                Log.d(TAG, "categoryID already exists.")
                return
            }
        }

        pageBelong += toDatabaseCategoryID(categoryID)

        val values = ContentValues()
        values.put(COLUMN_PAGE__BELONG, pageBelong)

        db.update(TABLE_PAGES, values, "$COLUMN_PAGE__ID=?", arrayOf("" + pageID))
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

            val pageID = getPageID(db, page)

            if (getPageID(db, page) != -1) {
                Log.d(TAG, "addPage with $page, already exists, update category id")
                addPageCategory(db, pageID, categoryID)
                db.close()
                return
            }

            val values = ContentValues()
            values.put(COLUMN_PAGE__BELONG, toDatabaseCategoryID(categoryID))
            values.put(COLUMN_PAGE__TITLE, page)
            values.put(COLUMN_PAGE__URL, url)

            try {
                db.insertOrThrow(TABLE_PAGES, null, values)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                db.close()
            }
        }
    }

    fun getPages(category: String, pairs: MutableList<Pair<String, URL>>) {
        pairs.clear()

        synchronized(this) {
            val db = readableDatabase
            val categoryID = getCategoryID(db, category)
            if (categoryID == -1) {
                Log.d(TAG, "Unable to find categoryID with $category")
                db.close()
                return
            }
            val cursor = db.query(TABLE_PAGES, arrayOf(COLUMN_PAGE__TITLE, COLUMN_PAGE__URL),
                    COLUMN_PAGE__BELONG + " LIKE '%${toDatabaseCategoryID(categoryID)}%'",
                    null, null, null,
                    "$COLUMN_PAGE__CREATE_TIME DESC")

            while (cursor.moveToNext()) {
                try {
                    pairs.add(Pair(cursor.getString(0), URL(cursor.getString(1))))
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }

            Log.d(TAG, "getPages get size: ${pairs.size}")

            cursor.close()
            db.close()
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
