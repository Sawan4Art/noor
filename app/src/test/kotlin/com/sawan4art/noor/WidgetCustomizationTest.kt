package com.sawan4art.noor

import com.sawan4art.noor.widget.WidgetCustomization
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * اختبارات تخصيص الويدجت (alpha56) — قسم «الويدجت»: JSON في
 * SharedPreferences يُقرأ عند كل رسم، فالعقد أن ينجو الكتابة والقراءة
 * ويأمن الفاسد والناقص على الافتراضي بلا انكسار.
 */
class WidgetCustomizationTest {

    @Test
    fun `التخصيص الافتراضي قيمه سليمة`() {
        val c = WidgetCustomization.DEFAULT
        assertEquals(85, c.opacityPercent)
        assertEquals(WidgetCustomization.Theme.APP, c.theme)
        assertEquals("emerald", c.accentId)
        assertTrue(c.showCity && c.showCountdown && c.showDates && c.showBrand)
    }

    @Test
    fun `التدوير JSON يفقد شيئا`() {
        val c = WidgetCustomization(
            theme = WidgetCustomization.Theme.DARK,
            opacityPercent = 40,
            accentId = "cobalt",
            showCity = false,
            showCountdown = false,
            showDates = false,
            showBrand = false,
        )
        val parsed = WidgetCustomization.fromJson(c.toJson())
        assertEquals(c, parsed)
    }

    @Test
    fun `الفاسد يسير بأمان إلى الافتراضي`() {
        assertEquals(WidgetCustomization.DEFAULT, WidgetCustomization.fromJson(""))
        assertEquals(WidgetCustomization.DEFAULT, WidgetCustomization.fromJson(null))
        assertEquals(WidgetCustomization.DEFAULT, WidgetCustomization.fromJson("not json at all"))
        assertEquals(WidgetCustomization.DEFAULT, WidgetCustomization.fromJson("{broken"))
    }

    @Test
    fun `الناقص من JSON يسير إلى الافتراضي والغريب يُهمل`() {
        val parsed = WidgetCustomization.fromJson("""{"opacity": 20, "theme": "WRONG"}""")
        assertEquals(20, parsed.opacityPercent)
        assertEquals(WidgetCustomization.Theme.APP, parsed.theme)
        // ما لم يُذكر في JSON يبقى افتراضياً
        assertTrue(parsed.showCity)
        assertEquals("emerald", parsed.accentId)
    }

    @Test
    fun `الشفافية محصورة بين صفر ومئة`() {
        val c = WidgetCustomization(opacityPercent = 140)
        assertEquals(1.0f, c.opacityFraction)
        val z = WidgetCustomization(opacityPercent = -10)
        assertEquals(0.0f, z.opacityFraction)
    }

    @Test
    fun `مظهر الويدجت ثلاث حالات فقط`() {
        assertEquals(3, WidgetCustomization.Theme.entries.size)
        assertFalse(WidgetCustomization.Theme.entries.isEmpty())
    }
}
