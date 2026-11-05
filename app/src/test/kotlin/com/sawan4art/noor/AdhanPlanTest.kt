package com.sawan4art.noor

import com.sawan4art.noor.domain.adhan.AdhanEventKind
import com.sawan4art.noor.domain.adhan.AdhanPlan
import com.sawan4art.noor.domain.adhan.AdhanPlanSettings
import com.sawan4art.noor.domain.prayer.PrayerTimeKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * اختبارات منطق خطة الأذان — فاصل الإقامة **لكل صلاة على حدة** (خانة
 * كتابة)، والجمعة استثناء بأذانين، وعَلَم بداية الكتم بعد الإقامة
 * (توجيه المالك 2026-11-06).
 */
class AdhanPlanTest {

    private val zone: ZoneId = ZoneId.of("Africa/Cairo")

    /** أقرب جمعة قادمة على الساعة ١٢:٠٠ ظهراً */
    private fun nextFridayNoon(): ZonedDateTime {
        var date = LocalDate.now(zone)
        while (date.dayOfWeek != DayOfWeek.FRIDAY) date = date.plusDays(1)
        return ZonedDateTime.of(date, LocalTime.NOON, zone)
    }

    /** ثلاثاء قادم (يوم غير الجمعة مضمون) على الساعة ١٢:٠٠ */
    private fun nextTuesdayNoon(): ZonedDateTime {
        var date = LocalDate.now(zone)
        while (date.dayOfWeek != DayOfWeek.TUESDAY) date = date.plusDays(1)
        return ZonedDateTime.of(date, LocalTime.NOON, zone)
    }

    private val defaults = AdhanPlanSettings()

    @Test
    fun `الأذان المعطل يعطي بلا أحداث إطلاقا`() {
        val events = AdhanPlan.eventsForPrayer(
            PrayerTimeKey.FAJR, "الفجر", nextTuesdayNoon(),
            defaults.copy(adhanEnabled = false),
        )
        assertTrue(events.isEmpty())
    }

    @Test
    fun `الشروق بلا أذان - وقت شمس لا كران`() {
        val events = AdhanPlan.eventsForPrayer(
            PrayerTimeKey.SUNRISE, "الشروق", nextTuesdayNoon(), defaults,
        )
        assertTrue(events.isEmpty())
    }

    @Test
    fun `الافتراضي - أذان واحد بلا إقامة وبلا كتم`() {
        val t = nextTuesdayNoon()
        val events = AdhanPlan.eventsForPrayer(PrayerTimeKey.DHUHR, "الظهر", t, defaults)
        assertEquals(1, events.size)
        assertEquals(AdhanEventKind.ADHAN, events[0].kind)
        assertEquals(t, events[0].at)
        assertEquals("الظهر", events[0].name)
        assertEquals(false, events[0].startsMute)
    }

    @Test
    fun `فاصل كل صلاة مستقل - الفجر خمسة عشر والباقي عشرة افتراضيا`() {
        val t = nextTuesdayNoon()
        val settings = defaults.copy(iqamahEnabled = true)

        val fajr = AdhanPlan.eventsForPrayer(PrayerTimeKey.FAJR, "الفجر", t, settings)
        assertEquals(t.plusMinutes(15), fajr[1].at)

        for (key in listOf(
            PrayerTimeKey.DHUHR, PrayerTimeKey.ASR,
            PrayerTimeKey.MAGHRIB, PrayerTimeKey.ISHA,
        )) {
            val events = AdhanPlan.eventsForPrayer(key, "صلاة", t, settings)
            assertEquals(t.plusMinutes(10), events[1].at)
        }
    }

    @Test
    fun `تغيير فاصل صلاة لا يمس باقي الصلوات`() {
        val t = nextTuesdayNoon()
        val settings = defaults.copy(iqamahEnabled = true, gapAsrMinutes = 45)
        val asr = AdhanPlan.eventsForPrayer(PrayerTimeKey.ASR, "العصر", t, settings)
        assertEquals(t.plusMinutes(45), asr[1].at)
        val maghrib = AdhanPlan.eventsForPrayer(PrayerTimeKey.MAGHRIB, "المغرب", t, settings)
        assertEquals(t.plusMinutes(10), maghrib[1].at)
    }

    @Test
    fun `علم الكتم على الإقامة لا على الأذان`() {
        val t = nextTuesdayNoon()
        val settings = defaults.copy(iqamahEnabled = true, muteEnabled = true)
        val events = AdhanPlan.eventsForPrayer(PrayerTimeKey.MAGHRIB, "المغرب", t, settings)

        // حدثان فقط: أذان ثم إقامة — الاستعادة بمنبه MuteManager بعد المدة
        assertEquals(2, events.size)
        assertEquals(AdhanEventKind.ADHAN, events[0].kind)
        assertEquals(false, events[0].startsMute)
        assertEquals(AdhanEventKind.IQAMAH, events[1].kind)
        assertEquals(t.plusMinutes(10), events[1].at)
        assertEquals(true, events[1].startsMute)
    }

