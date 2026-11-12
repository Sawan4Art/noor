package com.sawan4art.noor.data.mushaf

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * قارئ قاعدة توقيتات الآيات — جدول `timings(sura, ayah, time)` في ملف
 * SQLite يُحدد لحظة بداية كل آية في التلاوة، فتُزامَن الإبرازات مع
 * الصوت بمقارنة موضع المشغّل الحالي مع هذه التوقيتات لحظةً بلحظة.
 *
 * **من أين يأتي الملف؟** بثلاثة أبواب بالترتيب — أولها الحاضر:
 * ١. `filesDir/timings/timings.db` — ملفٌ وضعه المالك بيده (أو نُسخ
 *    من المرفقات قبله) فيكون هو المصدر دائماً.
 * ٢. `getExternalFilesDir(null)/timings.db` — إسقاطٌ خارجيٌّ خاصٌّ
 *    بالتطبيق للتجارب السريعة بلا وصلةً بالجهاز.
 * ٣. مرفقات التطبيق `assets/databases/timings.db` — إن أُرفق مع
 *    إصدارٍ قادم يُنسخ إلى الملفات مرةً واحدةً عند أول تشغيل.
 * وغياب الأبواب الثلاثة كلها يجعل الوحدة خاملةً بأمانٍ تام: كل
 * استعلامٍ يعود بلا شيء والتلاوة تسير بالمزامنة الافتراضية (انتقال
 * المسارات) بلا قاعدةٍ أصلاً.
 *
 * **الكفاءة** — عقد الأداء الصارم:
 * • استعلامٌ واحدٌ لكل سورةٍ يُحمَّل مرةً واحدةً (`WHERE sura=? ORDER
 *   BY time`) ويُحفظ في ذاكرة [LruCache] — لا لمسةً للقاعدة أثناء
 *   التشغيل مهما طالت السورة.
 * • «آية هذا الموضع» بحثٌ ثنائيٌّ O(log n) على مصفوفاتٍ صرفةٍ في
 *   الذاكرة بلا أي تخصيصٍ للكائنات في مسار التحديث الحي (كل ١٠٠ مللي).
 * • وحدات الزمن تُطبَّع آلياً: عمودٌ بالثواني يُضرب في ألفٍ فور
 *   تحميله فكل المقارنات بالمللي ثانية دائماً (الاكتشاف بالعقد:
 *   أقصى قيمةٍ أقل من ١٠ آلاف لا تكون مللي ثانية لسورةٍ قط).
 * • وكل قراءةٍ قاعدةٍ أو نسخٍ ملفٍ على Dispatchers.IO — الواجهة لا
 *   تلتقط أنفاساً.
 */
object AyahTimings {

    /**
     * توقيتات سورةٍ واحدة في الذاكرة — مصفوفاتٌ متوازية بترتيب الزمن:
     * `times[i]` لحظة بداية الآية `ayahs[i]` بالمللي ثانية.
     */
    class SuraTimings internal constructor(
        val sura: Int,
        /** لحظات بدايات الآيات بالمللي ثانية — تصاعديةٌ صارمة */
        val times: LongArray,
        /** أرقام الآيات — نظير زمنها في [times] */
        val ayahs: IntArray,
    ) {
        /** خريطة الآية ← لحظة بدايتها — لمرساة الموضع الحي في المشغّل */
        private val startByAyah: HashMap<Int, Long> = HashMap(ayahs.size * 2)

        init {
            for (i in ayahs.indices) startByAyah[ayahs[i]] = times[i]
        }

        /** عدد نقاط التوقيت */
        val size: Int get() = ayahs.size

        /** لحظة بداية الآية بالمللي ثانية — غير الموجودة تعود -١ */
        fun startTimeOf(ayah: Int): Long = startByAyah[ayah] ?: -1L

        /** لحظة نهاية الآية (بداية تاليتها) — الأخيرة لا نهاية لها فتعود -١ */
        fun endTimeOf(ayah: Int): Long {
            for (i in ayahs.indices) {
                if (ayahs[i] == ayah) return if (i + 1 < times.size) times[i + 1] else -1L
            }
            return -1L
        }
    }

