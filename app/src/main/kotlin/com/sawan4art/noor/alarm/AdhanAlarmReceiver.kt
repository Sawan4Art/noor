package com.sawan4art.noor.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * مستقبِل منبهات الأذان — يدق في وقته ولو الشاشة مطفأة والهاتف مقفول:
 *
 * - **أذان/إقامة**: يشغّل الخدمة الأمامية (الصوت + الإشعار المنبثق
 *   فوق شاشة القفل عبر full-screen intent الذي يفتح شاشة التنبيه).
 * - **استعادة الكتم**: يرجع صوت الهاتف كما كان بعد انقضاء مُدة الكتم.
 * - **التجديد اليومي**: يعيد بناء منبهات الأيام القادمة — بهذا يظل
 *   الأذان حياً ولو لم يفتح المستخدم التطبيق لأسابيع.
 */
class AdhanAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val kind = intent.getStringExtra(EXTRA_KIND) ?: return

        if (kind == ACTION_REFRESH || kind == "REFRESH") {
            // التجديد اليومي — إعادة بناء المنبهات القادمة من الإعدادات الحالية
            val pending = goAsync()
            CoroutineScope(Dispatchers.Default).launch {
                try {
                    AlarmScheduler.reschedule(context)
                } finally {
                    pending.finish()
                }
            }
            return
        }

        if (kind == "MUTE_RESTORE") {
            AlarmService.restoreMuteNow(context)
            return
        }

        // أذان أو إقامة — الخدمة الأمامية تشغّل الصوت وترفع الإشعار المنبثق
        val service = Intent(context, AlarmService::class.java)
            .putExtra(AlarmService.EXTRA_MODE, kind)
            .putExtra(AlarmService.EXTRA_PRAYER_NAME, intent.getStringExtra(EXTRA_NAME) ?: "الصلاة")
        runCatching {
            if (android.os.Build.VERSION.SDK_INT >= 26) {
                context.startForegroundService(service)
            } else {
                context.startService(service)
            }
        }.onFailure {
            // جهاز يقيد الخدمة الأمامية — نرفع إشعاراً هادئاً على الأقل
            AlarmService.postSilentPrayerNotice(context, intent.getStringExtra(EXTRA_NAME) ?: "الصلاة")
        }
    }

    companion object {
        const val EXTRA_KIND = "noor_extra_adhan_kind"
        const val EXTRA_NAME = "noor_extra_adhan_name"
        const val EXTRA_IQAMAH_AT = "noor_extra_adhan_iqamah_at"
        const val ACTION_REFRESH = "REFRESH"
    }
}
