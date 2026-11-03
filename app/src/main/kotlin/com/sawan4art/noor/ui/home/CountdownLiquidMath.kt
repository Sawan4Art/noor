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
 * - **الشريط الدائري حولها سائلاً حياً** أيضاً: أنبوب ماء بحافة
 *   متموجة دائماً، يمتلئ كالزئبق موضعاً موضعاً **مع كل ثانية** من عمر
 *   الفترة حتى لحظة الأذان (نسبة الممتلئ = الماضي ÷ كامل الفترة —
 *   «يتقدم مع تقدم انتهاء الوقت»)، وعلى رأس السائل الممتلئ قطرة قائدة.
 * - **الشريط جزء من النقطة نفسها** (توجيه المالك alpha32: «عايزه يكون
 *   عباره عن سائل حي محيط بالنقطه الكبيره و يكون خارج من النقطه
 *   الكبيره من نقطه البدايه و عند انتهاء الشريط المحيط يتم التصاقه
 *   بالنقطه الرئيسيه ثم يبدا الانفصال مره اخرى يعني الشريط المحيط
 *   بالنقطه كانه جزء منها نفسها») — فالسائل **يخرج من النقطة الكبيرة
 *   من نقطة البداية (الأعلى)** عبر **رقبة اتصال** سائلة تلحم النقطة
 *   بالأنبوب ([neckCenterRadius] و[neckRadius])، وعند اكتمال اللوب
 *   يعلق الرأس في نقطة البداية نفسها فيلتصق الشريط كله بالنقطة
 *   الرئيسية بدفعة وصول تتنفّس ([attachSurge])، ثم تبدأ الفترة الجديدة
 *   فينفصل السائل من جديد ويخرج يلتفّ — دورة خروج والتصاق لا تنقطع.
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

    // ===== رقبة الاتصال — الشريط يخرج من النقطة الكبيرة ويلتحق بها (alpha32) =====

    /**
     * بُعد مركز رقبة الاتصال عن مركز القطرة — منتصف المسافة بين سطح
     * النقطة الكبيرة وخط وسط الأنبوب عند نقطة البداية (الأعلى)، فتقع
     * الرقبة فوق الحافتين وتغمرهما معاً بلا فجوة.
     */
    fun neckCenterRadius(dropRadiusPx: Float, trackRadiusPx: Float): Float =
        (dropRadiusPx + trackRadiusPx) / 2f

    /**
     * نصف قطر رقبة الاتصال السائلة — أكبر من نصف الأنبوب كي تغمر حافة
     * الأنبوب من جهة وسطح النقطة المتموج من جهة أخرى وتلحمهما سائلةً
     * واحدة لا تنفصل.
     */
    fun neckRadius(bandHalfPx: Float): Float = bandHalfPx * 1.5f

    /**
     * دفعة الالتصاق عند اكتمال اللوب — من ٣٥٠° إلى ٣٦٠° تنهض من صفر
     * إلى واحد (منحنى خطي محكوم)، فينتفخ موضع الاتصال عند وصول رأس
     * السائل إلى النقطة الرئيسية كأن آخر قطرة التصقت بجسدها. القيم
     * الفاسدة تعود صفراً بأمان.
     */
    fun attachSurge(sweepDeg: Float): Float {
        if (!sweepDeg.isFinite()) return 0f
        val s = sweepDeg.coerceIn(0f, 360f)
        if (s < 350f) return 0f
        return (s - 350f) / 10f
    }

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
     *
     * alpha43 (بلاغ المالك: «الخط و ايقونه السائل الخاصه بالوقت المتبقي
     * صغيره هي كمان»): النسبة ٠.٣٠←٠.٣٦ والأرضية ١٣←١٦ والسقف ٢٢←٣٠ —
     * العدّاد بطل الشاشة فلا يجوز أن يكون أصغر من نص الجسم (كان ٢٢sp
     * بمعزل المقياس بينما متن الشاشة يُرى ٢٣٫٨ فوق الأساس).
     */
    fun countdownFontSizeSp(dropRadiusDp: Float): Float =
        (dropRadiusDp * 0.36f).coerceIn(16f, 30f)
}
