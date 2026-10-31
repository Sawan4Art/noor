package com.sawan4art.noor.ui.theme

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot

/**
 * رياضيات «مزلاج الشمس والقمر» — كوتلن خالص بلا أي اعتماديات Compose كي تُختبر وحدةً.
 *
 * قواعد المالك (2026-10-31) وتعديلاته (2026-10-31 مساءً):
 * 1) السحب لليمين = فاتح، وليسار = مظلم، والأيقونة تمشي مع اليد حرفياً
 *    (ما لم تُرفع اليد تبقى ممسوكة حيث هي — حتى في منتصف المسار).
 * 2) أثناء السحب تنقسم الشاشة نفسها: الفاتح خلف الأيقونة (جهة المغادرة)
 *    والمظلم أمامها — وحدّ الانقسام **خط مستقيم رأسي** (قرار المالك 2026-10-31
 *    مساءً: «خليه مستقيم تاني لكن اعمل عليه بلور») بحافة ريشية مموّهة
 *    بتدرج جيبي كوس — الضوء ينزلق ناعماً لا خط حادّ، والمنتصف نص/نص بالضبط.
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

    /** عرض ريش حافة الانقسام (التمويه بين الوضعين) — أوسع = ضوء أسموزي */
    const val MASK_FADE_DP = 168f

    /** عرض حزمة توهج الفاصل (شروق/غروب) */
    const val GLOW_BAND_DP = 176f

    /** نصف قطر توهج الأيقونة الشعاعي (مصدر الضوء) */
    const val GLOW_RADIAL_DP = 120f

    /** عدد شرائح ريش الحافة — أكبر عدد = تدرّج أنعمط أقرب للبلور الحقيقي */
    const val FEATHER_BANDS = 16

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
     * إزاحات شرائح الريش عن الحدّ — [K+1] قيمة صاعدة من −الشفق/2 إلى +الشفق/2:
     * المنطقة الهدف كاملة قبلهما، وشرائح ما بينها تتناوب بالشفافيات.
     * الحدّ مستقيم رأسياً فالإزاحات أفقية خالصة حول الأيقونة.
     */
    fun featherOffsets(fadePx: Float, bands: Int = FEATHER_BANDS): FloatArray {
        val k = bands.coerceAtLeast(1)
        val fade = fadePx.coerceAtLeast(1f)
        val out = FloatArray(k + 1)
        for (i in 0..k) out[i] = -fade / 2f + fade * i / k
        return out
    }

    /**
     * شفافية شريحة الريش رقم band (0..bands−1) — **تدرج جيبي (كوس) ناعم**:
     * تنزلق من شبه الكامل إلى الصفر بمشتقة صفرية عند الطرفين، فلا تبدو
     * الشرائح درجات متراصة بل بلوراً واحداً متصلاً — ومجموع متناظرتها 1 دائماً
     * (alpha(b) + alpha(k−1−b) = 1) فيبقى مركز التمويه على الحدّ نفسه نص/نص.
     */
    fun featherAlpha(band: Int, bands: Int = FEATHER_BANDS): Float {
        val k = bands.coerceAtLeast(1)
        val b = band.coerceIn(0, k - 1)
        val c = cos(PI * (b + 0.5f) / k).toFloat()
        return (c + 1f) / 2f
    }

    /**
     * هدف امتداد الحدّ عند التثبيت: يُساق خارج الشاشة بمقدار الريش كاملاً
     * كي تغطي الطبقة الهدف الشاشة كلها بلا حافة ظاهرة لحظة قلب الثيم
     * (الحدّ مستقيم فالهامش = نصف الريش خلف طرفه + هامش أمان).
     */
    fun sweepTargetX(commitLight: Boolean, screenW: Float, fadePx: Float): Float {
        val margin = fadePx.coerceAtLeast(1f)
        return if (commitLight) screenW + margin else -margin
    }

    /** قوة التوهج الذائبة مع تقدم الرحلة النهائية (0..1) — يحتفظ بحيويته حتى 70% ثم يذبل */
    fun glowDuringSweep(sweepFraction: Float): Float =
        (1f - (sweepFraction - 0.7f) / 0.3f).coerceIn(0f, 1f)
}
