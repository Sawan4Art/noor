package com.sawan4art.noor

import com.sawan4art.noor.ui.nav.GooeyMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/** اختبارات رياضيات القطرة السائلة — نفس الأرقام تُستخدم في معاينة بايثون للمالك */
class GooeyMathTest {

    private val y = 100f
    private val r = 25f

    @Test
    fun `التمطط كامل عند البعد الكامل وواحد عند الوصول`() {
        val travel = 300f
        assertEquals(1f, GooeyMath.stretchFactor(0f, travel), 1e-6f)
        assertEquals(1.45f, GooeyMath.stretchFactor(300f, travel), 1e-6f)
        assertEquals(1.45f, GooeyMath.stretchFactor(500f, travel), 1e-6f) // لا يتجاوز الحد
        assertTrue(GooeyMath.stretchFactor(150f, travel) in 1f..1.45f)
    }

    @Test
    fun `الضغط يحفظ الحجم تقريبًا`() {
        val sx = GooeyMath.stretchFactor(300f, 300f)
        val sy = GooeyMath.squashFactor(sx)
        assertEquals(1f, sx * sy, 0.02f)
    }

    @Test
    fun `لا جسر بين دائرتين متطابقتين تمامًا`() {
        assertNull(GooeyMath.bridge(50f, y, r, 50f, y, r, 80f))
    }

    @Test
    fun `لا جسر عند البعد الأكبر من مسافة الاندماج`() {
        assertNull(GooeyMath.bridge(0f, y, r, 81f, y, r, 80f))
        assertNotNull(GooeyMath.bridge(0f, y, r, 79f, y, r, 80f))
    }

    @Test
    fun `لا جسر حين تكون دائرة داخل الأخرى`() {
        assertNull(GooeyMath.bridge(0f, y, 30f, 5f, y, 10f, 80f))
    }

    @Test
    fun `الترتيب دائمًا يسار ثم يمين مهما كان ترتيب الإدخال`() {
        val forward = GooeyMath.bridge(10f, y, r, 70f, y, r, 80f)!!
        val backward = GooeyMath.bridge(70f, y, r, 10f, y, r, 80f)!!
        assertTrue(forward.ax <= forward.bx)
        assertTrue(backward.ax <= backward.bx)
        assertEquals(forward.ax, backward.ax, 1e-4f)
        assertEquals(forward.bx, backward.bx, 1e-4f)
    }

    @Test
    fun `نقاط الالتقاء على محيط الدائرتين تمامًا`() {
        val b = GooeyMath.bridge(0f, y, r, 60f, y, r, 80f)!!
        assertEquals(r, abs(b.p1y - b.ay), 1e-4f) // أعلى اليسرى
        assertEquals(r, abs(b.p2y - b.ay), 1e-4f) // أسفل اليسرى
        assertEquals(b.br, abs(b.p3y - b.by), 1e-4f) // أعلى اليمنى
        assertEquals(b.br, abs(b.p4y - b.by), 1e-4f) // أسفل اليمنى
        assertEquals(y - r, b.p1y, 1e-4f)
        assertEquals(y + r, b.p2y, 1e-4f)
    }

    @Test
    fun `الرقبة أضيق من كامل المسافة - مقعرية سائلة`() {
        val b = GooeyMath.bridge(0f, y, r, 70f, y, r, 80f)!!
        // منتصف الحافة العلوية يجب أن ينزل تحت أعلى الدائرتين (رقبة داخلية)
        val midTopY = (b.qA1y + b.qA2y) / 2f
        assertTrue("الرقبة يجب أن تكون مقعرة: midTop=$midTopY", midTopY > b.p1y)
        // ومنتصف الحافة السفلية فوق أسفل الدائرتين
        val midBottomY = (b.qB1y + b.qB2y) / 2f
        assertTrue(midBottomY < b.p2y)
    }

    @Test
    fun `نصف قطر النقطة يكبر كلما اقتربت من الاندماج`() {
        val far = GooeyMath.dropletRadius(10f, 78f, 80f)
        val near = GooeyMath.dropletRadius(10f, 10f, 80f)
        assertTrue(near > far)
        // 10×(0.55+0.45×(1-78/80)) = 5.6125 و 10×(0.55+0.45×0.875) = 9.4375
        assertEquals(5.6125f, far, 1e-4f)
        assertEquals(9.4375f, near, 1e-4f)
    }

    @Test
    fun `فحص الجسر السريع منطقي`() {
        assertTrue(GooeyMath.hasBridge(40f, 80f))
        assertFalse(GooeyMath.hasBridge(80f, 80f))
        assertFalse(GooeyMath.hasBridge(0f, 80f))
    }
}
