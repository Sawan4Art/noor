package com.sawan4art.noor.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sawan4art.noor.widget.WidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * مستقبل إقلاع الجهاز — المنبهات المعلّقة تموت مع إعادة التشغيل،
 * فهنا تُعاد جدولة منبهات الأذان كلها من مخزن الإعدادات (المواقيت
 * والحساب والتفضيلات) كأن التطبيق فُتح للتو، فيظل الأذان حياً
 * بعد إعادة تشغيل الهاتف أو تحديث التطبيق.
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
            } finally {
                pending.finish()
            }
        }
    }
}
