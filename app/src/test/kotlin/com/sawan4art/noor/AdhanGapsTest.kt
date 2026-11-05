package com.sawan4art.noor

import com.sawan4art.noor.domain.adhan.AdhanGaps
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * اختبارات قواعد الفواصل الزمنية — تحديث المالك 2026-11-06:
 * - الفاصل بين الأذان والإقامة **خانة كتابة رقم لكل صلاة على حدة**
 *   («مش اختيارات») من دقيقة إلى ١٢٠ دقيقة (ساعتان).
 * - الافتراضيات: كل الصلوات ١٠ دقائق ماعاد الفجر ١٥ دقيقة، والجمعة
 *   استثناء (بين الأذانين ١٠ وبين الثاني والإقامة ٣٠).
 */
class AdhanGapsTest {

    // ===== حدود خانة الكتابة (كل الفواصل) =====

    @Test
    fun `حدود الخانة من دقيقة الى مئة وعشرين`() {
        assertEquals(1, AdhanGaps.GAP_MIN_MINUTES)
        assertEquals(120, AdhanGaps.GAP_MAX_MINUTES)
    }

    @Test
    fun `الحصر يبقي القيم الصالحة كما هي`() {
        assertEquals(1, AdhanGaps.clampGap(1))
        assertEquals(10, AdhanGaps.clampGap(10))
        assertEquals(15, AdhanGaps.clampGap(15))
        assertEquals(30, AdhanGaps.clampGap(30))
        assertEquals(90, AdhanGaps.clampGap(90))
        assertEquals(120, AdhanGaps.clampGap(120))
    }

    @Test
    fun `القيم الخارج الحدود تُحصر لا تُرفض عند القراءة`() {
        assertEquals(1, AdhanGaps.clampGap(0))
        assertEquals(1, AdhanGaps.clampGap(-5))
        assertEquals(120, AdhanGaps.clampGap(121))
        assertEquals(120, AdhanGaps.clampGap(600))
    }

    @Test
    fun `قيم النسخ السابقة داخل الحدود تمر كما هي`() {
        // اختيارات alpha48 القديمة ١٠/١٥/٢٠ كلها داخل ١..١٢٠ فتبقى كما هي
        for (v in listOf(10, 15, 20)) assertEquals(v, AdhanGaps.clampGap(v))
    }

    @Test
    fun `الافتراضيات المعتمدة فجر خمسة عشر والباقي عشرة`() {
        assertEquals(15, AdhanGaps.DEFAULT_FAJR_GAP_MINUTES)
        assertEquals(10, AdhanGaps.DEFAULT_PRAYER_GAP_MINUTES)
        assertEquals(10, AdhanGaps.DEFAULT_FRIDAY_ADHAN_GAP_MINUTES)
        assertEquals(30, AdhanGaps.DEFAULT_FRIDAY_IQAMAH_GAP_MINUTES)
    }

    @Test
    fun `صلاحية مدخل الخانة`() {
        assertTrue(AdhanGaps.isGapInputValid(1))
        assertTrue(AdhanGaps.isGapInputValid(120))
        assertFalse(AdhanGaps.isGapInputValid(0))
        assertFalse(AdhanGaps.isGapInputValid(121))
        assertFalse(AdhanGaps.isGapInputValid(null))
    }

    // ===== تحويل نص خانة الكتابة =====

    @Test
    fun `الأرقام العربية تتحول لدقائق`() {
        assertEquals(30, AdhanGaps.parseMinutesInput("٣٠"))
        assertEquals(120, AdhanGaps.parseMinutesInput("١٢٠"))
        assertEquals(7, AdhanGaps.parseMinutesInput("٧"))
    }

    @Test
    fun `الأرقام الغربية تتحول لدقائق`() {
        assertEquals(45, AdhanGaps.parseMinutesInput("45"))
        assertEquals(0, AdhanGaps.parseMinutesInput("0"))
    }

    @Test
    fun `خلط الأرقام العربية والغربية يُقبل`() {
        assertEquals(63, AdhanGaps.parseMinutesInput("٦3"))
    }

    @Test
    fun `النصوص بلا أرقام تعيد فارغاً`() {
        assertNull(AdhanGaps.parseMinutesInput(""))
        assertNull(AdhanGaps.parseMinutesInput("abc"))
        assertNull(AdhanGaps.parseMinutesInput("دقائق"))
    }

    @Test
    fun `العرض بالأرقام العربية يقابل التحويل`() {
        assertEquals("٣٠", AdhanGaps.toArabicDigits(30))
        assertEquals("١٢٠", AdhanGaps.toArabicDigits(120))
        assertEquals(45, AdhanGaps.parseMinutesInput(AdhanGaps.toArabicDigits(45)))
    }
}
