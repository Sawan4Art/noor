package com.sawan4art.noor.ui.home

import androidx.compose.ui.graphics.Path
import com.sawan4art.noor.ui.nav.addLiquidBlob

/**
 * مسارات «الحلقة السائلة» لمؤقت الصلاة — الشريط الدائري نفسه ماء حي.
 *
 * طلب المالك (alpha31): «ممكن نعمله شريط دائري عباره عن سائل حي برضو» —
 * فالحلقة هنا حافتان متموجتان (خارجية وداخلية بطور معاكس) تُرسم موضعاً
 * موضعاً بحسب نسبة الامتلاء، وعلى رأس الممتلئ **قطرة قائدة** زئبقية
 * وأخرى واقفة عند نقطة البداية كي يبقى الأنبوب حياً حتى وهو فارغ.
 */
fun addLiquidRingBand(
    path: Path,
    cx: Float,
    cy: Float,
    trackPx: Float,
    bandHalfPx: Float,
    sweepDeg: Float,
    time: Float,
    wobblePx: Float,
    phase: Float = 0f,
) {
    val sweep = sweepDeg.coerceIn(0f, 360f)

    // أنبوب فارغ: قطرة واقفة أعلى الحلقة عند نقطة البداية فقط
    if (sweep <= 0.5f) {
        val a = Math.toRadians(-90.0).toFloat()
        addLiquidBlob(
            path,
            CountdownLiquidMath.pointX(cx, trackPx, a),
            CountdownLiquidMath.pointY(cy, trackPx, a),
            bandHalfPx * 1.15f,
            time,
            0f,
            phase + 1.7f,
        )
        return
    }

    val full = sweep >= 359.5f
    val steps = if (full) 96 else (sweep / 4f).toInt().coerceIn(8, 96)

    // الحافة الخارجية من البداية إلى الرأس
    for (i in 0..steps) {
        val frac = i.toFloat() / steps
        val ang = Math.toRadians((-90.0 + sweep * frac).toDouble()).toFloat()
        val r = CountdownLiquidMath.outerEdge(trackPx, bandHalfPx, ang, time, phase, wobblePx)
        val x = CountdownLiquidMath.pointX(cx, r, ang)
        val y = CountdownLiquidMath.pointY(cy, r, ang)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    // الحافة الداخلية من الرأس عائداً إلى البداية — بطور معاكس
    for (i in steps downTo 0) {
        val frac = i.toFloat() / steps
        val ang = Math.toRadians((-90.0 + sweep * frac).toDouble()).toFloat()
        val r = CountdownLiquidMath.innerEdge(trackPx, bandHalfPx, ang, time, phase + 2.3f, wobblePx)
        path.lineTo(CountdownLiquidMath.pointX(cx, r, ang), CountdownLiquidMath.pointY(cy, r, ang))
    }
    path.close()

    // القطرات الطرفية — لا معنى لها في دائرة مكتملة
    if (!full) {
        val headRad = Math.toRadians(CountdownLiquidMath.headAngleDeg(sweep).toDouble()).toFloat()
        val tailRad = Math.toRadians(-90.0).toFloat()
        // رأس زئبقي أكبر قليلاً من الأنبوب — يسبق الامتلاء كقطرة ماء مدفوعة
        addLiquidBlob(
            path,
            CountdownLiquidMath.pointX(cx, trackPx, headRad),
            CountdownLiquidMath.pointY(cy, trackPx, headRad),
            bandHalfPx * 1.35f,
            time,
            0f,
            phase + 1.1f,
        )
        addLiquidBlob(
            path,
            CountdownLiquidMath.pointX(cx, trackPx, tailRad),
            CountdownLiquidMath.pointY(cy, trackPx, tailRad),
            bandHalfPx * 1.05f,
            time,
            0f,
            phase + 3.0f,
        )
    }
}
