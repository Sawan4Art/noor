package com.sawan4art.noor

import com.sawan4art.noor.ui.nav.GooeyMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sin

/**
 * اختبارات «النقل السائل المتصل» — قاعدتا المالك:
 * 1) لا فصل: الجسر يبقى موصولاً حتى يكتمل نقل السائل كله.
 * 2) السائل حي: تموج وانسياب دائمان بلا توقف.
 * نفس الأرقام تُستخدم في معاينة بايثون للمالك.
 */
class GooeyMathTest {

    private val y = 100f
    private val r = 25f

    // ===== قاعدة لا فصل =====

    @Test
    fun `الجسر لا ينقطع مهما بعدت النقطتان - جوهر قاعدة المالك`() {
        // القديم كان يقطع الجسر عند مسافة الاندماج — الآن موصول دائماً
        for (d in listOf(60f, 100f, 200f, 400f, 1000f)) {
            val b = GooeyMath.bridge(0f, y, r, d, y, r, GooeyMath.waistFor(r, r))
            assertNotNull("الجسر انقطع عند بعد $d — مخالفة قاعدة لا فصل", b)
        }
    }

    @Test
    fun `لا جسر بين دائرتين متطابقتين تماما - الاندماج تلقائي`() {
        assertNull(GooeyMath.bridge(50f, y, r, 50f, y, r, 0.42f))
        assertNull(GooeyMath.bridge(50f, y, 0.5f, 50.00005f, y, r, 0.42f))
    }

    @Test
    fun `لا جسر حين تكون دائرة داخل الأخرى`() {
        assertNull(GooeyMath.bridge(0f, y, 30f, 5f, y, 10f, 0.42f))
    }

    @Test
    fun `الترتيب دائما يسار ثم يمين مهما كان ترتيب الإدخال`() {
        val forward = GooeyMath.bridge(10f, y, r, 70f, y, r, 0.42f)!!
        val backward = GooeyMath.bridge(70f, y, r, 10f, y, r, 0.42f)!!
        assertTrue(forward.ax <= forward.bx)
        assertTrue(backward.ax <= backward.bx)
        assertEquals(forward.ax, backward.ax, 1e-4f)
        assertEquals(forward.bx, backward.bx, 1e-4f)
    }

    @Test
    fun `نقاط الالتقاء على محيط الدائرتين تماما`() {
        val b = GooeyMath.bridge(0f, y, r, 60f, y, r, 0.42f)!!
        assertEquals(r, abs(b.p1y - b.ay), 1e-4f) // أعلى اليسرى
        assertEquals(r, abs(b.p2y - b.ay), 1e-4f) // أسفل اليسرى
        assertEquals(b.br, abs(b.p3y - b.by), 1e-4f) // أعلى اليمنى
        assertEquals(b.br, abs(b.p4y - b.by), 1e-4f) // أسفل اليمنى
        assertEquals(y - r, b.p1y, 1e-4f)
        assertEquals(y + r, b.p2y, 1e-4f)
    }

    @Test
    fun `الرقبة مقعرة سائلة - مناصف المنحنيات مسحوبة نحو المحور`() {
        val b = GooeyMath.bridge(0f, y, r, 70f, y, r, 0.42f)!!
        // منتصف الحافة العلوية يجب أن ينزل تحت قطبي الدائرتين (رقبة داخلية)
        assertTrue("الرقبة يجب أن تكون مقعرة", b.qA1y > b.p1y && b.qA2y > b.p3y)
        // ومنتصف الحافة السفلية فوق القطب السفلي
        assertTrue(b.qB1y < b.p2y && b.qB2y < b.p4y)
    }

    // ===== الرقبة المتكيفة — سحب آخر قطرة =====

    @Test
    fun `الرقبة سمينة والسائل كله في المصدر وخيط عند نفاده`() {
        val fat = GooeyMath.waistFor(r, r)
        val thin = GooeyMath.waistFor(0f, r)
        assertEquals(0.42f, fat, 1e-4f)
        assertTrue("الرقبة عند نفاد المصدر يجب أن تصل خيطاً: $thin", thin > 0.9f)
        assertTrue(thin <= 1f)
        // رتيبة التناقص: كلما نقص المصدر ضاقت الرقبة
        var prev = Float.MAX_VALUE
        for (i in 0..20) {
            val w = GooeyMath.waistFor(r * i / 20f, r)
            assertTrue("الرقبة يجب أن تضيق رتيباً مع نفاد المصدر", w <= prev + 1e-6f)
            prev = w
        }
    }

