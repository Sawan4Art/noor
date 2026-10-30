package com.sawan4art.noor

import com.sawan4art.noor.ui.theme.ThemeDialMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.PI
import kotlin.math.sin

/**
 * اختبارات «مزلاج الشمس والقمر» — قواعد المالك (2026-10-31) وتعديلات مساء اليوم:
 * 1) يمين = فاتح، يسار = مظلم، والأيقونة مع اليد حرفياً.
 * 2) الانقسام: الفاتح خلف الأيقونة والمظلم أمامها — والحدّ **قوس منحنٍ** ينتفخ
 *    حول الأيقونة بحافة ريشية مموّهة (الضوء لا يستقيم — درس alpha7).
 * 3) الرفع يحسم بالضفة التي عبرها، والنقرة تبدّل فوراً.
 * 4) الإيماءة من الأيقونة وحدها — الفراغ يمر للطبقات تحتيه.
 * نفس الأرقام تُستخدم في معاينة بايثون للمالك.
 */
class ThemeDialMathTest {

    private val anchorLeft = 54f
    private val anchorRight = 1026f
    private val mid = (anchorLeft + anchorRight) / 2f

    // ===== التقدم والحصر =====

    @Test
    fun `التقدم صفر عند المرسى الأيسر وواحد عند الأيمن`() {
        assertEquals(0f, ThemeDialMath.progress(anchorLeft, anchorLeft, anchorRight), 1e-5f)
        assertEquals(1f, ThemeDialMath.progress(anchorRight, anchorLeft, anchorRight), 1e-5f)
        assertEquals(0.5f, ThemeDialMath.progress(mid, anchorLeft, anchorRight), 1e-5f)
    }

    @Test
    fun `التقدم محصور لا يتجاوز المرسيين ولو خرجت اليد`() {
        assertEquals(0f, ThemeDialMath.progress(-500f, anchorLeft, anchorRight), 1e-5f)
        assertEquals(1f, ThemeDialMath.progress(5000f, anchorLeft, anchorRight), 1e-5f)
    }

    @Test
    fun `حصر الأيقونة يمنعها من مغادرة المسار - مع اليد حرفياً بين المرسيين`() {
        assertEquals(anchorLeft, ThemeDialMath.clampX(anchorLeft - 80f, anchorLeft, anchorRight), 1e-5f)
        assertEquals(anchorRight, ThemeDialMath.clampX(anchorRight + 80f, anchorLeft, anchorRight), 1e-5f)
        assertEquals(500f, ThemeDialMath.clampX(500f, anchorLeft, anchorRight), 1e-5f)
    }

    // ===== نقرة أم سحب =====

    @Test
    fun `الثبات أو الجذبة الصغيرة نقرة - الحركة الفعلية سحب`() {
        assertTrue(ThemeDialMath.isTap(0f, 0f))
        assertTrue(ThemeDialMath.isTap(10f, 6f)) // ≈11.7 داخل حد 14
        assertFalse(ThemeDialMath.isTap(30f, 0f))
        assertFalse(ThemeDialMath.isTap(0f, 30f))
        assertFalse(ThemeDialMath.isTap(12f, 12f)) // ≈17 خارج الحد
    }

    // ===== قرار التثبيت عند الرفع =====

    @Test
    fun `الرفع يمين المنتصف يثبت الفاتح ويساره يثبت المظلم`() {
        assertTrue(ThemeDialMath.commitLight(mid + 1f, anchorLeft, anchorRight))
        assertFalse(ThemeDialMath.commitLight(mid - 1f, anchorLeft, anchorRight))
        // الحد نفسه (تساوي تماماً) يُحسم للفاتح — سحب اليمين هو المطلوب
        assertTrue(ThemeDialMath.commitLight(mid, anchorLeft, anchorRight))
    }

    @Test
    fun `جذبة صغيرة لم تعبر المنتصف ترجع الأيقونة لمرساها`() {
        // مظلم مثبت: سحب يمين بسيط ثم رفع قبل المنتصف = رجوع مظلم
        assertFalse(ThemeDialMath.commitLight(anchorLeft + 120f, anchorLeft, anchorRight))
        // فاتح مثبت: سحب يسار بسيط ثم رفع بعد المنتصف = رجوع فاتح
        assertTrue(ThemeDialMath.commitLight(anchorRight - 120f, anchorLeft, anchorRight))
    }

