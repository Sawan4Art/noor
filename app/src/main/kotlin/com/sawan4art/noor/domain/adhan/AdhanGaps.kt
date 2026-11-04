package com.sawan4art.noor.domain.adhan

/**
 * قواعد الفواصل الزمنية للأذان والإقامة — منطق نقي بلا أندرويد (توجيه
 * المالك 2026-11-05):
 *
 * - **الفاصل بين الأذان والإقامة ثلاثة اختيارات فقط**: ١٠ و١٥ و٢٠ دقيقة —
 *   «اقصى حد ليه هو ٢٠ دقيقه و اقصر حد ليه هو ١٠ دقائق يعني عايز ثلاث
 *   اختيارات فقط ١٠ و ١٥ و٢٠».
 * - **فاصل الجمعة بين الأذان الثاني والإقامة خانة كتابة حرة** من دقيقة
 *   واحدة إلى ١٢٠ دقيقة (ساعتان) — «ممكن يصل لغايه ٢ ساعه ف عايز خانه
 *   كتابه و ليس اختيارات».
 */
object AdhanGaps {

    /** الاختيارات الثلاثة المسموحة للفاصل بين الأذان والإقامة */
    val IQAMAH_OFFSET_CHOICES: List<Int> = listOf(10, 15, 20)

    /** أدنى فاصل جمعة مقبول — دقيقة واحدة */
    const val FRIDAY_IQAMAH_GAP_MIN_MINUTES: Int = 1

    /** أقصى فاصل جمعة مقبول — ساعتان كما حدد المالك */
    const val FRIDAY_IQAMAH_GAP_MAX_MINUTES: Int = 120

    /**
     * أقرب اختيار مسموح لقيم النسخ السابقة التي خرجت عن الثلاثة
     * (٥→١٠، ٢٥→٢٠، ٣٠→٢٠…) — فلا يرى المستخدم قيمةً بلا زر.
     */
    fun snapIqamahOffset(minutes: Int): Int =
        IQAMAH_OFFSET_CHOICES.minByOrNull { kotlin.math.abs(it - minutes) } ?: 10

    /** حصر فاصل الجمعة داخل الحدود المسموحة (١..١٢٠ دقيقة) */
    fun clampFridayIqamahGap(minutes: Int): Int =
        minutes.coerceIn(FRIDAY_IQAMAH_GAP_MIN_MINUTES, FRIDAY_IQAMAH_GAP_MAX_MINUTES)

    /** هل النص المكتوب في خانة الفاصل صالح للحفظ كما هو */
    fun isFridayGapInputValid(minutes: Int?): Boolean =
        minutes != null && minutes in FRIDAY_IQAMAH_GAP_MIN_MINUTES..FRIDAY_IQAMAH_GAP_MAX_MINUTES

    /**
     * تحويل نص خانة الكتابة إلى دقائق — يقبل الأرقام العربية ٠-٩
     * والغربية 0-9 ويتجاهل ما سواهما، ويرجع null إن لم يوجد رقم صالح.
     */
    fun parseMinutesInput(text: String): Int? {
        val digits = text.map { ch -> if (ch in '٠'..'٩') '0' + (ch - '٠') else ch }
            .filter { it in '0'..'9' }
            .joinToString("")
        return digits.toIntOrNull()
    }

    /** عرض عدد الدقائق بالأرقام العربية في خانة الكتابة */
    fun toArabicDigits(value: Int): String = value.toString().map { ch ->
        if (ch in '0'..'9') '٠' + (ch - '0') else ch
    }.joinToString("")
}
