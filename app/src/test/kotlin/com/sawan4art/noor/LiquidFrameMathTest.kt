package com.sawan4art.noor

import com.sawan4art.noor.ui.theme.LiquidFrameMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * اختبارات الإطار السائل الحي المستطيل — شارة اسم المطور (alpha39).
 * العقد: الشكل مستطيل لا نقطة، الحافة حية تتموج مع الزمن، والموجة لا
 * تجعل الإطار يفيض عن صندوقه بأكثر من سعة الموجة المسموحة.
 */
class LiquidFrameMathTest {

    private val out = FloatArray(LiquidFrameMath.MAX_POINTS * 2)

    @Test
    fun `المحيط كامل يُعاير بعدد نقاط معقول وفوق الحد الأدنى`() {
        val n = LiquidFrameMath.sampleFrame(300f, 88f, 34f, 0f, 4f, 0f, out)
        // محيط ≈ 2*(232+20) + 2π*34 ≈ 717px → ≈ 102 نقطة على خطوة ٧px
        assertTrue("نقاط قليلة جداً: $n", n in 60..LiquidFrameMath.MAX_POINTS)
    }

    @Test
    fun `كل النقاط داخل صندوق الشكل زائد سعة الموجة - لا فيضان عن الحدود`() {
        val w = 260f; val h = 84f; val amp = h * 0.10f
        for (t in listOf(0f, 0.7f, 1.9f, 3.3f, 7.1f)) {
            val n = LiquidFrameMath.sampleFrame(w, h, h * 0.42f, t, amp, 0f, out)
            for (i in 0 until n) {
                val x = out[2 * i]
                val y = out[2 * i + 1]
                assertTrue("x خارج الحدود t=$t: $x", x in -amp - 0.5f..w + amp + 0.5f)
                assertTrue("y خارج الحدود t=$t: $y", y in -amp - 0.5f..h + amp + 0.5f)
            }
        }
    }

    @Test
    fun `مستطيل لا كبسولة - الحافة الجانبية المستقيمة موجودة عند زاوية ٤٢٪ وغائبة في الكبسولة`() {
        val w = 260f; val h = 140f
        // عدّ النقاط الواقعة على الجانب الأيسر تماماً (x≈0 بأي ارتفاع):
        // المستطيل بزوايا ٤٢٪ له حافة مستقيمة بين القوسين (٤ نقاط معايرة + نهاية القوس)
        fun leftEdgePoints(buf: FloatArray): Int {
            var c = 0
            for (i in 0 until buf.size / 2) {
                val x = buf[2 * i]; val y = buf[2 * i + 1]
                if (i > 0 && x == 0f && y == 0f) break // نهاية النقاط المعبأة
                if (abs(x) <= 0.01f) c++
            }
            return c
        }
        LiquidFrameMath.sampleFrame(w, h, h * 0.42f, 0f, 0f, 0f, out)
        val rectSide = leftEdgePoints(out)
        assertTrue("لا حافة مستقيمة جانبية — الشكل صار كبسولة: $rectSide", rectSide >= 4)
        // الكبسولة (زاوية = نصف الارتفاع): لا حافة مستقيمة على الجانب تقريباً —
        // وهذا بالضبط ما يمنعه المالك («ليس نقطه او هي شبه النقطه لاكن مستطيله»)
        val capsule = FloatArray(LiquidFrameMath.MAX_POINTS * 2)
        LiquidFrameMath.sampleFrame(w, h, h * 0.5f - 0.5f, 0f, 0f, 0f, capsule)
        val capsuleSide = leftEdgePoints(capsule)
        assertTrue(capsuleSide <= 2)
        assertTrue("الكبسولة أعادت حافة أطول من المستطيل؟", rectSide > capsuleSide)
    }

    @Test
    fun `الحافة حية - نفس الموقع يختلف مع الزمن ولا يتكرر في الفترات القصيرة`() {
        val w = 300f; val h = 88f; val corner = 30f; val amp = 4f
        LiquidFrameMath.sampleFrame(w, h, corner, 0f, amp, 0f, out)
        val atZero = out.copyOf()
        LiquidFrameMath.sampleFrame(w, h, corner, 1.3f, amp, 0f, out)
        var diff = 0
        for (i in 0 until out.size step 2) {
            if (abs(out[i] - atZero[i]) > 0.01f) diff++
        }
        assertTrue("الحافة ساكنة — الإطار ليس سائلاً حياً", diff > 10)
    }

    @Test
    fun `الموجة محصورة في النطاق المسموح وأمبليتيود الفعلي يمنع التقاطع الذاتي`() {
        for (s in listOf(0f, 50f, 200f, 600f, 1500f)) {
            for (t in listOf(0f, 2.2f, 5.7f)) {
                val v = LiquidFrameMath.edgeWave(s, t, 0f)
                assertTrue("موجة خارج النطاق: $v", abs(v) <= 1f + 1e-4f)
            }
        }
        // سعة آمنة: amp ≤ MAX_AMPLITUDE_RATIO × h
        val h = 90f
        val amp = h * LiquidFrameMath.MAX_AMPLITUDE_RATIO
        assertTrue(amp <= h / 2f)
    }

    @Test
    fun `الأبعاد المنحرفة لا تكسر المعاير - حماية الحواف الصغيرة`() {
        // أبعاد أصغر من ضعف الزاوية: تُقلَّص الزاوية تلقائياً بلا استثناء
        val n = LiquidFrameMath.sampleFrame(10f, 8f, 30f, 0f, 1f, 0f, out)
        assertTrue(n >= 3)
        // نقطة البداية أعلى المنتصف تقريباً
        assertEquals(0f, out[1], 1.5f) // y عند نقطة البداية
    }
}
