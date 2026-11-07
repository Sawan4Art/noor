package com.sawan4art.noor

import com.sawan4art.noor.widget.WidgetCustomization
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * اختبارات تخصيص الويدجت الواحدة (alpha57 — بلاغ المالك: «خليها
 * ويدجيت واحده و قابلة للتخصيص بالكامل من الاعدادات») — JSON في
 * SharedPreferences يُقرأ عند كل رسم، فالعقد أن ينجو الكتابة والقراءة
 * ويأمن الفاسد والناقص على الافتراضي بلا انكسار، وأن تنجو مفاتيح
 * alpha56 القديمة كوقفاتٍ ابتدائية.
 */
class WidgetCustomizationTest {

    @Test
    fun `التخصيص الافتراضي قيمه سليمة`() {
        val c = WidgetCustomization.DEFAULT
        assertEquals(85, c.opacityPercent)
        assertEquals(WidgetCustomization.Theme.APP, c.theme)
        assertEquals("emerald", c.accentId)
        // عناصر العرض الخمسة
        assertTrue(c.showClock)
        assertTrue(c.showPrayerTimes)
        assertTrue(c.showCountdown)
        assertTrue(c.showHijri)
        assertTrue(c.showGregorian)
        // المسبحة صامتة افتراضاً والفقاعات كما وردت في اللوحة
        assertFalse(c.showMisbaha)
        assertFalse(c.bubbleOnCountdown)
        assertTrue(c.bubbleOnNext)
        assertFalse(c.bubbleOnCurrent)
        // لون الخط يتبع المظهر
        assertEquals(WidgetCustomization.COLOR_AUTO, c.textColor)
    }

    @Test
    fun `التدوير JSON يفقد شيئا`() {
        val c = WidgetCustomization(
            theme = WidgetCustomization.Theme.DARK,
            opacityPercent = 40,
            accentId = "cobalt",
            textColor = "#233029",
            showCity = false,
            showBrand = false,
            showClock = false,
            showPrayerTimes = false,
            showCountdown = false,
            showHijri = false,
            hijriFormat = 2,
            showGregorian = false,
            gregFormat = 1,
            showMisbaha = true,
            bubbleOnCountdown = true,
            bubbleColorCountdown = "#5B6E9E",
            bubbleOnNext = false,
            bubbleOnCurrent = true,
            bubbleColorCurrent = "#B04A3A",
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
        assertTrue(parsed.showClock)
        assertTrue(parsed.showPrayerTimes)
        assertEquals("emerald", parsed.accentId)
    }

    @Test
    fun `مفاتيح alpha56 القديمة تُوقّف البدائل الجديدة`() {
        // «dates» القديم يطفي التاريخين معاً
        val old = WidgetCustomization.fromJson("""{"dates": false, "countdown": false}""")
        assertFalse(old.showHijri)
        assertFalse(old.showGregorian)
        assertFalse(old.showCountdown)
        // وبغيابهما يظل الافتراضي كما هو
        val fresh = WidgetCustomization.fromJson("""{"opacity": 50}""")
        assertTrue(fresh.showHijri && fresh.showGregorian)
    }

    @Test
    fun `لون الخط الصريح يُقرأ والتلقائي بلا لون`() {
        assertNull(WidgetCustomization.parseColor(WidgetCustomization.COLOR_AUTO))
        assertEquals(-16777216, WidgetCustomization.parseColor("#000000"))
        assertNull(WidgetCustomization.parseColor("بلا معنى"))
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

    @Test
    fun `صيغتا التاريخ محصورتان في الثلاث`() {
        val parsed = WidgetCustomization.fromJson("""{"hijriFormat": 9, "gregFormat": -3}""")
        assertTrue(parsed.hijriFormat in 0..2)
        // التخزين يحصر عند الكتابة — والقراءة تسير بأمان
        val stored = WidgetCustomization.fromJson(
            WidgetCustomization(hijriFormat = 9, gregFormat = -3).toJson(),
        )
        assertEquals(2, stored.hijriFormat)
        assertEquals(0, stored.gregFormat)
    }
}