    @Test
    fun `بلا إقامة - الكتم يبدأ بعد الأذان مباشرة`() {
        val t = nextTuesdayNoon()
        val settings = defaults.copy(iqamahEnabled = false, muteEnabled = true)
        val events = AdhanPlan.eventsForPrayer(PrayerTimeKey.ASR, "العصر", t, settings)

        assertEquals(1, events.size)
        assertEquals(AdhanEventKind.ADHAN, events[0].kind)
        assertEquals(true, events[0].startsMute)
    }

    @Test
    fun `كتم معطل - لا علامة على أي حدث`() {
        val t = nextTuesdayNoon()
        val settings = defaults.copy(iqamahEnabled = true, muteEnabled = false)
        val events = AdhanPlan.eventsForPrayer(PrayerTimeKey.MAGHRIB, "المغرب", t, settings)
        assertEquals(false, events[0].startsMute)
        assertEquals(false, events[1].startsMute)
    }

    @Test
    fun `الجمعة الافتراضية - أذانان بينهما عشر ثم اقامة بعد ثلاثين`() {
        val friday = nextFridayNoon()
        val settings = defaults.copy(iqamahEnabled = true)
        val events = AdhanPlan.eventsForPrayer(PrayerTimeKey.DHUHR, "الجمعة", friday, settings)

        assertEquals(3, events.size)
        // الأذان الأول في وقته بلا اقامة محمولة
        assertEquals(AdhanEventKind.ADHAN, events[0].kind)
        assertEquals(friday, events[0].at)
        assertEquals(null, events[0].iqamahAt)
        // الأذان الثاني بعد ١٠ دقائق ويحمل موعد الإقامة بعد ٣٠ دقيقة منه
        assertEquals(AdhanEventKind.ADHAN, events[1].kind)
        assertEquals(friday.plusMinutes(10), events[1].at)
        assertTrue(events[1].carriesIqamah)
        assertEquals(friday.plusMinutes(40), events[1].iqamahAt)
        // الإقامة نفسها في موعدها
        assertEquals(AdhanEventKind.IQAMAH, events[2].kind)
        assertEquals(friday.plusMinutes(40), events[2].at)
    }

    @Test
    fun `الجمعة - فواصلها المستقلة لا تتأثر بفواصل الصلوات`() {
        val friday = nextFridayNoon()
        val settings = defaults.copy(
            iqamahEnabled = true,
            gapDhuhrMinutes = 99, // فاصل الظهر العادي — لا يمس الجمعة
            fridayAdhanGapMinutes = 20,
            fridayIqamahGapMinutes = 60,
        )
        val events = AdhanPlan.eventsForPrayer(PrayerTimeKey.DHUHR, "الجمعة", friday, settings)
        assertEquals(friday.plusMinutes(20), events[1].at)
        assertEquals(friday.plusMinutes(80), events[2].at)
    }

    @Test
    fun `الجمعة بميزة الازدواج مطفأة - اذان واحد باقي النظام عادي`() {
        val friday = nextFridayNoon()
        val settings = defaults.copy(iqamahEnabled = true, fridayDouble = false)
        val events = AdhanPlan.eventsForPrayer(PrayerTimeKey.DHUHR, "الجمعة", friday, settings)

        assertEquals(2, events.size)
        assertEquals(AdhanEventKind.ADHAN, events[0].kind)
        assertEquals(friday, events[0].at)
        assertEquals(friday.plusMinutes(10), events[1].at) // إقامة المسار العادي
    }

    @Test
    fun `الجمعة بلا اقامة - الكتم يبدأ بعد الأذان الثاني`() {
        val friday = nextFridayNoon()
        val settings = defaults.copy(iqamahEnabled = false, muteEnabled = true)
        val events = AdhanPlan.eventsForPrayer(PrayerTimeKey.DHUHR, "الجمعة", friday, settings)

        assertEquals(2, events.size)
        assertNull(events[1].iqamahAt)
        assertEquals(false, events[1].carriesIqamah)
        assertEquals(false, events[0].startsMute) // الأذان الأول لا يكتم
        assertEquals(true, events[1].startsMute) // الثاني هو آخر صوت
    }

    @Test
    fun `الظهر يوم غير الجمعة يبقى باذان واحد حتى مع تفعيل ميزة الجمعة`() {
        val tuesday = nextTuesdayNoon()
        val settings = defaults.copy(iqamahEnabled = true)
        val events = AdhanPlan.eventsForPrayer(PrayerTimeKey.DHUHR, "الظهر", tuesday, settings)

        assertEquals(2, events.size)
        assertEquals(tuesday.plusMinutes(10), events[1].at)
    }
}
