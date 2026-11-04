package com.sawan4art.noor

import com.sawan4art.noor.domain.adhan.AdhanGaps
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * اختبارات قواعد الفواصل الزمنية (توجيه المالك 2026-11-05):
 * - الفاصل بين الأذان والإقامة ثلاثة اختيارات فقط: ١٠ و١٥ و٢٠ دقيقة
 *   («اقصى حد ليه هو ٢٠ دقيقه و اقصر حد ليه هو ١٠ دقائق»).
 * - فاصل الجمعة بين الأذان الثاني والإقامة خانة كتابة من دقيقة إلى
 *   ١٢٠ دقيقة («ممكن يصل لغايه ٢ ساعه»).
 */
class AdhanGapsTest {

    // ===== الاختيارات الثلاثة =====

    @Test
    fun `الاختيارات الثلاثة هي عشرة وخمسة عشر وعشرين فقط`() {
        assertEquals(listOf(10, 15, 20), AdhanGaps.IQAMAH_OFFSET_CHOICES)
    }

    @Test
    fun `قيم النسخ السابقة تلزم على أقرب اختيار`() {
        assertEquals(10, AdhanGaps.snapIqamahOffset(5))
        assertEquals(10, AdhanGaps.snapIqamahOffset(8))
        assertEquals(15, AdhanGaps.snapIqamahOffset(15))
        assertEquals(20, AdhanGaps.snapIqamahOffset(25))
        assertEquals(20, AdhanGaps.snapIqamahOffset(30))
        assertEquals(20, AdhanGaps.snapIqamahOffset(60))
        assertEquals(10, AdhanGaps.snapIqamahOffset(0))
        assertEquals(20, AdhanGaps.snapIqamahOffset(100))
    }

    @Test
    fun `القيم داخل الثلاثة تمر كما هي`() {
        assertEquals(10, AdhanGaps.snapIqamahOffset(10))
        assertEquals(15, AdhanGaps.snapIqamahOffset(15))
        assertEquals(20, AdhanGaps.snapIqamahOffset(20))
    }

    @Test
    fun `أقرب اختيار يحسم القيم بين الاختيارات`() {
        // منتصف المسافة بين 10 و15 هو 12.5 — فـ12 يؤول لـ10 و13 يؤول لـ15
        assertEquals(10, AdhanGaps.snapIqamahOffset(12))
        assertEquals(15, AdhanGaps.snapIqamahOffset(13))
        // ومنتصف المسافة بين 15 و20 هو 17.5 — فـ17 يؤول لـ15 و18 يؤول لـ20
        assertEquals(15, AdhanGaps.snapIqamahOffset(17))
        assertEquals(20, AdhanGaps.snapIqamahOffset(18))
    }

    // ===== حصر فاصل الجمعة =====

    @Test
    fun `فاصل الجمعة محصور بين دقيقة ومئة وعشرين`() {
        assertEquals(1, AdhanGaps.clampFridayIqamahGap(0))
        assertEquals(1, AdhanGaps.clampFridayIqamahGap(-5))
        assertEquals(1, AdhanGaps.clampFridayIqamahGap(1))
        assertEquals(30, AdhanGaps.clampFridayIqamahGap(30))
        assertEquals(90, AdhanGaps.clampFridayIqamahGap(90))
        assertEquals(120, AdhanGaps.clampFridayIqamahGap(120))
        assertEquals(120, AdhanGaps.clampFridayIqamahGap(121))
        assertEquals(120, AdhanGaps.clampFridayIqamahGap(600))
    }

    @Test
    fun `صلاحية مدخل فاصل الجمعة`() {
        assertTrue(AdhanGaps.isFridayGapInputValid(1))
        assertTrue(AdhanGaps.isFridayGapInputValid(120))
        assertFalse(AdhanGaps.isFridayGapInputValid(0))
        assertFalse(AdhanGaps.isFridayGapInputValid(121))
        assertFalse(AdhanGaps.isFridayGapInputValid(null))
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
