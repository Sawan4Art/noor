package com.sawan4art.noor.domain.adhan

import com.sawan4art.noor.domain.prayer.PrayerTimeKey
import java.time.DayOfWeek
import java.time.ZonedDateTime

/**
 * إعدادات الأذان التي يحتاجها بنّاء الخطة — قيمها الافتراضية هي
 * الافتراضيات المعتمدة (تحديث المالك 2026-11-06: فاصل الإقامة خانة
 * كتابة لكل صلاة على حدة — كل الصلوات ١٠ دقائق ماعاد الفجر ١٥ دقيقة —
 * والجمعة استثناء: فاصل ١٠ دقائق بين الأذانين ثم الإقامة بعد ٣٠ دقيقة).
 */
data class AdhanPlanSettings(
    val adhanEnabled: Boolean = true,
    val iqamahEnabled: Boolean = false,
    /** فاصل الأذان←الإقامة لكل صلاة على حدة (خانة كتابة لكل واحدة) */
    val gapFajrMinutes: Int = 15,
    val gapDhuhrMinutes: Int = 10,
    val gapAsrMinutes: Int = 10,
    val gapMaghribMinutes: Int = 10,
    val gapIshaMinutes: Int = 10,
    val fridayDouble: Boolean = true,
    val fridayAdhanGapMinutes: Int = 10,
    val fridayIqamahGapMinutes: Int = 30,
    val muteEnabled: Boolean = false,
) {
    /** فاصل الصلاة المطلوبة — الفجر استثناء بـ١٥ والباقي بـ١٠ افتراضياً */
    fun gapFor(key: PrayerTimeKey): Int = when (key) {
        PrayerTimeKey.FAJR -> gapFajrMinutes
        PrayerTimeKey.DHUHR -> gapDhuhrMinutes
        PrayerTimeKey.ASR -> gapAsrMinutes
        PrayerTimeKey.MAGHRIB -> gapMaghribMinutes
        PrayerTimeKey.ISHA -> gapIshaMinutes
        PrayerTimeKey.SUNRISE -> 0
    }
}

/** نوع حدث الأذان */
enum class AdhanEventKind { ADHAN, IQAMAH, REMINDER }

/**
 * وضع الصوت وعقده النصي — درس alpha60 (بلاغ المالك: «صوت الاقامه لا
 * يعمل بل بالعكس يتم تشغيل صوت من اصوات الاذان»): نية المستقبِل كانت
 * تحمل اسم النوع بحروفه الكبيرة (IQAMAH — اسم العَلَم AdhanEventKind)
 * بينما عقد الخدمة «iqamah» بالصغيرة، فسقطت المقارنة دائماً على
 * الأذان وصار صوت الإقامة صوتَ أذان! فالموحِّد هنا نقطة الحقيقة
 * الواحدة: أي نصٍّ بأي حالةٍ يعود بالوضع المتعارف عليه بالصغيرة،
 * وكلٌّ من المستقبِل والخدمة وشاشة التنبيه يمرّ عليه.
 */
object AdhanMode {

    const val ADHAN = "adhan"

    const val IQAMAH = "iqamah"

    /**
     * وضع التنبيه قبل/بعد الأذان (alpha61) — ثالث الأوضاع: يشغل صوتاً
     * بنصّ تنبيهٍ لا نصّ أذان («قلب الصلاة قريب» لا «حان وقت الصلاة»).
     */
    const val REMINDER = "reminder"

    /** أي نصٍّ غريبٍ أو فارغٍ أو بحروفٍ كبيرةٍ يعود وضعاً متعارفاً عليه */
    fun canonical(raw: String?): String = when {
        raw != null && raw.equals(IQAMAH, ignoreCase = true) -> IQAMAH
        raw != null && raw.equals(REMINDER, ignoreCase = true) -> REMINDER
        else -> ADHAN
    }
}

/**
 * حدث مُجدول في الوقت المحدد — أذان أو إقامة. `prayerKey` يحدد الصلاة.
 *
 * `startsMute` (تحديث المالك 2026-11-06): هل يبدأ كتم أصوات الهاتف بعد
 * انتهاء صوت هذا الحدث؟ الإقامة تبدأه (الكتم «بعد الاقامه» كما حدد
 * المالك)، والأذان يبدأه فقط إن كانت الإقامة مطفأة — وكل إدارة الكتم
 * (المُدة والاستعادة والمنبه الاحتياطي) في MuteManager وقت التنفيذ.
 */
data class AdhanEvent(
    val kind: AdhanEventKind,
    val at: ZonedDateTime,
    val prayerKey: PrayerTimeKey? = null,
    val name: String = "",
    /** هل هذا الأذان الثاني في الجمعة (يحمل وقت الإقامة المحدد سلفاً) */
    val carriesIqamah: Boolean = false,
    val iqamahAt: ZonedDateTime? = null,
    /** يبدأ كتم أصوات الهاتف بعد انتهاء صوت هذا الحدث */
    val startsMute: Boolean = false,
)

/**
 * بنّاء خطة أحداث صلاة واحدة — النقل الأمين لمنطق النسخة المرجعية:
 *
 * - **باقي الأيام**: أذان واحد عند وقت الصلاة، والإقامة (إن مفعّلة)
 *   بعده بفاصل الصلاة نفسها (خانة الكتابة الخاصة بها).
 * - **يوم الجمعة** (صلاة الظهر فقط): أذانان ثم الإقامة — الفاصل بين
 *   الأذان الأول والثاني خانته الخاصة (افتراضي ١٠ دقائق)، والفاصل
 *   بين الأذان الثاني والإقامة خانته الخاصة (افتراضي ٣٠ دقيقة).
 * - **الشروق ليست صلاة** — بلا أذان (وقت شمس لا كِران).
 * - **الكتم** عَلَم `startsMute` على الحدث الأخير قبل الصلاة: الإقامة
 *   إن مفعّلة وإلا الأذان — والاستعادة يتولاها MuteManager بعد المُدة
 *   المحددة فلا حاجة لحدث استعادة مجدول سلفاً.
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
                // بلا إقامة: الأذان الثاني هو آخر صوت قبل الصلاة — يبدأ الكتم
                startsMute = settings.muteEnabled && !settings.iqamahEnabled,
            )
            if (settings.iqamahEnabled) {
                events += AdhanEvent(
                    kind = AdhanEventKind.IQAMAH,
                    at = iqamah,
                    prayerKey = key,
                    name = prayerName,
                    startsMute = settings.muteEnabled,
                )
            }
            return events
        }

        // المسار العادي: أذان واحد (+ إقامة اختيارية بفاصل الصلاة نفسها)
        events += AdhanEvent(
            kind = AdhanEventKind.ADHAN,
            at = prayerTime,
            prayerKey = key,
            name = prayerName,
        )
        if (settings.iqamahEnabled) {
            val iqamah = prayerTime.plusMinutes(settings.gapFor(key).toLong())
            events += AdhanEvent(
                kind = AdhanEventKind.IQAMAH,
                at = iqamah,
                prayerKey = key,
                name = prayerName,
                startsMute = settings.muteEnabled,
            )
        } else if (settings.muteEnabled) {
            // بلا إقامة: الأذان هو آخر صوت — الكتم يبدأ بعد انتهائه
            events[0] = events[0].copy(startsMute = true)
        }
        return events
    }
}
