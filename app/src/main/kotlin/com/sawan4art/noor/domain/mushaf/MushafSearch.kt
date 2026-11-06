package com.sawan4art.noor.domain.mushaf

/**
 * البحث بكلمة واحدة في آيات المصحف — توجيه المالك ٢٠٢٦-١١-٠٦:
 * «نظام بحث بالكلمه في ايات المصحف و يكون نظام بحث بدون تشكيل يكفي فقط
 * كتابه كلمه واحده من قبل المستخدم ليتم عرض كل الايات التي تحمل نفس
 * الكلمه له مع طبعا توضيح لكل ايه هي من سوره اي و ايه رقم اي».
 *
 * التطبيع يحذف التشكيل كله ويوحّد صور الحروف المتشابهة في الكتابة
 * (الألف بأشكالها الأربعة، والتاء المربوطة هاءً، والألف المقصورة ياءً)
 * على طرفَي البحث والنص كليهما — فكلمة يكتبها المستخدم بلا تشكيل
 * تلحق نصَّ المصحف المشكول أينما وردت.
 */
object MushafSearch {

    /**
     * تطبيع نص قرآني للمقارنة — حذف الحركات وعلامات التلاوة والوقف
     * وتوحيد الألف والتاء المربوطة والألف المقصورة. يُطبَّق على النص
     * والكلمة المبحوثة معاً فالمساواة مضمونة من الجهتين.
     */
    fun normalize(text: String): String {
        val sb = StringBuilder(text.length)
        for (c in text) {
            when (c) {
                // التشكيل وعلامات الوقف والتلاوة والمدّ الصغير — تحذف كلها
                in '\u064B'..'\u065F', '\u0670', in '\u06D6'..'\u06ED', '\u0640' -> Unit
                // الألف بأشكالها — ألفاً واحدة ساكنة
                'أ', 'إ', 'آ', 'ٱ', 'ٳ', 'ٵ' -> sb.append('ا')
                // التاء المربوطة هاءً والألف المقصورة ياءً
                'ة' -> sb.append('ه')
                'ى' -> sb.append('ي')
                else -> if (!c.isWhitespace() || c == ' ') sb.append(c)
            }
        }
        return sb.toString()
    }

    /** نتيجة بحث واحدة — سورة الآية ورقمها ونصها كما في المصحف */
    data class Hit(
        val sura: Int,
        val ayah: Int,
        val text: String,
    )

    /**
     * البحث بكلمة واحدة — أية آية تحمل الكلمة بعد التطبيع تُرجَع بالترتيب
     * المصحفي (سورةً سورة وآيةً آية)، وحذف الكلمة الفارغة يعيد لا شيء.
     */
    fun search(query: String, allAyahs: List<Hit>, limit: Int = 200): List<Hit> {
        val needle = normalize(query).trim()
        if (needle.isEmpty()) return emptyList()
        val hits = ArrayList<Hit>()
        for (ayah in allAyahs) {
            if (normalize(ayah.text).contains(needle)) {
                hits.add(ayah)
                if (hits.size >= limit) break
            }
        }
        return hits
    }
}
