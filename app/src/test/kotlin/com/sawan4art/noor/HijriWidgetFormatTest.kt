package com.sawan4art.noor

import com.sawan4art.noor.util.Hijri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * اختبارات صيغ التاريخ للويدجت (alpha57 — بلاغ المالك: «التاريخ الهجري
 * و الميلادي بكذا صيغه») — ثلاث صيغٍ لكل تقويمٍ بعربيةٍ سليمة.
 */
class HijriWidgetFormatTest {

    private val hijri = Hijri.HijriDate(14, 9, "رمضان", 1447)
    private val greg = LocalDate.of(2026, 11, 7)

    @Test
    fun `صيغ الهجري الثلاث`() {
        assertEquals("١٤ رمضان ١٤٤٧ هـ", Hijri.formatHijri(hijri, 0))
        assertEquals("١٤ رمضان", Hijri.formatHijri(hijri, 1))
        assertEquals("١٤/٠٩/١٤٤٧", Hijri.formatHijri(hijri, 2))
    }

    @Test
    fun `الصيغة الغريبة تسير إلى الكاملة`() {
        assertEquals(Hijri.formatHijri(hijri, 0), Hijri.formatHijri(hijri, 99))
        assertEquals(Hijri.formatHijri(hijri, 0), Hijri.formatHijri(hijri, -1))
    }

    @Test
    fun `صيغ الميلادي الثلاث تحمل اليوم والأرقام الهندية`() {
        val full = Hijri.formatGregorian(greg, 0)
        val short = Hijri.formatGregorian(greg, 1)
        val numeric = Hijri.formatGregorian(greg, 2)
        assertTrue("التاريخ الكامل يذكر اليوم: $full", full.startsWith("السبت"))
        assertTrue("المختصرة بلا سنة: $short", !short.contains("٢٠٢٦"))
        assertEquals("٠٧/١١/٢٠٢٦", numeric)
    }

    @Test
    fun `أسماء الأيام بالترتيب`() {
        assertEquals("السبت", Hijri.dayNameAr(LocalDate.of(2026, 11, 7)))
        assertEquals("الأحد", Hijri.dayNameAr(LocalDate.of(2026, 11, 8)))
        assertEquals("الإثنين", Hijri.dayNameAr(LocalDate.of(2026, 11, 9)))
        assertEquals("الجمعة", Hijri.dayNameAr(LocalDate.of(2026, 11, 6)))
    }

    @Test
    fun `الأرقام الهندية توحّد كل الأرقام`() {
        assertEquals("٠٧/١١/٢٠٢٦", Hijri.arabicDigits("07/11/2026"))
        assertEquals("لا أرقام هنا", Hijri.arabicDigits("لا أرقام هنا"))
    }
}
