package com.sawan4art.noor.ui.theme

import kotlin.math.hypot

/**
 * رياضيات «مزلاج الشمس والقمر» — كوتلن خالص بلا أي اعتماديات Compose كي تُختبر وحدةً.
 *
 * قاعدة المالك (2026-10-31):
 * 1) السحب لليمين = فاتح، وليسار = مظلم، والأيقونة تمشي مع اليد حرفياً
 *    (ما لم تُرفع اليد تبقى ممسوكة حيث هي — حتى في منتصف المسار).
 * 2) أثناء السحب تنقسم الشاشة نفسها: الفاتح خلف الأيقونة (جهة المغادرة)
 *    والمظلم أمامها، والفاصل عند الأيقونة بتوهج متدرج يحاكي الشروق.
 * 3) عند الرفع: الضفة التي عبرها الإصبع من المنتصف تحسم الوضع،
 *    وإلا رجعت الأيقونة إلى مرساها بالسائل نفسه.
 * 4) النقرة (بلا سحب) = تبديل فوري برحلة الشمس من مرساها — نفس حيوية الشريط السفلي.
 *
 * الاتجاه ثابت فيزيائياً (يمين الشاشة = فاتح) مهما كان اتجاه التخطيط RTL.
 */
object ThemeDialMath {

    /** أقصى مسافة حركة تُعدّ نقرة لا سحباً */
    const val TAP_SLOP_PX = 14f

    /** عرض تدرّج حافة الانقسام (الشفق بين الوضعين) — يُمرَّر بالبكسل من الواجهة */
    const val MASK_FADE_DP = 96f

    /** عرض حزمة توهج الفاصل (شروق/غروب) */
    const val GLOW_BAND_DP = 176f

    /** نصف قطر توهج الأيقونة الشعاعي (مصدر الضوء) */
    const val GLOW_RADIAL_DP = 120f

    /** تقدّم المزلاج: 0 = مظلم (مرسى اليسار)، 1 = فاتح (مرسى اليمين) */
    fun progress(iconX: Float, anchorLeft: Float, anchorRight: Float): Float {
        val span = anchorRight - anchorLeft
        if (span <= 0f) return 0f
        return ((iconX - anchorLeft) / span).coerceIn(0f, 1f)
    }

    /** حصر موضع الأيقونة داخل المسار بين المرسيين */
    fun clampX(x: Float, anchorLeft: Float, anchorRight: Float): Float =
        x.coerceIn(anchorLeft, anchorRight)

    /** هل إيماءة الرفع نقرة (تبديل) أم سحب؟ */
    fun isTap(totalDx: Float, totalDy: Float, slopPx: Float = TAP_SLOP_PX): Boolean =
        hypot(totalDx, totalDy) <= slopPx

    /**
     * قرار التثبيت عند رفع اليد من السحب: الضفة التي يقف عليها الإصبع من منتصف
     * المسار تحسم الوضع (فاتح يمين المنتصف، مظلم يساره) — وإلا فالرجوع لمرسى البداية.
     */
    fun commitLight(releaseX: Float, anchorLeft: Float, anchorRight: Float): Boolean =
        releaseX >= (anchorLeft + anchorRight) / 2f

    /**
     * شفافيتا الشمس والقمر — الظهور التدريجي محصور في نطاق النصفة:
     * القمر ثابت كامل حتى 0.35 ثم يذوب لصالح الشمس حتى 0.65 (مجموعهما 1 دائماً).
     */
    fun sunAlpha(p: Float): Float = ((p - 0.35f) / 0.3f).coerceIn(0f, 1f)

    fun moonAlpha(p: Float): Float = sunAlpha(1f - p)

