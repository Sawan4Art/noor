package com.sawan4art.noor

import com.sawan4art.noor.widget.WidgetArt
import com.sawan4art.noor.widget.WidgetCustomization
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * عقارب الساعة التناظرية في الويدجت (alpha59 — بلاغ المالك: «يتم ايضا
 * اضافه ساعه تناظريه يمكن للمستخدم الاختيار بينها و بين الرقميه») —
 * الدوال النقية لزاوية العقارب تُختبر هنا كي تقف العقارب في مواعيدها
 * أياً كانت اللحظة المرسومة.
 */
class WidgetAnalogClockTest {

    @Test
    fun `عقرب الدقيقة دورة كل ستين درجة كل دقيقة`() {
        assertEquals(0f, WidgetArt.minuteAngleDeg(0))
        assertEquals(6f, WidgetArt.minuteAngleDeg(1))
        assertEquals(180f, WidgetArt.minuteAngleDeg(30))
        assertEquals(354f, WidgetArt.minuteAngleDeg(59))
        // وحدود ٦٠ تلف إلى الصفر بأمان
        assertEquals(0f, WidgetArt.minuteAngleDeg(60))
    }

    @Test
    fun `عقرب الساعة ثلاثون درجة كل ساعة ونصف درجة كل دقيقة`() {
        assertEquals(0f, WidgetArt.hourAngleDeg(12, 0))
        assertEquals(30f, WidgetArt.hourAngleDeg(1, 0))
        assertEquals(90f, WidgetArt.hourAngleDeg(3, 0))
        // الثالثة والنصف: ٩٠ + ١٥ = ١٠٥
        assertEquals(105f, WidgetArt.hourAngleDeg(3, 30))
        // الحادية عشرة إلا دقيقة: ٣٣٠ + ٢٩٫٥ = ٣٥٩٫٥
        assertEquals(359.5f, WidgetArt.hourAngleDeg(11, 59))
        // والثانية عشرة إلا دقيقة قرب الواحدة: ٢٩٫٥
        assertEquals(29.5f, WidgetArt.hourAngleDeg(12, 59))
        // ومن بعد الثانية عشرة (الظهيرة) يعود من الصفر
        assertEquals(0f, WidgetArt.hourAngleDeg(24, 0))
    }

    @Test
    fun `المفاتيح والعناصر الثمانية لعناصر الحجم واللون — عقد alpha61`() {
        // alpha61 أضاف مفتاح الترويسة (الاسم والمدينة) بعد أن كانت سبعة
        assertEquals(8, WidgetCustomization.ELEMENT_KEYS.size)
        assertEquals(8, WidgetCustomization.ELEMENT_KEYS.toSet().size)
        assertEquals(WidgetCustomization.ELEMENT_KEYS.size, WidgetCustomization.DEFAULT_ORDER.size)
    }

    @Test
    fun `مقامات الرسم محصورة`() {
        // أبعاد الفقاعات والميناء ثابتة وصغيرة عمداً — درس alpha58 البايندر
        assertTrue(WidgetArt.COUNTDOWN_BUBBLE_W <= 220)
        assertTrue(WidgetArt.GRID_BUBBLE_W <= 110)
        assertTrue(WidgetArt.ANALOG_SIZE <= 130)
    }
}
