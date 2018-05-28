package com.willchou.dapenti.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.util.Pair;

import java.net.URL;
import java.util.List;

public class Database extends SQLiteOpenHelper {
    private static final String TAG = "DaPenTiDatabase";
    private static final String DATABASE_NAME = "daPenTi.db";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_CATEGORIES = "categories";
    private static final String TABLE_PAGES = "pages";

    private static final String COLUMN_CATEGORY__ID = "id";
    private static final String COLUMN_CATEGORY__TITLE = "title";
    private static final String COLUMN_CATEGORY__URL = "url";
    private static final String COLUMN_CATEGORY__VISIBLE = "visible";
    private static final String COLUMN_CATEGORY__DISPLAY_ORDER = "display_order";

    private static final String COLUMN_PAGE__ID = "id";
    private static final String COLUMN_PAGE__TITLE = "tile";
    private static final String COLUMN_PAGE__URL = "url";
    private static final String COLUMN_PAGE__BELONG = "belong_category_id";
    private static final String COLUMN_PAGE__CREATE_TIME = "create_at";
    private static final String COLUMN_PAGE__CONTENT = "html_content";

    private static Database database = null;

    public Database(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        database = this;
    }

    public static Database getDatabase() {
        return database;
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        String sql;
        sql = "CREATE TABLE IF NOT EXISTS " + TABLE_CATEGORIES + "(" +
                COLUMN_CATEGORY__ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                COLUMN_CATEGORY__TITLE + " TEXT UNIQUE NOT NULL," +
                COLUMN_CATEGORY__VISIBLE + " INTERGER NOT NULL DEFAULT 1," +
                COLUMN_CATEGORY__URL + " TEXT NOT NULL," +
                COLUMN_CATEGORY__DISPLAY_ORDER + " INTEGER DEFAULT 0);";
        Log.d(TAG, "sql: " + sql);
        sqLiteDatabase.execSQL(sql);

        sql = "CREATE TABLE IF NOT EXISTS " + TABLE_PAGES + "(" +
                COLUMN_PAGE__ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                COLUMN_PAGE__TITLE + " TEXT UNIQUE NOT NULL," +
                COLUMN_PAGE__URL + " TEXT NOT NULL," +
                COLUMN_PAGE__BELONG + " INTERGER," +
                COLUMN_PAGE__CREATE_TIME + " TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                COLUMN_PAGE__CONTENT + " TEXT," +
                "FOREIGN KEY(" + COLUMN_PAGE__BELONG + ") REFERENCES "+
                TABLE_CATEGORIES + "(" + COLUMN_CATEGORY__ID + ")" +
                ");";
        Log.d(TAG, "sql: " + sql);
        sqLiteDatabase.execSQL(sql);

        Log.d(TAG, "onCreate");
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }

    private int getCategoryID(SQLiteDatabase db, String title) {
        String sql = "SELECT " + COLUMN_CATEGORY__ID + " FROM " + TABLE_CATEGORIES
                + " WHERE " + COLUMN_CATEGORY__TITLE + " =? ";
        Cursor cursor = db.rawQuery(sql, new String[]{title});

        int categoryID = -1;
        while (cursor.moveToNext()) {
            categoryID = cursor.getInt(0);
            if (categoryID != -1)
                break;
        }

        cursor.close();
        return categoryID;
    }

    private int getPageID(SQLiteDatabase db, String page) {
        String sql = "SELECT " + COLUMN_PAGE__ID + " FROM " + TABLE_PAGES
                + " WHERE " + COLUMN_PAGE__TITLE + " =? ";
        Cursor cursor = db.rawQuery(sql, new String[]{page});

        int pageID = -1;
        while (cursor.moveToNext()) {
            pageID = cursor.getInt(0);
            if (pageID != -1)
                break;
        }

        cursor.close();
        return pageID;
    }

