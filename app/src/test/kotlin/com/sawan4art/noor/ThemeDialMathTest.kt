package com.sawan4art.noor

import com.sawan4art.noor.ui.theme.ThemeDialMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * اختبارات «مزلاج الشمس والقمر» — قواعد المالك حتى الكلمة الأخيرة alpha23:
 * 1) يمين = فاتح، يسار = مظلم، والأيقونة مع اليد حرفياً.
 * 2) الانقسام: النصف الفاتح يسار الحدّ والمظلم يمينه — والحدّ نفسه **تدرّج
 *    مزج ناعم** (alpha23: حزام ±15% من عرض الشاشة حول الحدّ بمنحنى
 *    smoothstep — مزج 50/50 عند الحدّ نفسه بلا أي حواف حادة) بلا أي
 *    عنصر ضوئي (حكم alpha22 على حكاية الشعاع قائم).
 * 3) الرفع يحسم بالضفة التي عبرها، والنقرة تبدّل فوراً.
 * 4) الإيماءة من الأيقونة وحدها — الفراغ يمر للطبقات تحتيه.
 * 5) المزلاج في الرئيسية فقط — لا أثر له في بقية الصفحات.
 * 6) هدف المسح يُساق الحدّ خارج الشاشة بحزام المزج كاملاً ببكسل أمان —
 *    الغطاء صورة كاملة للثيم الجديد بلا حزام متبقٍّ لحظة القلب.
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

    @Test
    fun `منتصف المسار بموضع اليد الحقيقي - نصف الشاشة لكل وضع`() {
        // المالك: «ممسك في المنتصف = نصف فاتح ونص مظلم» — الحدّ المستقيم عند الأيقونة
        // والتقدم عنده نصفاً تماماً والقرار عند العبور للفاتح
        val w = 1080f
        assertEquals(0.5f, ThemeDialMath.progress(mid, anchorLeft, anchorRight), 1e-5f)
        assertTrue(ThemeDialMath.commitLight(w / 2f, anchorLeft, anchorRight))
    }

    // ===== هدف المسح — الحدّ يغادر الشاشة بحزام المزج كاملاً =====

    @Test
    fun `التثبيت الفاتح يساق الحد خارج الحافة اليمنى والعكس يسارا`() {
        val w = 1080f
        // فاتح: الحدّ خارج الحافة اليمنى — مظلم: خارج اليسرى (متناسب الاتجاهين)
        val endLight = ThemeDialMath.sweepTargetX(true, w)
        assertTrue("حدّ الفاتح يجب أن يخرج من الشاشة", endLight >= w)
        val endDark = ThemeDialMath.sweepTargetX(false, w)
        assertTrue("حدّ المظلم يجب أن يخرج من الشاشة", endDark <= 0f)
    }

    @Test
    fun `هدف المسح لا ينهار على حواف متطرفة`() {
        // انسياب معدوم ومسار معدوم — لا استثناءات ولا قيم متلاعبة
        val zeroLight = ThemeDialMath.sweepTargetX(true, 0f)
        assertTrue("يجب هامش أمان موجب للفاتح", zeroLight >= 1f)
        val zeroDark = ThemeDialMath.sweepTargetX(false, 0f)
        assertTrue("يجب هامش أمان سالب للمظلم", zeroDark <= -1f)
        ThemeDialMath.sweepTargetX(false, -100f)
        ThemeDialMath.progress(50f, 100f, 100f) // مسار معدوم
    }

    // ===== حزام المزج الناعم (alpha23) — تدرّج smoothstep بلا حواف حادة =====

    @Test
    fun `المزج نصف شفافية عند الحد نفسه وكامل قبل الحزام ونافد بعده`() {
        val w = 1080f
        val edge = 540f
        val half = ThemeDialMath.blendHalfSpan(w)
        assertEquals(half, 162f, 1e-3f) // 15% من العرض — مثال الطلب الحرفي
        assertEquals(0.5f, ThemeDialMath.blendMaskAlpha(edge, edge, w, true), 1e-5f)
        assertEquals(0.5f, ThemeDialMath.blendMaskAlpha(edge, edge, w, false), 1e-5f)
        // شافل الحزام الأيسر: شفافية كاملة للجهة الظاهرة ونفاد للعكس
        assertEquals(1f, ThemeDialMath.blendMaskAlpha(edge - half, edge, w, true), 1e-5f)
        assertEquals(0f, ThemeDialMath.blendMaskAlpha(edge - half, edge, w, false), 1e-5f)
        // شافل الحزام الأيمن: العكس تماماً
        assertEquals(0f, ThemeDialMath.blendMaskAlpha(edge + half, edge, w, true), 1e-5f)
        assertEquals(1f, ThemeDialMath.blendMaskAlpha(edge + half, edge, w, false), 1e-5f)
        // قبل الحزام وبعده: قطعتان صلبتان كما كان قبل الحزام إنما بنعومة عند شرفه
        assertEquals(1f, ThemeDialMath.blendMaskAlpha(0f, edge, w, true), 1e-5f)
        assertEquals(0f, ThemeDialMath.blendMaskAlpha(w, edge, w, true), 1e-5f)
    }

    @Test
    fun `حزام المزج رتيب للفاتح متزايد للمظلم ومتتامان مجموعهما واحد`() {
        val w = 1080f
        val edge = w / 2f
        var prevLight = 2f
        var prevDark = -1f
        var x = 0f
        while (x <= w) {
            val l = ThemeDialMath.blendMaskAlpha(x, edge, w, true)
            val d = ThemeDialMath.blendMaskAlpha(x, edge, w, false)
            assertTrue("الفاتح يتناقص فقط عند x=$x", l <= prevLight + 1e-6f)
            assertTrue("المظلم يتزايد فقط عند x=$x", d >= prevDark - 1e-6f)
            assertEquals("متتامّان مجموعهما 1 عند x=$x", 1f, l + d, 1e-5f)
            prevLight = l
            prevDark = d
            x += 8f
        }
    }

    @Test
    fun `منحنى smoothstep طرفاه بانحدار معدوم ووسطه النصف`() {
        assertEquals(0f, ThemeDialMath.smoothstep(0f), 1e-6f)
        assertEquals(1f, ThemeDialMath.smoothstep(1f), 1e-6f)
        assertEquals(0.5f, ThemeDialMath.smoothstep(0.5f), 1e-6f)
        assertEquals(0.15625f, ThemeDialMath.smoothstep(0.25f), 1e-5f)
        assertEquals(0.84375f, ThemeDialMath.smoothstep(0.75f), 1e-5f)
        // خارج النطاق يُحصر — لا قيم سالبة ولا أكبر من واحد
        assertEquals(0f, ThemeDialMath.smoothstep(-3f), 1e-6f)
        assertEquals(1f, ThemeDialMath.smoothstep(3f), 1e-6f)
    }

    @Test
    fun `تماثل الحزام حول الحد - اللونان يتبادلان مكانيهما بنفس الشكل`() {
        val w = 1080f
        val edge = 400f
        for (d in intArrayOf(1, 20, 60, 120, 160)) {
            assertEquals(
                "التماثل عند المسافة d=$d",
                ThemeDialMath.blendMaskAlpha(edge + d, edge, w, true),
                ThemeDialMath.blendMaskAlpha(edge - d, edge, w, false),
                1e-5f,
            )
        }
    }

    @Test
    fun `هدف المسح الجديد يغمر الشاشة كلها بشفافية كاملة بلا حزام متبقٍّ`() {
        val w = 1080f
        val half = ThemeDialMath.blendHalfSpan(w)
        val endLight = ThemeDialMath.sweepTargetX(true, w)
        val endDark = ThemeDialMath.sweepTargetX(false, w)
        assertEquals(w + half + 1f, endLight, 1e-3f)
        assertEquals(-half - 1f, endDark, 1e-3f)
        // الغطاء صورة كاملة للثيم الجديد: القناع معتم عبر الشاشة كلها
        for (x in floatArrayOf(0f, w / 4f, w / 2f, 3f * w / 4f, w)) {
            assertEquals("فاتح معتم عند x=$x", 1f, ThemeDialMath.blendMaskAlpha(x, endLight, w, true), 1e-5f)
            assertEquals("مظلم معتم عند x=$x", 1f, ThemeDialMath.blendMaskAlpha(x, endDark, w, false), 1e-5f)
        }
    }

    @Test
    fun `شاشة معدومة تعود للقص الثنائي الاحتياطي بلا انفجار`() {
        assertEquals(0f, ThemeDialMath.blendHalfSpan(0f), 1e-6f)
        assertEquals(1f, ThemeDialMath.blendMaskAlpha(50f, 100f, 0f, true), 1e-6f)
        assertEquals(0f, ThemeDialMath.blendMaskAlpha(150f, 100f, 0f, true), 1e-6f)
        assertEquals(0f, ThemeDialMath.blendMaskAlpha(50f, 100f, 0f, false), 1e-6f)
        assertEquals(1f, ThemeDialMath.blendMaskAlpha(150f, 100f, 0f, false), 1e-6f)
    }
}