    @Test
    fun `الرقبة تتكيف مع أنصاف أقطار غير متساوية بلا NaN`() {
        for (src in listOf(0.1f, 1.2f, 5f, 13f, 25f)) {
            for (head in listOf(3f, 12f, 25f)) {
                val b = GooeyMath.bridge(0f, y, src, 60f, y, head, GooeyMath.waistFor(src, 25f))
                if (b != null) {
                    val values = arrayOf(
                        b.qA1y, b.qA2y, b.qB1y, b.qB2y,
                        b.p1y, b.p2y, b.p3y, b.p4y,
                    )
                    assertTrue(values.all { it.isFinite() })
                }
            }
        }
    }

    // ===== السائل الحي — تموج وانسياب دائمان =====

    @Test
    fun `التموج السطحي حي ومحدود ومتكرر بلا NaN`() {
        var maxIdle = 0f
        var maxLive = 0f
        for (t in 0..200) {
            for (i in 0..24) {
                val angle = i * (2f * Math.PI.toFloat() / 24f)
                val idle = GooeyMath.surfaceWave(angle, t * 0.05f, 0f)
                val live = GooeyMath.surfaceWave(angle, t * 0.05f, 1f)
                assertTrue(idle.isFinite() && live.isFinite())
                maxIdle = maxOf(maxIdle, abs(idle))
                maxLive = maxOf(maxLive, abs(live))
                // نفس المدخلات = نفس الناتج (حتمية المعاينة البايثونية)
                assertEquals(idle, GooeyMath.surfaceWave(angle, t * 0.05f, 0f), 1e-7f)
            }
        }
        assertTrue("تموج الثبات مفرط: $maxIdle", maxIdle < 0.12f)
        assertTrue("تموج الطاقة مفرط: $maxLive", maxLive < 0.2f)
        assertTrue("التموج لا بد أن يكون محسوساً: $maxIdle", maxIdle > 0.03f)
    }

    @Test
    fun `الانسياب المركزي وموجة الجسر محدودان دائما`() {
        var maxDrift = 0f
        var maxSway = 0f
        for (t in 0..300) {
            val time = t * 0.03f
            maxDrift = maxOf(maxDrift, abs(GooeyMath.centerDrift(time)))
            maxSway = maxOf(maxSway, abs(GooeyMath.bridgeSway(time)))
        }
        assertTrue("انسياب مفرط: $maxDrift", maxDrift < 0.07f)
        assertTrue("رفرفة مفرطة: $maxSway", maxSway <= 1.0001f)
        // موجات حقيقية لا تنعدم
        assertTrue(maxSway > 0.5f)
    }

    @Test
    fun `التموج بطورين مختلفين يعطي سطحين متمايلين لا متطابقين`() {
        // المصدر والرأس بطورين مختلفين — كي لا يبدو السائل قالباً متزامناً
        val a = GooeyMath.surfaceWave(0.8f, 1.37f, 0f, phase = 0f)
        val b = GooeyMath.surfaceWave(0.8f, 1.37f, 0f, phase = 1.3f)
        assertTrue(abs(a - b) > 1e-4f)
    }

    // ===== حدود الأمان العددية =====

    @Test
    fun `ضنك الرقبة يتحمل مصدرا ممتلئا فوق الكامل أو فارغا تماما`() {
        assertEquals(0.42f, GooeyMath.waistFor(100f, 25f), 1e-4f) // يُقيَّد عند الكامل
        assertEquals(0.97f, GooeyMath.waistFor(5f, 0f), 1e-4f) // تقسيم على صفر محروس
    }

    @Test
    fun `الجسر عند مسافات وبعوض شديد بلا انفجار عددي`() {
        // رأس ضئيل جدا يبرع من المصدر عند بعدها الأقصى — خيط رفيع لا NaN
        val b = GooeyMath.bridge(0f, y, r, 500f, y, 2f, GooeyMath.waistFor(r, r))
        assertNotNull(b)
        assertTrue(b!!.qA1y.isFinite() && b.qA2y.isFinite())
    }

    // ===== تطابق المعاينة البايثونية =====

    @Test
    fun `قيم مرجعية مثبتة تطابق معاينة بايثون حرفيا`() {
        // من scripts/make_liquid_preview_gif.py — نفس الثوابت (alpha4)
        val remaining = 14.4f / 25f
        val w = GooeyMath.waistFor(14.4f, 25f)
        assertEquals(0.42f + 0.55f * (1f - remaining).pow(1.5f), w, 1e-4f)
        val angle = 0.8f
        val time = 1.37f
        // الـ phase = 1 كي تظهر معاملات الأطوار (+1 و ×2.1 و ×4.2) في القيمة المرجعية
        val expected = 0.038f * sin(2f * angle + 1.0f * time + 1f) +
            0.028f * sin(4f * angle - 1.5f * time + 2.1f) +
            0.017f * sin(6f * angle + 2.1f * time + 4.2f)
        assertEquals(expected, GooeyMath.surfaceWave(angle, time, 0f, phase = 1f), 1e-6f)
    }
}
