package com.sawan4art.noor.domain.adhan

/**
 * قواعد الفواصل الزمنية للأذان والإقامة — منطق نقي بلا أندرويد.
 *
 * تطور التوجيهات:
 * - alpha48: «اقصى حد ليه هو ٢٠ دقيقه و اقصر حد ليه هو ١٠ دقائق يعني
 *   عايز ثلاث اختيارات فقط ١٠ و ١٥ و٢٠».
 * - **تحديث المالك 2026-11-06 يلغي الاختيارات نهائياً**: «تحديد الوقت
 *   بين الاذان يكون خانه كتابه رقم مش اختيارات و يكون لكل صلاه على
 *   حده» — فبقيت كل الفواصل خانة كتابة حرة من دقيقة إلى ١٢٠ دقيقة
 *   (ساعتان)، والافتراضيات هي التي تحمل تمييز الفجر: كل الصلوات ١٠
 *   دقائق ماعاد الفجر ١٥ دقيقة، «و طبعا صلاه الجمعه دي استثناء»
 *   (فاصلا الجمعة بين الأذانين وبين الأذان الثاني والإقامة خانتان
 *   مستقلتان بذات الحدود).
 */
object AdhanGaps {

    /** أدنى فاصل مقبول لأي خانة كتابة — دقيقة واحدة */
    const val GAP_MIN_MINUTES: Int = 1

    /** أقصى فاصل مقبول لأي خانة كتابة — ساعتان كما حدد المالك للجمعة */
    const val GAP_MAX_MINUTES: Int = 120

    /** افتراضي فاصل الفجر — ١٥ دقيقة (الاستثناء الوحيد بين الصلوات الخمس) */
    const val DEFAULT_FAJR_GAP_MINUTES: Int = 15

    /** افتراضي فاصل باقي الصلوات — ١٠ دقائق */
    const val DEFAULT_PRAYER_GAP_MINUTES: Int = 10

    /** افتراضي فاصل الجمعة بين الأذان الأول والثاني */
    const val DEFAULT_FRIDAY_ADHAN_GAP_MINUTES: Int = 10

    /** افتراضي فاصل الجمعة بين الأذان الثاني والإقامة */
    const val DEFAULT_FRIDAY_IQAMAH_GAP_MINUTES: Int = 30

    /** حصر قيمة أي خانة كتابة داخل الحدود المسموحة (١..١٢٠ دقيقة) —
     *  قيم النسخ السابقة كلها داخل الحدود فتمر كما هي */
    fun clampGap(minutes: Int): Int =
        minutes.coerceIn(GAP_MIN_MINUTES, GAP_MAX_MINUTES)

    /** هل النص المكتوب في خانة الفاصل صالح للحفظ كما هو */
    fun isGapInputValid(minutes: Int?): Boolean =
        minutes != null && minutes in GAP_MIN_MINUTES..GAP_MAX_MINUTES

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
