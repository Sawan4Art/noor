package com.sawan4art.noor.data.mushaf

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.sawan4art.noor.domain.mushaf.MushafSegment
import java.io.File

/**
 * قاعدة المصحف المصاحبة للطبعة — مرفق واحد (databases/mushaf_ayahinfo.db)
 * يضم جدولين:
 *
 * 1) glyphs: شريحة سطر لكل آية على كل صفحة (١٣٬٧٦٦ صف تغطي ٦٬٢٣٦ آية في
 *    ٦٠٤ صفحة بإحداثيات بكسل فراغ مرجعي ٧٧٦×١٠٥٣) — مصدره قاعدة
 *    ayahinfo_1053 المرافقة لصور «مصحف التجويد الملون» نفسها في
 *    murtraja/quran-android-images-helper، فالمحاذاة للطبعة مطابقة تحقّقها
 *    تراكب بكسلي على صفحات مفصلية (٣، ٥٠، ١، ٢، ٦٠٤).
 * 2) ayah_text: نص الآيات العثماني الكامل (تانزيل verbatim كما في نسخة
 *    alpha49 — CC BY 3.0) للمشاركة والنسخ وحساب ترتيب آية التلاوة.
 * 3) tafsir_muyassar (الإصدار ٢): التفسير الميسر كاملاً (٦٬٢٣٦ آية — مجمع
 *    الملك فهد، من edition ar.muyassar في api.alquran.cloud) للتفاسير المنبثقة
 *    من زر التفسير — مرفوق يعمل بلا إنترنت بعد أول فتح.
 *
 * القراءة فقط: لا إدراج ولا تعديل إطلاقاً — ينسخ المرفق إلى ملفات التطبيق
 * عند أول فتح ويستبدل النسخة الأقدم إن تغيّر الإصدار (نمط HadithDatabase).
 */
object MushafDb {

    const val NAME = "mushaf_ayahinfo.db"
    const val VERSION = 2

    @Volatile
    private var db: SQLiteDatabase? = null

    /** كاش شرائح الصفحات — ٦٠٤ صفحة × نحو ٢٣ شريحة صفّيّة لا تذكر بالذاكرة */
    private val pageCache = HashMap<Int, List<MushafSegment>>()

    /** عدّ آيات السور — قراءة واحدة عند أول تلاوة أو مشاركة */
    @Volatile
    private var surahCounts: Map<Int, Int>? = null

    /** فتح القاعدة للقراءة فقط — نسخ المرفق عند الحاجة ثم إعادة الاتصال الواحد */
    fun open(context: Context): SQLiteDatabase =
        db ?: synchronized(this) {
            db ?: run {
                val ctx = context.applicationContext
                val file = ctx.getDatabasePath(NAME)
                replaceStaleCopy(ctx, file)
                SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY).also { db = it }
            }
        }

    /** نسخة المستخدم غائبة أو أقدم من المرفق؟ احذفها ليُعاد نسخ الأصل */
    private fun replaceStaleCopy(context: Context, file: File) {
        val fresh = if (file.isFile) {
            runCatching {
                SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY)
                    .use { it.version >= VERSION }
            }.getOrDefault(false)
        } else {
            false
        }
        if (fresh) return
        file.parentFile?.mkdirs()
        context.assets.open("databases/$NAME").use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        }
        // user_version في المرفق نفسه يحمل الإصدار — صحيح بعد النسخ مباشرة
    }

    /** شرائح صفحة واحدة — مرتَّبة من أعلى الصفحة إلى أسفلها */
    fun segments(context: Context, page: Int): List<MushafSegment> =
        synchronized(pageCache) {
            pageCache[page]
        } ?: run {
            val rows = open(context).query(
                "glyphs",
                arrayOf("sura_number", "ayah_number", "min_x", "max_x", "min_y", "max_y"),
                "page_number = ?",
                arrayOf(page.toString()),
                null,
                null,
                "line_number, position",
            ).use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(
                            MushafSegment(
                                page = page,
                                sura = cursor.getInt(0),
                                ayah = cursor.getInt(1),
                                minX = cursor.getFloat(2),
                                maxX = cursor.getFloat(3),
                                minY = cursor.getFloat(4),
                                maxY = cursor.getFloat(5),
                            ),
                        )
                    }
                }
            }
            synchronized(pageCache) { pageCache[page] = rows }
            rows
        }

    /** نص آية واحدة — null إن غابت */
    fun ayahText(context: Context, sura: Int, ayah: Int): String? =
        open(context).query(
            "ayah_text",
            arrayOf("text"),
            "sura = ? AND ayah = ?",
            arrayOf(sura.toString(), ayah.toString()),
            null,
            null,
            null,
        ).use { if (it.moveToFirst()) it.getString(0) else null }

    /** نص مُدى آيات متصل بسورة واحدة — لصنيع المشاركة والنسخ */
    fun ayahRangeText(context: Context, sura: Int, from: Int, to: Int): String {
        val lo = minOf(from, to)
        val hi = maxOf(from, to)
        return open(context).query(
            "ayah_text",
            arrayOf("text"),
            "sura = ? AND ayah BETWEEN ? AND ?",
            arrayOf(sura.toString(), lo.toString(), hi.toString()),
            null,
            null,
            "ayah",
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(cursor.getString(0))
            }
        }.joinToString(" ")
    }

    /** عدّ آيات سورة — من النص نفسه (أعلى ترقيم) فمصدر العدّ واحد */
    fun surahAyahCount(context: Context, sura: Int): Int {
        if (surahCounts == null) {
            synchronized(this) {
                if (surahCounts == null) {
                    surahCounts = open(context).query(
                        "ayah_text",
                        arrayOf("sura", "MAX(ayah)"),
                        null,
                        null,
                        "sura",
                        null,
                        null,
                    ).use { cursor ->
                        buildMap {
                            while (cursor.moveToNext()) put(cursor.getInt(0), cursor.getInt(1))
                        }
                    }
                }
            }
        }
        return surahCounts?.get(sura) ?: 0
    }

    /** ترتيب الآية العالمي (١..٦٢٣٦) — رابط تلاوة الآية في شبكة القرآن */
    fun globalAyah(context: Context, sura: Int, ayah: Int): Int {
        var before = 0
        for (s in 1 until sura) before += surahAyahCount(context, s)
        return before + ayah
    }

    /**
     * تفسير المُدى المحدد — التفسير الميسر لكل آية بالترتيب، وnull للآية
     * الغائبة تُسقط بصمت فيبقى الباقي.
     */
    fun tafsirRange(context: Context, sura: Int, from: Int, to: Int): List<Pair<Int, String>> {
        val lo = minOf(from, to)
        val hi = maxOf(from, to)
        return open(context).query(
            "tafsir_muyassar",
            arrayOf("ayah", "text"),
            "sura = ? AND ayah BETWEEN ? AND ?",
            arrayOf(sura.toString(), lo.toString(), hi.toString()),
            null,
            null,
            "ayah",
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(cursor.getInt(0) to cursor.getString(1))
            }
        }
    }
}
