package com.sawan4art.noor.widget

import android.content.Context

/**
 * مخزن الويدجت — درس v0.4 المعتمد: الإعدادات JSON في
 * SharedPreferences كي تقرأها ويدجتات النظام عند كل رسمٍ بلا دورة
 * DataStore، والتطبيق يكتبها عند كل تغيير من قسم «الويدجت».
 *
 * ولون الإطار (alpha63) يُختار من لوحة قسم الويدجت — تلقائي أو
 * لونٌ صريح أو بلا إطارٍ خالصاً — وضغطة الويدجت على الشاشة تفتح
 * قسم إعداداتها داخل التطبيق بدل التبديل بدورةٍ (كانت تعيد الرسم
 * فتكشف عقد addView القديم قبل إصلاحه).
 *
 * **تخزينٌ لكل ويدجتٍ على حدة** (بلاغ المالك 2026-11-11: «٦ اشكال
 * للويدجت تكون جاهزة للمستخدم ممكن يختار منهم»): كل ويدجتٍ منصوبةٍ
 * تحمل JSON الخاص بها بمفتاح [customKeyFor]، والغائبُ يعود إلى
 * JSON العام (مفاتيح النسخ القديمة كما هي) — فيمكن أن تعيش ويدجتان
 * من شكلين مختلفين جنباً إلى جنب، وويدجتُ المستخدم القديمة تجد
 * تخصيصها العام سليماً كما هو بلا ترحيل.
 */
object WidgetStore {

    /** قراءة التخصيص العام — الفاسد أو الغائب يسير للافتراضي */
    fun customization(context: Context): WidgetCustomization {
        val prefs = prefs(context)
        return WidgetCustomization.fromJson(prefs.getString(WidgetCustomization.PREFS_KEY, null))
    }

    /** حفظ التخصيص العام بعد تغييره من قسم «الويدجت» */
    fun save(context: Context, customization: WidgetCustomization) {
        prefs(context).edit()
            .putString(WidgetCustomization.PREFS_KEY, customization.toJson())
            .apply()
    }

    /** مفتاح JSON الخاص بويدجتٍ بمعرّفها */
    private fun customKeyFor(appWidgetId: Int): String = "widget_custom_$appWidgetId"

    /**
     * تخصيص ويدجتٍ بمعرّفها — الخاصُّ بها إن وُجد وإلا العام. المعرّف
     * غير الصالح (من الحوارات بلا ويدجت) يعود للعام مباشرةً.
     */
    fun customizationFor(context: Context, appWidgetId: Int): WidgetCustomization {
        if (appWidgetId <= 0) return customization(context)
        val prefs = prefs(context)
        val own = prefs.getString(customKeyFor(appWidgetId), null)
        if (own != null) return WidgetCustomization.fromJson(own)
        return customization(context)
    }

    /** حفظ تخصيص ويدجتٍ بمعرّفها */
    fun saveFor(context: Context, appWidgetId: Int, customization: WidgetCustomization) {
        if (appWidgetId <= 0) {
            save(context, customization)
            return
        }
        prefs(context).edit()
            .putString(customKeyFor(appWidgetId), customization.toJson())
            .apply()
    }

    /**
     * **زرع الشكل الجاهز** في أول رسمٍ لويدجتٍ من مستقبِلات الأشكال
     * الستة — لا يُزرَع إلا مرةً واحدةً: لو حُفظ للويدجت JSON خاصٌّ
     * (تخصيص يدوي بعدها) فلا يُمسّ أبداً.
     */
    fun seedPresetIfAbsent(context: Context, appWidgetId: Int, preset: WidgetCustomization) {
        if (appWidgetId <= 0) return
        val prefs = prefs(context)
        if (prefs.getString(customKeyFor(appWidgetId), null) != null) return
        saveFor(context, appWidgetId, preset)
    }

    /** مسح تخصيص ويدجتٍ محذوفة — كي لا يتراكم الموتى في الذاكرة */
    fun clearFor(context: Context, appWidgetId: Int) {
        if (appWidgetId <= 0) return
        prefs(context).edit().remove(customKeyFor(appWidgetId)).apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(WidgetCustomization.PREFS_NAME, Context.MODE_PRIVATE)
}
