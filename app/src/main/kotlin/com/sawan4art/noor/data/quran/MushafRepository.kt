package com.sawan4art.noor.data.quran

import android.content.Context
import com.sawan4art.noor.domain.quran.MushafDoc
import com.sawan4art.noor.domain.quran.MushafLine
import com.sawan4art.noor.domain.quran.MushafSpan
import com.sawan4art.noor.domain.quran.MushafSurah
import com.sawan4art.noor.domain.quran.MushafWord
import org.json.JSONObject

/**
 * مستودع المصحف — يقرأ الأصول المبنية سلفاً (assets/quran_mushaf.json):
 * صفحات الطبعة وسطورها وكلماتها ومدى تجويدها، ونصوص الآيات verbatim
 * (تانزيل CC BY 3.0) للنسخ والمشاركة.
 *
 * القراءة تتم مرة واحدة عند أول فتح على مجرى الإدخال/الإخراج، ثم تبقى
 * المستند كله في الذاكرة (٩٠ ألف كلمة تقريباً — حجمها لا يذكر).
 */
object MushafRepository {

    @Volatile
    private var cached: MushafDoc? = null

    /** تحميل المستند — مخزَّن بعد أول مرة؛ التكرار رخيص */
    fun load(context: Context): MushafDoc {
        cached?.let { return it }
        synchronized(this) {
            cached?.let { return it }
            val json = context.assets.open("quran_mushaf.json")
                .bufferedReader().use { it.readText() }
            val doc = parse(json)
            cached = doc
            return doc
        }
    }

    /** فكّ بنية الأصول — نفس ترتيب سكربت البناء حرفياً */
    internal fun parse(json: String): MushafDoc {
        val root = JSONObject(json)
        val surahs = mutableListOf<MushafSurah>()
        root.getJSONArray("surahs").let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONArray(i)
                surahs += MushafSurah(o.getInt(0), o.getString(1), o.getInt(2))
            }
        }

        fun parseWord(a: org.json.JSONArray): MushafWord {
            val spans = mutableListOf<MushafSpan>()
            a.getJSONArray(4).let { sp ->
                for (k in 0 until sp.length()) {
                    val s = sp.getJSONArray(k)
                    spans += MushafSpan(s.getInt(0), s.getInt(1), s.getString(2))
                }
            }
            return MushafWord(a.getInt(0), a.getInt(1), a.getInt(2), a.getString(3), spans)
        }

        val pages = mutableListOf<List<MushafLine>>()
        root.getJSONArray("pages").let { parr ->
            for (p in 0 until parr.length()) {
                val lines = mutableListOf<MushafLine>()
                val larr = parr.getJSONArray(p)
                for (l in 0 until larr.length()) {
                    val line = larr.getJSONObject(l)
                    val words = mutableListOf<MushafWord>()
                    line.optJSONArray("w")?.let { warr ->
                        for (w in 0 until warr.length()) {
                            words += parseWord(warr.getJSONArray(w))
                        }
                    }
                    when (line.getString("t")) {
                        "h" -> lines += MushafLine.Header(line.getInt("n"), line.getString("x"))
                        "b" -> lines += MushafLine.Basmala(words)
                        else -> lines += MushafLine.Text(words)
                    }
                }
                pages += lines
            }
        }

        val texts = mutableMapOf<String, String>()
        root.getJSONObject("texts").let { tarr ->
            val keys = tarr.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                texts[k] = tarr.getString(k)
            }
        }
        return MushafDoc(surahs, pages, texts)
    }

    /* ==================== المفضلة (العلامات المرجعية) ==================== */

    private const val PREFS = "noor_quran"
    private const val KEY_BOOKMARKS = "bookmarks"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** الآيات المعلَّمة — «سورة:آية» بأحدث علامة أولاً */
    fun bookmarks(context: Context): List<String> =
        prefs(context).getString(KEY_BOOKMARKS, "")?.split(',')?.filter { it.isNotEmpty() }
            ?: emptyList()

    fun isBookmarked(context: Context, surah: Int, ayah: Int): Boolean =
        "$surah:$ayah" in bookmarks(context)

    /** تعليم/إلغاء تعليم آية — الأحدث يتصدر القائمة */
    fun toggleBookmark(context: Context, surah: Int, ayah: Int) {
        val key = "$surah:$ayah"
        val rest = bookmarks(context) - key
        val next = if (isBookmarked(context, surah, ayah)) rest else listOf(key) + rest
        prefs(context).edit().putString(KEY_BOOKMARKS, next.joinToString(",")).apply()
    }

    /* ==================== آخر صفحة مقروءة ==================== */

    private const val KEY_LAST_PAGE = "last_page"

    /** آخر صفحة كان يقرأها المستخدم — تستأنف القراءة منها */
    fun lastPage(context: Context): Int = prefs(context).getInt(KEY_LAST_PAGE, 1)

    fun saveLastPage(context: Context, page: Int) {
        prefs(context).edit().putInt(KEY_LAST_PAGE, page.coerceIn(1, 604)).apply()
    }
}
