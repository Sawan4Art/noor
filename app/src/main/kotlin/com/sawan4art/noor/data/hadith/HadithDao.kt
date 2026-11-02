package com.sawan4art.noor.data.hadith

import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery

/** صف عددٍ واحد من استعلام COUNT — Room لا يعرف جدول FTS5 فرجع POJO */
data class CountRow(val n: Int)

@Dao
interface HadithDao {

    @Query("SELECT * FROM books ORDER BY sortOrder")
    suspend fun books(): List<BookEntity>

    @Query("SELECT * FROM hadiths WHERE id = :id")
    suspend fun byId(id: Long): HadithEntity?

    /**
     * البحث والتصفح عبر SQL يُبنى في [HadithRepository] — استعلامات
     * hadiths_fts (فهرس FTS5 مُعبَّأ مسبقاً داخل ملف الـ assets) لا يعرفها
     * Room لأنه لا يملك كياناً لجدول افتراضي FTS5، لذا RawQuery هو الباب
     * الصحيح: بلا تحقق زمن الترجمة، ومع تتبع تعديل جدول hadiths.
     */
    @RawQuery(observedEntities = [HadithEntity::class])
    suspend fun searchRaw(query: SupportSQLiteQuery): List<HadithEntity>

    @RawQuery(observedEntities = [HadithEntity::class])
    suspend fun countRaw(query: SupportSQLiteQuery): CountRow?
}
