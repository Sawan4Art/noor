package com.sawan4art.noor

import com.sawan4art.noor.ui.theme.ThemeDialMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * اختبارات «مزلاج الشمس والقمر» — قواعد المالك حتى تعديل alpha16:
 * 1) يمين = فاتح، يسار = مظلم، والأيقونة مع اليد حرفياً.
 * 2) الانقسام: النصف الفاتح يسار الحدّ والمظلم يمينه — **قصّ صلب** بلا قناع
 *    (الآلية القديمة فشلت بصمت على جهاز المالك — بلاغ بلقطة شاشة)، والفصل
 *    **حاد وصريح** وعلى الحدّ خط ضوء رفيع بهالة ضيقة مركّزة عليه حصرياً.
 * 3) الرفع يحسم بالضفة التي عبرها، والنقرة تبدّل فوراً.
 * 4) الإيماءة من الأيقونة وحدها — الفراغ يمر للطبقات تحتيه.
 * 5) المزلاج في الرئيسية فقط — لا أثر له في بقية الصفحات.
 * 6) خط الضوء (alpha16): **شدة إضاءته ديناميكية = نسبة النهار على الشاشة** —
 *    تبدأ ضعيفة وتزداد مع السحب نحو الفاتح حتى شدة النهار الكاملة، والعكس
 *    نحو المظلم تبدأ عالية وتنحدر حتى الانطفاء تماماً («والعكس صحيح»).
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

    // ===== هدف المسح — الحد وخط ضوئه يغادران الشاشة كليا بلا بقايا =====

    @Test
    fun `التثبيت الفاتح يساق الحد وخط ضوئه خارج الحافة والعكس يسارا`() {
        val w = 1080f
        // نصف هالة الضوء بالبكسل (7dp عند 3x ≈ 21px — 40f هامش أمان أكبر)
        val extent = 40f
        // فاتح: الحد عند w+الهالة — الهالة بكامل امتدادها خلفه خارج الشاشة
        val endLight = ThemeDialMath.sweepTargetX(true, w, extent)
        assertTrue("بقايا توهج فاتح عند الطرف", endLight - extent >= w)
        // مظلم: المرآة يساراً بنفس الامتداد (متناسب — «والعكس صحيح»)
        val endDark = ThemeDialMath.sweepTargetX(false, w, extent)
        assertTrue("بقايا توهج مظلم عند الطرف", endDark + extent <= 0f)
    }

    // ===== شدة الضوء المنبعث من النهار — توجيه المالك alpha16 =====

    @Test
    fun `شدة الضوء صفر في المظلم الكامل وكاملة في الفاتح الكامل`() {
        val w = 1080f
        assertEquals(0f, ThemeDialMath.beamIntensity(0f, w), 1e-5f)
        // الحدّ مسوق خارج الشاشة — المظلم الكامل بلا أي بقايا ضوء
        assertEquals(0f, ThemeDialMath.beamIntensity(-40f, w), 1e-5f)
        // الفاتح الكامل: نفس شدة إضاءة الوضع النهاري حرفياً
        assertEquals(1f, ThemeDialMath.beamIntensity(w, w), 1e-5f)
        assertEquals(1f, ThemeDialMath.beamIntensity(w + 40f, w), 1e-5f)
    }

    @Test
    fun `شدة الضوء تبدأ ضعيفة مرئية وتزداد مع كل بكسل سحب نحو الفاتح`() {
        val w = 1080f
        // بداية السحب من مرسى المظلم: ضعيفة لكنها تُرى — لا صفر ولا سطوع كامل
        val start = ThemeDialMath.beamIntensity(54f, w)
        assertTrue("البداية يجب أن تكون ضعيفة: $start", start < 0.45f)
        assertTrue("البداية يجب أن تُرى: $start", start > 0.05f)
        // النمو رتيب مع كل بكسل سحب حتى آخر الشاشة
        var x = 54f
        var prev = start
        while (x < w) {
            x += 30f
            val cur = ThemeDialMath.beamIntensity(x, w)
            assertTrue("الشدة يجب أن تزداد مع السحب عند x=$x", cur > prev)
            prev = cur
        }
        assertEquals(1f, ThemeDialMath.beamIntensity(w, w), 1e-5f)
    }

    @Test
    fun `شدة الضوء تبدأ عالية عند السحب نحو المظلم وتنحدر حتى الانطفاء`() {
        val w = 1080f
        // البداية من مرسى الفاتح: شديدة عالية
        val start = ThemeDialMath.beamIntensity(1026f, w)
        assertTrue("البداية من الفاتح يجب أن تكون عالية: $start", start > 0.9f)
        // الانحدار رتيب نحو الصفر تماماً في النهاية
        var x = 1026f
        var prev = start
        while (x > 0f) {
            x -= 30f
            val cur = ThemeDialMath.beamIntensity(x, w)
            assertTrue("الشدة يجب أن تنحدر تدريجياً عند x=$x", cur < prev)
            prev = cur
        }
        assertEquals(0f, ThemeDialMath.beamIntensity(0f, w), 1e-5f)
    }

    @Test
    fun `شدة الضوء محصورة ومتينة على الحواف المتطرفة`() {
        assertTrue(ThemeDialMath.beamIntensity(5000f, 1080f) <= 1f)
        assertTrue(ThemeDialMath.beamIntensity(-5000f, 1080f) >= 0f)
        // شاشة معدومة أو سالبة — صفر بلا استثناءات ولا قيم متلاعبة
        assertEquals(0f, ThemeDialMath.beamIntensity(100f, 0f), 1e-5f)
        assertEquals(0f, ThemeDialMath.beamIntensity(100f, -50f), 1e-5f)
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
