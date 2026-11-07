package com.sawan4art.noor.widget

import org.json.JSONObject

/**
 * تخصيص الويدجت — توجيه المالك ٢٠٢٦-١١-٠٧:
 * «عايز قسم جديد في الاعدادات خاص بالويدجت و هنعمل خمس اشكال مختلفه
 * بنفس تصميم التطبيق و هتكون الويدجت دي قابله للتخصيص والتعديل الكامل
 * من قبل المستخدم».
 *
 * خمسة أشكال يعني خمسة ويدجتات يستدلي بها المستخدم من قائمة النظام،
 * والهذا الكائن هو **التخصيص المشترك** بينها كله: المظهر (فاتح/داكن/بالثيم)،
 * الشفافية خمس درجات، لون التطبيق، وعناصر العرض (المدينة، العدّاد
 * التنازلي، التاريخان، اسم التطبيق) — يُخزَّن JSON في SharedPreferences
 * (ويدجتات النظام تقرأ بلا دورة DataStore) ويُقرأ عند كل رسم.
 *
 * دروس النسخة السابقة (AGENTS.md دفعة v0.4): تخزين JSON في
 * SharedPreferences (`widget_settings_json`) وقراءته عند كل رسم —
 * والخلفيات بألفافٍ مرسومة لا ألوانٍ مربعة.
 */
data class WidgetCustomization(
    /** المظهر: يتبع ثيم التطبيق، أو فاتح، أو داكن — تثبيت يدوي */
    val theme: Theme = Theme.APP,
    /** الشفافية ٠..١٠٠ — صفر يعني شفافاً كلياً ومئة معتمًا */
    val opacityPercent: Int = 85,
    /** لون التطبيق المختار (معرّف من AccentPalette) — للتوهج والتمييز */
    val accentId: String = "emerald",
    /** إظهار اسم المدينة */
    val showCity: Boolean = true,
    /** إظهار العدّاد التنازلي للصلاة القادمة */
    val showCountdown: Boolean = true,
    /** إظهار التاريخ الهجري والميلادي */
    val showDates: Boolean = true,
    /** إظهار اسم التطبيق وشعاره الصغير */
    val showBrand: Boolean = true,
) {

    /** مظهر الويدجت — الثيم الحي أو تثبيت يدوي */
    enum class Theme { APP, LIGHT, DARK }

    /** ألفا الخلفية ٠..١ */
    val opacityFraction: Float get() = opacityPercent.coerceIn(0, 100) / 100f

    fun toJson(): String = JSONObject().apply {
        put("theme", theme.name)
        put("opacity", opacityPercent.coerceIn(0, 100))
        put("accent", accentId)
        put("city", showCity)
        put("countdown", showCountdown)
        put("dates", showDates)
        put("brand", showBrand)
    }.toString()

    companion object {
        const val PREFS_NAME = "widget_settings"
        const val PREFS_KEY = "widget_settings_json"

        /** الافتراضي — قبل أي تخصيص */
        val DEFAULT = WidgetCustomization()

        /** قراءة JSON — والناقص أو الفاسد يسير بأمان إلى الافتراضي */
        fun fromJson(raw: String?): WidgetCustomization {
            if (raw.isNullOrBlank()) return DEFAULT
            return runCatching {
                val o = JSONObject(raw)
                WidgetCustomization(
                    theme = runCatching {
                        Theme.valueOf(o.optString("theme", Theme.APP.name))
                    }.getOrDefault(Theme.APP),
                    opacityPercent = o.optInt("opacity", 85),
                    accentId = o.optString("accent", "emerald"),
                    showCity = o.optBoolean("city", true),
                    showCountdown = o.optBoolean("countdown", true),
                    showDates = o.optBoolean("dates", true),
                    showBrand = o.optBoolean("brand", true),
                )
            }.getOrDefault(DEFAULT)
        }
    }
}
