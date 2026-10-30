package com.sawan4art.noor.ui.nav

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/**
 * رياضيات «النقل السائل المتصل» — كوتلن خالص بلا أي اعتماديات Compose
 * كي تُختبر وحدةً وتُعاد معاينتها ببايثون بنفس الأرقام حرفياً.
 *
 * قاعدة المالك (2026-10-30): السائل ينتقل من الزر القديم إلى الجديد **دون فصل** —
 * ينقص من القديم ويزيد في الجديد عبر جسر يبقى موصولاً حتى يكتمل النقل كله،
 * والسائل نفسه حيّ لا يسكن: تموج سطحي وانسياب ورفرفة دائمة كقطرة ماء لزجة.
 */
object GooeyMath {

    /** أدنى نصف قطر يُرسم له شكل — تحته يُعد السائل قد انتقل بالكامل */
    const val MIN_RADIUS = 1.2f

    /**
     * ضنك رقبة الجسر يتكيف مع ما تبقى في المصدر: مصدر ممتلئ = رقبة سمينة (0.42)،
     * وكلما نفد المصدر ضاقت حتى تصير خيطاً يُسحب فيه آخر قطرة (→0.97).
     */
    fun waistFor(sourceRadius: Float, fullRadius: Float): Float {
        if (fullRadius <= 0f) return 0.97f
        val remaining = (sourceRadius / fullRadius).coerceIn(0f, 1f)
        return 0.42f + 0.55f * (1f - remaining).pow(1.5f)
    }

