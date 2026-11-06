package com.sawan4art.noor

import android.app.Application
import com.sawan4art.noor.data.update.UpdateManager
import com.sawan4art.noor.data.update.UpdateNotifier
import com.sawan4art.noor.data.update.UpdateWorker

/**
 * منصة التطبيق — تهيئة قناة إشعارات التحديث وجدولة الفاحص الدوري
 * ومدير التحديثات مرة واحدة لكل عملية (كلها عملية رخيصة ومعرّفة idempotent).
 *
 * (alpha53) الصفحات كلها مرفق داخل التطبيق فلا محمّل صور شبكة بعدما —
 * محمّل Coil الذي كان يخزّن صفحات المصحف من الشبكة حُذف من الأصل،
 * والصفحات تُقرأ من مرفقات الجهاز نفسه في لمح البصر بلا إنترنت.
 */
class NoorApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        UpdateManager.init(this)
        UpdateNotifier.ensureChannel(this)
        UpdateWorker.schedule(this)
    }
}
