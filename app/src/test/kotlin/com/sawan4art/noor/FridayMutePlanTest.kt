package com.sawan4art.noor

import com.sawan4art.noor.domain.adhan.AdhanEventKind
import com.sawan4art.noor.domain.adhan.AdhanPlan
import com.sawan4art.noor.domain.adhan.AdhanPlanSettings
import com.sawan4art.noor.domain.prayer.PrayerTimeKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.Month
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * عقود كتم الجمعة — v1.82 (بلاغ المالك: «كل كتم الاصوات في يوم
 * الجمعه لصلاه الجمعه يكون مخصص بعد الاذان الاول بمده يحددها
 * المستخدم و افتراضي يكون ساعه ونص و قابله للتخصيص حتى ٣ ساعات»):
 *
 * ١. الجمعة بالأذان المزدوج: عَلَم الكتم ومدته المخصصة على **الأذان
 *    الأول** وحده، والأذان الثاني والإقامة بلا كتمٍ فلا يعاد البدء.
 * ٢. الجمعة بلا أذانٍ مزدوج (الأذان الوحيد هو الأول): عَلَمُه عليها
 *    كذلك بمُدتها المخصصة لا بمُدة الإقامة العادية.
 * ٣. باقي الصلوات كما كانت: الكتم بعد الإقامة (أو الأذان) بالمُدة
 *    العادية — الجمعة استثناء لا غيرها.
 * ٤. الافتراضي ٩٠ دقيقة والحد الأقصى ١٨٠ — حصر AdhanGaps.clampFridayMute.
 */
class FridayMutePlanTest {

    private val zone: ZoneId = ZoneId.of("Africa/Cairo")
    /** جمعةٌ حقيقية: ١٣ فبراير ٢٠٢٦ — ١٢:٣٠ ظهراً */
    private val fridayDhuhr: ZonedDateTime = ZonedDateTime.of(
        LocalDateTime.of(2026, Month.FEBRUARY, 13, 12, 30), zone,
    )

    init {
        assertTrue("الوقت الاختباري يجب أن يكون جمعة", fridayDhuhr.dayOfWeek == DayOfWeek.FRIDAY)
    }

    private fun settings(
        iqamah: Boolean = true,
        mute: Boolean = true,
        fridayMute: Int = 90,
    ) = AdhanPlanSettings(
        adhanEnabled = true,
        iqamahEnabled = iqamah,
        fridayDouble = true,
        muteEnabled = mute,
        muteMinutes = 10,
        fridayMuteMinutes = fridayMute,
    )

    @Test
    fun `الجمعة المزدوجة — الكتم على الأذان الأول وحده بمدته المخصصة`() {
        val events = AdhanPlan.eventsForPrayer(
            key = PrayerTimeKey.DHUHR,
            prayerName = "الظهر",
            prayerTime = fridayDhuhr,
            settings = settings(),
        )
        val adhans = events.filter { it.kind == AdhanEventKind.ADHAN }
        assertEquals("أذانا الجمعة مزدوجان", 2, adhans.size)
        val first = adhans[0]
        assertTrue("الأذان الأول يبدأ الكتم", first.startsMute)
        assertEquals("مدته المخصصة لا العادية", 90, first.muteMinutes)
        val second = adhans[1]
        assertFalse("الأذان الثاني لا يعيد الكتم", second.startsMute)
        val iqamah = events.first { it.kind == AdhanEventKind.IQAMAH }
        assertFalse("الإقامة لا تعيد الكتم في الجمعة", iqamah.startsMute)
    }

    @Test
    fun `الجمعة بلا مزدوج — الأذان الوحيد هو الأول يكتم بمُدتها`() {
        val events = AdhanPlan.eventsForPrayer(
            key = PrayerTimeKey.DHUHR,
            prayerName = "الظهر",
            prayerTime = fridayDhuhr,
            settings = settings().copy(fridayDouble = false),
        )
        val adhan = events.first { it.kind == AdhanEventKind.ADHAN }
        assertTrue("الأذان الأول (الوحيد) يبدأ الكتم", adhan.startsMute)
        assertEquals("المدة المخصصة للجمعة", 90, adhan.muteMinutes)
        val iqamah = events.filter { it.kind == AdhanEventKind.IQAMAH }
        assertTrue("لا إعادة كتمٍ من الإقامة", iqamah.all { !it.startsMute })
    }

    @Test
    fun `باقي الصلوات — الكتم بعد الإقامة بالمُدة العادية كما كان`() {
        val tuesdayDhuhr = fridayDhuhr.plusDays(3) // ثلاثاء ١٦ فبراير
        val events = AdhanPlan.eventsForPrayer(
            key = PrayerTimeKey.DHUHR,
            prayerName = "الظهر",
            prayerTime = tuesdayDhuhr,
            settings = settings(),
        )
        assertTrue("الثلاثاء ليس جمعة", tuesdayDhuhr.dayOfWeek != DayOfWeek.FRIDAY)
        val adhan = events.first { it.kind == AdhanEventKind.ADHAN }
        assertFalse("الأذان العادي لا يبدأ الكتم مع الإقامة المفعلة", adhan.startsMute)
        val iqamah = events.first { it.kind == AdhanEventKind.IQAMAH }
        assertTrue("الإقامة تبدأ الكتم", iqamah.startsMute)
        assertEquals("المُدة العادية ١٠ د", 10, iqamah.muteMinutes)
    }

    @Test
    fun `حصر المدة — الافتراضي ٩٠ والحد الأقصى ١٨٠`() {
        assertEquals(90, com.sawan4art.noor.domain.adhan.AdhanGaps.DEFAULT_FRIDAY_MUTE_MINUTES)
        assertEquals(180, com.sawan4art.noor.domain.adhan.AdhanGaps.FRIDAY_MUTE_MAX_MINUTES)
        assertEquals(90, com.sawan4art.noor.domain.adhan.AdhanGaps.clampFridayMute(90))
        assertEquals(1, com.sawan4art.noor.domain.adhan.AdhanGaps.clampFridayMute(0))
        assertEquals(180, com.sawan4art.noor.domain.adhan.AdhanGaps.clampFridayMute(300))
        assertEquals(180, com.sawan4art.noor.domain.adhan.AdhanGaps.clampFridayMute(180))
        // الجمعة تستأذن ما فوق ١٢٠ التي تحصر الفواصل العادية
        assertFalse(com.sawan4art.noor.domain.adhan.AdhanGaps.isGapInputValid(150))
        assertTrue(
            com.sawan4art.noor.domain.adhan.AdhanGaps.isGapInputValid(
                150,
                com.sawan4art.noor.domain.adhan.AdhanGaps.FRIDAY_MUTE_MAX_MINUTES,
            ),
        )
    }

    @Test
    fun `الكتم مطفأ — لا عَلَم على أي حدثٍ في الجمعة`() {
        val events = AdhanPlan.eventsForPrayer(
            key = PrayerTimeKey.DHUHR,
            prayerName = "الظهر",
            prayerTime = fridayDhuhr,
            settings = settings(mute = false),
        )
        assertTrue("بلا كتمٍ إطلاقاً", events.none { it.startsMute })
    }
}