    /**
     * الجسر السائل بين دائرتين على خط أفقي — **بلا حد مسافة إطلاقاً**:
     * يبقى موصولاً مهما بعدت النقطتان (وهذا جوهر قاعدة «لا فصل»).
     * يُرجع null فقط عند التطابق التام أو احتواء إحداهما داخل الأخرى —
     * حينئذٍ يكفي رسم الدائرتين وحدهما فالاندماج تلقائي.
     * الدائرة الأولى في الناتج هي الأيسر دائماً مهما كان ترتيب الإدخال.
     * [waist] (0..1) كم يُسحب من مناصف المنحنيات نحو محور المراكز — أكبر = رقبة أنحف.
     */
    fun bridge(
        x1: Float, y1: Float, r1: Float,
        x2: Float, y2: Float, r2: Float,
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
        if (d <= abs(ar - br)) return null // واحدة داخل الأخرى — الاندماج تلقائي بلا جسر

        val mx = (ax + bx) / 2f
        val my = (ay + by) / 2f

        // نقاط الالتقاء عند قطبي الدائرتين العموديين على خط المراكز
        val p1y = ay - ar // أعلى اليسرى
        val p2y = ay + ar // أسفل اليسرى
        val p3y = by - br // أعلى اليمنى
        val p4y = by + br // أسفل اليمنى

        // مناصف منحنيات بيزيه تُسحب نحو محور المراكز = رقبة مقعّرة سائلة
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

    /**
     * التموج السطحي الدائم — قلب «السائل الحي»:
     * ثلاث موجات سفر بترددات مختلفة في الثبات، وموجتان أقوى بطاقة الهبوط [energy]
     * (0..1) التي تذبل بعد وصول الرأس. الناتج إزاحة نسبية لنصف القطر (±8% تقريباً).
     *
     * دفعة alpha4 (بطلب المالك: لزوجة أعلى وحركة أبطأ): ترددات مكانية وزمنية أقل
     * (موجات أطول وأبطأ = سائل ألزق كالعسل) وسعات أوضح — وقمة الثبات+الطاقة مجتمعة
     * ≤ 0.187 داخل سقف الأمان 0.2. ومعاينة بايثون scripts/make_liquid_preview_gif.py
     * بنفس هذه الأرقام حرفياً.
     */
    fun surfaceWave(angle: Float, time: Float, energy: Float, phase: Float = 0f): Float {
        val idle = 0.038f * sin(2f * angle + 1.0f * time + phase) +
            0.028f * sin(4f * angle - 1.5f * time + 2.1f * phase) +
            0.017f * sin(6f * angle + 2.1f * time + 4.2f * phase)
        val live = energy * (
            0.062f * sin(4f * angle + 4.8f * time + phase) +
                0.042f * sin(7f * angle - 3.6f * time + 1.7f * phase)
            )
        return idle + live
    }

    /** انسياب رأسي بطيء للمركز — السائل يعوم في الزجاج بدل أن يثبت (أبطأ في alpha4) */
    fun centerDrift(time: Float, phase: Float = 0f): Float =
        0.040f * sin(0.7f * time + phase) + 0.024f * sin(1.3f * time + 1.3f + phase)

    /** موجة الجسر (−1..1) — تُضرب في مقياس صغير فترفرف الرقبة كماء حي (أبطأ في alpha4) */
    fun bridgeSway(time: Float, phase: Float = 0f): Float =
        0.6f * sin(1.2f * time + phase) + 0.4f * sin(1.9f * time + 1.9f + phase)
}

/**
 * كل نقاط الجسر بترتيب الرسم:
 * أعلى-يسار ← منحنى علوي ← أعلى-يمين ← قبة يمين ← أسفل-يمين ← منحنى سفلي ← أسفل-يسار ← قبة يسار
 * الدائرة «أ» دائمًا الأيسر والدائرة «ب» الأيمن.
 * حقول منحنيات الرقبة متغيرة كي تُدمج فيها موجة الرفرفة وقت الرسم مباشرة.
 */
class GooeyBridge(
    val ax: Float, val ay: Float, val ar: Float,
    val bx: Float, val by: Float, val br: Float,
    val p1x: Float, val p1y: Float,
    var qA1x: Float, var qA1y: Float,
    var qA2x: Float, var qA2y: Float,
    val p3x: Float, val p3y: Float,
    val p4x: Float, val p4y: Float,
    var qB2x: Float, var qB2y: Float,
    var qB1x: Float, var qB1y: Float,
    val p2x: Float, val p2y: Float,
)

/** نقطة سائلة حية: دائرة سطحوها متموج بتموج GooeyMath ومنعّم بكاتمول-روم مغلق */
fun addLiquidBlob(
    path: Path,
    cx: Float,
    cy: Float,
    baseRadius: Float,
    time: Float,
    energy: Float,
    phase: Float,
) {
    val n = 44
    val xs = FloatArray(n)
    val ys = FloatArray(n)
    val twoPi = (2.0 * PI).toFloat()
    for (i in 0 until n) {
        // البداية من الأعلى والدوران باتجاه عقارب الساعة — مواكباً لاتجاه مسار الجسر
        val angle = twoPi * i / n - PI.toFloat() / 2f
        val radius = baseRadius * (1f + GooeyMath.surfaceWave(angle, time, energy, phase))
        xs[i] = cx + radius * cos(angle)
        ys[i] = cy + radius * sin(angle)
    }
    for (i in 0 until n) {
        val prev = (i + n - 1) % n
        val next = (i + 1) % n
        val next2 = (i + 2) % n
        if (i == 0) path.moveTo(xs[i], ys[i])
        path.cubicTo(
            xs[i] + (xs[next] - xs[prev]) / 6f,
            ys[i] + (ys[next] - ys[prev]) / 6f,
            xs[next] - (xs[next2] - xs[i]) / 6f,
            ys[next] - (ys[next2] - ys[i]) / 6f,
            xs[next],
            ys[next],
        )
    }
    path.close()
}

/** بناء مسار الجسر السائل داخل مسار السائل الموحد — قبتان دقيقتان وحافتا بيزيه مقعّرتان */
fun addBridgePath(path: Path, b: GooeyBridge) {
    path.moveTo(b.p1x, b.p1y)
    path.cubicTo(b.qA1x, b.qA1y, b.qA2x, b.qA2y, b.p3x, b.p3y)
    path.arcTo(Rect(b.bx - b.br, b.by - b.br, b.bx + b.br, b.by + b.br), -90f, 180f, false)
    path.cubicTo(b.qB2x, b.qB2y, b.qB1x, b.qB1y, b.p2x, b.p2y)
    path.arcTo(Rect(b.ax - b.ar, b.ay - b.ar, b.ax + b.ar, b.ay + b.ar), 90f, 180f, false)
    path.close()
}
