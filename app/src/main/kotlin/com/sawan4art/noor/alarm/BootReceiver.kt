package com.sawan4art.noor.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sawan4art.noor.data.SettingsRepository
import com.sawan4art.noor.data.location.AutoLocationWorker
import com.sawan4art.noor.widget.WidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * مستقبل إقلاع الجهاز — المنبهات المعلّقة تموت مع إعادة التشغيل،
 * فهنا تُعاد جدولة منبهات الأذان كلها من مخزن الإعدادات (المواقيت
 * والحساب والتفضيلات) كأن التطبيق فُتح للتو، فيظل الأذان حياً
 * بعد إعادة تشغيل الهاتف أو تحديث التطبيق.
 *
 * **والتحديد التلقائي للموقع** (بلاغ المالك 2026-11-11: «التطبيق
 * لازم يشتغل في الخلفيه») يعود دورتُه الدورية مع الإقلاع إن كان
 * مفعّلاً — فالعمل الدوري يبقى مسجّلاً لكن إعادة الجدولة هنا
 * تضمن حياته بعد أي إقلاعٍ أو قتلٍ لعملية التطبيق.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                AlarmScheduler.reschedule(context)
                // الويدجتات تُصحى مع الإقلاع — المواقيت من يومٍ جديد
                WidgetUpdater.refreshAll(context)
                // دورة التحديد التلقائي تعود مع الإقلاع إن كان مفعّلاً
                if (runCatching { SettingsRepository(context).autoLocationOnce() }
                        .getOrDefault(false)
                ) {
                    AutoLocationWorker.schedule(context)
                }
            } finally {
                pending.finish()
            }
        }
    }
}

