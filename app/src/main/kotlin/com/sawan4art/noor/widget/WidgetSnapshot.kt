package com.sawan4art.noor.widget

import android.content.Context
import com.sawan4art.noor.data.SettingsRepository
import com.sawan4art.noor.domain.prayer.PrayerMath
import com.sawan4art.noor.domain.prayer.PrayerTimeKey
import com.sawan4art.noor.domain.prayer.PrayerTimes
import com.sawan4art.noor.util.Hijri
import com.sawan4art.noor.util.TimeFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * لقطة بيانات الويدجت — كل ما تحتاجه الويدجت الواحدة من مواقيت
 * وتواريخ، تُحسب لحظة الرسم من إعدادات التطبيق نفسها (الموقع والطريقة
 * والمذهب) فتطابق الشاشة الرئيسية عينها.
 */
data class WidgetSnapshot(
    /** اسم الصلاة القادمة كما يظهر في الرئيسية (الجمعة باسمها يومها) */
    val nextName: String,
    /** نص وقتها ٤:٣٩ م */
    val nextTimeText: String,
    /** ثوانِ الباقية حتى أذانها */
    val remainingSeconds: Long,
    /** أوقات اليوم الستة — (الاسم، النص، هل هي القادمة، هل انقضت) */
    val rows: List<Row>,
    /** اسم المدينة المحفوظ */
    val city: String,
    /** التاريخ الهجري — بصيغه ما شاء التخصيص */
    val hijriDate: Hijri.HijriDate,
    /** التاريخ الميلادي — بصيغه ما شاء التخصيص */
    val gregorianDate: LocalDate,
) {
    data class Row(val name: String, val time: String, val isNext: Boolean, val isPast: Boolean)

    companion object {

        /** حساب اللقطة من إعدادات التطبيق — يُستدعى من ويدجتات النظام مباشرة */
        fun compute(context: Context, now: ZonedDateTime = ZonedDateTime.now()): WidgetSnapshot {
            val settings = runCatching {
                kotlinx.coroutines.runBlocking { SettingsRepository(context).settingsOnce() }
            }.getOrElse { com.sawan4art.noor.data.NoorSettingsState() }
            val today: LocalDate = now.toLocalDate()
            val times: PrayerTimes = runCatching {
                PrayerMath.compute(
                    today, settings.latitude, settings.longitude,
                    now.zone, settings.method, settings.isHanafi,
                )
            }.getOrElse {
                // فشل نادر في الحساب — مواقيت القاهرة الافتراضية كي لا يظهر ويدجت فارغاً
                PrayerMath.compute(today, 30.0444, 31.2357, now.zone,
                    com.sawan4art.noor.domain.prayer.CalculationMethod.EGYPT, false)
            }
            val next = PrayerMath.nextPrayer(times, now)
            val rows = PrayerTimeKey.entries.map { key ->
                val target = times[key]
                Row(
                    name = PrayerMath.displayName(key, today),
                    time = TimeFormat.prayerTime(target),
                    isNext = key == next.key,
                    isPast = target.isBefore(now) && key != next.key,
                )
            }
            return WidgetSnapshot(
                nextName = next.arabicName,
                nextTimeText = TimeFormat.prayerTime(next.target),
                remainingSeconds = next.remainingSeconds.toLong(),
                rows = rows,
                city = settings.city,
                hijriDate = Hijri.of(today),
                gregorianDate = today,
            )
        }

        /**
         * الصلاة «الحالية» في الصفوف — آخر صلاةٍ انقضت أذانُها ولم تأتِ
         * القادمة (مقام فقاعة التحديد بلاغ المالك: «على الصلاه التاليه
         * او الحاليه او الاثنين مع بعض»).
         */
        fun currentRowIndex(rows: List<Row>): Int =
            rows.indexOfLast { it.isPast }
    }
}

/** قاعدة Chronometer التنازلي — زمن الإقلاع + المتبقي (الويدجت يعدّ حياً) */
internal fun chronometerBase(remainingSeconds: Long, elapsedRealtime: Long): Long =
    elapsedRealtime + (remainingSeconds * 1000L).coerceAtLeast(0L)
