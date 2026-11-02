package com.sawan4art.noor.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * مستقبل الإقلاع (تجهيز مرحلة الأذان بطلب المالك 2026-11-03).
 *
 * **مكانته اليوم**: مسجّل في المانيفست بـ `exported="true"` مع فلتر
 * `BOOT_COMPLETED` — الصيغة الوحيد الصحيحة لاستقبال بث النظام المحمي
 * (متطلب أندرويد ١٢+ أن يكون المستقبل الخارجي صريح التصدير)، وموجود
 * من الآن كي لا يحتاج عمل الأذان لاحقاً لمسّ المانيفست.
 *
 * **ماذا يفعل بعد إعادة تشغيل الجهاز**: مرحلة الأذان القادمة تعيد
 * هنا جدولة منبهات الصلوات الخمس من مخزن الإعدادات (المنبهات المعلّقة
 * تموت مع إعادة التشغيل وإلا فات الأذان حتى مع إذن الإقلاع الممنوح).
 * اليوم: لا منبهات مجدولة بعد — لا شيء يُنفَّذ، والمستقبل عمداً
 * أخف ما يكون (حد النظام ١٠ ثوانٍ على المستقبلات).
 *
 * ملاحظة: فاحص تحديثات التطبيق الدوري (WorkManager) يعيد جدولة نفسه
 * بعد الإقلاع وحده — لا حاجة للمسّه هنا.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        // مرحلة الأذان القادمة: AlarmScheduler.rescheduleAll(context)
        // — إعادة جدولة منبهات الصلوات من مخزن الإعدادات
    }
}
