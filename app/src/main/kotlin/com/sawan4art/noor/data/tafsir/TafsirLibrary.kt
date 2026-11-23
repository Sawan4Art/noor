package com.sawan4art.noor.data.tafsir

import android.content.Context
import com.sawan4art.noor.domain.tafsir.TafsirEditions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * مكتبة التفاسير — v1.82 (بلاغ المالك: «انا عايز التفاسير تكون مدمجه
 * داخل التطبيق نفسه مش قابله للتنزيل لا تكون موجوده بشكل افتراضي في
 * التطبيق و المستخدم يختار بينهم»):
 *
 * الطبعات الست كلها **داخل حزمة التطبيق نفسها** كأصولٍ نصيةٍ —
 * ملفٌ لكل سورةٍ في كل طبعة (assets/tafsir/<المعرّف>/NNN.txt، الآيات
 * يفصلها محرف فاصل السجلات U+001E) — فالقراءة Inflateٌ مباشرٌ من
 * الأصول لحظة فتح اللوحة: بلا إنترنت، بلا تنزيل، بلا استخراجٍ إلى
 * ذاكرة التخزين، وبلا قواعد بيانات منفصلة — والذاكرة لا تحمل إلا
 * سورةً واحدةً (٢٠٠–٤٠٠ ك.ب) لطبعةٍ واحدة.
 *
 * وقد أُسقطت كل بنية v1.81 (التنزيل بخصائص الاستئناف والبصمات
 * وحالة التقدم) — ودرس انهيار زر التفاسير محفوظٌ هنا: لا تنسيق
 * أرقامٍ ولا اعتمادٍ على لغة الجهاز في التحليل إطلاقاً؛ المسارات
 * لاتينيةٌ ثابتةٌ والفواصل محارف تحكّمٍ لا نصُّ عرض.
 *
 * الميسر أيضاً من الأصول نفسها (وُلِّد من قاعدة المصحف المضمّنة) —
 * مسارُ قراءةٍ واحدٌ للست طبقاتٍ بلا تفريقٍ بين مضمّنٍ وغيره.
 */
object TafsirLibrary {

    /** فاصل الآيات — محرف فاصل السجلات U+001E بين كل آيتين (من المولّد) */
    private const val SEP = "\n\u001e\n"

    /** مخزن مؤقت صغيرٌ لآخر السورات المقروءة — تبديل الطبعات سلسٌ وسريع */
    private val cache = object : LinkedHashMap<String, List<Pair<Int, String>>>(
        4, 0.75f, true,
    ) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<Pair<Int, String>>>): Boolean =
            size > 8
    }

    /**
     * نصوص مُدى آياتٍ من طبعةٍ — (الآية، نصّها) بالترتيب، والآيات
     * التي لم يفسّرها المؤلف تعود نصاً فارغاً (كلاسيكياتٌ تدمج تعليق
     * آياتٍ مجاورة) فتعرض اللوحة لها ملاحظةً لطيفةً بدل إخفائها.
     */
    suspend fun rangeFor(
        context: Context,
        editionId: String,
        sura: Int,
        lo: Int,
        hi: Int,
    ): List<Pair<Int, String>> = withContext(Dispatchers.IO) {
        runCatching {
            val key = "$editionId/$sura"
            val ayat = synchronized(cache) { cache[key] } ?: readSurah(context, editionId, sura)
                .also { full -> synchronized(cache) { cache[key] = full } }
            // القائمة محاذيةٌ للآيات (الموضع ٠ = الآية ١) وعنصرُها
            // زوجٌ جاهزٌ (الآية، النص)
            (lo..hi).mapNotNull { a -> ayat.getOrNull(a - 1) }
        }.getOrDefault(emptyList())
    }

    /** نص تفسير آيةٍ واحدة — فارغٌ لو لم يفسّرها المؤلف في هذه الطبعة */
    suspend fun textFor(
        context: Context,
        editionId: String,
        sura: Int,
        ayah: Int,
    ): String? = rangeFor(context, editionId, sura, ayah, ayah)
        .firstOrNull()?.second?.takeIf { it.isNotBlank() }

    /** قراءة ملف السورة من الأصول وتحليله إلى (الآية، النص) */
    private fun readSurah(context: Context, editionId: String, sura: Int): List<Pair<Int, String>> {
        val path = TafsirEditions.assetPath(editionId, sura)
        val blob = context.assets.open(path).use { input ->
            input.readBytes().toString(Charsets.UTF_8)
        }
        return blob.split(SEP).mapIndexed { i, t -> (i + 1) to t.trim() }
    }
}
