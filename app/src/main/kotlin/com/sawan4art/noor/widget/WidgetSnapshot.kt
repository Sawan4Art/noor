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
    /** أوقات اليوم الخمسة — بلا الشروق (alpha62) — (الاسم، النص، هل هي القادمة، هل انقضت) */
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

        /**
         * حساب اللقطة من إعدادات التطبيق — يُستدعى من ويدجتات النظام مباشرة.
         *
         * التحصين (alpha58 — بلاغ المالك: «الويدجت تظهر في التطبيقات
         * المصغرة لكن لا تعمل»): أي غلطةٍ غير متوقعة في أي حسابٍ ترجع
         * لقطةً افتراضية سليمة (مواقيت القاهرة) كي لا يظهر الويدجت
         * معطلاً ولا يُهدم تحديثه — أول المحاولة بالإعدادات الحية
         * فالاحتياط بالافتراضية فآخر العلاج لقطةٌ لا تنفجر أبداً.
         */
        fun compute(context: Context, now: ZonedDateTime = ZonedDateTime.now()): WidgetSnapshot {
            val primary = runCatching {
                val settings = kotlinx.coroutines.runBlocking {
                    SettingsRepository(context).settingsOnce()
                }
                computeFrom(settings, now)
            }
            primary.getOrNull()?.let { return it }
            return runCatching {
                computeFrom(com.sawan4art.noor.data.NoorSettingsState(), now)
            }.getOrElse { emptySnapshot(now) }
        }

        /** الحساب من إعداداتٍ معطاة — المكوّنات تُرمى فتُلتقط في [compute] */
        private fun computeFrom(
            settings: com.sawan4art.noor.data.NoorSettingsState,
            now: ZonedDateTime,
        ): WidgetSnapshot {
            val today: LocalDate = now.toLocalDate()
            val times: PrayerTimes = PrayerMath.compute(
                today, settings.latitude, settings.longitude,
                now.zone, settings.method, settings.isHanafi,
            )
            val next = PrayerMath.nextPrayer(times, now)
            // alpha62 — بلاغ المالك: «ازل توقيت الشروق من الويدجت تماماً»
            // فالخلايا خمسٌ: الفجر والظهر والعصر والمغرب والعشاء — والظهر
            // يوم الجمعة باسمها «الجمعة» من displayName نفسها.
            return WidgetSnapshot(
                nextName = next.arabicName,
                nextTimeText = TimeFormat.prayerTime(next.target),
                remainingSeconds = next.remainingSeconds.toLong(),
                rows = rows(times, now),
                city = settings.city,
                hijriDate = Hijri.of(today),
                gregorianDate = today,
            )
        }

        /**
         * صفوف مواقيت الويدجت من حسابٍ معطى — دالةٌ نقية تُختبر آلياً:
         * خمسُ صلواتٍ بلا الشروق، وأسمُّ الظهر يوم الجمعة «الجمعة».
         */
        internal fun rows(times: PrayerTimes, now: ZonedDateTime): List<Row> {
            val nextKey = PrayerMath.nextPrayer(times, now).key
            return PRAYER_KEYS.map { key ->
                val target = times[key]
                Row(
                    name = PrayerMath.displayName(key, now.toLocalDate()),
                    time = TimeFormat.prayerTime(target),
                    isNext = key == nextKey,
                    isPast = target.isBefore(now) && key != nextKey,
                )
            }
        }

        /** مفاتيح الصلوات الخمس الظاهرة في الويدجت — الشروق مستثناة */
        internal val PRAYER_KEYS: List<PrayerTimeKey> =
            PrayerTimeKey.entries.filter { it != PrayerTimeKey.SUNRISE }

        /** آخر العلاج — لقطةٌ شبه فارغة تركيبها سليم لا تنفجر أبداً */
        private fun emptySnapshot(now: ZonedDateTime): WidgetSnapshot = WidgetSnapshot(
            nextName = "",
            nextTimeText = "",
            remainingSeconds = 0,
            rows = PRAYER_KEYS.map {
                Row(
                    name = PrayerMath.displayName(it, now.toLocalDate()),
                    time = "",
                    isNext = false,
                    isPast = false,
                )
            },
            city = "",
            hijriDate = Hijri.HijriDate(1, 1, "محرم", 1),
            gregorianDate = now.toLocalDate(),
        )

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

/** صيغة العدّاد الحي — عقد alpha58: تُمرَّر لـ setChronometer بمنادى started=true */
internal const val COUNTDOWN_FORMAT = "متبقي %s حتى الأذان"
