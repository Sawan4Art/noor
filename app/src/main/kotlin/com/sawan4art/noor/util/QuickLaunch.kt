package com.sawan4art.noor.util

import android.content.Context
import com.sawan4art.noor.data.SettingsRepository
import com.sawan4art.noor.domain.prayer.PrayerMath
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * تجديد عدّاد «الوقت المتبقي» بإشعار التشغيل السريع — v1.80 (بلاغ
 * المالك 2026-11-21: «اضف ايقونه داخل شريط الاشعارات لعرض الوقت
 * المتبقي و فتح التطبيق بشكل سريع»).
 *
 * المنطق نقيٌّ قابلٌ للاختبار: من إعدادات الموقع والطريقة تُحسب
 * الصلاة القادمة **الحقيقية** (نفس [PrayerMath.nextPrayer] التي
 * تدير النقطة السائلة في الرئيسية) فيُسمّى الإشعار «متبقي حتى أذان
 * الفلان» بعدّادٍ تنازليٍّ حيٍّ نحو لحظته (خاصية usesChronometer —
 * نظام التشغيل يحدّثه بنفسه كل دقيقة بلا خدمةٍ ولا بطارية).
 *
 * يُستدعى عند: فتح التطبيق (onResume) وعند كل إعادة جدولة المنبهات
 * (AlarmScheduler.reschedule) فيتدحرج العدّاد من صلاةٍ لصلاةٍ طوال
 * اليوم حتى لو لم يُفتح التطبيق.
 */
object QuickLaunch {

    /** نتيجة الصلاة القادمة للإشعار — نقية كي تُختبر وحدةً */
    data class NextTarget(val prayerName: String, val atMillis: Long)

    /**
     * حساب الصلاة القادمة من الإعدادات الحالية — نقية بلا أندرويد:
     * نفس رياضيات الرئيسية (الموقع والطريقة والمذهب) فلا يختلف
     * عدّاد الإشعار عن نقطة الشاشة أبداً. تُرجع null لو تعذّر
     * الحساب (لا إعدادات بعد).
     */
    fun computeNextTarget(
        latitude: Double,
        longitude: Double,
        zone: ZoneId,
        method: com.sawan4art.noor.domain.prayer.CalculationMethod,
        isHanafi: Boolean,
        now: ZonedDateTime,
    ): NextTarget? {
        val times = PrayerMath.compute(
            now.toLocalDate(),
            latitude,
            longitude,
            zone,
            method,
            isHanafi,
        )
        val next = PrayerMath.nextPrayer(times, now)
        return NextTarget(next.arabicName, next.target.toInstant().toEpochMilli())
    }

    /** تجديد الإشعار من الإعدادات المحفوظة — للقراءة من خلفية أو واجهة */
    fun refreshFromSettings(context: Context) {
        runCatching {
            val settings = kotlinx.coroutines.runBlocking {
                SettingsRepository(context).settingsOnce()
            }
            val target = computeNextTarget(
                latitude = settings.latitude,
                longitude = settings.longitude,
                zone = ZoneId.systemDefault(),
                method = settings.method,
                isHanafi = settings.isHanafi,
                now = ZonedDateTime.now(ZoneId.systemDefault()),
            )
            if (target != null) {
                QuickLaunchNotification.refresh(
                    context,
                    target.prayerName,
                    target.atMillis,
                )
            }
        }
    }
}
