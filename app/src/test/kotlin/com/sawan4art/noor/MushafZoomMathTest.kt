package com.sawan4art.noor

import com.sawan4art.noor.domain.mushaf.MushafZoomMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * اختبارات رياضيات تكبير صفحات المصحف (alpha54) — حدود المعامل
 * والإزاحة وثبات نقطة القرص والنقرة المزدوجة — كلها وحدات نقية.
 */
class MushafZoomMathTest {

    private val viewW = 1000f
    private val viewH = 1600f

    @Test
    fun `حصر المعامل داخل الحدود لا يتجاوز القصوى ولا تحت الوحدة`() {
        assertEquals(MushafZoomMath.MAX_SCALE, MushafZoomMath.clampScale(4f, 2.5f))
        assertEquals(MushafZoomMath.MIN_SCALE, MushafZoomMath.clampScale(1f, 0.3f))
        assertEquals(2.1f, MushafZoomMath.clampScale(1.5f, 1.4f), 1e-4f)
    }

    @Test
    fun `الصفحة عند واحد ليست مكبرة وفوقها مكبرة`() {
        assertFalse(MushafZoomMath.isZoomed(1f))
        assertFalse(MushafZoomMath.isZoomed(1.01f))
        assertTrue(MushafZoomMath.isZoomed(1.05f))
        assertTrue(MushafZoomMath.isZoomed(2.5f))
    }

    @Test
    fun `الإزاحة المحصورة عند واحد تصفر دائما`() {
        assertEquals(0f to 0f, MushafZoomMath.clampOffset(1f, 50f, -70f, viewW, viewH))
    }

    @Test
    fun `الإزاحة المحصورة متماثلة حول الصفر وعند التكبير`() {
        // معامل ٢ — المدى ±نصف العرض؛ الصفر داخل المدى
        val (ox, oy) = MushafZoomMath.clampOffset(2f, 999f, -999f, viewW, viewH)
        assertEquals(viewW / 2f, ox, 1e-3f)
        assertEquals(-viewH / 2f, oy, 1e-3f)
        val (ox2, oy2) = MushafZoomMath.clampOffset(2f, 0f, 0f, viewW, viewH)
        assertEquals(0f, ox2, 1e-4f)
        assertEquals(0f, oy2, 1e-4f)
    }

    @Test
    fun `قرص حول مركز الشاشة لا يحرك الإزاحة`() {
        val (ox, oy) = MushafZoomMath.pinchedOffset(
            1f, 2f, 0f, 0f,
            cx = viewW / 2f, cy = viewH / 2f, viewW = viewW, viewH = viewH,
        )
        assertEquals(0f, ox, 1e-3f)
        assertEquals(0f, oy, 1e-3f)
    }

    @Test
    fun `قرص حول نقطة تثبت النقطة في مكانها`() {
        // نقطة (٧٥٠، ٩٠٠) عند تكبير ١←٢ يجب أن تظل تحت الإصبع
        val cx = 750f
        val cy = 900f
        val (ox, oy) = MushafZoomMath.pinchedOffset(
            1f, 2f, 0f, 0f, cx = cx, cy = cy, viewW = viewW, viewH = viewH,
        )
        // المعادلة الأمامية: s = center + (c − center)·scale + off
        val sx = viewW / 2f + (cx - viewW / 2f) * 2f + ox
        val sy = viewH / 2f + (cy - viewH / 2f) * 2f + oy
        assertEquals(cx, sx, 0.5f)
        assertEquals(cy, sy, 0.5f)
    }

    @Test
    fun `النقرة المزدوجة من غير تكبير تكبر حول الضغطة`() {
        val (scale, offset) = MushafZoomMath.doubleTapped(
            1f, 0f, 0f, tapX = 700f, tapY = 500f, viewW = viewW, viewH = viewH,
        )
        assertEquals(MushafZoomMath.DOUBLE_TAP_SCALE, scale, 1e-4f)
        val sx = viewW / 2f + (700f - viewW / 2f) * scale + offset.first
        val sy = viewH / 2f + (500f - viewH / 2f) * scale + offset.second
        assertEquals(700f, sx, 0.5f)
        assertEquals(500f, sy, 0.5f)
    }

    @Test
    fun `النقرة المزدوجة من مكبر تعيد العرض الطبيعي مصفرة الإزاحة`() {
        val (scale, offset) = MushafZoomMath.doubleTapped(
            3f, 120f, -80f, tapX = 500f, tapY = 800f, viewW = viewW, viewH = viewH,
        )
        assertEquals(MushafZoomMath.MIN_SCALE, scale, 1e-4f)
        assertEquals(0f, offset.first, 1e-4f)
        assertEquals(0f, offset.second, 1e-4f)
    }

    @Test
    fun `تحويل السكرين للمحتوى عكس التحويل الأمامي تماما`() {
        val scale = 2.3f
        val offX = 140f
        val offY = -90f
        val content = Pair(620f, 1100f)
        // الأمامي: s = center + (c − center)·scale + off
        val sx = viewW / 2f + (content.first - viewW / 2f) * scale + offX
        val sy = viewH / 2f + (content.second - viewH / 2f) * scale + offY
        val back = MushafZoomMath.toContent(sx, sy, scale, offX, offY, viewW, viewH)
        assertEquals(content.first, back.first, 0.01f)
        assertEquals(content.second, back.second, 0.01f)
    }
}
