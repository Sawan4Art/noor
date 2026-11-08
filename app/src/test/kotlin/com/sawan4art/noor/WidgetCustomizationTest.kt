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
        // عناصر العرض
        assertTrue(c.showClock)
        assertTrue(c.showPrayerTimes)
        assertTrue(c.showCountdown)
        assertTrue(c.showHijri)
        assertTrue(c.showGregorian)
        // alpha62: المسبحة والفقاعات انتهتا والإطار يتبع المظهر افتراضاً
        assertEquals(WidgetCustomization.COLOR_AUTO, c.frameColor)
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
            clockStyle = WidgetCustomization.ClockStyle.ANALOG,
            elementSizes = mapOf(
                WidgetCustomization.ELEMENT_CLOCK to 135,
                WidgetCustomization.ELEMENT_GRID to 70,
            ),
            elementColors = mapOf(
                WidgetCustomization.ELEMENT_HIJRI to "#5B6E9E",
                WidgetCustomization.ELEMENT_GREG to "auto",
            ),
            frameColor = "#5B6E9E",
        )
        val parsed = WidgetCustomization.fromJson(c.toJson())
        assertEquals(c, parsed)
    }

    @Test
    fun `مفاتيح المسبحة والفقاعات القديمة تُرمى والإطار يُقرأ — alpha62`() {
        // JSON من نسخة alpha61 بمفاتيحها المنتهية: لا انكسار ولا عودة
        val old = WidgetCustomization.fromJson(
            """{"misbaha": true, "bubbleNext": true, "bubbleColorNext": "#5B6E9E",
               "bubbleCountdown": true, "bubbleCurrent": true, "frameColor": "#B04A3A"}""",
        )
        assertTrue(old.elementOrder.none { it == "misbaha" })
        assertEquals("#B04A3A", old.frameColor)
        // ولون إطارٍ فاسد يعود تلقائياً
        val broken = WidgetCustomization.fromJson("""{"frameColor": "أحمر"}""")
        assertEquals(WidgetCustomization.COLOR_AUTO, broken.frameColor)
    }

    @Test
    fun `دورة لون الإطار — الضغطة تُقدّم خطوةً والغريب يدخل من أولها`() {
        val cycle = WidgetCustomization.FRAME_CYCLE
        assertEquals(
            cycle[1],
            WidgetCustomization.nextFrameColor(WidgetCustomization.COLOR_AUTO),
        )
        // آخر القائمة يعود إلى أولها
        assertEquals(cycle.first(), WidgetCustomization.nextFrameColor(cycle.last()))
        // والغريب يبدأ الدورة
        assertEquals(cycle.first(), WidgetCustomization.nextFrameColor("غير موجود"))
        // والدورة محكمة: كل خطوةٍ ضمن القائمة
        var current = WidgetCustomization.COLOR_AUTO
        repeat(cycle.size) { current = WidgetCustomization.nextFrameColor(current) }
        assertEquals(WidgetCustomization.COLOR_AUTO, current)
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
    fun `صيغتا التاريخ محصورتان في الست — بلاغ alpha59`() {
        val parsed = WidgetCustomization.fromJson("""{"hijriFormat": 9, "gregFormat": -3}""")
        assertTrue(parsed.hijriFormat in 0..5)
        // التخزين يحصر عند الكتابة — والقراءة تسير بأمان
        val stored = WidgetCustomization.fromJson(
            WidgetCustomization(hijriFormat = 9, gregFormat = -3).toJson(),
        )
        assertEquals(5, stored.hijriFormat)
        assertEquals(0, stored.gregFormat)
    }

    @Test
    fun `شكل الساعة رقمية افتراضاً والغريب يعود رقمياً`() {
        assertEquals(WidgetCustomization.ClockStyle.DIGITAL, WidgetCustomization.DEFAULT.clockStyle)
        val analog = WidgetCustomization.fromJson("""{"clockStyle": "ANALOG"}""")
        assertEquals(WidgetCustomization.ClockStyle.ANALOG, analog.clockStyle)
        val broken = WidgetCustomization.fromJson("""{"clockStyle": "SUNDIAL"}""")
        assertEquals(WidgetCustomization.ClockStyle.DIGITAL, broken.clockStyle)
    }

    @Test
    fun `خرائط الحجم واللون للعناصر تُقرأ بسلامة وتُقذّر الغريب`() {
        // مفاتيح غريبة تُرمى والقيم خارج الحدود تُحصر (alpha61: ٥٠..٢٥٠)
        val parsed = WidgetCustomization.fromJson(
            """{"elementSizes": {"clock": 300, "ghost": 150, "grid": 40},
               "elementColors": {"hijri": "#5B6E9E", "ghost": "#123456"}}""",
        )
        assertEquals(250, parsed.elementSizes[WidgetCustomization.ELEMENT_CLOCK])
        assertEquals(50, parsed.elementSizes[WidgetCustomization.ELEMENT_GRID])
        assertEquals(null, parsed.elementSizes["ghost"])
        assertEquals("#5B6E9E", parsed.elementColors[WidgetCustomization.ELEMENT_HIJRI])
        assertEquals(null, parsed.elementColors["ghost"])
        // وتدوير القيم السليمة يفقد شيئاً
        val c = WidgetCustomization(
            elementSizes = mapOf(WidgetCustomization.ELEMENT_NEXT to 135),
            elementColors = mapOf(WidgetCustomization.ELEMENT_GRID to "#C98A3D"),
        )
        assertEquals(c, WidgetCustomization.fromJson(c.toJson()))
    }
}
