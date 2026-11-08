package com.sawan4art.noor

import com.sawan4art.noor.util.Hijri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * اختبارات صيغ التاريخ للويدجت (alpha57: «التاريخ الهجري و الميلادي
 * بكذا صيغه» — وأوسع في alpha59 بلاغ المالك: «عايز صيغ اكثر للتواريخ
 * و عند اختيار صيغه للتاريخ تفتح له قائمه منبثقه») — ستُّ صيغٍ لكل
 * تقويمٍ بعربيةٍ سليمة، ونماذج القائمة المنبثقة ستةٌ لا أكثر ولا أقل.
 */
class HijriWidgetFormatTest {

    private val hijri = Hijri.HijriDate(14, 9, "رمضان", 1447)
    private val greg = LocalDate.of(2026, 11, 7)

    @Test
    fun `صيغ الهجري الست`() {
        assertEquals("١٤ رمضان ١٤٤٧ هـ", Hijri.formatHijri(hijri, 0))
        assertEquals("١٤ رمضان", Hijri.formatHijri(hijri, 1))
        assertEquals("١٤/٠٩/١٤٤٧", Hijri.formatHijri(hijri, 2))
        assertEquals("١٤ رمضان ١٤٤٧", Hijri.formatHijri(hijri, 3))
        // بيوم الأسبوع من الميلادي الموافق (٧ نوفمبر ٢٠٢٦ = السبت)
        assertEquals("السبت ١٤ رمضان ١٤٤٧ هـ", Hijri.formatHijri(hijri, 4, greg))
        // وبغياب الميلادي يصير كاملاً بلا اسم يوم
        assertEquals("١٤ رمضان ١٤٤٧ هـ", Hijri.formatHijri(hijri, 4))
        assertEquals("١٤/٠٩", Hijri.formatHijri(hijri, 5))
    }

    @Test
    fun `الصيغة الغريبة تسير إلى الكاملة`() {
        assertEquals(Hijri.formatHijri(hijri, 0), Hijri.formatHijri(hijri, 99))
        assertEquals(Hijri.formatHijri(hijri, 0), Hijri.formatHijri(hijri, -1))
    }

    @Test
    fun `صيغ الميلادي الست`() {
        val full = Hijri.formatGregorian(greg, 0)
        assertTrue("التاريخ الكامل يذكر اليوم: $full", full.startsWith("السبت"))
        val short = Hijri.formatGregorian(greg, 1)
        assertTrue("المختصرة بلا سنة: $short", !short.contains("٢٠٢٦"))
        assertEquals("٠٧/١١/٢٠٢٦", Hijri.formatGregorian(greg, 2))
        assertEquals("٧ نوفمبر ٢٠٢٦", Hijri.formatGregorian(greg, 3))
        assertEquals("السبت ٧ نوفمبر", Hijri.formatGregorian(greg, 4))
        assertEquals("٠٧/١١", Hijri.formatGregorian(greg, 5))
    }

    @Test
    fun `نماذج القائمة المنبثقة ستة لكل تقويم — عقد alpha59`() {
        assertEquals(6, Hijri.hijriFormatSamples().size)
        assertEquals(6, Hijri.gregorianFormatSamples().size)
        // والنموذج الرابع للهجري يذكر يوم الأسبوع لأنه يُحسب من اليوم الحي
        assertTrue(Hijri.hijriFormatSamples()[4].contains("هـ"))
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
