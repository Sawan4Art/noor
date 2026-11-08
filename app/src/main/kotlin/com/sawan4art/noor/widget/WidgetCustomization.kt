package com.sawan4art.noor.widget

import org.json.JSONObject

/**
 * تخصيص الويدجت الواحدة — alpha57 بلاغ المالك:
 *
 * «الويدجت مش كلها شغاله اقولك افضل خليها ويدجيت واحده و قابلة
 * للتخصيص بالكامل من الاعدادات التخصيص المتاح هو عرض او ايقاف الوقت
 * و عرض او ايقاف مواقيت الصلاه و عرض او ايقاف المتبقي على الصلاه
 * التاليه و عرض او ايقاف التاريخ الهجري و الميلادي بكذا صيغه و عرض
 * او ايقاف المسبحه للتسبيح من خلال الويدجت و امكانيه جعلها شفافه و
 * اختيار الوان الخط او الوانها و امكانيه اضافه شكل النقطه السائله في
 * اي مكان مثل الوقت المتبقي او التحديد بها على الصلاه التاليه او
 * الحاليه او الاثنين مع بعض مع تغيير لون كل فقاعه للتميز بينهم».
 *
 * فالعقد هنا: خمسُ مفاتيحِ عرض (الوقت، المواقيت، العدّاد، التاريخان،
 * المسبحة) + صيغةُ كل تاريخٍ من ستٍّ + شكلُ الساعة (رقمية أو تناظرية)
 * + الشفافية + لونُ الخط + لونُ التطبيق + الفقاعاتُ السائلة الثلاث
 * (الوقت المتبقي، الصلاة القادمة، الحالية) كلٌّ بلونها — وفي alpha59
 * توسّع العقد ببلاغ المالك: «عايز خانات لتحديد حجم الخط ولونه لكل
 * عنصر» فحجمُ كل عنصرٍ ولونه خريطتان مستقلتان (غيابُ المفتاح = الوضع
 * التلقائي)، و«عايز صيغ اكثر للتواريخ و عند اختيار صيغه للتاريخ تفتح
 * له قائمه منبثقه» فالصيغ ستٌّ لكل تقويم، و«يتم ايضا اضافه ساعه
 * تناظريه يمكن للمستخدم الاختيار بينها و بين الرقميه»، و«مواقيت
 * الصلاه و اسماءها تظهر بشكل صغير في الويدجت» فالخلايا بأسِمها
 * الصغير فوق وقتها، وضمُّ الباقين عند إزالة عنصرٍ (لا فراغات مكانه).
 *
 * يُخزَّن JSON في SharedPreferences (ويدجتات النظام تقرأ بلا دورة
 * DataStore) ويُقرأ عند كل رسم — درس v0.4 المعتمد. المفاتيح القديمة
 * (alpha56: dates/countdown/…) تُقرأ كوقفاتٍ ابتدائية فلا يفقد مستخدم
 * النسخة السابقة تخصيصه.
 */
