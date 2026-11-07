package com.sawan4art.noor.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sawan4art.noor.data.SettingsRepository
import com.sawan4art.noor.domain.adhan.AdhanSoundKind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * مستقبِل منبهات الأذان — يدق في وقته ولو الشاشة مطفأة والهاتف مقفول:
 *
 * - **أذان/إقامة**: يقرأ الصوت المختار وإعدادات الكتم لحظة الدقّ من
 *   الإعدادات (فأي تغيير لحظة قبل الدقيقة يعمل فوراً)، ثم يشغّل الخدمة
 *   الأمامية (الصوت + الإشعار المنبثق فوق شاشة القفل عبر full-screen
 *   intent الذي يفتح شاشة التنبيه).
 * - **استعادة الكتم**: منبه MuteManager الاحتياطي — يرجع صوت الهاتف
 *   كما كان حتى لو قُتلت العملية كلها قبل نهاية المدة.
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
            // منبه الاستعادة الاحتياطي — يرجع الصوت ولو العملية ماتت
            AlarmService.restoreMuteNow(context)
            return
        }

        // أذان أو إقامة — الصوت المختار وإعدادات الكتم لحظة الدقّ
        val prefs = runCatching {
            kotlinx.coroutines.runBlocking { SettingsRepository(context).adhanPrefsOnce() }
        }.getOrNull()

        val isIqamah = kind == "IQAMAH"
        val prayerName = intent.getStringExtra(EXTRA_NAME) ?: "الصلاة"
        val soundId = when {
            prefs == null -> "main"
            isIqamah -> prefs.iqamahVoice
            else -> prefs.adhanVoice
        }
        val customPath = when {
            prefs == null -> ""
            isIqamah -> prefs.customIqamahPath
            else -> prefs.customAdhanPath
        }

        val service = Intent(context, AlarmService::class.java)
            .putExtra(AlarmService.EXTRA_MODE, kind)
            .putExtra(AlarmService.EXTRA_PRAYER_NAME, prayerName)
            .putExtra(AlarmService.EXTRA_SOUND_ID, soundId)
            .putExtra(AlarmService.EXTRA_CUSTOM_PATH, customPath)
            .putExtra(AlarmService.EXTRA_MUTE_START, intent.getBooleanExtra(EXTRA_MUTE_START, false))
            .putExtra(AlarmService.EXTRA_MUTE_MINUTES, prefs?.muteMinutes ?: 10)
            .putExtra(
                AlarmService.EXTRA_MUTE_CANCEL_ON_UNLOCK,
                prefs?.muteCancelOnUnlock ?: true,
            )
        runCatching {
            if (android.os.Build.VERSION.SDK_INT >= 26) {
                context.startForegroundService(service)
            } else {
                context.startService(service)
            }
        }.onFailure {
            // جهاز يقيد الخدمة الأمامية — نرفع إشعاراً هادئاً على الأقل
            AlarmService.postSilentPrayerNotice(context, prayerName)
        }
        // الويدجتات تتبدل مع أذان كل صلاة — القادمة الجديدة تظهر فوراً (alpha56)
        com.sawan4art.noor.widget.WidgetUpdater.refreshAll(context)
    }

    companion object {
        const val EXTRA_KIND = "noor_extra_adhan_kind"
        const val EXTRA_NAME = "noor_extra_adhan_name"
        const val EXTRA_IQAMAH_AT = "noor_extra_adhan_iqamah_at"
        const val EXTRA_MUTE_START = "noor_extra_adhan_mute_start"
        const val ACTION_REFRESH = "REFRESH"
    }
}