    public void addCategory(String category, String url) {
        Log.d(TAG, "addCategory: " + category + ", url: " + url);

        ContentValues values = new ContentValues();
        values.put(COLUMN_CATEGORY__TITLE, category);
        values.put(COLUMN_CATEGORY__URL, url);

        synchronized (this) {
            SQLiteDatabase db = getWritableDatabase();
            if (getCategoryID(db, category) != -1) {
                Log.d(TAG, "addCategory with " + category + ", already exists");
                db.close();
                return;
            }

            try {
                db.insertOrThrow(TABLE_CATEGORIES, null, values);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                db.close();
            }
        }
    }

    public void getCategories(List<Pair<String, URL>> pairs) {
        pairs.clear();

        synchronized (this) {
            SQLiteDatabase db = getReadableDatabase();
            Cursor cursor = db.query(TABLE_CATEGORIES, new String[]{COLUMN_CATEGORY__TITLE, COLUMN_CATEGORY__URL},
                    COLUMN_CATEGORY__VISIBLE + "=?", new String[]{"1"},
                    null, null,
                    COLUMN_CATEGORY__DISPLAY_ORDER);

            while (cursor.moveToNext()) {
                try {
                    pairs.add(new Pair<>(cursor.getString(0),
                            new URL(cursor.getString(1))));
                } catch (Exception e) { e.printStackTrace(); }
            }

            Log.d(TAG, "getCategories get size: " + pairs.size());

            cursor.close();
            db.close();
        }
    }

    public void addPage(String category, String page, String url) {
        synchronized (this) {
            SQLiteDatabase db = getWritableDatabase();
            int categoryID = getCategoryID(db, category);
            if (categoryID == -1) {
                Log.d(TAG, "Unable to find categoryID with " + category);
                db.close();
                return;
            }

            if (getPageID(db, page) != -1) {
                Log.d(TAG, "addPage with " + page + ", already exists");
                db.close();
                return;
            }

            ContentValues values = new ContentValues();
            values.put(COLUMN_PAGE__BELONG, categoryID);
            values.put(COLUMN_PAGE__TITLE, page);
            values.put(COLUMN_PAGE__URL, url);

            try {
                db.insertOrThrow(TABLE_PAGES, null, values);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                db.close();
            }
        }
    }

    public void getPages(String category, List<Pair<String, URL>> pairs) {
        pairs.clear();

        synchronized (this) {
            SQLiteDatabase db = getReadableDatabase();
            int categoryID = getCategoryID(db, category);
            if (categoryID == -1) {
                Log.d(TAG, "Unable to find categoryID with " + category);
                db.close();
                return;
            }
            Cursor cursor = db.query(TABLE_PAGES, new String[]{COLUMN_PAGE__TITLE, COLUMN_PAGE__URL},
                    COLUMN_PAGE__BELONG + "=?", new String[]{"" + categoryID},
                    null, null,
                    COLUMN_PAGE__CREATE_TIME);

            while (cursor.moveToNext()) {
                try {
                    pairs.add(new Pair<>(cursor.getString(0),
                            new URL(cursor.getString(1))));
                } catch (Exception e) { e.printStackTrace(); }
            }

            Log.d(TAG, "getPages get size: " + pairs.size());

            cursor.close();
            db.close();
        }
    }

    public void updatePageContent(String page, String content) {
        synchronized (this) {
            SQLiteDatabase db = getWritableDatabase();
            int pageID = getPageID(db, page);
            if (pageID == -1) {
                Log.d(TAG, "Unable to find page with " + page);
                db.close();
                return;
            }

            ContentValues values = new ContentValues();
            values.put(COLUMN_PAGE__CONTENT, content);

            try {
                db.update(TABLE_PAGES, values,
                       COLUMN_PAGE__ID + "=?", new String[]{"" + pageID});
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                db.close();
            }
        }
    }

    public String getPageContent(String page) {
        String content = "";
        synchronized (this) {
            SQLiteDatabase db = getReadableDatabase();
            Cursor cursor = db.query(TABLE_PAGES, new String[]{COLUMN_PAGE__CONTENT},
                    COLUMN_PAGE__TITLE + "=?", new String[]{page},
                    null, null, null);

            while (cursor.moveToNext()) {
                content = cursor.getString(0);
                if (content != null)
                    break;
            }

            cursor.close();
            db.close();
        }
        return content;
    }
}