data class WidgetCustomization(
    // ===== المظهر العام =====
    /** المظهر: يتبع ثيم التطبيق، أو فاتح، أو داكن — تثبيت يدوي */
    val theme: Theme = Theme.APP,
    /** الشفافية ٠..١٠٠ — صفر يعني شفافاً كلياً ومئة معتماً */
    val opacityPercent: Int = 85,
    /** لون التطبيق المختار (معرّف من AccentPalette) — للتوهج والتمييز */
    val accentId: String = "emerald",
    /** لون الخط — "auto" يتبع المظهر، أو لون صريح «#RRGGBB» */
    val textColor: String = COLOR_AUTO,
    /** إظهار اسم المدينة */
    val showCity: Boolean = true,
    /** إظهار اسم التطبيق وشعاره الصغير */
    val showBrand: Boolean = true,

    // ===== عناصر العرض الخمسة (بلاغ المالك) =====
    /** الوقت الحالي — ساعة حيّة */
    val showClock: Boolean = true,
    /** مواقيت الصلاة الست */
    val showPrayerTimes: Boolean = true,
    /** المتبقي على الصلاة التالية */
    val showCountdown: Boolean = true,
    /** التاريخ الهجري */
    val showHijri: Boolean = true,
    /** صيغة الهجري ٠ كاملة — ١ مختصرة — ٢ رقمية — ٣ بلا هـ — ٤ بيوم الأسبوع — ٥ يوم/شهر */
    val hijriFormat: Int = 0,
    /** التاريخ الميلادي */
    val showGregorian: Boolean = true,
    /** صيغة الميلادي ٠ كاملة — ١ مختصرة — ٢ رقمية — ٣ بلا اليوم — ٤ بيوم بلا سنة — ٥ يوم/شهر */
    val gregFormat: Int = 0,
    /** المسبحة للتسبيح من خلال الويدجت */
    val showMisbaha: Boolean = false,

    // ===== شكل الساعة وضباط كل عنصر (alpha59) =====
    /** شكل الساعة — رقمية حيّة أو تناظرية مرسومة */
    val clockStyle: ClockStyle = ClockStyle.DIGITAL,
    /** حجم كل عنصرٍ بالمئة — مفتاح ELEMENT_* إلى ٥٠..٢٥٠ (الغياب = ١٠٠) */
    val elementSizes: Map<String, Int> = emptyMap(),
    /** لون كل عنصرٍ — مفتاح ELEMENT_* إلى «auto» أو «#RRGGBB» (الغياب = لونه الافتراضي) */
    val elementColors: Map<String, String> = emptyMap(),
    /**
     * ترتيب العناصر من أعلى لأسفل (alpha61 — بلاغ المالك: «عايز شكل
     * الويدجت يظهر في اعدادت الويدجت داخل التطبيق و يكون قابل لتحريك
     * كل العناصر») — قائمةٌ بمفاتيح ELEMENT_* بترتيبها المختار، والمفاتيح
     * الغائبة تلحق بترتيبها الافتراضي والمفاتيح الغريبة تُرمى.
     */
    val elementOrder: List<String> = DEFAULT_ORDER,

    // ===== النقطة السائلة — كل مقامٍ بفقاعتِه ولونها =====
    /** فقاعةٌ خلف الوقت المتبقي */
    val bubbleOnCountdown: Boolean = false,
    /** لون فقاعة الوقت المتبقي */
    val bubbleColorCountdown: String = DEFAULT_BUBBLE,
    /** فقاعةٌ تُحدّد الصلاة القادمة في المواقيت */
    val bubbleOnNext: Boolean = true,
    /** لون فقاعة القادمة */
    val bubbleColorNext: String = DEFAULT_BUBBLE,
    /** فقاعةٌ تُحدّد الصلاة الحالية في المواقيت */
    val bubbleOnCurrent: Boolean = false,
    /** لون فقاعة الحالية */
    val bubbleColorCurrent: String = DEFAULT_BUBBLE_ALT,
) {

    /** مظهر الويدجت — الثيم الحي أو تثبيت يدوي */
    enum class Theme { APP, LIGHT, DARK }

    /** شكل الساعة في الويدجت — الرقمية TextClock الحية أو التناظرية المرسومة */
    enum class ClockStyle { DIGITAL, ANALOG }

    /** ألفا الخلفية ٠..١ */
    val opacityFraction: Float get() = opacityPercent.coerceIn(0, 100) / 100f

    fun toJson(): String = JSONObject().apply {
        put("theme", theme.name)
        put("opacity", opacityPercent.coerceIn(0, 100))
        put("accent", accentId)
        put("textColor", textColor)
        put("city", showCity)
        put("brand", showBrand)
        put("clock", showClock)
        put("prayers", showPrayerTimes)
        put("countdown", showCountdown)
        put("hijri", showHijri)
        put("hijriFormat", hijriFormat.coerceIn(0, 5))
        put("greg", showGregorian)
        put("gregFormat", gregFormat.coerceIn(0, 5))
        put("misbaha", showMisbaha)
        put("clockStyle", clockStyle.name)
        if (elementSizes.isNotEmpty()) {
            put("elementSizes", JSONObject().apply { elementSizes.forEach { (k, v) -> put(k, v.coerceIn(50, 250)) } })
        }
        if (elementColors.isNotEmpty()) {
            put("elementColors", JSONObject().apply { elementColors.forEach { (k, v) -> put(k, v) } })
        }
        if (elementOrder != DEFAULT_ORDER) {
            put("elementOrder", org.json.JSONArray(elementOrder))
        }
        put("bubbleCountdown", bubbleOnCountdown)
        put("bubbleColorCountdown", bubbleColorCountdown)
        put("bubbleNext", bubbleOnNext)
        put("bubbleColorNext", bubbleColorNext)
        put("bubbleCurrent", bubbleOnCurrent)
        put("bubbleColorCurrent", bubbleColorCurrent)
    }.toString()

    companion object {
        const val PREFS_NAME = "widget_settings"
        const val PREFS_KEY = "widget_settings_json"

        /** لون الخط يتبع المظهر */
        const val COLOR_AUTO = "auto"

        // ===== مفاتيح عناصر الحجم/اللون لكل عنصرٍ (alpha59) =====
        const val ELEMENT_HEADER = "header"
        const val ELEMENT_CLOCK = "clock"
        const val ELEMENT_HIJRI = "hijri"
        const val ELEMENT_GREG = "greg"
        const val ELEMENT_NEXT = "next"
        const val ELEMENT_COUNTDOWN = "countdown"
        const val ELEMENT_GRID = "grid"
        const val ELEMENT_MISBAHA = "misbaha"

        /** كل مفاتيح العناصر — لطبقة الإعدادات وحلقة التطبيق */
        val ELEMENT_KEYS = listOf(
            ELEMENT_HEADER, ELEMENT_CLOCK, ELEMENT_HIJRI, ELEMENT_GREG, ELEMENT_NEXT,
            ELEMENT_COUNTDOWN, ELEMENT_GRID, ELEMENT_MISBAHA,
        )

        /** الترتيب الافتراضي للعناصر من أعلى لأسفل — كما كان العمود دوماً */
        val DEFAULT_ORDER = listOf(
            ELEMENT_HEADER, ELEMENT_CLOCK, ELEMENT_HIJRI, ELEMENT_GREG,
            ELEMENT_NEXT, ELEMENT_COUNTDOWN, ELEMENT_GRID, ELEMENT_MISBAHA,
        )

        /**
         * تطبيع ترتيب: الغريب يُرمى والمكرر يُختزل والغائب يُلحق بترتيبه
         * الافتراضي — دالة نقية تُختبر آلياً وتضمن أن الترتيب دائماً
         * تبديلاً كاملاً لمفاتيح العناصر المعروفة لا أكثر ولا أقل.
         */
        fun normalizeOrder(order: List<String>): List<String> {
            val seen = LinkedHashSet<String>()
            for (key in order) {
                if (key in ELEMENT_KEYS) seen.add(key)
            }
            for (key in DEFAULT_ORDER) {
                if (key !in seen) seen.add(key)
            }
            return seen.toList()
        }

        /** لون الفقاعة الافتراضي — فيروزي التطبيق الهادئ */
        const val DEFAULT_BUBBLE = "#2FA48C"

        /** لون بديل للفقاعة الثانية كي يتميز عن الأولى */
        const val DEFAULT_BUBBLE_ALT = "#C98A3D"

        /** قراءة خريطة الحجم من JSON — الغريب يُرمى والمحصور ٥٠..٢٥٠ */
        private fun sizesFrom(o: JSONObject): Map<String, Int> {
            val raw = o.optJSONObject("elementSizes") ?: return emptyMap()
            val out = mutableMapOf<String, Int>()
            for (key in ELEMENT_KEYS) {
                val v = raw.optInt(key, Int.MIN_VALUE)
                if (v != Int.MIN_VALUE) out[key] = v.coerceIn(50, 250)
            }
            return out
        }

        /** قراءة خريطة اللون من JSON — مفاتيح العناصر وحدها */
        private fun colorsFrom(o: JSONObject): Map<String, String> {
            val raw = o.optJSONObject("elementColors") ?: return emptyMap()
            val out = mutableMapOf<String, String>()
            for (key in ELEMENT_KEYS) {
                val v = raw.optString(key, "")
                if (v.isNotEmpty()) out[key] = v
            }
            return out
        }

        /** الافتراضي — قبل أي تخصيص */
        val DEFAULT = WidgetCustomization()

        /** قراءة JSON — والناقص أو الفاسد يسير بأمان إلى الافتراضي */
        fun fromJson(raw: String?): WidgetCustomization {
            if (raw.isNullOrBlank()) return DEFAULT
            return runCatching {
                val o = JSONObject(raw)
                // مفاتيح alpha56 القديمة تُوقّف البدائل الجديدة إن حضرت
                val legacyDates = if (o.has("dates")) o.optBoolean("dates", true) else null
                WidgetCustomization(
                    theme = runCatching {
                        Theme.valueOf(o.optString("theme", Theme.APP.name))
                    }.getOrDefault(Theme.APP),
                    opacityPercent = o.optInt("opacity", 85),
                    accentId = o.optString("accent", "emerald"),
                    textColor = o.optString("textColor", COLOR_AUTO),
                    showCity = o.optBoolean("city", true),
                    showBrand = o.optBoolean("brand", true),
                    showClock = o.optBoolean("clock", true),
                    showPrayerTimes = o.optBoolean("prayers", true),
                    showCountdown = if (o.has("countdown")) {
                        o.optBoolean("countdown", true)
                    } else {
                        true
                    },
                    showHijri = legacyDates ?: o.optBoolean("hijri", true),
                    hijriFormat = o.optInt("hijriFormat", 0).coerceIn(0, 5),
                    showGregorian = legacyDates ?: o.optBoolean("greg", true),
                    gregFormat = o.optInt("gregFormat", 0).coerceIn(0, 5),
                    showMisbaha = o.optBoolean("misbaha", false),
                    clockStyle = runCatching {
                        ClockStyle.valueOf(o.optString("clockStyle", ClockStyle.DIGITAL.name))
                    }.getOrDefault(ClockStyle.DIGITAL),
                    elementSizes = sizesFrom(o),
                    elementColors = colorsFrom(o),
                    elementOrder = normalizeOrder(
                        buildList {
                            val arr = o.optJSONArray("elementOrder")
                            if (arr != null) {
                                for (i in 0 until arr.length()) add(arr.optString(i, ""))
                            }
                        },
                    ),
                    bubbleOnCountdown = o.optBoolean("bubbleCountdown", false),
                    bubbleColorCountdown = o.optString("bubbleColorCountdown", DEFAULT_BUBBLE),
                    bubbleOnNext = o.optBoolean("bubbleNext", true),
                    bubbleColorNext = o.optString("bubbleColorNext", DEFAULT_BUBBLE),
                    bubbleOnCurrent = o.optBoolean("bubbleCurrent", false),
                    bubbleColorCurrent = o.optString("bubbleColorCurrent", DEFAULT_BUBBLE_ALT),
                )
            }.getOrDefault(DEFAULT)
        }

        /**
         * هل النص لونٌ صريح صالح «#RRGGBB» أو «#AARRGGBB»؟ — تحليلٌ يدوي
         * خالص بلا فئات أندرويد كي يُختبر في اختبارات الجهاز الافتراضي
         * (فئات android.graphics مطروحةٌ هناك).
         */
        fun parseColor(raw: String): Int? {
            if (raw == COLOR_AUTO) return null
            val hex = raw.removePrefix("#")
            val ok = when (hex.length) {
                6, 8 -> hex.all { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }
                else -> false
            }
            if (!ok) return null
            val value = hex.toLongOrNull(16) ?: return null
            return if (hex.length == 6) {
                ((0xFFL shl 24) or value).toInt()
            } else {
                value.toInt()
            }
        }
    }
}
