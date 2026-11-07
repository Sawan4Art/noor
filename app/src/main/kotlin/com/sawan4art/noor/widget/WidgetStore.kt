package com.sawan4art.noor.widget

import android.content.Context
import android.content.SharedPreferences

/**
 * مخزن الويدجت — درس v0.4 المعتمد: الإعدادات JSON في
 * SharedPreferences كي تقرأها ويدجتات النظام عند كل رسمٍ بلا دورة
 * DataStore، والتطبيق يكتبها عند كل تغيير من قسم «الويدجت».
 *
 * وعدّاد المسبحة داخل الويدجت (alpha57) في التخزين نفسه — ضغطة
 * التسبيح في الويدجت تزيد به فوراً بلا دورة DataStore كي يعمل
 * المستقبِل أسرعَ ما يمكن.
 */
object WidgetStore {

    private const val MISBAHA_KEY = "widget_misbaha_count"

    /** قراءة التخصيص الحالي — الفاسد أو الغائب يسير للافتراضي */
    fun customization(context: Context): WidgetCustomization {
        val prefs = context.getSharedPreferences(
            WidgetCustomization.PREFS_NAME, Context.MODE_PRIVATE,
        )
        return WidgetCustomization.fromJson(prefs.getString(WidgetCustomization.PREFS_KEY, null))
    }

    /** حفظ التخصيص بعد تغييره من قسم «الويدجت» */
    fun save(context: Context, customization: WidgetCustomization) {
        context.getSharedPreferences(WidgetCustomization.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(WidgetCustomization.PREFS_KEY, customization.toJson())
            .apply()
    }

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(WidgetCustomization.PREFS_NAME, Context.MODE_PRIVATE)

    /** عدد تسبيحات الويدجت الحالي */
    fun misbahaCount(context: Context): Int = prefs(context).getInt(MISBAHA_KEY, 0)

    /** ضغطة تسبيح — يزيد العدّ ويُرجع الجديد */
    fun misbahaTap(context: Context): Int {
        val next = misbahaCount(context) + 1
        prefs(context).edit().putInt(MISBAHA_KEY, next).apply()
        return next
    }

    /** تصفير المسبحة */
    fun misbahaReset(context: Context) {
        prefs(context).edit().putInt(MISBAHA_KEY, 0).apply()
    }
}
