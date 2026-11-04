package com.sawan4art.noor.domain.adhan

import com.sawan4art.noor.domain.prayer.PrayerTimeKey
import java.time.DayOfWeek
import java.time.ZonedDateTime

/**
 * إعدادات الأذان التي يحتاجها بنّاء الخطة — قيمها الافتراضية هي
 * الافتراضيات المعتمدة (الإقامة بعد الأذان ١٠ دقائق، والجمعة أذان
 * مرتين: فاصل ١٠ دقائق بين الأذانين ثم الإقامة بعد ٣٠ دقيقة).
 */
data class AdhanPlanSettings(
    val adhanEnabled: Boolean = true,
    val iqamahEnabled: Boolean = false,
    val iqamahOffsetMinutes: Int = 10,
    val fridayDouble: Boolean = true,
    val fridayAdhanGapMinutes: Int = 10,
    val fridayIqamahGapMinutes: Int = 30,
    val muteEnabled: Boolean = false,
    val muteMinutes: Int = 10,
)

/** نوع حدث الأذان */
enum class AdhanEventKind { ADHAN, IQAMAH, MUTE_RESTORE }

/**
 * حدث مُجدول في الوقت المحدد — أذان أو إقامة أو استعادة صوت الهاتف
 * بعد الكتم. `prayerKey` فارغ لأحداث الاستعادة (لا صلاة لها).
 */
data class AdhanEvent(
    val kind: AdhanEventKind,
    val at: ZonedDateTime,
    val prayerKey: PrayerTimeKey? = null,
    val name: String = "",
    /** هل هذا الأذان الثاني في الجمعة (يحمل وقت الإقامة المحدد سلفاً) */
    val carriesIqamah: Boolean = false,
    val iqamahAt: ZonedDateTime? = null,
)

/**
 * بنّاء خطة أحداث صلاة واحدة — النقل الأمين لمنطق النسخة المرجعية:
 *
 * - **باقي الأيام**: أذان واحد عند وقت الصلاة، والإقامة (إن مفعّلة)
 *   بعده بعدة الدقائق المحددة، واستعادة الصوت بعد الإقامة بمُدة الكتم.
 * - **يوم الجمعة** (صلاة الظهر فقط): أذانان ثم الإقامة — الفاصل بين
 *   الأذان الأول والثاني يحدده المستخدم (افتراضي ١٠ دقائق)، والفاصل
 *   بين الأذان الثاني والإقامة يحدده (افتراضي ٣٠ دقيقة)، والاستعادة
 *   بعدها بمُدة الكتم.
 * - **الشروق ليست صلاة** — بلا أذان (وقت شمس لا كِران).
 */
object AdhanPlan {

    fun eventsForPrayer(
        key: PrayerTimeKey,
        prayerName: String,
        prayerTime: ZonedDateTime,
        settings: AdhanPlanSettings,
    ): List<AdhanEvent> {
        if (!settings.adhanEnabled) return emptyList()
        // الشروق وقت شمس لا أذان فيه
        if (key == PrayerTimeKey.SUNRISE) return emptyList()

        val isFridayDhuhr =
            key == PrayerTimeKey.DHUHR && prayerTime.dayOfWeek == DayOfWeek.FRIDAY

        val events = mutableListOf<AdhanEvent>()

        if (isFridayDhuhr && settings.fridayDouble) {
            val second = prayerTime.plusMinutes(settings.fridayAdhanGapMinutes.toLong())
            val iqamah = second.plusMinutes(settings.fridayIqamahGapMinutes.toLong())
            events += AdhanEvent(
                kind = AdhanEventKind.ADHAN,
                at = prayerTime,
                prayerKey = key,
                name = prayerName,
            )
            events += AdhanEvent(
                kind = AdhanEventKind.ADHAN,
                at = second,
                prayerKey = key,
                name = prayerName,
                carriesIqamah = settings.iqamahEnabled,
                iqamahAt = if (settings.iqamahEnabled) iqamah else null,
            )
            if (settings.iqamahEnabled) {
                events += AdhanEvent(
                    kind = AdhanEventKind.IQAMAH,
                    at = iqamah,
                    prayerKey = key,
                    name = prayerName,
                )
            }
            // استعادة الصوت بعد الإقامة (أو بعد الأذان الثاني إن بلا إقامة)
            if (settings.muteEnabled) {
                val restoreBase = if (settings.iqamahEnabled) iqamah else second
                events += AdhanEvent(
                    kind = AdhanEventKind.MUTE_RESTORE,
                    at = restoreBase.plusMinutes(settings.muteMinutes.toLong()),
                    prayerKey = key,
                    name = prayerName,
                )
            }
            return events
        }

        // المسار العادي: أذان واحد (+ إقامة اختيارية + استعادة كتم)
        events += AdhanEvent(
            kind = AdhanEventKind.ADHAN,
            at = prayerTime,
            prayerKey = key,
            name = prayerName,
        )
        if (settings.iqamahEnabled) {
            val iqamah = prayerTime.plusMinutes(settings.iqamahOffsetMinutes.toLong())
            events += AdhanEvent(
                kind = AdhanEventKind.IQAMAH,
                at = iqamah,
                prayerKey = key,
                name = prayerName,
            )
            if (settings.muteEnabled) {
                events += AdhanEvent(
                    kind = AdhanEventKind.MUTE_RESTORE,
                    at = iqamah.plusMinutes(settings.muteMinutes.toLong()),
                    prayerKey = key,
                    name = prayerName,
                )
            }
        } else if (settings.muteEnabled) {
            // بلا إقامة: الكتم ينتهي بعد الأذان بمُدة الكتم نفسها
            events += AdhanEvent(
                kind = AdhanEventKind.MUTE_RESTORE,
                at = prayerTime.plusMinutes(settings.muteMinutes.toLong()),
                prayerKey = key,
                name = prayerName,
            )
        }
        return events
    }
}
