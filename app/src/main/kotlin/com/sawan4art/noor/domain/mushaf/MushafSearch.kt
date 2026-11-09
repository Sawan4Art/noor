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

    // ===== بحث أسماء السور وقفزة الآية (alpha63) =====
    // بلاغ المالك: «اجعل البحث الموجود داخل صفحه اختيار السوره يدعم البحث
    // باسم السوره و عده كلمات او كلمه واحده و يدعم البحث برقم الايه
    // و لتفعيل البحث بالايه يجب علي المستخدم كتابه اسم السوره اولا ثم
    // يتبعها رقم الايه المراد البحث عنها مثلا مريم ٥٠ فتأتي له الايه
    // رقم ٥٠ من سوره مريم و هكذا».

    /** نتيجة قفزة آية — سورةً ورقماً بعد استخراج «الاسم + الرقم» */
    data class SurahJump(val suraNameQuery: String, val ayah: Int)

    /** رقم عربي هندية أو لاتينية — للقراءة بعد التطبيع */
    private val TRAILING_NUMBER = Regex("([٠-٩0-9]+)\\s*$")

    /**
     * استخراج «اسم السورة + رقم الآية» من نصّ البحث — «مريم ٥٠» و«مريم
     * 50» و«آل عمران ٢٠٠» كلها تعود باسم السورة ورقمها، وبدون رقمٍ في
     * الذيل يعود null فالبحث باسمٍ أو كلمةٍ آيةٍ كما هو.
     */
    fun extractSurahJump(query: String): SurahJump? {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return null
        val m = TRAILING_NUMBER.find(trimmed) ?: return null
        val digits = m.groupValues[1]
        val ayah = digits.map { c ->
            if (c in '٠'..'٩') c - '٠' else c - '0'
        }.reduceOrNull { acc, d -> acc * 10 + d } ?: return null
        if (ayah !in 1..286) return null
        val namePart = trimmed.substring(0, m.range.first).trim()
        if (namePart.isEmpty()) return null
        return SurahJump(namePart, ayah)
    }

    /**
     * مطابقة أسماء السور بنصّ البحث — كلمةً واحدةٍ أو عدة كلماتٍ (آل
     * عمران / ال عمران / عمران)، والسابقة «سورة» تُسقط إن حضرت. النتيجة
     * معرّفات السور بترتيبها المعتمد.
     */
    fun matchingSurahs(
        query: String,
        names: List<Pair<Int, String>>,
    ): List<Int> {
        val stripped = query.trim()
            .replace(Regex("^(سورة|سوره|سور)\\s+"), "")
        val needle = normalize(stripped).trim()
        if (needle.isEmpty()) return emptyList()
        val needleTight = needle.replace(" ", "")
        val out = ArrayList<Int>()
        for ((sura, name) in names) {
            val hay = normalize(name)
            if (hay.contains(needle) || hay.replace(" ", "").contains(needleTight)) {
                out.add(sura)
            }
        }
        return out
    }
}