    /**
     * حدود قناع الطبقة الهدف — يُرجع مواضع وشفافيات متناوبة:
     * [p0, a0, p1, a1, ...] مواضع 0..1 مرتبة بدقة على عرض الشاشة.
     *
     * قاعدة الانقسام: المنطقة الهدف خلف الأيقونة (جهة المغادرة):
     * lightOnLeft=true: الفاتح يسار الحدّ (خلف شمسٍ مسحوبة يميناً) —
     * شفافية كاملة حتى (الحدّ − الشفق) ثم تذوب إلى صفر عند الحدّ نفسه،
     * فيظهر الثيم الأساسي تحت الأيقونة تحديداً والتوهج يضيء الحادّ.
     */
    fun maskStops(boundaryX: Float, screenW: Float, fadePx: Float, lightOnLeft: Boolean): FloatArray {
        val w = screenW.coerceAtLeast(1f)
        val fade = fadePx.coerceAtLeast(1f)
        return if (lightOnLeft) {
            // الفاتح يسار الحدّ: كامل حتى الحدّ − الشفق، يذوب إلى صفر عند الحدّ
            val f0 = (boundaryX - fade) / w
            val f1 = boundaryX / w
            val v: (Float) -> Float = { x ->
                when {
                    x <= f0 -> 1f
                    x >= f1 -> 0f
                    else -> 1f - (x - f0) / (f1 - f0)
                }
            }
            sampled(w, listOf(f0, f1), v)
        } else {
            // المظلم يمين الحدّ: صفر حتى الحدّ، يصعد إلى الكامل عند الحدّ + الشفق
            val f0 = boundaryX / w
            val f1 = (boundaryX + fade) / w
            val v: (Float) -> Float = { x ->
                when {
                    x <= f0 -> 0f
                    x >= f1 -> 1f
                    else -> (x - f0) / (f1 - f0)
                }
            }
            sampled(w, listOf(f0, f1), v)
        }
    }

    /**
     * بروفايل توهج الفاصل (الشروق): قمة عند الحدّ تذبل في الاتجاهين
     * — [p0, a0, p1, a1, ...] بذروة 1 تُضرب في لون التوهج وشفقته.
     */
    fun glowStops(boundaryX: Float, screenW: Float, bandPx: Float): FloatArray {
        val w = screenW.coerceAtLeast(1f)
        val half = (bandPx / 2f).coerceAtLeast(1f)
        val lo = (boundaryX - half) / w
        val hi = (boundaryX + half) / w
        val mid = boundaryX / w
        val v: (Float) -> Float = { x ->
            when {
                x <= lo || x >= hi -> 0f
                x <= mid -> (x - lo) / (mid - lo)
                else -> (hi - x) / (hi - mid)
            }
        }
        return sampled(w, listOf(lo, mid, hi), v)
    }

    /**
     * هدف امتداد القناع عند التثبيت: يُساق خارج الشاشة بمقدار الشفق
     * كي تغطي الطبقة الهدف الشاشة كلها بلا حافة ظاهرة لحظة قلب الثيم.
     */
    fun sweepTargetX(commitLight: Boolean, screenW: Float, fadePx: Float): Float =
        if (commitLight) screenW + fadePx else -fadePx

    /** قوة التوهج الذائبة مع تقدم الرحلة النهائية (0..1) — يحتفظ بحيويته حتى 70% ثم يذبل */
    fun glowDuringSweep(sweepFraction: Float): Float =
        (1f - (sweepFraction - 0.7f) / 0.3f).coerceIn(0f, 1f)

    /**
     * بناء القناع من دالة شفافية تحليلية: تُقيَّم الدالة عند المواضع المفتاحية
     * المحصورة إلى 0..1 (بعد إسقاط التكرارات وترتيبها) وعند طرفي الشاشة —
     * فلا تنهار الحدود حين يُساق الحدّ خارج الشاشة وقت التثبيت (لا وميض قلب).
     */
    private fun sampled(screenW: Float, keys: List<Float>, v: (Float) -> Float): FloatArray {
        val positions = (keys.map { it.coerceIn(0f, 1f) } + listOf(0f, 1f))
            .sorted()
            .distinctBy { (it * 10000f).toInt() } // تقريب إلى 1e-4 لضمان الصعود الدقيق
        val out = ArrayList<Float>(positions.size * 2)
        for (p in positions) {
            out.add(p)
            out.add(v(p).coerceIn(0f, 1f))
        }
        return out.toFloatArray()
    }
}
