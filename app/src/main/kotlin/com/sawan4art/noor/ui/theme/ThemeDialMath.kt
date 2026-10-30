package com.sawan4art.noor.ui.theme

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.sin

/**
 * رياضيات «مزلاج الشمس والقمر» — كوتلن خالص بلا أي اعتماديات Compose كي تُختبر وحدةً.
 *
 * قواعد المالك (2026-10-31) وتعديلاته (2026-10-31 مساءً):
 * 1) السحب لليمين = فاتح، وليسار = مظلم، والأيقونة تمشي مع اليد حرفياً
 *    (ما لم تُرفع اليد تبقى ممسوكة حيث هي — حتى في منتصف المسار).
 * 2) أثناء السحب تنقسم الشاشة نفسها: الفاتح خلف الأيقونة (جهة المغادرة)
 *    والمظلم أمامها — وحدّ الانقسام **قوس منحنٍ ينتفخ حول الأيقونة** لا خط مستقيم:
 *    «الضوء لا يستقيم بل يظهر كقوس»، وحافته **مموّهة** (ريش متدرج) لا حادة.
 * 3) عند الرفع: الضفة التي عبرها الإصبع من المنتصف تحسم الوضع،
 *    وإلا رجعت الأيقونة إلى مرساها بالسائل نفسه.
 * 4) النقرة (بلا سحب) = تبديل فوري برحلة الشمس من مرساها — نفس حيوية الشريط السفلي.
 * 5) تغيير المظهر لا يعمل إلا من خلال الأيقونة نفسها — لا الإمساك بالمسار ولا الفراغ.
 *
 * الاتجاه ثابت فيزيائياً (يمين الشاشة = فاتح) مهما كان اتجاه التخطيط RTL.
 */
object ThemeDialMath {

    /** أقصى مسافة حركة تُعدّ نقرة لا سحباً */
    const val TAP_SLOP_PX = 14f

    /** عرض ريش حافة الانقسام (التمويه بين الوضعين) — يُمرَّر بالبكسل من الواجهة */
    const val MASK_FADE_DP = 120f

    /** عرض حزمة توهج الفاصل (شروق/غروب) */
    const val GLOW_BAND_DP = 176f

    /** نصف قطر توهج الأيقونة الشعاعي (مصدر الضوء) */
    const val GLOW_RADIAL_DP = 120f

    /** انفتاح القوس: أقصى انسحاب للحافّة عن قمتها — كسر من ارتفاع الشاشة */
    const val ARC_SAG_FRACTION = 0.085f

    /** مدى القوس الرأسي: تبلغ الرجفة أقصاها ثم يستقيم الحدّ — كسر من ارتفاع الشاشة */
    const val ARC_REACH_FRACTION = 0.55f

    /** عدد شرائح ريش الحافة — كل شريحة شفافية ثابتة وتتتابع فتشبه التمويه */
    const val FEATHER_BANDS = 9

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
     * رجفة القوس عند بُعد رأسي عن قمته: sin² ناعمة — صفر عند القمة تماماً
     * (الحدّ يلمس الأيقونة نفسها) وأقصاها عند مدى القوس ثم تثبت، بلا انكسار عند القطع.
     */
    fun arcSag(dyAbs: Float, reachPx: Float, sagPx: Float): Float {
        val reach = reachPx.coerceAtLeast(1f)
        val t = (abs(dyAbs) / reach).coerceIn(0f, 1f)
        val s = sin(PI / 2.0 * t).toFloat()
        return sagPx * s * s
    }

    /**
     * موضع حدّ الانقسام المنحني عند ارتفاع y بالبكسل:
     * القمة عند الأيقونة نفسها، والطرفان ينسحبان **خلف** اتجاه التقدم —
     * فاتح يسار الحدّ (سحب يميناً): القمة أقصى نقطة يميناً والطرفان يسارها، والعكس صحيح.
     */
    fun edgeX(
        y: Float,
        apexX: Float,
        centerY: Float,
        reachPx: Float,
        sagPx: Float,
        lightOnLeft: Boolean,
    ): Float {
        val sag = arcSag(abs(y - centerY), reachPx, sagPx)
        return if (lightOnLeft) apexX - sag else apexX + sag
    }

    /**
     * إزاحات شرائح الريش عن الحدّ — [K+1] قيمة صاعدة من −الشفق/2 إلى +الشفق/2:
     * المنطقة الهدف كاملة قبلهما، وشرائح ما بينها تتناوب بالشفافيات.
     */
    fun featherOffsets(fadePx: Float, bands: Int = FEATHER_BANDS): FloatArray {
        val k = bands.coerceAtLeast(1)
        val fade = fadePx.coerceAtLeast(1f)
        val out = FloatArray(k + 1)
        for (i in 0..k) out[i] = -fade / 2f + fade * i / k
        return out
    }

    /**
     * شفافية شريحة الريش رقم band (0..bands−1): تنزلق من شبه الكامل إلى الصفر
     * بتتابع ناعم — مجموع الشرائح يرسم شفقيةً مموّهة بدل خط حادّ.
     */
    fun featherAlpha(band: Int, bands: Int = FEATHER_BANDS): Float {
        val k = bands.coerceAtLeast(1)
        val b = band.coerceIn(0, k - 1)
        return 1f - (b + 0.5f) / k
    }

    /**
     * هدف امتداد الحدّ عند التثبيت: يُساق خارج الشاشة بمقدار الريش كاملاً **ورجفة القوس**
     * كي تغطي الطبقة الهدف الشاشة كلها بلا حافة ظاهرة لحظة قلب الثيم
     * (أقصى نقطة للحدّ عند الطرف: القمة − رجفة القوس، والريش يمتد نصفه خلفها).
     */
    fun sweepTargetX(commitLight: Boolean, screenW: Float, fadePx: Float, sagPx: Float): Float {
        val margin = fadePx.coerceAtLeast(1f) + sagPx.coerceAtLeast(0f)
        return if (commitLight) screenW + margin else -margin
    }

    /** قوة التوهج الذائبة مع تقدم الرحلة النهائية (0..1) — يحتفظ بحيويته حتى 70% ثم يذبل */
    fun glowDuringSweep(sweepFraction: Float): Float =
        (1f - (sweepFraction - 0.7f) / 0.3f).coerceIn(0f, 1f)
}
