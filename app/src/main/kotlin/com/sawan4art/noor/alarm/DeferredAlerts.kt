package com.sawan4art.noor.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import org.json.JSONObject

/**
 * غفّوة التنبيهات المؤجَّلة — النصف الآليّ من بلاغ المالك (2026-11-15):
 *
 * «اما باقي التنبيهات يصدر رنين التنبيه بعد انتهاء المكالمه ب ٥ ثواني
 * كده اللي كأن التنبيه حصل لي غفوة»
 *
 * تنبيهٌ دقّ أثناء مكالمةٍ يُحفَظ حملُه كاملاً في التفضيلات، وتدور
 * نبضةُ فحصٍ كل [CallAlertPolicy.RECHECK_DELAY_MILLIS] عبر منبهاتٍ دقيقةٍ
 * توقظ الجهاز (بلا إذنِ قراءةِ حالةِ الهاتف فالفحصُ دوريٌّ لا مستمعٌ)،
 * فإذا انتهت المكالمة رنّ الحملُ بعد [CallAlertPolicy.RING_DELAY_MILLIS]
 * في المسار العادي نفسه (الخدمة الأمامية بنصّه وصوته واهتزازه) — وإن
 * دخلَت مكالمةٌ أخرى قبل لحظة الرنين عادت الغفوة من جديد. المنبهات من
 * النظام تُحييها [BootReceiver] بعد إعادة التشغيل، وأرقامُ طلباتها خارج
 * نطاق جدولة الأذان كي لا يمسّها التجديد اليومي.
 */
object DeferredAlerts {

    const val ACTION_RECHECK = "com.sawan4art.noor.action.DEFERRED_RECHECK"
    const val ACTION_RING = "com.sawan4art.noor.action.DEFERRED_RING"

    private const val PREFS = "noor_deferred_alert"
    private const val KEY_PAYLOAD = "payload"
    /** خارج نطاق جدولة الأذان (2900..3120) كي لا يُلغيها التجديد اليومي */
    private const val RECHECK_REQUEST_CODE = 3300
    private const val RING_REQUEST_CODE = 3301

    /** حفظ حمل التنبيه المؤجَّل وبدء دورة الفحص */
    fun defer(context: Context, payload: JSONObject) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PAYLOAD, payload.toString())
            .apply()
        scheduleRecheck(context, System.currentTimeMillis() + CallAlertPolicy.RECHECK_DELAY_MILLIS)
    }

    fun hasPending(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).contains(KEY_PAYLOAD)

    /** استئناف الغفوة بعد الإقلاع — حملُها في التفضيلات ومنبهاتُها تموت معه */
    fun resume(context: Context) {
        if (!hasPending(context)) return
        scheduleRecheck(context, System.currentTimeMillis() + CallAlertPolicy.RECHECK_DELAY_MILLIS)
    }

    /** نبضة الفحص — المكالمة باقية؟ انتظارٌ آخر. انتهت؟ رنينٌ بعد المهلة */
    fun onRecheck(context: Context) {
        val now = System.currentTimeMillis()
        when (val result = CallAlertPolicy.recheck(CallGate.isInCall(context), now)) {
            is CallAlertPolicy.Recheck.KeepWaiting -> scheduleRecheck(context, result.recheckAtMillis)
            is CallAlertPolicy.Recheck.Ring -> scheduleRing(context, result.ringAtMillis)
        }
    }

    /** لحظة الرنين وصلت — مكالمةٌ جديدةٌ تعيد الغفوة، وإلا تسليمُ الحمل */
    fun onRing(context: Context) {
        val now = System.currentTimeMillis()
        when (val decision = CallAlertPolicy.ringDue(CallGate.isInCall(context), now)) {
            is CallAlertPolicy.Decision.Defer -> scheduleRecheck(context, decision.recheckAtMillis)
            else -> deliver(context)
        }
    }

    /** تسليم الحمل — الخدمة الأمامية بالمسار العادي نفسه كأن التنبيه دقّ الآن */
    private fun deliver(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val payload = prefs.getString(KEY_PAYLOAD, null) ?: return
        prefs.edit().remove(KEY_PAYLOAD).apply()
        runCatching {
            val json = JSONObject(payload)
            val intent = Intent(context, AlarmService::class.java)
                .putExtra(AlarmService.EXTRA_MODE, json.optString(AlarmService.EXTRA_MODE, AlarmService.MODE_REMINDER))
                .putExtra(AlarmService.EXTRA_PRAYER_NAME, json.optString(AlarmService.EXTRA_PRAYER_NAME, "الصلاة"))
                .putExtra(AlarmService.EXTRA_SOUND_ID, json.optString(AlarmService.EXTRA_SOUND_ID, "main"))
                .putExtra(AlarmService.EXTRA_CUSTOM_PATH, json.optString(AlarmService.EXTRA_CUSTOM_PATH, ""))
                .putExtra(AlarmService.EXTRA_NOTE, json.optString(AlarmService.EXTRA_NOTE, ""))
                .putExtra(AlarmService.EXTRA_VIBRATE, json.optBoolean(AlarmService.EXTRA_VIBRATE, true))
                .putExtra(AlarmService.EXTRA_MUTE_START, json.optBoolean(AlarmService.EXTRA_MUTE_START, false))
                .putExtra(AlarmService.EXTRA_MUTE_MINUTES, json.optInt(AlarmService.EXTRA_MUTE_MINUTES, 10))
                .putExtra(
                    AlarmService.EXTRA_MUTE_CANCEL_ON_UNLOCK,
                    json.optBoolean(AlarmService.EXTRA_MUTE_CANCEL_ON_UNLOCK, true),
                )
            if (Build.VERSION.SDK_INT >= 26) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    private fun scheduleRecheck(context: Context, atMillis: Long) =
        setExactSafely(context, atMillis, buildPending(context, RECHECK_REQUEST_CODE, ACTION_RECHECK, "DEFERRED_RECHECK"))

    private fun scheduleRing(context: Context, atMillis: Long) =
        setExactSafely(context, atMillis, buildPending(context, RING_REQUEST_CODE, ACTION_RING, "DEFERRED_RING"))

    private fun buildPending(context: Context, requestCode: Int, action: String, kind: String): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(context, AdhanAlarmReceiver::class.java)
                .setAction(action)
                .putExtra(AdhanAlarmReceiver.EXTRA_KIND, kind),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    /** منبهٌ دقيقٌ بأمانٍ — عادة AlarmScheduler نفسها: بلا إذنٍ يعمل شبه دقيق */
    private fun setExactSafely(context: Context, atMillis: Long, pendingIntent: PendingIntent) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        try {
            if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pendingIntent)
            } else {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pendingIntent)
            }
        } catch (se: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pendingIntent)
        }
    }
}
