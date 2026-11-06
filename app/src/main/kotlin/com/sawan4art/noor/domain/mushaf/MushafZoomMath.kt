package com.sawan4art.noor.domain.mushaf

import kotlin.math.abs

/**
 * رياضيات تكبير صفحات المصحف — توجيه المالك ٢٠٢٦-١١-٠٦:
 * «اضف امكانيه عمل زوم في صفحات المصحف».
 *
 * وحدات نقية بلا أندرويد — تُختبر آلياً:
 *
 * • **القرص بإصبعين** يكبّر ويصغّر حول مركز القرص (المشطوط يظل تحت
 *   الإصبعين ما أمكن) بحدود MIN_SCALE..MAX_SCALE.
 * • **النقرة المزدوجة** تقلب بين ١× ومستوى التكبير المرتاح [DOUBLE_TAP_SCALE]
 *   حول نقطة الضغطة، والعودة من أي تكبيرٍ إلى ١× تُصفر الإزاحة كي لا
 *   تظل الصفحة معلقة جانباً.
 * • **الإزاحة** أثناء التكبير تُحصر دائماً بحيث تغطي الصفحة المكبَّرة
 *   مساحة العرض كلها — لا هوامش سوداء تظهر مهما جرّ السحب.
 * • الصفحة عند ١× بالضبط تُعامل «غير مكبَّرة» فتعود إيماءة السحب الأفقي
 *   لتقليب الصفحات (isZoomed).
 */
object MushafZoomMath {

    /** أصغر معامل تكبير — العرض العادي الطبيعي */
    const val MIN_SCALE = 1f

    /** أكبر معامل تكبير — يكفي قراءة السطر صفاً صفاً */
    const val MAX_SCALE = 4.5f

    /** مستوى النقرة المزدوجة — قفزة واحدة مرتاحة للقراءة */
    const val DOUBLE_TAP_SCALE = 2.5f

    /// تسامح معامل التكبير فوق الوحدة ليعدّ «مكبَّراً» — يقاوم فتات الفلوط
    const val ZOOMED_EPSILON = 0.02f

    /** هل المعامل الحالي يعني صفحة مكبَّرة (وإذن الإزاحة تعمل والتنقل متوقف) */
    fun isZoomed(scale: Float): Boolean = scale > MIN_SCALE + ZOOMED_EPSILON

    /**
     * معامل التكبير الجديد بعد قرصٍ بمعامل [zoomChange] — مضروباً في
     * الحالي ومحصوراً في الحدود دائماً فلا يتجاوز القصوى ولا يهبط تحت
     * الوحدة (الصغَر دون ١× يُقرأ منع الارتداد: يلتصق عند ١×).
     */
    fun clampScale(current: Float, zoomChange: Float): Float =
        (current * zoomChange).coerceIn(MIN_SCALE, MAX_SCALE)

    /**
     * إزاحة محصورة لمعامل تكبير معطى — التحويل: s = المركز +
     * (c − المركز)·scale + off، ومجال المحتوى [0..viewW]، فتغطية
     * العرض بلا هوامش سوداء تعني: حافة المحتوى اليسرى ≤ ٠ واليمنى ≥
     * العرض، فيخرج المدى المسموح للإزاحة متماثلاً حول الصفر:
     * ±نصف العرض×(scale−1). عند ١× تكون الإزاحة صفراً دائماً.
     */
    fun clampOffset(scale: Float, offsetX: Float, offsetY: Float, viewW: Float, viewH: Float): Pair<Float, Float> {
        if (!isZoomed(scale)) return 0f to 0f
        val limitX = (viewW / 2f) * (scale - 1f)
        val limitY = (viewH / 2f) * (scale - 1f)
        return offsetX.coerceIn(-limitX, limitX) to offsetY.coerceIn(-limitY, limitY)
    }

