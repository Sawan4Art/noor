package com.sawan4art.noor.domain.hadith

import java.text.Normalizer

/**
 * مُطبِّع البحث الأوفلاين في مكتبة الأحاديث — قلب البحث النقي:
 *
 * الفهرس (hadiths_fts — جدول FTS4 داخل databases/hadiths.db — أندرويد لا
 * يبني SQLite بنظام FTS5 نهائياً) مبني مسبقاً على
 * نص مُطبَّع بهذه الخطوات الحرفية، فالاستعلام يُطبَّع بنفس الخطوات فيصل
 * المستخدم للحديث سواء كتب بالتشكيل أو بدونه، وبأي صور همزة أو تاء مربوطة
 * أو ألف مقصورة أو تطويل:
 *
 * 1) NFKC — يفكّك ﷺ و﷽ وأشكال العرض، ويُركّب الهمزات المفككة.
 * 2) حذف التشكيل والحركات القرآنية والتطويل وألف الخنجرية وعلامات الاتجاه.
 * 3) توحيد الحروف: أ إ آ ٱ ← ا ، ة ← ه ، ى ← ي ، ؤ ← و ، ئ ← ي
 *    (والهمزة المفردة ء كما هي).
 * 4) توحيد الأرقام العربية والفارسية إلى 0-9.
 *
 * **عقد التوافق**: هذه الخطوات هي انعكاس حرفي لمنطق
 * scripts/build_hadith_db.py الذي بنى الفهرس، وعقد اختبار
 * HadithSearchTest (عبر HadithNormalizationFixtures المولَّد آلياً
 * من نصوص الكوربس نفسها) يمنع انحراف أحدهما عن الآخر —
 * أي تعديل هنا بلا مقابل في المولّد يكسر البناء فوراً.
 */
object HadithSearch {

    /**
     * نطاق الحذف — مطابق للـ STRIP في مولّد قاعدة البيانات حرفياً:
     * حركات الإعراب والتشكيل U+064B-065F، علامات وقف وتزيين المصحف
     * U+0610-061A/U+06D6-06ED/U+08D3-08FF، التطويل U+0640، ألف خنجرية
     * U+0670، زخارف ﴾﴿، وعلامات الاتجاه والحروف الصفرية العرض.
     */
    private val STRIP = Regex(
        "[\\u0610-\\u061A\\u0640\\u064B-\\u065F\\u0670\\u06D6-\\u06ED" +
            "\\u08D3-\\u08FF\\uFD3E\\uFD3F\\u200B-\\u200F\\u202A-\\u202E" +
            "\\u2066-\\u2069\\uFEFF\\uFFF9-\\uFFFC]",
    )

    /** فاصل الكلمات — كل ما ليس حرفاً أو رقماً يفصل بين كلمات الاستعلام */
    private val TOKEN_SPLIT = Regex("[^\\p{L}\\p{N}]+")

    /**
     * كلمات عطفٍ تُحذف عند فحص «رقم الحديث» — «حديث ٥» و«رقم ١٢»
     * و«الحديث ١٤» تصير كلها رقماً خالصاً يُبحث به في numberInBook.
     */
    private val NUMBER_NOISE = setOf("حديث", "الحديث", "رقم", "الرقم", "أحاديث", "احاديث")

    /**
     * الزخارف تُفكّك صراحةً **قبل** NFKC — إصدارات Unicode تختلف بين
     * أجهزة أندرويد وجلسات الاختبار في فكّ بعضها (﷽ لم تُفكّك في JDK
     * الحديث بينما فكّتها بايثون المولّدة) فالخريطة الصريحة تجعل
     * النتيجة حتمية على كل جهاز وتحفظ عقد التوافق مع المولّد.
     */
    private val LIGATURES = listOf(
        "\uFDFA" to "صلى الله عليه وسلم",
        "\uFDF2" to "الله",
        "\uFDFD" to "بسم الله الرحمن الرحيم",
    )

