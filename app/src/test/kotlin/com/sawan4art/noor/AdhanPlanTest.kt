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
 * اختبارات منطق خطة الأذان — خاصة أذان الجمعة المزدوج وفواصل الإقامة
 * والكتم (القيم الافتراضية المعتمدة: إقامة ١٠، الجمعة ١٠ و٣٠).
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
    }

    @Test
    fun `الإقامة بعد الأذان بعشر دقائق افتراضيا والاستعادة بعدها بمقدار الكتم`() {
        val t = nextTuesdayNoon()
        val settings = defaults.copy(iqamahEnabled = true, muteEnabled = true, muteMinutes = 10)
        val events = AdhanPlan.eventsForPrayer(PrayerTimeKey.MAGHRIB, "المغرب", t, settings)

        assertEquals(3, events.size)
        assertEquals(AdhanEventKind.ADHAN, events[0].kind)
        assertEquals(AdhanEventKind.IQAMAH, events[1].kind)
        assertEquals(t.plusMinutes(10), events[1].at) // الإقامة بعد ١٠ دقائق
        assertEquals(AdhanEventKind.MUTE_RESTORE, events[2].kind)
        assertEquals(t.plusMinutes(20), events[2].at) // الاستعادة بعد الإقامة بـ١٠
    }

    @Test
    fun `بلا إقامة - الاستعادة بعد الأذان مباشرة بمقدار مدة الكتم`() {
        val t = nextTuesdayNoon()
        val settings = defaults.copy(iqamahEnabled = false, muteEnabled = true, muteMinutes = 15)
        val events = AdhanPlan.eventsForPrayer(PrayerTimeKey.ASR, "العصر", t, settings)

        assertEquals(2, events.size)
        assertEquals(t.plusMinutes(15), events[1].at)
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
    fun `الجمعة - استعادة الكتم بعد الاذان الثاني بلا اقامة`() {
        val friday = nextFridayNoon()
        val settings = defaults.copy(iqamahEnabled = false, muteEnabled = true, muteMinutes = 20)
        val events = AdhanPlan.eventsForPrayer(PrayerTimeKey.DHUHR, "الجمعة", friday, settings)

        assertEquals(3, events.size)
        assertNull(events[1].iqamahAt)
        assertEquals(false, events[1].carriesIqamah)
        assertEquals(AdhanEventKind.MUTE_RESTORE, events[2].kind)
        // بعد الأذان الثاني (بعد ١٠) بمقدار مدة الكتم ٢٠
        assertEquals(friday.plusMinutes(30), events[2].at)
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
