package com.sawan4art.noor.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.sawan4art.noor.data.SettingsRepository
import com.sawan4art.noor.domain.adhan.AdhanEventKind
import com.sawan4art.noor.domain.adhan.AdhanPlan
import com.sawan4art.noor.domain.adhan.AdhanPlanSettings
import com.sawan4art.noor.domain.prayer.PrayerMath
import com.sawan4art.noor.domain.prayer.PrayerTimeKey
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * مُجدول منبهات الأذان — قلب مرحلة الأذان:
 *
 * يحسب مواقيت اليومين القادمين من إعدادات الموقع والطريقة، ويبني من
 * كل صلاة أحداثها (أذان/إقامة/استعادة كتم) وفق تفضيلات المستخدم —
 * وضمنهم أذانا الجمعة المزدوج بالفواصل المحددة — ثم يسجّلها منبهات
 * دقيقة توقظ الجهاز والشاشة مطفأة.
 *
 * - المنبهات تُعاد جدولتها عند أي تغيير في الإعدادات المؤثرة، وعند
 *   كل فتح للتطبيق، وبعد إعادة تشغيل الهاتف، وعندما يدق منبه
 *   «التجديد اليومي» قرب منتصف الليل فيبقى الأذان حياً ولو لم
 *   يفتح المستخدم التطبيق أبداً.
 * - كل جدولة تُلغي المنبهات القديمة أولاً (نطاق أرقام الطلبات ثابت
 *   ومطابق لنطاق الإلغاء — درس النطاقات من النسخة المرجعية).
 */
object AlarmScheduler {

    /** نطاق أرقام طلبات المنبهات — ١٢٠ فتحة تكفي يومين من الأحداث */
    private const val FIRST_REQUEST_CODE = 3000
    private const val SLOTS = 120
    /** رمز طلب منبه التجديد اليومي — خارج نطاق الأحداث كي لا يُلغى بغيره */
    private const val REFRESH_REQUEST_CODE = 2900

    /** الحد الأقصى لمُدة تشغيل الخدمة (أمان لو فشل اكتمال المشغّل) */
    const val MAX_PLAY_MINUTES: Long = 8

    fun reschedule(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        cancelAll(context, am)

        val repo = SettingsRepository(context)
        val prefs = kotlinx.coroutines.runBlocking { repo.adhanPrefsOnce() } ?: return
        val settings = kotlinx.coroutines.runBlocking { repo.settingsOnce() }

        if (!prefs.enabled) return

        val zone = ZoneId.systemDefault()
        val now = ZonedDateTime.now(zone)
        val plan = AdhanPlanSettings(
            adhanEnabled = prefs.enabled,
            iqamahEnabled = prefs.iqamahEnabled,
            gapFajrMinutes = prefs.gapFajrMinutes,
            gapDhuhrMinutes = prefs.gapDhuhrMinutes,
            gapAsrMinutes = prefs.gapAsrMinutes,
            gapMaghribMinutes = prefs.gapMaghribMinutes,
            gapIshaMinutes = prefs.gapIshaMinutes,
            fridayDouble = prefs.fridayDouble,
            fridayAdhanGapMinutes = prefs.fridayAdhanGapMinutes,
            fridayIqamahGapMinutes = prefs.fridayIqamahGapMinutes,
            muteEnabled = prefs.muteEnabled,
        )

        // مواقيت اليوم + الغد + بعده — ثلاثة أيام تكفي حتى لو لم يُفتح التطبيق
        val events = buildList {
            for (dayOffset in 0..2L) {
                val date: LocalDate = now.toLocalDate().plusDays(dayOffset)
                val times = PrayerMath.compute(
                    date = date,
                    lat = settings.latitude,
                    lng = settings.longitude,
                    zone = zone,
                    method = settings.method,
                    isHanafi = settings.isHanafi,
                )
                for (key in PrayerTimeKey.entries) {
                    val t = times[key]
                    if (t.isBefore(now)) continue
                    addAll(
                        AdhanPlan.eventsForPrayer(
                            key = key,
                            prayerName = PrayerMath.displayName(key, date),
                            prayerTime = t,
                            settings = plan,
                        ),
                    )
                }
            }
        }.sortedBy { it.at.toInstant().toEpochMilli() }

        var slot = 0
        for (event in events) {
            if (slot >= SLOTS) break
            val atMillis = event.at.toInstant().toEpochMilli()
            val pi = buildEventPendingIntent(context, FIRST_REQUEST_CODE + slot, event) ?: continue
            setExactSafely(am, atMillis, pi)
            slot++
        }

        // التجديد اليومي قرب منتصف الليل: يعيد بناء الأحداث لل أيام القادمة
        var refresh = now.toLocalDate().plusDays(1).atTime(0, 7).atZone(zone)
        if (!refresh.isAfter(now)) refresh = refresh.plusDays(1)
        setExactSafely(am, refresh.toInstant().toEpochMilli(), buildRefreshPendingIntent(context))
    }

    /** إلغاء كل منبهات الأذان — النطاق كله وليس القديم فقط (درس النطاقات) */
    fun cancelAll(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        cancelAll(context, am)
    }

    private fun cancelAll(context: Context, am: AlarmManager) {
        for (code in FIRST_REQUEST_CODE until FIRST_REQUEST_CODE + SLOTS) {
            buildEventPendingIntent(context, code, null)?.let { am.cancel(it) }
        }
        am.cancel(buildRefreshPendingIntent(context))
    }

    /** إعداد منبه دقيق — وبغير الإذن يعمل شبه دقيق (فرق ثوانٍ) بدل الانهيار */
    private fun setExactSafely(am: AlarmManager, atMillis: Long, pi: PendingIntent) {
        try {
            if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pi)
            } else {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pi)
            }
        } catch (se: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pi)
        }
    }

    /**
     * بناء PendingIntent لحدث — بحمل فارغ يُستخدم للإلغاء فقط
     * (يجب أن يطابق بنية الحمل الحقيقي حتى يلغي النظام المنبه الصحيح).
     */
    private fun buildEventPendingIntent(
        context: Context,
        requestCode: Int,
        event: com.sawan4art.noor.domain.adhan.AdhanEvent?,
    ): PendingIntent? {
        val intent = Intent(context, AdhanAlarmReceiver::class.java).setAction(
            "com.sawan4art.noor.ADHAAN_EVENT_$requestCode",
        )
        if (event != null) {
            intent
                .putExtra(AdhanAlarmReceiver.EXTRA_KIND, event.kind.name)
                .putExtra(AdhanAlarmReceiver.EXTRA_NAME, event.name)
                .putExtra(AdhanAlarmReceiver.EXTRA_IQAMAH_AT, event.iqamahAt?.toInstant()?.toEpochMilli() ?: 0L)
                .putExtra(AdhanAlarmReceiver.EXTRA_MUTE_START, event.startsMute)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, requestCode, intent, flags)
    }

    private fun buildRefreshPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, AdhanAlarmReceiver::class.java)
            .setAction("com.sawan4art.noor.ADHAAN_REFRESH")
            .putExtra(AdhanAlarmReceiver.EXTRA_KIND, "REFRESH")
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, REFRESH_REQUEST_CODE, intent, flags)
    }
}