    /** ذاكرة السور المحمَّلة — آمنة الخيوط وتُسيح إعادة الاستعلام بلا قاعدة */
    private val cache = LruCache<Int, SuraTimings>(30)

    /** قاعدة مفتوحة قراءةً فقط — تُفتح مرةً واحدةً لكل عملية */
    @Volatile
    private var db: SQLiteDatabase? = null

    @Volatile
    private var dbFailed = false

    private val dbLock = Any()

    /** هل توجد قاعدة توقيتاتٍ صالحة؟ — يُعرف من أول محاولةٍ فحسب */
    val isAvailable: Boolean get() = db != null || !dbFailed

    /**
     * توقيتات السورة — تُحمَّل من القاعدة مرةً واحدةً وتُحفظ في الذاكرة.
     * الاستدعاءات التالية رخيصةٌ بلا استثناء. غياب القاعدة أو السورة
     * يعود بلا شيء فتسير المزامنة على العقد الافتراضي.
     */
    suspend fun suraTimings(context: Context, sura: Int): SuraTimings? {
        cache.get(sura)?.let { return it }
        if (dbFailed) return null
        return withContext(Dispatchers.IO) {
            val cached = cache.get(sura)
            if (cached != null) return@withContext cached
            val database = openDb(context) ?: return@withContext null
            val rows = runCatching {
                val rows = mutableListOf<Pair<Int, Long>>()
                database.rawQuery(
                    "SELECT ayah, time FROM timings WHERE sura = ? ORDER BY time ASC",
                    arrayOf(sura.toString()),
                ).use { cursor ->
                    while (cursor.moveToNext()) {
                        val ayah = cursor.getInt(0)
                        val time = cursor.getLong(1)
                        rows += ayah to time
                    }
                }
                rows
            }.getOrDefault(emptyList())
            val built = buildSuraTimings(sura, rows)
            if (built != null) cache.put(sura, built)
            built
        }
    }

    /**
     * توقيتات السورة **من الذاكرة وحدها** — لمسار التحديث الحي في
     * المشغّل (كل ١٠٠ مللي) حيث لا يجوز أي انتظارٍ ولا أي قراءةٍ
     * قاعدةٍ. غير المحمَّلة تعود بلا شيء وتُدفَّأ من [play] مبكراً.
     */
    fun cachedSuraTimings(sura: Int): SuraTimings? = cache.get(sura)

    /**
     * «الآية النشطة عند موضعٍ معطى» — بحثٌ ثنائيٌّ عن آخر بدايةٍ
     * لا تتجاوز الموضع: الموضع بين بدايتي آيتين يعود بالأسبق منهما،
     * وقبل أول بدايةٍ يعود بالأولى (الآية sounding من صفر)، والقاعدة
     * الفارغة تعود صفراً. دالةٌ نقيةٌ تُختبر آلياً.
     */
    fun ayahAt(timings: SuraTimings, positionMs: Long): Int {
        if (timings.size == 0) return 0
        var lo = 0
        var hi = timings.size - 1
        var result = 0
        while (lo <= hi) {
            val mid = (lo + hi) ushr 1
            if (timings.times[mid] <= positionMs) {
                result = mid
                lo = mid + 1
            } else {
                hi = mid - 1
            }
        }
        return timings.ayahs[result]
    }