    // ===== شفافيتا الأيقونتين =====

    @Test
    fun `القمر كامل في المظلم والشمس كاملة في الفاتح`() {
        assertEquals(1f, ThemeDialMath.moonAlpha(0f), 1e-5f)
        assertEquals(0f, ThemeDialMath.sunAlpha(0f), 1e-5f)
        assertEquals(1f, ThemeDialMath.sunAlpha(1f), 1e-5f)
        assertEquals(0f, ThemeDialMath.moonAlpha(1f), 1e-5f)
    }

    @Test
    fun `مجموع الشفافيتين واحد دائما والتحول في نطاق النصفة فقط`() {
        var p = 0f
        while (p <= 1f) {
            assertEquals(
                "المجموع عند p=$p",
                1f,
                ThemeDialMath.sunAlpha(p) + ThemeDialMath.moonAlpha(p),
                1e-4f,
            )
            // خارج نطاق النصفة (0.35..0.65) لا تحول جزئي
            if (p < 0.35f || p > 0.65f) {
                val a = ThemeDialMath.sunAlpha(p)
                assertTrue("لا تحول جزئي خارج النطاق عند p=$p", a == 0f || a == 1f)
            }
            p += 0.01f
        }
        assertEquals(0.5f, ThemeDialMath.sunAlpha(0.5f), 1e-5f)
    }

    @Test
    fun `القمر مرآة الشمس - تماثل تام حول المنتصف`() {
        for (p in intArrayOf(0, 10, 25, 40, 50, 60, 75, 90, 100)) {
            val f = p / 100f
            assertEquals(
                ThemeDialMath.sunAlpha(f),
                ThemeDialMath.moonAlpha(1f - f),
                1e-6f,
            )
        }
    }

    // ===== القوس — الضوء لا يستقيم بل يظهر كقوس =====

    @Test
    fun `رجفة القوس صفر عند القمة تماما فالحد يلمس الأيقونة`() {
        assertEquals(0f, ThemeDialMath.arcSag(0f, 900f, 140f), 1e-5f)
    }

    @Test
    fun `رجفة القوس تبلغ أقصاها عند المدى ثم تثبت`() {
        val sag = 140f
        val reach = 900f
        assertEquals(sag, ThemeDialMath.arcSag(reach, reach, sag), 1e-5f)
        assertEquals(sag, ThemeDialMath.arcSag(reach * 3f, reach, sag), 1e-5f)
        // عند نصف المدى: sin²(π/4) = نصف الرجفة بالضبط
        assertEquals(sag / 2f, ThemeDialMath.arcSag(reach / 2f, reach, sag), 1e-4f)
    }

    @Test
    fun `رجفة القوس رتيبة الصعود ومتماثلة أعلى وأسفل القمة`() {
        val reach = 900f
        val sag = 140f
        var prev = -1f
        var dy = 0f
        while (dy <= reach) {
            val s = ThemeDialMath.arcSag(dy, reach, sag)
            assertTrue("رتيبة عند dy=$dy", s >= prev)
            assertEquals("تماثل عند dy=$dy", s, ThemeDialMath.arcSag(-dy, reach, sag), 1e-6f)
            prev = s
            dy += 60f
        }
    }

