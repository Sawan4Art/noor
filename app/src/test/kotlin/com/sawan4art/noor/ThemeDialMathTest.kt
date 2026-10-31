package com.sawan4art.noor

import com.sawan4art.noor.ui.theme.ThemeDialMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.PI
import kotlin.math.cos

/**
 * اختبارات «مزلاج الشمس والقمر» — قواعد المالك حتى تعديل مساء 2026-10-31 (alpha9):
 * 1) يمين = فاتح، يسار = مظلم، والأيقونة مع اليد حرفياً.
 * 2) الانقسام: الفاتح خلف الأيقونة والمظلم أمامها — والحدّ **مستقيم رأسي**
 *    (قرار المالك: «خليه مستقيم تاني لكن اعمل عليه بلور») بحافة ريشية
 *    جيبية التدرج — الضوء سموزي لا خط حادّ، والمنتصف نص/نص بالضبط.
 * 3) الرفع يحسم بالضفة التي عبرها، والنقرة تبدّل فوراً.
 * 4) الإيماءة من الأيقونة وحدها — الفراغ يمر للطبقات تحتيه.
 * 5) المزلاج في الرئيسية فقط — لا أثر له في بقية الصفحات.
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

    // ===== الحد المستقيم — قرار المالك: مستقيم + بلور =====

    @Test
    fun `الريش متماثل حول الحد فمركز التمويه على الأيقونة نفسها`() {
        // الحدّ مستقيم عند موضع الأيقونة، والشفق يمتد نصفه عن يمينه ونصفه عن يساره —
        // هذا ما يجعل الإمساك في المنتصف = نص فاتح ونص مظلم بالضبط
        val fade = ThemeDialMath.MASK_FADE_DP * 3f // بكسل افتراضي للأرقام
        val offs = ThemeDialMath.featherOffsets(fade)
        assertEquals(-fade / 2f, offs.first(), 1e-4f)
        assertEquals(fade / 2f, offs.last(), 1e-4f)
        // تماثل الإزاحات: off(i) = -off(K-i)
        val k = offs.size - 1
        for (i in offs.indices) {
            assertEquals("تماثل عند i=$i", -offs[k - i], offs[i], 1e-4f)
        }
    }

    @Test
    fun `منتصف المسار بموضع اليد الحقيقي - نصف الشاشة لكل وضع`() {
        // المالك: «ممسك في المنتصف = نصف فاتح ونص مظلم» — الحدّ المستقيم عند الأيقونة
        // والتقدم عنده نصفاً تماماً والقرار عند العبور للفاتح
        val w = 1080f
        assertEquals(0.5f, ThemeDialMath.progress(mid, anchorLeft, anchorRight), 1e-5f)
        assertTrue(ThemeDialMath.commitLight(w / 2f, anchorLeft, anchorRight))
    }

    // ===== ريش الحافة — البلور الجيبي =====

    @Test
    fun `إزاحات الريش صاعدة متساوية الخطوات بعرض الشفق كاملا`() {
        val fade = 168f
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
    fun `شفافية الريش جيبية ناعمة من شبه الكامل إلى الصفر`() {
        val bands = ThemeDialMath.FEATHER_BANDS
        var prev = Float.MAX_VALUE
        for (k in 0 until bands) {
            val a = ThemeDialMath.featherAlpha(k)
            assertTrue("داخل النطاق عند k=$k", a in 0f..1f)
            assertTrue("تنازلية عند k=$k", a < prev)
            prev = a
        }
        // عقدة الصيغة المغلقة: (cos(π(b+½)/K)+1)/2 — تمنع أي تغيير صامت في التدرج
        val k16 = 16
        val expected = { b: Int -> (cos(PI * (b + 0.5f) / k16).toFloat() + 1f) / 2f }
        for (b in intArrayOf(0, 3, 7, 8, 12, 15)) {
            assertEquals(expected(b), ThemeDialMath.featherAlpha(b, k16), 1e-5f)
        }
        // الطرفان: شبه الكامل ثم شبه الصفر — لا انقطاع حاد عند حافتي الشفق
        assertTrue("بداية الشفق تكاد كاملة", ThemeDialMath.featherAlpha(0, k16) > 0.97f)
        assertTrue("نهاية الشفق تكاد صفر", ThemeDialMath.featherAlpha(k16 - 1, k16) < 0.03f)
    }

    @Test
    fun `متناظرة الريش تحيط بالحد بلا انحياز - مجموعها مع معكوسها واحد`() {
        val bands = ThemeDialMath.FEATHER_BANDS
        for (b in 0 until bands) {
            assertEquals(
                "تماثل عند b=$b",
                1f,
                ThemeDialMath.featherAlpha(b) + ThemeDialMath.featherAlpha(bands - 1 - b),
                1e-5f,
            )
        }
        // ومنتصف الشفق ≈ نصف — الحد المموه متماثل حول موضع الأيقونة
        val midA = ThemeDialMath.featherAlpha(bands / 2)
        assertTrue("منتصف الشفق قريب من النصف: $midA", abs(midA - 0.5f) < 0.06f)
    }

    // ===== هدف المسح — الحد يغادر الشاشة كليا بلا حافة ظاهرة =====

    @Test
    fun `التثبيت الفاتح يساق الحد والريش خارج الحافة اليمنى والعكس يسارا`() {
        val w = 1080f
        val fade = 168f
        // فاتح: الحد المستقيم عند w+fade ومعه نصف الريش خلفه — لا بقايا على الشاشة
        val endLight = ThemeDialMath.sweepTargetX(true, w, fade)
        assertTrue("بقايا حد فاتح عند الطرف", endLight - fade / 2f >= w)
        // مظلم: المرآة يساراً
        val endDark = ThemeDialMath.sweepTargetX(false, w, fade)
        assertTrue("بقايا حد مظلم عند الطرف", endDark + fade / 2f <= 0f)
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
    fun `الريش والتمويه لا ينهاران على حواف متطرفة`() {
        // شفق معدوم ومسار معدوم — لا استثناءات ولا قيم متلاعبة
        val offs = ThemeDialMath.featherOffsets(0f)
        assertTrue(offs.size >= 2)
        for (i in 0 until offs.size - 1) {
            assertTrue(offs[i + 1] >= offs[i])
        }
        ThemeDialMath.featherAlpha(0, 1)
        ThemeDialMath.featherAlpha(-5, 1)
        ThemeDialMath.featherAlpha(99, 3)
        ThemeDialMath.progress(50f, 100f, 100f) // مسار معدوم
        ThemeDialMath.sweepTargetX(true, 0f, 0f)
        ThemeDialMath.sweepTargetX(false, -100f, 0f)
    }
}
