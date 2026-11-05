package com.sawan4art.noor.domain.quran

/**
 * نماذج المصحف — طبعة المدينة بتلوين التجويد (توجيه المالك 2026-11-06:
 * «ابدا شغل في المصحف و هديك صوره لطبعه المصحف عايز زيها»).
 *
 * البيانات مبنية سلفاً في الأصول: كل صفحة سطورها، وكل سطر كلماته، وكل
 * كلمة مدى قواعد التجويد عليها بالنسبة لبدايتها — فيبقى العمل في الواجهة
 * رسمَ الطبعة لا حسابَ التخطيط.
 */

/** مدى قاعدة تجويد داخل نص كلمة — [start,end) بالنسبة لبداية الكلمة */
data class MushafSpan(val start: Int, val end: Int, val rule: String)

/** كلمة في سطر المصحف — موقعها سورة:آية:ترتيب ونصها ومدى تجويدها */
data class MushafWord(
    val surah: Int,
    val ayah: Int,
    val index: Int,
    val text: String,
    val spans: List<MushafSpan>,
)

/** سطر من سطور الصفحة — عنوان سورة أو بسملة أو سطر نص */
sealed class MushafLine {
    /** عنوان سورة بنصه المطبوع «سورة كذا» */
    data class Header(val surah: Int, val text: String) : MushafLine()

    /** سطر البسملة بكلماتها الصياغية الخاصة بتلك السورة */
    data class Basmala(val words: List<MushafWord>) : MushafLine()

    /** سطر نص من الآيات */
    data class Text(val words: List<MushafWord>) : MushafLine()
}

/** سورة: رقمها واسمها وعدد آياتها */
data class MushafSurah(val number: Int, val name: String, val ayahCount: Int)

/** المستند كاملاً: ٦٠٤ صفحات وسطورها ونصوص الآيات للنسخ والمشاركة */
data class MushafDoc(
    val surahs: List<MushafSurah>,
    /** صفحات المصحف ١..٦٠٤ — كل صفحة قائمة سطورها */
    val pages: List<List<MushafLine>>,
    /** نص الآية الكامل verbatim — «سورة:آية» -> النص */
    val texts: Map<String, String>,
) {
    fun surah(number: Int): MushafSurah = surahs.firstOrNull { it.number == number }
        ?: surahs.first()

    fun ayahText(surah: Int, ayah: Int): String = texts["$surah:$ayah"].orEmpty()
}

/**
 * حسابات رقمية على بنية المصحف — الرقم العالمي للآية (للتلاوة الصوتية
 * والترتيب) وعكسه، وترقيم الأجزاء.
 */
object MushafMath {

    /** الرقم العالمي للآية (١ إلى ٦٢٣٦) — للقراءة الصوتية المتسلسلة */
    fun globalAyah(surah: Int, ayah: Int, surahs: List<MushafSurah>): Int {
        var g = 0
        for (s in surahs) {
            if (s.number >= surah) break
            g += s.ayahCount
        }
        return g + ayah
    }

    /** اسم الجزء والصفحة — بحدود الأجزاء الحقيقية في طبعة المدينة */
    fun juzOf(page: Int): Int {
        val starts = JUZ_START_PAGES
        var juz = 1
        for (i in starts.indices) {
            if (page >= starts[i]) juz = i + 1
        }
        return juz
    }

    /** صفحات بداية الأجزاء الثلاثين في طبعة المدينة (٦٠٤ صفحات) */
    private val JUZ_START_PAGES = listOf(
        1, 21, 41, 62, 82, 102, 122, 142, 162, 182,
        202, 222, 242, 262, 282, 302, 322, 342, 362, 382,
        402, 422, 442, 462, 482, 502, 522, 542, 562, 582,
    )

    /** أول صفحة للسورة — يُستخرج من صفحات المصحف نفسها (أول عنوان لها) */
    fun firstPageOf(surah: Int, pages: List<List<MushafLine>>): Int {
        for (i in pages.indices) {
            for (line in pages[i]) {
                if (line is MushafLine.Header && line.surah == surah) return i + 1
            }
        }
        return 1
    }
}