    @Test
    fun `حد القوس قمته عند الأيقونة وطرفه ينسحب خلف اتجاه التقدم`() {
        val apex = 540f
        val cy = 200f
        val reach = 900f
        val sag = 140f
        // الفاتح خلف الأيقونة (يسار الحدّ): القمة أقصى نقطة يميناً والطرفان يسارها
        assertEquals(apex, ThemeDialMath.edgeX(cy, apex, cy, reach, sag, lightOnLeft = true), 1e-5f)
        assertTrue(ThemeDialMath.edgeX(0f, apex, cy, reach, sag, lightOnLeft = true) < apex)
        assertTrue(ThemeDialMath.edgeX(cy + 400f, apex, cy, reach, sag, lightOnLeft = true) < apex)
        // المظلم خلف الأيقونة (يمين الحدّ): المرآة المعاكسة
        assertEquals(apex, ThemeDialMath.edgeX(cy, apex, cy, reach, sag, lightOnLeft = false), 1e-5f)
        assertTrue(ThemeDialMath.edgeX(0f, apex, cy, reach, sag, lightOnLeft = false) > apex)
        assertTrue(ThemeDialMath.edgeX(cy + 400f, apex, cy, reach, sag, lightOnLeft = false) > apex)
        // تماثل رأسي حول القمة في الاتجاهين
        assertEquals(
            ThemeDialMath.edgeX(cy - 300f, apex, cy, reach, sag, true),
            ThemeDialMath.edgeX(cy + 300f, apex, cy, reach, sag, true),
            1e-4f,
        )
    }

    @Test
    fun `حد القوس قوس حقيقي لا خط - الانحناء ملموس لا صفري`() {
        val apex = 540f
        val cy = 200f
        val reach = 900f
        val sag = 140f
        val bottom = ThemeDialMath.edgeX(cy + reach, apex, cy, reach, sag, lightOnLeft = true)
        // الانسحاب عند بلوغ المدى = أقصى الرجفة تقريبا — قوس واضح للعين لا شبه مستقيم
        assertTrue("القوس ضحل أكثر من اللازم", apex - bottom > sag * 0.9f)
        // وعند منتصف المدى نحو نصف الرجفة — انحناء تدريجي مقوّس لا قطعة مستقيمة
        val midReach = ThemeDialMath.edgeX(cy + reach / 2f, apex, cy, reach, sag, lightOnLeft = true)
        assertEquals(sag / 2f, apex - midReach, sag * 0.02f)
    }

    // ===== ريش الحافة — التمويه المتدرج =====

    @Test
    fun `إزاحات الريش صاعدة متماثلة حول الحد بعرض الشفق كاملا`() {
        val fade = 120f
        val offs = ThemeDialMath.featherOffsets(fade)
        assertEquals(ThemeDialMath.FEATHER_BANDS + 1, offs.size)
        assertEquals(-fade / 2f, offs.first(), 1e-4f)
        assertEquals(fade / 2f, offs.last(), 1e-4f)
        for (i in 0 until offs.size - 1) {
            assertTrue("صاعدة عند i=$i", offs[i + 1] > offs[i])
            assertEquals("خطوات متساوية عند i=$i", offs[1] - offs[0], offs[i + 1] - offs[i], 1e-4f)
        }
    }

    @Test
    fun `شفافية الريش تنزلق من شبه الكامل إلى الصفر بلا قيم خارج النطاق`() {
        val bands = ThemeDialMath.FEATHER_BANDS
        var prev = Float.MAX_VALUE
        for (k in 0 until bands) {
            val a = ThemeDialMath.featherAlpha(k)
            assertTrue("داخل النطاق عند k=$k", a in 0f..1f)
            assertTrue("تنازلية عند k=$k", a < prev)
            prev = a
        }
        // منتصف الريش ≈ نصف — الحد المموه متماثل
        assertEquals(0.5f, ThemeDialMath.featherAlpha(bands / 2), 0.02f)
    }

    @Test
    fun `منتصف المسار بموضع اليد الحقيقي - نصف الشاشة لكل وضع`() {
        // المالك: «ممسك في المنتصف = نصف فاتح ونصف مظلم» — الحدّ عند الأيقونة نفسها
        val w = 1080f
        val apex = w / 2f
        val cy = 200f
        val reach = 950f
        val sag = w * 0f // القمة عند منتصف الارتفاع تقريبا للتبسيط — نتحقق من المركز
        val edge = ThemeDialMath.edgeX(cy, apex, cy, reach, sag, lightOnLeft = true)
        assertEquals(w / 2f, edge, 1e-5f)
        // والقرار عند المنتصف: العبور يثبت الفاتح
        assertTrue(ThemeDialMath.commitLight(w / 2f, anchorLeft, anchorRight))
        assertTrue(abs(ThemeDialMath.progress(mid, anchorLeft, anchorRight) - 0.5f) < 1e-5f)
    }

