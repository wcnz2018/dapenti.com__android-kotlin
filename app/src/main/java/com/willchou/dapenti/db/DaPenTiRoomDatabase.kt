package com.willchou.dapenti.db

import android.arch.paging.DataSource
import android.arch.persistence.room.*
import android.arch.persistence.room.Database
import android.content.Context
import com.willchou.dapenti.db.DaPenTiData.Companion.COLUMN_CATEGORY__DISPLAY_ORDER
import com.willchou.dapenti.db.DaPenTiData.Companion.COLUMN_CATEGORY__TITLE
import com.willchou.dapenti.db.DaPenTiData.Companion.COLUMN_CATEGORY__VISIBLE
import com.willchou.dapenti.db.DaPenTiData.Companion.COLUMN_PAGE_INDEX__CATEGORY_ID
import com.willchou.dapenti.db.DaPenTiData.Companion.COLUMN_PAGE_INDEX__CREATE_AT
import com.willchou.dapenti.db.DaPenTiData.Companion.COLUMN_PAGE_INDEX__PAGE_ID
import com.willchou.dapenti.db.DaPenTiData.Companion.COLUMN_PAGE__CONTENT
import com.willchou.dapenti.db.DaPenTiData.Companion.COLUMN_PAGE__FAVORITE
import com.willchou.dapenti.db.DaPenTiData.Companion.COLUMN_PAGE__FAVORITE_AT
import com.willchou.dapenti.db.DaPenTiData.Companion.COLUMN_PAGE__ID
import com.willchou.dapenti.db.DaPenTiData.Companion.COLUMN_PAGE__TITLE
import com.willchou.dapenti.db.DaPenTiData.Companion.TABLE_CATEGORIES
import com.willchou.dapenti.db.DaPenTiData.Companion.TABLE_PAGES
import com.willchou.dapenti.db.DaPenTiData.Companion.TABLE_PAGE_INDEX
import java.util.*

@Database(entities = [DaPenTiData.Category::class, DaPenTiData.Page::class, DaPenTiData.Index::class],
        version = DaPenTiData.DATABASE_VERSION)
@TypeConverters(DaPenTiRoomDatabase.Converters::class)
abstract class DaPenTiRoomDatabase : RoomDatabase() {
    companion object {
        private var INSTANCE: DaPenTiRoomDatabase? = null

        @Synchronized
        fun get(context: Context): DaPenTiRoomDatabase {
            if (INSTANCE == null) {
                INSTANCE = Room.databaseBuilder(context.applicationContext,
                        DaPenTiRoomDatabase::class.java, /*DaPenTiData.DATABASE_NAME*/"DaPenTi2.db")
                        .build()
            }
            return INSTANCE!!
        }
    }

    abstract fun categoryDao() : CategoryDao

    @Dao
    interface CategoryDao {
        @Query("SELECT * FROM $TABLE_CATEGORIES " +
                "ORDER BY $COLUMN_CATEGORY__DISPLAY_ORDER")
        fun allCategories(): List<DaPenTiData.Category>

        @Query("SELECT * FROM $TABLE_CATEGORIES " +
                "WHERE $COLUMN_CATEGORY__VISIBLE='1' " +
                "ORDER BY $COLUMN_CATEGORY__DISPLAY_ORDER")
        fun visibleCategories(): List<DaPenTiData.Category>

        @Query("SELECT * FROM $TABLE_CATEGORIES " +
                "WHERE $COLUMN_CATEGORY__TITLE=:categoryTitle")
        fun getCategory(categoryTitle: String): DaPenTiData.Category

        @Insert(onConflict = OnConflictStrategy.IGNORE)
        fun insert(categories: List<DaPenTiData.Category>): List<Long>
    }

    abstract fun pageDao() : PageDao

    @Dao
    interface PageDao {
        @Query("SELECT * FROM $TABLE_PAGES LEFT JOIN $TABLE_PAGE_INDEX " +
                "ON $TABLE_PAGES.$COLUMN_PAGE__ID = $TABLE_PAGE_INDEX.$COLUMN_PAGE_INDEX__PAGE_ID " +
                "WHERE $TABLE_PAGE_INDEX.$COLUMN_PAGE_INDEX__CATEGORY_ID=:categoryID " +
                "ORDER BY $TABLE_PAGE_INDEX.$COLUMN_PAGE_INDEX__CREATE_AT DESC")
        fun getPages(categoryID: Int): DataSource.Factory<Int, DaPenTiData.Page>

        @Query("SELECT * FROM $TABLE_PAGES " +
                "WHERE $COLUMN_PAGE__TITLE=:pageTitle")
        fun getPage(pageTitle: String): DaPenTiData.Page

        @Query("SELECT $COLUMN_PAGE__FAVORITE FROM $TABLE_PAGES " +
                "WHERE $COLUMN_PAGE__TITLE=:pageTitle")
        fun getFavorite(pageTitle: String): Int

        @Update
        fun update(page: DaPenTiData.Page)

        @Query("UPDATE $TABLE_PAGES SET $COLUMN_PAGE__CONTENT = :content " +
                "WHERE $COLUMN_PAGE__ID = :pageID")
        fun updateContent(pageID: Int, content: String)

        @Query("UPDATE $TABLE_PAGES SET $COLUMN_PAGE__FAVORITE = :favorite," +
                "$COLUMN_PAGE__FAVORITE_AT = :at WHERE $COLUMN_PAGE__ID = :pageID")
        fun updateFavorite(pageID: Int, favorite: Int, at: Date = Calendar.getInstance().time)

        @Insert(onConflict = OnConflictStrategy.IGNORE)
        fun insert(pages: List<DaPenTiData.Page>)

        @Insert(onConflict = OnConflictStrategy.IGNORE)
        fun insert(page: DaPenTiData.Page)
    }

    abstract fun indexDao() : IndexDao

    @Dao
    interface IndexDao {
        @Query("SELECT * FROM $TABLE_PAGE_INDEX" +
                " WHERE $COLUMN_PAGE_INDEX__CATEGORY_ID=:categoryID")
        fun getIndices(categoryID: Int): List<DaPenTiData.Index>

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        fun insert(index: DaPenTiData.Index): Long
    }

    class Converters {
        @TypeConverter
        fun fromTimestamp(value: Long?): Date? {
            return if (value == null) null else Date(value)
        }

        @TypeConverter
        fun dateToTimestamp(date: Date?): Long? {
            return date?.time
        }
    }
}