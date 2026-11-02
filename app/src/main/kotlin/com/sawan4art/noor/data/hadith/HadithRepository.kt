package com.sawan4art.noor.data.hadith

import android.content.Context
import androidx.sqlite.db.SimpleSQLiteQuery
import com.sawan4art.noor.domain.hadith.HadithSearch

/**
 * مستودع مكتبة الأحاديث — القراءة فقط فوق قاعدة الـ assets.
 *
 * نمطان للقراءة:
 * - **التصفح**: بلا استعلام — يُقرأ بترتيب المعرف (ترتيب الإدخال في
 *   المولّد هو ترتيب العرض نفسه: الكتاب ثم رقم الحديث) فيمشي على
 *   الصفوف بالتسلسل بلا فرز.
 * - **البحث**: MATCH على فهرس FTS5 بتعبير [HadithSearch.matchExpression]
 *   مرتب بالصلة (rank) — ولأن MATCH يُكتب باسم الجدول الأصلي حتى مع
 *   الاسم المستعار (سلوك SQLite) فالصيغة أدناه مثبتة بفحص المولّد.
 *
 * في الحالين التقييد بكتابٍ معين اختياري عبر (? IS NULL OR bookId = ?)
 * وهو نمط يترك المحدِّد فارغاً = كل الكتب.
 */
class HadithRepository(context: Context) {

    private val dao = HadithDatabase.get(context).hadithDao()

    suspend fun books(): List<BookEntity> = dao.books()

    /** صفحة تصفح — [bookId] فارغ يعني كل الكتب */
    suspend fun browse(bookId: Long?, limit: Int, offset: Int): List<HadithEntity> =
        dao.searchRaw(
            SimpleSQLiteQuery(
                "SELECT h.* FROM hadiths h " +
                    "WHERE (? IS NULL OR h.bookId = ?) " +
                    "ORDER BY h.id LIMIT ? OFFSET ?",
                arrayOf(bookId, bookId, limit, offset),
            ),
        )

    /** عدد أحاديث التصفح — عرض «عدد الأحاديث» فوق القائمة */
    suspend fun browseCount(bookId: Long?): Int =
        dao.countRaw(
            SimpleSQLiteQuery(
                "SELECT COUNT(*) AS n FROM hadiths WHERE ? IS NULL OR bookId = ?",
                arrayOf(bookId, bookId),
            ),
        )?.n ?: 0

    /**
     * صفحة نتائج بحث مرتبة بالصلة — [expr] تعبير MATCH جاهز من
     * HadithSearch.matchExpression، والمستدعي يتأكد أنه غير فارغ أولاً.
     */
    suspend fun search(expr: String, bookId: Long?, limit: Int, offset: Int): List<HadithEntity> =
        dao.searchRaw(
            SimpleSQLiteQuery(
                "SELECT h.* FROM hadiths h " +
                    "JOIN hadiths_fts f ON f.rowid = h.id " +
                    "WHERE hadiths_fts MATCH ? AND (? IS NULL OR h.bookId = ?) " +
                    "ORDER BY rank LIMIT ? OFFSET ?",
                arrayOf(expr, bookId, bookId, limit, offset),
            ),
        )
}
