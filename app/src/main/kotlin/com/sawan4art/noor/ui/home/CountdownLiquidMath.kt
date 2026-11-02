package com.sawan4art.noor.ui.home

import com.sawan4art.noor.ui.nav.GooeyMath
import kotlin.math.cos
import kotlin.math.sin

/**
 * رياضيات «مؤقت الصلاة السائل» — كوتلن خالص بلا أي اعتماديات Compose
 * كي تُختبر وحدةً (نمط GooeyMath نفسه).
 *
 * طلب المالك (alpha31):
 * «الشريط الذي يحيط ب الوقت المتبقي اجعله يتقدم مع تقدم انتهاء الوقت
 * و اجعل الوقت المتبقي داخل سائل حي و يلتف حوله الشريط الدائري الاسبق»
 * و«لو في شكل افضل من الشريط الدائري مثلا ممكن نعمله شريط دائري عباره
 * عن سائل حي برضو».
 *
 * فالتصميم المتبع:
 * - **النقطة السائلة الحية** في مركز الحلقة بداخلها العدّاد التنازلي
 *   (٠٣:٢٤:١٥) — بلغة المسبحة والقبلة نفسها: سطح متموج وتنفس وانسياب.
 * - **الشريط الدائري حولها صار سائلاً حياً** أيضاً: أنبوب ماء بحافة
 *   متموجة دائماً، يمتلئ كالزئبق موضعاً موضعاً **مع كل ثانية** من عمر
 *   الفترة حتى لحظة الأذان (نسبة الممتلئ = الماضي ÷ كامل الفترة —
 *   «يتقدم مع تقدم انتهاء الوقت»)، وعلى رأس السائل الممتلئ قطرة قائدة.
 *
 * كل الأبعاد بالبكسل الفعلي (px) — التحويل من dp يحدث في الشاشة.
 */
object CountdownLiquidMath {

    /** أقصى امتداد نسبي للنقطة السائلة: موجة الثبات (~٨٪) + التنفس (٣٫٥٪) */
    const val MAX_BLOB_EXTENT = 1.12f

    /** سعة نفَس القطرة المركزية (±نسبة من نصف قطرها) */
    const val BREATH_AMPLITUDE = 0.035f

    /** نقطة أمان: أدنى نصف قطر مقبول كي لا تنهار الحسابات على شاشة ضئيلة */
    const val MIN_RADIUS = 1f

    /**
     * نصف قطر خط وسط الأنبوب — مطروحاً منه هامش أمان خارجي [marginPx]
     * يحمي تموج الحافة الخارجية من خروجها عن الصندوق.
     */
    fun trackRadius(sidePx: Float, marginPx: Float): Float =
        (sidePx / 2f - marginPx).coerceAtLeast(MIN_RADIUS)

    /**
     * نصف قطر النقطة السائلة داخل الحلقة — بهوة [gapPx] تفصل سطحها
     * الأقصى عن الحافة الداخلية للأنبوب كي لا يحدث ملامسة مهما تموج.
     */
    fun dropRadius(ringInnerPx: Float, gapPx: Float): Float =
        (ringInnerPx - gapPx).coerceAtLeast(MIN_RADIUS)

    /** هل القطرة تنسع داخل الحلقة حتى في أسوأ امتداد لها؟ */
    fun fitsInsideRing(dropRadiusPx: Float, ringInnerPx: Float): Boolean =
        dropRadiusPx * MAX_BLOB_EXTENT < ringInnerPx

    /**
     * درجة امتلاء السباحة (٠..٣٦٠°) لنسبة التقدم — القيم الفاسدة
     * (NaN/لانهائي) تعود صفراً بأمان لا انهياراً.
     */
    fun sweepDegrees(fraction: Float): Float =
        if (!fraction.isFinite()) 0f else fraction.coerceIn(0f, 1f) * 360f

    /** زاوية رأس السائل بالدرجات — من الأعلى (−٩٠°) باتجاه عقارب الساعة */
    fun headAngleDeg(sweepDeg: Float): Float = -90f + sweepDeg

    /**
     * نصف قطر الحافة **الخارجية** للأنبوب عند زاوية [angleRad] —
     * خط الأنبوب + نصف سماكته + تموج سائل حي صغير [wobblePx].
     */
    fun outerEdge(
        trackPx: Float,
        bandHalfPx: Float,
        angleRad: Float,
        time: Float,
        phase: Float,
        wobblePx: Float,
    ): Float = trackPx + bandHalfPx + GooeyMath.surfaceWave(angleRad, time, 0f, phase) * wobblePx

    /**
     * نصف قطر الحافة **الداخلية** للأنبوب عند زاوية [angleRad] —
     * بطور معاكس للخارجية كي يتنفس الماء بين الحافتين كنهر حي.
     */
    fun innerEdge(
        trackPx: Float,
        bandHalfPx: Float,
        angleRad: Float,
        time: Float,
        phase: Float,
        wobblePx: Float,
    ): Float = trackPx - bandHalfPx + GooeyMath.surfaceWave(angleRad, time, 0f, phase) * wobblePx

    /** إحداثي س لموضع زاوية بالراديان حول مركز [cx] */
    fun pointX(cx: Float, radiusPx: Float, angleRad: Float): Float = cx + radiusPx * cos(angleRad)

    /** إحداثي ص لموضع زاوية بالراديان حول مركز [cy] */
    fun pointY(cy: Float, radiusPx: Float, angleRad: Float): Float = cy + radiusPx * sin(angleRad)

    /**
     * حجم خط العدّاد داخل القطرة — نسبة ثابتة من نصف قطرها محكومة
     * بسقف وأرضية كي يبقى مقروءاً في أكبر شاشة وأصغرها.
     */
    fun countdownFontSizeSp(dropRadiusDp: Float): Float =
        (dropRadiusDp * 0.30f).coerceIn(13f, 22f)
}