    // ===== هدف المسح — الحد يغادر الشاشة كليا بلا حافة ظاهرة =====

    @Test
    fun `التثبيت الفاتح يساق الحد والريش خارج الحافة اليمنى والعكس يسارا`() {
        val w = 1080f
        val h = 1900f
        val fade = 120f
        val cy = 200f
        val sag = h * ThemeDialMath.ARC_SAG_FRACTION
        val reach = h * ThemeDialMath.ARC_REACH_FRACTION
        // فاتح: أبعد نقطة يسرى للحد القوسي (عند أقصى رجفة) مع نصف الريش تجاوزت يمين الشاشة
        val endLight = ThemeDialMath.sweepTargetX(true, w, fade, sag)
        val leftmost = ThemeDialMath.edgeX(0f, endLight, cy, reach, sag, lightOnLeft = true)
        assertTrue("بقايا حد فاتح عند الطرف: $leftmost", leftmost - fade / 2f >= w)
        // مظلم: أبعد نقطة يمنى مع نصف الريش قبلت يسار الشاشة
        val endDark = ThemeDialMath.sweepTargetX(false, w, fade, sag)
        val rightmost = ThemeDialMath.edgeX(0f, endDark, cy, reach, sag, lightOnLeft = false)
        assertTrue("بقايا حد مظلم عند الطرف: $rightmost", rightmost + fade / 2f <= 0f)
    }

    // ===== توهج الفاصل =====

    @Test
    fun `التوهج يحتفظ بحيويته حتى سبعين بالمئة من الرحلة ثم يذبل`() {
        assertEquals(1f, ThemeDialMath.glowDuringSweep(0f), 1e-5f)
        assertEquals(1f, ThemeDialMath.glowDuringSweep(0.7f), 1e-5f)
        assertEquals(0f, ThemeDialMath.glowDuringSweep(1f), 1e-5f)
        assertEquals(0.5f, ThemeDialMath.glowDuringSweep(0.85f), 1e-5f)
    }

    // ===== متانة الحواف =====

    @Test
    fun `القوس والريش لا ينهاران على حواف متطرفة`() {
        // شاشة معدومة ومدى معدوم وشفق معدوم — لا استثناءات ولا قيم متلاعبة
        ThemeDialMath.arcSag(0f, 0f, 0f)
        ThemeDialMath.arcSag(100f, 0f, 140f)
        ThemeDialMath.edgeX(-5000f, 0f, 200f, 0f, 0f, true)
        ThemeDialMath.edgeX(5000f, 0f, 200f, 1f, 0f, false)
        val offs = ThemeDialMath.featherOffsets(0f)
        assertTrue(offs.size >= 2)
        for (i in 0 until offs.size - 1) {
            assertTrue(offs[i + 1] >= offs[i])
        }
        ThemeDialMath.progress(50f, 100f, 100f) // مسار معدوم
        // حدود خارج الشاشة كليا — إزاحات ريش سليمة
        for (apex in listOf(-500f, 0f, 1080f, 1580f)) {
            ThemeDialMath.edgeX(0f, apex, 200f, 900f, 140f, true)
            ThemeDialMath.edgeX(1900f, apex, 200f, 900f, 140f, false)
        }
    }

    @Test
    fun `صيغة الرجفة sin محصورة رياضيا - عينة مرجعية مثبتة`() {
        // مرجع مغلق: sin²(π/2 · t) — يمنع أي تغيير صامت في شكل القوس
        val sag = 100f
        val reach = 1000f
        val expected = { t: Float -> sag * sin(PI / 2.0 * t).toFloat() * sin(PI / 2.0 * t).toFloat() }
        for (t in listOf(0f, 0.1f, 0.25f, 0.5f, 0.75f, 0.9f, 1f)) {
            assertEquals(expected(t), ThemeDialMath.arcSag(t * reach, reach, sag), 1e-3f)
        }
    }
}