    /**
     * إزاحة ناتجة عن سحبٍ بإصبع — تُجمع على الحالية وتُحصر كي لا تخرج
     * الصفحة عن حوافها أبداً.
     */
    fun pannedOffset(
        currentX: Float, currentY: Float,
        panX: Float, panY: Float,
        scale: Float, viewW: Float, viewH: Float,
    ): Pair<Float, Float> = clampOffset(scale, currentX + panX, currentY + panY, viewW, viewH)

    /**
     * تكبير القرص حول نقطة [centroid] — النقطة تحت الإصبعين تظل ثابتة:
     * الإزاحة الجديدة = (المركز − النقطة)×scale حيث المركز = مركز العرض
     * مطروحاً منه نصف الإزاحة الحالية… بصياغة أوضح: النقطة السكرينية
     * تُحوَّل إلى إحداثيات المحتوى ثم تُعاد إلى السكرين بالمعامل الجديد
     * والإزاحة الجديدة لتبقى مطابقة. يُعاد أيضاً بالحصر النهائي فلا
     * يسمح القرص بكسر حدود التغطية.
     *
     * @param cx, cy مركز القرص بإحداثيات مساحة العرض
     */
    fun pinchedOffset(
        scaleBefore: Float, scaleAfter: Float,
        offsetX: Float, offsetY: Float,
        cx: Float, cy: Float,
        viewW: Float, viewH: Float,
    ): Pair<Float, Float> {
        if (abs(scaleAfter - scaleBefore) < 1e-6f) {
            return clampOffset(scaleAfter, offsetX, offsetY, viewW, viewH)
        }
        // نقطة السكرين تحت الإصبعين ثابتة: s = center + (c−center)·k + off
        // ⟹ off_after = (s − center)·(1 − ratio) + off_before·ratio
        val halfW = viewW / 2f
        val halfH = viewH / 2f
        val ratio = scaleAfter / scaleBefore
        val nx = (cx - halfW) * (1f - ratio) + offsetX * ratio
        val ny = (cy - halfH) * (1f - ratio) + offsetY * ratio
        return clampOffset(scaleAfter, nx, ny, viewW, viewH)
    }

    /**
     * حالة النقرة المزدوجة — من غير مكبَّر إلى [DOUBLE_TAP_SCALE] حول
     * نقطة الضغطة، ومن مكبَّر (أي مستوى) إلى العرض الطبيعي مصفَّرة
     * الإزاحة. يُعاد زوج (المعامل، الإزاحة).
     */
    fun doubleTapped(
        scale: Float, offsetX: Float, offsetY: Float,
        tapX: Float, tapY: Float,
        viewW: Float, viewH: Float,
    ): Pair<Float, Pair<Float, Float>> {
        return if (isZoomed(scale)) {
            MIN_SCALE to (0f to 0f)
        } else {
            val target = clampOffset(
                DOUBLE_TAP_SCALE,
                tapX - viewW / 2f - (tapX - viewW / 2f) * DOUBLE_TAP_SCALE,
                tapY - viewH / 2f - (tapY - viewH / 2f) * DOUBLE_TAP_SCALE,
                viewW, viewH,
            )
            DOUBLE_TAP_SCALE to target
        }
    }

    /**
     * تحويل نقطة سكرين إلى نقطة محتوى (فراغ الصفحة غير المكبَّر) —
     * عكس: s = center + (c − center)·scale + off ⟹
     * c = (s − center − off) / scale + center. يُستخدم لمس الآيات بعد
     * التكبير فتبقى المعاينة مليمترية مع الصفحة المكبَّرة.
     */
    fun toContent(sx: Float, sy: Float, scale: Float, offX: Float, offY: Float, viewW: Float, viewH: Float): Pair<Float, Float> {
        val halfW = viewW / 2f
        val halfH = viewH / 2f
        return Pair((sx - halfW - offX) / scale + halfW, (sy - halfH - offY) / scale + halfH)
    }
}
