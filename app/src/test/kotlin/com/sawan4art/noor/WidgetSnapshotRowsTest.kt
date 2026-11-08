package com.sawan4art.noor

import com.sawan4art.noor.domain.prayer.CalculationMethod
import com.sawan4art.noor.domain.prayer.PrayerMath
import com.sawan4art.noor.domain.prayer.PrayerTimeKey
import com.sawan4art.noor.widget.WidgetSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * صفوف مواقيت الويدجت (alpha62 — بلاغ المالك: «ازل توقيت الشروق من
 * الويدجت تماماً» و«تأكد ان يوم الجمعه يتغير اسم صلاه الظهر الى الجمعه
 * في الويدجت») — دالة الصفوف نقيةٌ فتُختبر هنا: خمسُ خلاياٍ بلا الشروق
 * وليلةُ الجمعة الظهر باسم «الجمعة» فيها وفي الصلاة القادمة.
 */
class WidgetSnapshotRowsTest {

    private val zone = ZoneId.of("Africa/Cairo")

    private fun times(date: LocalDate) = PrayerMath.compute(
        date, 30.0444, 31.2357, zone,
        CalculationMethod.EGYPT, isHanafi = false,
    )

    @Test
    fun `الصفوف خمس صلوات بلا الشروق`() {
        val now = ZonedDateTime.of(2026, 11, 4, 10, 0, 0, 0, zone) // أربعاء
        val rows = WidgetSnapshot.rows(times(now.toLocalDate()), now)
        assertEquals(5, rows.size)
        val names = rows.map { it.name }
        assertEquals(
            listOf("الفجر", "الظهر", "العصر", "المغرب", "العشاء"),
            names,
        )
        assertTrue("الشروق" !in names)
    }

    @Test
    fun `يوم الجمعة الظهر باسم الجمعة في الصفوف والقادمة`() {
        // جمعة ٦ نوفمبر ٢٠٢٦ — والظهر في الصفوف باسم «الجمعة»
        val friday = ZonedDateTime.of(2026, 11, 6, 13, 30, 0, 0, zone)
        assertEquals(DayOfWeek.FRIDAY, friday.dayOfWeek)
        val rows = WidgetSnapshot.rows(times(friday.toLocalDate()), friday)
        assertEquals(
            listOf("الفجر", "الجمعة", "العصر", "المغرب", "العشاء"),
            rows.map { it.name },
        )
        // وقبل الظهر يوم الجمعة القادمة هي الجمعة باسمها
        val before = ZonedDateTime.of(2026, 11, 6, 9, 0, 0, 0, zone)
        val rowsBefore = WidgetSnapshot.rows(times(before.toLocalDate()), before)
        assertEquals("الجمعة", rowsBefore.first { it.isNext }.name)
    }

    @Test
    fun `مفاتيح الويدجت الخمسة بلا الشروق`() {
        assertEquals(
            PrayerTimeKey.entries.filterNot { it == PrayerTimeKey.SUNRISE },
            WidgetSnapshot.PRAYER_KEYS,
        )
        assertTrue(PrayerTimeKey.SUNRISE !in WidgetSnapshot.PRAYER_KEYS)
    }
}