    /**
     * بناء توقيتات سورةٍ من صفوفها الخام — دالةٌ نقيةٌ تُختبر آلياً:
     * • الصفوف الفاسدة (آيةٌ بلا رقمٍ أو زمنٌ سالب) تُرمى.
     * • الآية المكررة تنال أول لحظةٍ لها.
     * • الترتيب زمنياً صارماً تصاعدياً أياً كان ترتيب الملف.
     * • **تطبيع الوحدات**: أقصى قيمةٍ دون ١٠ آلاف تكون ثوانٍ فتُضرب
     *   في ألفٍ — فالمللي ثانية هي عملةُ المقارنة الوحيدة.
     * • لا صفوفٍ سليمة → لا توقيتات (null).
     */
    internal fun buildSuraTimings(sura: Int, rows: List<Pair<Int, Long>>): SuraTimings? {
        if (rows.isEmpty()) return null
        val earliest = HashMap<Int, Long>()
        for ((ayah, time) in rows) {
            if (ayah < 1 || ayah > 286 || time < 0L) continue
            val prev = earliest[ayah]
            if (prev == null || time < prev) earliest[ayah] = time
        }
        if (earliest.isEmpty()) return null
        val sorted = earliest.entries.sortedBy { it.value }
        // تطبيع الوحدات — ثوانٍ يُعرف بأقصى قيمةٍ دون ١٠ آلاف
        val multiplier = if (sorted.last().value in 1 until 10_000L) 1_000L else 1L
        val times = LongArray(sorted.size)
        val ayahs = IntArray(sorted.size)
        for ((i, entry) in sorted.withIndex()) {
            ayahs[i] = entry.key
            times[i] = entry.value * multiplier
        }
        return SuraTimings(sura, times, ayahs)
    }

    /** فتح القاعدة مرةً واحدةً — وفشلُها يُختَم فلا تُعاد المحاولة كل مرة */
    private fun openDb(context: Context): SQLiteDatabase? {
        db?.let { return it }
        synchronized(dbLock) {
            db?.let { return it }
            if (dbFailed) return null
            val file = resolveDbFile(context)
            if (file == null) {
                dbFailed = true
                return null
            }
            val opened = runCatching {
                val flags = SQLiteDatabase.OPEN_READONLY or
                    SQLiteDatabase.NO_LOCALIZED_COLLATORS
                SQLiteDatabase.openDatabase(file.absolutePath, null, flags)
            }.getOrNull()
            if (opened == null) {
                dbFailed = true
                return null
            }
            // عقد المخطط — جدول timings بأعمدته الثلاثة أو لا شيء
            val schemaOk = runCatching {
                opened.rawQuery(
                    "SELECT 1 FROM sqlite_master WHERE type='table' AND name='timings'",
                    null,
                ).use { it.moveToFirst() }
            }.getOrDefault(false)
            if (!schemaOk) {
                runCatching { opened.close() }
                dbFailed = true
                return null
            }
            db = opened
            return opened
        }
    }

    /** أبواب الملف الثلاثة بالترتيب — انظر توثيق الكائن */
    internal fun resolveDbFile(context: Context): java.io.File? {
        // ١) الملف الموضوع بيد المالك في مجلد الملفات الداخلي
        val internal = java.io.File(context.filesDir, "timings/timings.db")
        if (internal.isFile && internal.length() > 0L) return internal
        // ٢) الإسقاط الخارجي الخاص بالتطبيق
        val external = context.getExternalFilesDir(null)?.let { java.io.File(it, "timings.db") }
        if (external?.isFile == true && external.length() > 0L) return external
        // ٣) مرفقات التطبيق — تُنسخ إلى الملفات مرةً واحدةً ثم تصير الباب الأول
        if (!internal.isFile) {
            runCatching {
                context.assets.open("databases/timings.db").use { input ->
                    internal.parentFile?.mkdirs()
                    val tmp = java.io.File(internal.parentFile, "timings.db.tmp")
                    tmp.outputStream().use { output -> input.copyTo(output) }
                    if (!tmp.renameTo(internal)) tmp.delete()
                }
            }
        }
        if (internal.isFile && internal.length() > 0L) return internal
        return null
    }

    /** تفريغ الذاكرة وغلق القاعدة — للاختبارات وتغيير الملف الحي */
    fun reset() {
        synchronized(dbLock) {
            runCatching { db?.close() }
            db = null
            dbFailed = false
        }
        cache.evictAll()
    }
}
