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
 * لقطة بيانات الويدجت — كل ما تحتاجه الأشكال الخمسة من مواقيت وتواريخ
 * وأذكار، تُحسب لحظة الرسم من إعدادات التطبيق نفسها (الموقع والطريقة
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
    /** التاريخ الهجري بصيغته العربية */
    val hijri: String,
    /** التاريخ الميلادي بصيغته العربية */
    val gregorian: String,
    /** ذكر اليوم — يدور بالأيام في ويدجت الذكر */
    val zikr: String,
) {
    data class Row(val name: String, val time: String, val isNext: Boolean, val isPast: Boolean)

    companion object {

        /** أذكار ويدجت الذكر — دورة أيام السنة، عبارات مأثورة قصيرة */
        val ZIKRS = listOf(
            "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ، سُبْحَانَ اللَّهِ الْعَظِيمِ",
            "لَا إِلَٰهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ",
            "اللَّهُمَّ صَلِّ وَسَلِّمْ عَلَى نَبِيِّنَا مُحَمَّدٍ",
            "أَسْتَغْفِرُ اللَّهَ الْعَظِيمَ وَأَتُوبُ إِلَيْهِ",
            "لَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللَّهِ",
            "الْحَمْدُ لِلَّهِ حَمْدًا كَثِيرًا طَيِّبًا مُبَارَكًا فِيهِ",
            "حَسْبِيَ اللَّهُ وَنِعْمَ الْوَكِيلُ",
        )

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
            val zikr = ZIKRS[(today.dayOfYear - 1).mod(ZIKRS.size)]
            return WidgetSnapshot(
                nextName = next.arabicName,
                nextTimeText = TimeFormat.prayerTime(next.target),
                remainingSeconds = next.remainingSeconds.toLong(),
                rows = rows,
                city = settings.city,
                hijri = Hijri.of(today).formatted,
                gregorian = Hijri.gregorianArabic(today),
                zikr = zikr,
            )
        }
    }
}

/** قاعدة Chronometer التنازلي — زمن الإقلاع + المتبقي (الويدجت يعدّ حياً) */
internal fun chronometerBase(remainingSeconds: Long, elapsedRealtime: Long): Long =
    elapsedRealtime + (remainingSeconds * 1000L).coerceAtLeast(0L)
