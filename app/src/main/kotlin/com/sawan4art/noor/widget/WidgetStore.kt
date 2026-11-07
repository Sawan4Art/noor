package com.sawan4art.noor.widget

import android.content.Context

/**
 * مخزن تخصيص الويدجت — درس v0.4 المعتمد: الإعدادات JSON في
 * SharedPreferences كي تقرأها ويدجتات النظام عند كل رسمٍ بلا دورة
 * DataStore، والتطبيق يكتبها عند كل تغيير من قسم «الويدجت».
 */
object WidgetStore {

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
}
