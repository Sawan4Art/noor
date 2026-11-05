package com.sawan4art.noor.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.app.NotificationManager
import java.time.ZonedDateTime

/**
 * مدير كتم أصوات الهاتف — توجيه المالك 2026-11-06:
 *
 * - «ميزه لكتم الاصوات لو المستخدم حدد ٣٠ دقيقه مثلا بعد الاقامه» —
 *   فالكتم يبدأ بعد انتهاء صوت الإقامة (أو الأذان إن كانت الإقامة
 *   مطفأة) ويستمر المُدة المحددة ثم يرجع الصوت تلقائياً.
 * - «بغض النظر عن الوقت المحدد يتم الغاء تفعيل كتم الاصوات عند فتح
 *   قفل الشاشه يعني مثال لو الوقت المحدد هو ١٥ دقيقه بعد الاقامه و
 *   المستخدم خلص صلاه ب ١٠ دقائق و فتح تلفونه ف اصوات الهاتف ترجع
 *   تشتغل تاني حتى لو لسه الوقت لم ينقضي» — فتح القفل يستقبل في
 *   مستقبِل ديناميكي داخل نافذة الكتم (الخدمة الأمامية حية تحفظه).
 *
 * - التطبيق الأولوية لعدم الإزعاج الكامل إن مُنح إذنه («امنح التطبيق
 *   اذونات عدم الازعاج»)، وإلا الوضع الصامت على أي حال.
 * - القيم السابقة (فلتر عدم الإزعاج ووضع الرن) تُحفظ فوراً في
 *   SharedPreferences كي يستطيع أي مستقبِل استعادتها حتى لو قُتلت
 *   العملية — ومعها منبه احتياطي مستقل يضمن العودة مهما حدث.
 */
object MuteManager {

    private const val PREFS = "noor_mute_state"
    private const val KEY_ACTIVE = "active"
    private const val KEY_PREV_FILTER = "prev_filter"
    private const val KEY_PREV_RINGER = "prev_ringer"

    /** رمز طلب منبه الاستعادة الاحتياطي — خارج نطاق أحداث الأذان */
    private const val RESTORE_REQUEST_CODE = 2950

    /** فعل منبه الاستعادة — يستقبله AdhanAlarmReceiver */
    const val ACTION_RESTORE = "com.sawan4art.noor.action.MUTE_RESTORE"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** هل الكتم مفعّل حالياً (صوت الهاتف مكتوم من التطبيق)؟ */
    fun isActive(context: Context): Boolean = prefs(context).getBoolean(KEY_ACTIVE, false)

    /**
     * تطبيق الكتم الآن — يحفظ القيم السابقة ثم يكتم، ويجدول المنبه
     * الاحتياطي عند نهاية المُدة. يرجع true إن طُبّق فعلاً.
     */
    fun apply(context: Context, minutes: Int): Boolean {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

        val prevFilter = nm?.currentInterruptionFilter
            ?: NotificationManager.INTERRUPTION_FILTER_ALL
        val prevRinger = audio?.ringerMode ?: AudioManager.RINGER_MODE_NORMAL

        var applied = false
        // الأولوية لعدم الإزعاج الكامل إن مُنح الإذن (الأشمل)
        if (nm?.isNotificationPolicyAccessGranted == true) {
            applied = runCatching {
                nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
            }.isSuccess
        }
        if (!applied && audio != null) {
            applied = runCatching {
                audio.ringerMode = AudioManager.RINGER_MODE_SILENT
            }.isSuccess
        }
        if (!applied) return false

        prefs(context).edit()
            .putBoolean(KEY_ACTIVE, true)
            .putInt(KEY_PREV_FILTER, prevFilter)
            .putInt(KEY_PREV_RINGER, prevRinger)
            .apply()
        scheduleRestoreAlarm(context, System.currentTimeMillis() + minutes * 60_000L)
        return true
    }

    /**
     * استعادة أصوات الهاتف كما كانت — من القيم المحفوظة، وتُلغي
     * المنبه الاحتياطي. آمنة للاستدعاء المتكرر (بلا حالة = بلا فعل).
     */
    fun restore(context: Context) {
        val p = prefs(context)
        cancelRestoreAlarm(context)
        if (!p.getBoolean(KEY_ACTIVE, false)) return

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        if (nm?.isNotificationPolicyAccessGranted == true) {
            runCatching { nm.setInterruptionFilter(p.getInt(KEY_PREV_FILTER, NotificationManager.INTERRUPTION_FILTER_ALL)) }
        }
        if (audio != null) {
            runCatching { audio.ringerMode = p.getInt(KEY_PREV_RINGER, AudioManager.RINGER_MODE_NORMAL) }
        }
        p.edit().putBoolean(KEY_ACTIVE, false).apply()
    }

    /** منبه الاستعادة الاحتياطي — يعمل ولو قُتلت العملية كلها */
    private fun scheduleRestoreAlarm(context: Context, atMillis: Long) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pi = buildRestorePendingIntent(context) ?: return
        runCatching {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pi)
        }
    }

    /** إلغاء منبه الاستعادة الاحتياطي */
    fun cancelRestoreAlarm(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        buildRestorePendingIntent(context)?.let { am.cancel(it) }
    }

    private fun buildRestorePendingIntent(context: Context): PendingIntent? {
        val intent = Intent(context, AdhanAlarmReceiver::class.java)
            .setAction(ACTION_RESTORE)
            .putExtra(AdhanAlarmReceiver.EXTRA_KIND, "MUTE_RESTORE")
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return runCatching {
            PendingIntent.getBroadcast(context, RESTORE_REQUEST_CODE, intent, flags)
        }.getOrNull()
    }

    /** وصف مُدة الكتم للإشعار — «بعد كذا» بأرقام واضحة */
    fun restoreAtText(context: Context, minutes: Int): String {
        val at = ZonedDateTime.now().plusMinutes(minutes.toLong())
        return String.format("%02d:%02d", at.hour, at.minute)
    }
}
