package com.sawan4art.noor

import com.sawan4art.noor.util.Hijri
import com.sawan4art.noor.util.TimeFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HijriAndFormatTest {

    @Test
    fun `hijri conversion works and month names are arabic`() {
        val hijri = Hijri.of(LocalDate.of(2026, 10, 30))
        // نطاق معقول للتاريخ الهجري في 2026-10-30 (ربيع الأول 1448 تقريباً)
        assertTrue("year خارجة عن النطاق: ${hijri.year}", hijri.year in 1447..1449)
        assertTrue("اسم شهر فاضي", hijri.monthName.isNotBlank())
        assertTrue(hijri.formatted.endsWith("هـ"))
    }

    @Test
    fun `remaining formatting pads correctly`() {
        assertEquals("03:24:15", TimeFormat.formatRemaining(3 * 3600 + 24 * 60 + 15))
        assertEquals("00:00:00", TimeFormat.formatRemaining(0))
        assertEquals("00:00:00", TimeFormat.formatRemaining(-5))
    }

    @Test
    fun `arabic remaining converts digits only`() {
        val formatted = TimeFormat.remainingArabic(12345)
        // 12345 ثانية = 03:25:45
        assertEquals("٠٣:٢٥:٤٥", formatted)
    }
}
