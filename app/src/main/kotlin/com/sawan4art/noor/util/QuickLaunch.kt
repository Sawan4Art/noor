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
            // الإشعار الدائم — بمفتاحه المستقل (v1.82): يظهر ويُغلق
            // بحسب الإعداد هنا فلا يعتمد على المتصلين
            if (settings.quickNotification) {
                val target = computeNextTarget(
                    latitude = settings.latitude,
                    longitude = settings.longitude,
                    zone = ZoneId.systemDefault(),
                    method = settings.method,
                    isHanafi = settings.isHanafi,
                    now = ZonedDateTime.now(ZoneId.systemDefault()),
                )
                if (target != null) {
                    QuickLaunchNotification.show(
                        context,
                        target.prayerName,
                        target.atMillis,
                    )
                }
            } else {
                QuickLaunchNotification.dismiss(context)
            }
            // البلاطة السريعة — مفتاحُها المستقلُّ الآخر؛ طلبُ إعادة
            // الرسم يفزّع onStartListening فيقرأ quickTile فيصير
            // نشطةً أو «غير متاحة» بلا تأثيرٍ من مفتاح الإشعار
            runCatching {
                android.service.quicksettings.TileService.requestListeningState(
                    context,
                    android.content.ComponentName(context, QuickLaunchTileService::class.java),
                )
            }
        }
    }

    /**
     * حساب الصلاة القادمة من الإعدادات المحفوظة — نسخة الحجب للتخدم
     * من مستقبِلات البلاطة (runBlocking قصير: رياضياتٌ صرفة لا شبكة)
     */
    fun computeNextTargetBlocking(context: Context): NextTarget? {
        val settings = kotlinx.coroutines.runBlocking {
            SettingsRepository(context).settingsOnce()
        }
        return computeNextTarget(
            latitude = settings.latitude,
            longitude = settings.longitude,
            zone = ZoneId.systemDefault(),
            method = settings.method,
            isHanafi = settings.isHanafi,
            now = ZonedDateTime.now(ZoneId.systemDefault()),
        )
    }

    /**
     * نص سطر البلاطة — «أذان العصر ٤:١٥ م» — v1.85 (بلاغ المالك:
     * «خلي بلاطه الاعدادات السريعه تعرض وقت الصلاه القادمه بدل عرض
     * الوقت المتبقي») — نقيةٌ مع زمنِ
     * الآن ومنطقةٍ زمنيةٍ ممرّرين كي تُختبر عقودُه: المتبقي تشوشه
     * قراءته الحية (رقمٌ ينقص كل دقيقةٍ والبلاطة تُقرأ لحظةَ السحب)،
     * أما وقت الأذان نفسه فثابتٌ يُقرأ مرةً ويفهم مرةً — وصيغته صيغة
     * المواقيت نفسها بالأرقام العربية وقرابةُ العرض لأعلى الدقيقة
     * (العرض المتأخر أسلم من المقدَّم — قاعدة alpha63 في
     * TimeFormat.prayerTime). اللحظة الماضية أو فارغ الاسم يعطي النص
     * العام (والنظام يطلب الرسم لحظة السحب فلا يكاد يقع).
     */
    fun tileSubtitle(
        prayerName: String,
        atMillis: Long,
        @Suppress("UNUSED_PARAMETER") nowMillis: Long = System.currentTimeMillis(),
        zone: java.time.ZoneId = java.time.ZoneId.systemDefault(),
    ): String {
        if (prayerName.isBlank() || atMillis <= nowMillis) {
            return "وقت الصلاة القادمة — اضغط للفتح"
        }
        val time = TimeFormat.prayerTime(
            java.time.Instant.ofEpochMilli(atMillis).atZone(zone),
        )
        // الأرقام الهندية قسراً — بيئات CLDR الحديثة تجعلها لاتينية
        return "أذان $prayerName " + TimeFormat.arabicDigits(time)
    }
}
