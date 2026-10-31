package com.sawan4art.noor

import com.sawan4art.noor.ui.theme.ThemeDialMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * اختبارات «مزلاج الشمس والقمر» — قواعد المالك حتى تعديل alpha13:
 * 1) يمين = فاتح، يسار = مظلم، والأيقونة مع اليد حرفياً.
 * 2) الانقسام: النصف الفاتح يسار الحدّ والمظلم يمينه — **قصّ صلب** بلا قناع
 *    (الآلية القديمة فشلت بصمت على جهاز المالك — بلاغ بلقطة شاشة)، وعلى الحدّ
 *    **حزام انسياب** بخلفية الثيم الهدف نفسها — موك-أب المالك المرسوم يدوياً
 *    (2026-10-31 مساءً: «علشان تحاكي زيه») قيس بالبكسل: عبور 10%..90% بعرض
 *    0.15..0.20 من الشاشة بخطّية، منتظم الارتفاع بلا حدّ ولا بؤرة.
 * 3) الرفع يحسم بالضفة التي عبرها، والنقرة تبدّل فوراً.
 * 4) الإيماءة من الأيقونة وحدها — الفراغ يمر للطبقات تحتيه.
 * 5) المزلاج في الرئيسية فقط — لا أثر له في بقية الصفحات.
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

    // ===== حزام الانسياب — عقد المالك (موك-أب معدّل يدوياً «علشان تحاكي زيه») =====

    @Test
    fun `حزام الانسياب عريض منتظم بجزء معتدل من عرض الشاشة`() {
        // قياس الموك-أب بالبكسل: عبور 10%..90% امتدّ 0.15..0.20 من العرض —
        // الحزام الخطي الكامل ≈ 0.22: أضيق من ذلك يترك حدّ القص ظاهراً
        // (الشعاع المستقيم الذي أنكره المالك)، وأعرض من نصف الشاشة يبتلع الهدف
        assertTrue(
            "الحزام ${ThemeDialMath.WASH_FRACTION} أضيق من قياس الموك-أب",
            ThemeDialMath.WASH_FRACTION >= 0.15f,
        )
        assertTrue(
            "الحزام ${ThemeDialMath.WASH_FRACTION} أعرض من نصف الشاشة",
            ThemeDialMath.WASH_FRACTION <= 0.30f,
        )
        // العرض نسبي لعرض الشاشة كي يطابق شكل المالك على كل الأجهزة
        assertEquals(220f, ThemeDialMath.bandWidthPx(1000f), 1e-4f)
        assertEquals(0f, ThemeDialMath.bandWidthPx(0f), 1e-4f)
    }

    @Test
    fun `منتصف المسار بموضع اليد الحقيقي - نصف الشاشة لكل وضع`() {
        // المالك: «ممسك في المنتصف = نصف فاتح ونص مظلم» — الحدّ المستقيم عند الأيقونة
        // والتقدم عنده نصفاً تماماً والقرار عند العبور للفاتح
        val w = 1080f
        assertEquals(0.5f, ThemeDialMath.progress(mid, anchorLeft, anchorRight), 1e-5f)
        assertTrue(ThemeDialMath.commitLight(w / 2f, anchorLeft, anchorRight))
    }

    // ===== هدف المسح — الحد وحزامه يغادران الشاشة كليا بلا بقايا =====

    @Test
    fun `التثبيت الفاتح يساق الحد وحزامه خارج الحافة والعكس يسارا`() {
        val w = 1080f
        val band = ThemeDialMath.bandWidthPx(w)
        // فاتح: الحد عند w+الحزام — الحزام بكامل عرضه خلفه خارج الشاشة
        val endLight = ThemeDialMath.sweepTargetX(true, w, band)
        assertTrue("بقايا حزام فاتح عند الطرف", endLight - band >= w)
        // مظلم: المرآة يساراً بنفس العرض (متناسب — «والعكس صحيح»)
        val endDark = ThemeDialMath.sweepTargetX(false, w, band)
        assertTrue("بقايا حزام مظلم عند الطرف", endDark + band <= 0f)
    }

    // ===== متانة الحواف =====

    @Test
    fun `هدف المسح لا ينهار على حواف متطرفة`() {
        // انسياب معدوم ومسار معدوم — لا استثناءات ولا قيم متلاعبة
        val zeroLight = ThemeDialMath.sweepTargetX(true, 0f, 0f)
        assertTrue("يجب هامش أمان موجب للفاتح", zeroLight >= 1f)
        val zeroDark = ThemeDialMath.sweepTargetX(false, 0f, 0f)
        assertTrue("يجب هامش أمان سالب للمظلم", zeroDark <= -1f)
        ThemeDialMath.sweepTargetX(false, -100f, 0f)
        ThemeDialMath.progress(50f, 100f, 100f) // مسار معدوم
    }
}