    fun normalize(raw: String): String {
        var preMapped = raw
        for ((ligature, expansion) in LIGATURES) {
            preMapped = preMapped.replace(ligature, expansion)
        }
        val nfkc = Normalizer.normalize(preMapped, Normalizer.Form.NFKC)
        val stripped = STRIP.replace(nfkc, "")
        val sb = StringBuilder(stripped.length)
        for (ch in stripped) {
            sb.append(
                when (ch) {
                    'أ', 'إ', 'آ', 'ٱ' -> 'ا'
                    'ة' -> 'ه'
                    'ى' -> 'ي'
                    'ؤ' -> 'و'
                    'ئ' -> 'ي'
                    in '٠'..'٩' -> ('0' + (ch - '٠'))
                    in '۰'..'۹' -> ('0' + (ch - '۰'))
                    else -> ch
                },
            )
        }
        return sb.toString()
    }

    /**
     * تعبير MATCH لـ FTS4 من استعلام المستخدم — طبقتان تكمل كل منهما
     * الأخرى (توجيه المالك: «البحث بالكلمة أو بعض الجمل من الحديث»):
     *
     * - **العبارة الحرفية** `"كلمة كلمة أخيرة*"` — آخر كلمة فيها بادئة —
     *   فمن كتب جزء جملة متصلاً جاء الحديث المطابق **أولاً** بالترتيب.
     * - **كلمات متفرقة** `"أ"* "ب"* ...` (AND ضمني ببادئة لكل كلمة) —
     *   فتُصاد الكلمات مبعثرة بأي ترتيب حتى لو فصلها كلام.
     *
     * وكلمة واحدة تبقى بادئة خالصة كما هي. يُرجع null إذا لم يبقَ كلمة
     * تُبحث بها (استعلام فارغ أو علامات فقط) — فيعيد المستدعي التصفح
     * العادي بدل البحث.
     *
     * ملاحظة: الاقتباس "" يمنع تفسير أي رمز كصيغة FTS خاصة، والفاصل
     * أعلاه يضمن ألا تحتوي الكلمة نفسها على علامة اقتباس أو نجمة أصلاً.
     */
    fun matchExpression(rawQuery: String): String? {
        val tokens = TOKEN_SPLIT
            .split(normalize(rawQuery))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        if (tokens.isEmpty()) return null
        if (tokens.size == 1) return "\"${tokens[0]}\"*"
        // العبارة الحرفية: كل الكلمات متتالية وآخرها ببادئة
        val phrase = "\"" + tokens.joinToString(" ") + "\"*"
        // الكلمات المتفرقة: بادئة لكل كلمة تُضمّ بـ AND الضمني
        val loose = tokens.joinToString(" ") { token -> "\"$token\"*" }
        return "($phrase) OR ($loose)"
    }

    /**
     * فحص «رقم الحديث» — إن كان الاستعلام رقماً خالصاً (بعد التطبيع فيوحد
     * ٠١٢ و۱۲۳ إلى 0-9) أو رقماً مسبوقاً بكلمة عطفٍ مثل «حديث ٥» و«رقم ١٢»
     * فيُبحث في عمود numberInBook بدل نص الحديث. حدّ ٤ أرقام لأن أكبر
     * رقم حديث في الكتب العشرة أصله بلا صفير إضافي، وأي مزج بكلمة أخرى
     * يُرجعه بحثاً نصياً.
     */
    fun pureNumber(rawQuery: String): Long? {
        val tokens = TOKEN_SPLIT
            .split(normalize(rawQuery))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        val rest = tokens.filter { it !in NUMBER_NOISE }
        if (rest.size != 1) return null
        val candidate = rest[0]
        if (candidate.isEmpty() || candidate.length > 4) return null
        if (candidate.any { it !in '0'..'9' }) return null
        return candidate.toLongOrNull()
    }
}
