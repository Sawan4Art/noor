package com.sawan4art.noor.ui.nav

import kotlin.math.abs

/**
 * رياضيات «القطرة السائلة» — كوتلن خالص بلا أي اعتماديات Compose
 * كي تُختبر وحدةً وتُعاد معاينتها ببايثون بنفس الأرقام حرفياً.
 *
 * الجسر (bridge) هو الرقبة المقعّرة التي تصل دائرتين فيُشعران بأنهما
 * سائل واحد يتفصل ويندمج — تقنية metaball بمنحنيات بيزيه وضبط وسطي.
 */
object GooeyMath {

    /** أقصى امتداد أفقي للقطة عند بعد كامل عن الهدف (نسبة) */
    private const val MAX_STRETCH = 0.45f

    /**
     * معامل التمطط الأفقي للقطرة مع اقترابها من الهدف:
     * عند البعد الكامل تتمطط (1 + MAX_STRETCH) وعند الوصول تكتمل دائرة.
     * [distance] البعد الحالي عن الهدف، [travelScale] مرجع البعد (عرض تبويب مثلًا).
     */
    fun stretchFactor(distance: Float, travelScale: Float): Float {
        if (travelScale <= 0f) return 1f
        val t = (distance / travelScale).coerceIn(0f, 1f)
        return 1f + MAX_STRETCH * t * t // تربيعية: تمطط خفيف في البداية ثم أوضح في منتصف الرحلة
    }

    /** الضغط العمودي المقابل للحفاظ على مساحة القطرة أثناء التمطط (سلوك السائل الحقيقي) */
    fun squashFactor(stretchX: Float): Float = if (stretchX <= 0f) 1f else 1f / stretchX

    /**
     * وصف جسر الجيلي بين دائرتين متمركزتين على خط أفقي واحد.
     * الدائرة الأولى في الناتج هي الأيسر دائمًا مهما كان ترتيب الإدخال.
     * يُرجع null إذا كانت الدائرتان بعيدتين (لا جسر) أو متطابقتين تمامًا.
     *
     * [waist] (0..1) كم تُسحب مناصف المنحنى نحو المنتصف — أكبر = رقبة أنحف.
     */
    fun bridge(
        x1: Float, y1: Float, r1: Float,
        x2: Float, y2: Float, r2: Float,
        mergeDistance: Float,
        waist: Float = 0.42f,
    ): GooeyBridge? {
        // ترتيب: الدائرة اليسرى أولًا كي تكون قبب الأقواس باتجاه ثابت
        val ax: Float; val ay: Float; val ar: Float
        val bx: Float; val by: Float; val br: Float
        if (x1 <= x2) {
            ax = x1; ay = y1; ar = r1; bx = x2; by = y2; br = r2
        } else {
            ax = x2; ay = y2; ar = r2; bx = x1; by = y1; br = r1
        }

        val d = bx - ax
        if (d < 1e-4f) return null
        if (d >= mergeDistance) return null
        if (d <= abs(ar - br)) return null // واحدة داخل الأخرى — الاندماج تلقائي بلا جسر

        val mx = (ax + bx) / 2f
        val my = (ay + by) / 2f

        // نقاط الالتقاء عمودية على خط المراكز (الجسور أفقية دائمًا في شريطنا)
        val p1y = ay - ar // أعلى اليسرى
        val p2y = ay + ar // أسفل اليسرى
        val p3y = by - br // أعلى اليمنى
        val p4y = by + br // أسفل اليمنى

        // مناصف منحنيات بيزيه تُسحب نحو مركز المسافة = رقبة مقعّرة سائلة
        val qAx = ax + (mx - ax) * waist
        val qBx = bx + (mx - bx) * waist

        return GooeyBridge(
            ax = ax, ay = ay, ar = ar,
            bx = bx, by = by, br = br,
            p1x = ax, p1y = p1y,
            qA1x = qAx, qA1y = p1y + (my - p1y) * waist,
            qA2x = qBx, qA2y = p3y + (my - p3y) * waist,
            p3x = bx, p3y = p3y,
            p4x = bx, p4y = p4y,
            qB2x = qBx, qB2y = p4y + (my - p4y) * waist,
            qB1x = qAx, qB1y = p2y + (my - p2y) * waist,
            p2x = ax, p2y = p2y,
        )
    }

    /** نصف قطر النقطة المنفصلة: صغيرة عند الانفصال وتكبر كلما اقتربت من الاندماج */
    fun dropletRadius(baseRadius: Float, distance: Float, mergeDistance: Float): Float {
        if (mergeDistance <= 0f) return baseRadius
        val closeness = (1f - distance / mergeDistance).coerceIn(0f, 1f)
        return baseRadius * (0.55f + 0.45f * closeness)
    }

    /** هل يلزم رسم الجسر بين نقطتين؟ (فحص سريع قبل بناء المسار) */
    fun hasBridge(distance: Float, mergeDistance: Float): Boolean =
        distance < mergeDistance && distance > 1e-4f
}

/**
 * كل نقاط الجسر بترتيب الرسم:
 * أعلى-يسار ← منحنى علوي ← أعلى-يمين ← قبة يمين ← أسفل-يمين ← منحنى سفلي ← أسفل-يسار ← قبة يسار
 * الدائرة «أ» دائمًا الأيسر والدائرة «ب» الأيمن.
 */
class GooeyBridge(
    val ax: Float, val ay: Float, val ar: Float,
    val bx: Float, val by: Float, val br: Float,
    val p1x: Float, val p1y: Float,
    val qA1x: Float, val qA1y: Float,
    val qA2x: Float, val qA2y: Float,
    val p3x: Float, val p3y: Float,
    val p4x: Float, val p4y: Float,
    val qB2x: Float, val qB2y: Float,
    val qB1x: Float, val qB1y: Float,
    val p2x: Float, val p2y: Float,
)
