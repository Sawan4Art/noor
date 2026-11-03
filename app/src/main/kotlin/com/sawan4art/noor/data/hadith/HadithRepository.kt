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
 * - **البحث**: MATCH على فهرس FTS4 بتعبير [HadithSearch.matchExpression]
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
     * صفحة نتائج بحث مرتبة بترتيب الكتاب — [expr] تعبير MATCH جاهز من
     * HadithSearch.matchExpression، والمستدعي يتأكد أنه غير فارغ أولاً.
     *
     * الفهرس **FTS4** (لا FTS5) — أندرويد لا يبني SQLite بنظام FTS5 نهائياً
     * («no such module: fts5» على أجهزة حقيقية منذ الأقدم وحتى الأحدث)،
     * فكان البحث بالكلمة يعمل في المحاكي ويسقط في الميدان. FTS4 مضمون
     * من API 21، ولا يملك عمود rank فالترتيب بترتيب المعرف (ترتيب
     * الكتاب ثم الأصل في الكتاب).
     */
    suspend fun search(expr: String, bookId: Long?, limit: Int, offset: Int): List<HadithEntity> =
        dao.searchRaw(
            SimpleSQLiteQuery(
                "SELECT h.* FROM hadiths h " +
                    "JOIN hadiths_fts f ON f.docid = h.id " +
                    "WHERE hadiths_fts MATCH ? AND (? IS NULL OR h.bookId = ?) " +
                    "ORDER BY h.id LIMIT ? OFFSET ?",
                arrayOf(expr, bookId, bookId, limit, offset),
            ),
        )

    /**
     * البحث برقم الحديث (alpha38 توجيه المالك: «ليشمل البحث عن رقم
     * الحديث») — مطابقة تامة في numberInBook مرتبة بترتيب الكتب كما على
     * رفّ المكتبة، فـ«حديث ٥» بلا كتاب محدد يجيء من كل الكتب بالتتابع.
     */
    suspend fun byNumber(number: Long, bookId: Long?, limit: Int, offset: Int): List<HadithEntity> =
        dao.searchRaw(
            SimpleSQLiteQuery(
                "SELECT h.* FROM hadiths h JOIN books b ON b.id = h.bookId " +
                    "WHERE h.numberInBook = ? AND (? IS NULL OR h.bookId = ?) " +
                    "ORDER BY b.sortOrder LIMIT ? OFFSET ?",
                arrayOf(number, bookId, bookId, limit, offset),
            ),
        )

    /** عدد أحاديث الرقم الواحد — لإظهار «النتائج» وضبط نهاية الترقيم */
    suspend fun countByNumber(number: Long, bookId: Long?): Int =
        dao.countRaw(
            SimpleSQLiteQuery(
                "SELECT COUNT(*) AS n FROM hadiths " +
                    "WHERE numberInBook = ? AND (? IS NULL OR bookId = ?)",
                arrayOf(number, bookId, bookId),
            ),
        )?.n ?: 0

    /**
     * الأحاديث المحفوظة — [ids] معرفات بترتيب الحفظ (الأحدث أولاً) فتُعاد
     * بترتيبها نفسه لا بترتيب المعرف. الدفعات تُقطَّع دون حدّ متغيرات
     * SQLite، وصفحات الترقيم تُمرَّر شريحةً جاهزة فلا تحميل كله دفعة.
     */
    suspend fun byIds(ids: List<Long>): List<HadithEntity> {
        if (ids.isEmpty()) return emptyList()
        val rows = ids.chunked(500).flatMap { chunk ->
            val placeholders = chunk.joinToString(",") { "?" }
            dao.searchRaw(
                SimpleSQLiteQuery(
                    "SELECT * FROM hadiths WHERE id IN ($placeholders)",
                    chunk.toTypedArray(),
                ),
            )
        }
        val byId = rows.associateBy { it.id }
        return ids.mapNotNull { byId[it] }
    }
}
