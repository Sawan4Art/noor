package com.sawan4art.noor

import android.app.Application
import com.sawan4art.noor.data.update.UpdateManager
import com.sawan4art.noor.data.update.UpdateNotifier
import com.sawan4art.noor.data.update.UpdateWorker
import com.sawan4art.noor.util.CrashLog

/**
 * منصة التطبيق — تهيئة قناة إشعارات التحديث وجدولة الفاحص الدوري
 * ومدير التحديثات مرة واحدة لكل عملية (كلها عملية رخيصة ومعرّفة idempotent).
 *
 * (alpha53) الصفحات كلها مرفق داخل التطبيق فلا محمّل صور شبكة بعدما —
 * محمّل Coil الذي كان يخزّن صفحات المصحف من الشبكة حُذف من الأصل،
 * والصفحات تُقرأ من مرفقات الجهاز نفسه في لمح البصر بلا إنترنت.
 *
 * (alpha61) **دفتر الأعطال**: بلاغ المالك المتكرر «زر إعدادات الخطبة
 * يحدث له جلتش ثم يغلق التطبيق» أعادَنا ثلاثَ مرات دون أثرٍ للسبب
 * على جهازه — فأصبح كل انهيارٍ غير متوقع يُكتَب سطرُه الأخير كاملاً
 * في [CrashLog]، وزر «تقرير آخر عطل» في الإعدادات يشاركه بالبريد
 * فنرى الجذر الحقيقي من أول بلاغٍ قادم بدل تخمينٍ أعمى.
 */
class NoorApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        CrashLog.install(this)
        UpdateManager.init(this)
        UpdateNotifier.ensureChannel(this)
        UpdateWorker.schedule(this)
    }
}
