package com.sawan4art.noor

import com.sawan4art.noor.ui.theme.ThemeDialMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * اختبارات «مزلاج الشمس والقمر» — قواعد المالك (2026-10-31):
 * 1) يمين = فاتح، يسار = مظلم، والأيقونة مع اليد حرفياً.
 * 2) الانقسام: الفاتح خلف الأيقونة والمظلم أمامها، والحدّ عند الأيقونة.
 * 3) الرفع يحسم بالضفة التي عبرها، والنقرة تبدّل فوراً.
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

    // ===== قناع الانقسام =====

    @Test
    fun `الشمس خلف الأيقونة - الفاتح يسار الحد ويذوب قبله`() {
        val w = 1080f
        val fade = 96f
        val b = 540f
        val s = ThemeDialMath.maskStops(b, w, fade, lightOnLeft = true)
        val alphaAt: (Float) -> Float = { xPx ->
            // استيفاء خطي بين حدود القناع — المواضع كسور من عرض الشاشة
            val x = xPx / w
            var i = 0
            var result = Float.NaN
            while (i < s.size - 2) {
                val p0 = s[i]; val a0 = s[i + 1]; val p1 = s[i + 2]; val a1 = s[i + 3]
                if (x in p0..p1) {
                    val t = if (p1 - p0 <= 1e-6f) 1f else (x - p0) / (p1 - p0)
                    result = a0 + (a1 - a0) * t
                    break
                }
                i += 2
            }
            if (result.isNaN()) result = s[s.size - 1]
            result
        }
        // عمق المنطقة الفاتحة كامل، ويلمس الصفر عند الأيقونة تماماً
        assertEquals(1f, alphaAt(0f), 1e-5f)
        assertEquals(1f, alphaAt(b - fade - 1f), 1e-3f)
        assertEquals(0f, alphaAt(b), 1e-5f)
        assertEquals(0f, alphaAt(w - 1f), 1e-5f)
        // منتصف الشفق = نصف
        assertEquals(0.5f, alphaAt(b - fade / 2f), 0.02f)
        // المواضع صاعدة بدقة
        for (i in 0 until s.size - 2 step 2) {
            assertTrue(s[i + 2] >= s[i])
        }
    }

    @Test
    fun `القمر خلف الأيقونة - المظلم يمين الحد ويصعد بعده`() {
        val w = 1080f
        val fade = 96f
        val b = 540f
        val s = ThemeDialMath.maskStops(b, w, fade, lightOnLeft = false)
        // مواضع الحدود: الحد ثم الحد + الشفق
        assertEquals(b / w, s[2], 1e-5f)
        assertEquals(0f, s[3], 1e-5f) // صفر عند الحدّ
        assertEquals((b + fade) / w, s[4], 1e-5f)
        assertEquals(1f, s[5], 1e-5f) // كامل بعد الشفق
        assertEquals(0f, s[1], 1e-5f) // يسار الشاشة مظلم المظهر المضاف صفر
        assertEquals(1f, s[s.size - 1], 1e-5f) // أقصى اليمين كامل
    }

    @Test
    fun `القناع خارج الشاشة تماما عند التثبيت - بلا حافة ظاهرة`() {
        val w = 1080f
        val fade = 96f
        // تثبيت فاتح: الحد يُساق أبعد من الحافة اليمنى بمقدار الشفق
        val endLight = ThemeDialMath.sweepTargetX(true, w, fade)
        val sLight = ThemeDialMath.maskStops(endLight, w, fade, lightOnLeft = true)
        assertEquals(1f, sLight[1], 1e-5f) // الشاشة كلها بطبقة الفاتح
        // تثبيت مظلم: الحد أبعد من الحافة اليسرى
        val endDark = ThemeDialMath.sweepTargetX(false, w, fade)
        val sDark = ThemeDialMath.maskStops(endDark, w, fade, lightOnLeft = false)
        assertEquals(1f, sDark[sDark.size - 1], 1e-5f)
    }

    // ===== توهج الفاصل =====

    @Test
    fun `توهج الشروق قمته عند الأيقونة ويذبل في الاتجاهين`() {
        val w = 1080f
        val band = 176f
        val b = 540f
        val s = ThemeDialMath.glowStops(b, w, band)
        assertEquals(b / w, s[4], 1e-5f) // موضع القمة عند الحدّ
        assertEquals(1f, s[5], 1e-5f) // قمة عند الحدّ
        // الطرفان صفر
        assertEquals(0f, s[1], 1e-5f)
        assertEquals(0f, s[s.size - 1], 1e-5f)
    }

    @Test
    fun `التوهج يحتفظ بحيويته حتى سبعين بالمئة من الرحلة ثم يذبل`() {
        assertEquals(1f, ThemeDialMath.glowDuringSweep(0f), 1e-5f)
        assertEquals(1f, ThemeDialMath.glowDuringSweep(0.7f), 1e-5f)
        assertEquals(0f, ThemeDialMath.glowDuringSweep(1f), 1e-5f)
        assertEquals(0.5f, ThemeDialMath.glowDuringSweep(0.85f), 1e-5f)
    }

    // ===== متانة الحواف =====

    @Test
    fun `قناع وتوهج لا ينهاران على حواف متطرفة`() {
        val w = 1080f
        val fade = 96f
        val band = 176f
        // حد عند صفر وخارج الشاشة — لا استثناءات ولا مواضع متناقصة
        for (b in listOf(-500f, 0f, 1f, w - 1f, w, w + 500f)) {
            val m1 = ThemeDialMath.maskStops(b, w, fade, lightOnLeft = true)
            val m2 = ThemeDialMath.maskStops(b, w, fade, lightOnLeft = false)
            val g = ThemeDialMath.glowStops(b, w, band)
            assertTrue(m1.size >= 4 && m1.size % 2 == 0)
            assertTrue(m2.size >= 4 && m2.size % 2 == 0)
            assertTrue(g.size >= 4 && g.size % 2 == 0)
            for (i in 0 until m1.size - 2 step 2) {
                assertTrue("مواضع القناع صاعدة عند b=$b", m1[i + 2] >= m1[i] - 1e-6f)
            }
        }
        // عرض شاشة معدوم لا يقسم على صفر
        ThemeDialMath.maskStops(100f, 0f, fade, lightOnLeft = true)
        ThemeDialMath.glowStops(100f, 0f, band)
        ThemeDialMath.progress(50f, 100f, 100f) // مسار معدوم
    }

    @Test
    fun `منتصف المسار بموضع اليد الحقيقي - نصف الشاشة لكل وضع`() {
        // المالك: «ممسك في المنتصف = نصف فاتح ونصف مظلم» — الحدّ عند الأيقونة نفسها
        val w = 1080f
        val b = w / 2f
        val s = ThemeDialMath.maskStops(b, w, 96f, lightOnLeft = true)
        // القمة الكاملة تبقى حتى الحد − الشفق، أي أكثر من نصف الشاشة الفاتح خلف الأيقونة
        assertTrue(s[2] >= 0.40f) // (540-96)/1080 ≈ 0.411
        assertEquals(0.5f, s[4], 1e-5f) // الذوبان ينتهي عند الأيقونة
        // والقرار عند المنتصف: العبور يثبت الفاتح
        assertTrue(ThemeDialMath.commitLight(w / 2f, anchorLeft, anchorRight) == true)
        assertTrue(abs(ThemeDialMath.progress(mid, anchorLeft, anchorRight) - 0.5f) < 1e-5f)
    }
}
