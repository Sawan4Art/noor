package com.sawan4art.noor.ui.theme

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import com.sawan4art.noor.ui.icons.NoorIcons
import com.sawan4art.noor.ui.nav.GooeyMath
import com.sawan4art.noor.ui.nav.addBridgePath
import com.sawan4art.noor.ui.nav.addLiquidBlob
import kotlinx.coroutines.launch

/**
 * مزلاج الشمس والقمر — طبقة فوق كل شيء (فوق طبقتي الثيم):
 * كبسولة المسار + السائل الحي (ذيل ورأس وجسر بلا فصل) + توهج الشروق عند الحدّ
 * + أيقونتا الشمس والقمر تتقاطفان بموضع اليد.
 *
 * الإيماءة: قبضة على المسار كله؛ الحركة الأفقية تحرك الأيقونة مع اليد حرفياً،
 * والرفع يحسم بالضفة، والنقرة تبدّل. الاتجاه فيزيائي: يمين = فاتح.
 */
@Composable
fun ThemeDialOverlay(
    dial: ThemeDialState,
    committedDark: Boolean,
    onCommitLight: (Boolean) -> Unit,
) {
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val currentCommittedDark by rememberUpdatedState(committedDark)
    val currentCommit by rememberUpdatedState(onCommitLight)

    // مقاييس بالبكسل — نفس لغة الشريط السفلي (قطرات أكبر قليلاً من شريط رفيع)
    val blobR = with(density) { 17.dp.toPx() }
    val fadePx = with(density) { ThemeDialMath.MASK_FADE_DP.dp.toPx() }
    val bandPx = with(density) { ThemeDialMath.GLOW_BAND_DP.dp.toPx() }
    val radialPx = with(density) { ThemeDialMath.GLOW_RADIAL_DP.dp.toPx() }
    val iconPx = with(density) { 24.dp.toPx() }
    val strokePx = with(density) { 1.dp.toPx() }
    val anchorPad = blobR * 1.6f

    SideEffect {
        dial.configure(dial.screenWidth, blobR, fadePx, bandPx, radialPx, anchorPad)
    }

    // مزامنة المرسى مع الثيم المثبت (أول قياس + تغيّر الإعدادات + الدوران)
    LaunchedEffect(committedDark, dial.trackWidth) {
        dial.syncTo(committedDark, scope)
    }

    // زمن حي لا يتوقف — السائل لا يسكن أبداً
    var frameNanos by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            withInfiniteAnimationFrameNanos { frameNanos = it }
        }
    }

    // ألوان الثيم المثبت — الكبسولة والحدود والسائل والتوهج
    val scheme = MaterialTheme.colorScheme
    val capsuleFill = scheme.surface.copy(alpha = 0.45f)
    val capsuleBorder = scheme.outline.copy(alpha = 0.35f)
    val dark = scheme.background.luminance() < 0.5f
    val sunTop = Color(0xFFFFE2AE)
    val sunBottom = Color(0xFFF4A85C)
    val moonTop = Color(0xFFD7E3F4)
    val moonBottom = Color(0xFF8FA6C8)
    val sunTint = Color(0xFF452B10)
    val moonTint = Color(0xFF1A2433)
    val sunriseBand = Color(0xFFFFB968)
    val sunriseCore = Color(0xFFFFDFA6)
    val moonBand = Color(0xFFAFC6EA)
    val moonCore = Color(0xFFD6E4FA)

    val sunPainter = rememberVectorPainter(NoorIcons.Sun)
    val moonPainter = rememberVectorPainter(NoorIcons.Moon)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { dial.configure(it.width.toFloat(), blobR, fadePx, bandPx, radialPx, anchorPad) }
            .semantics {
                contentDescription =
                    "مزلاج المظهر — اسحب يميناً للوضع الفاتح ويساراً للوضع المظلم، أو انقر للتبديل"
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    if (dial.busy || dial.dragging || !dial.isInsideTrack(down.position.x, down.position.y)) {
                        return@awaitEachGesture // خارج المسار: يمر للطبقات تحتيه بلا استهلاك
                    }
                    down.consume()
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    // داخل awaitEachGesture تُمنع التعليقات العامة — تعديلات الحالة
                    // عبر مسرح النطاق الرئيسي فتتسلسل بترتيب الأحداث نفسه
                    scope.launch { dial.beginDrag(down.position.x, currentCommittedDark) }
                    var totalDx = 0f
                    var totalDy = 0f
                    var crossedLeft = dial.progressOfHead() < 0.5f
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) {
                            // الرفع: نقرة أم سحب؟
                            if (ThemeDialMath.isTap(totalDx, totalDy)) {
                                scope.launch { dial.toggle(currentCommittedDark, currentCommit, scope) }
                            } else {
                                scope.launch { dial.endDrag(currentCommit, scope) }
                            }
                            break
                        }
                        val delta = change.positionChange()
                        totalDx += delta.x
                        totalDy += delta.y
                        val fingerX = change.position.x
                        scope.launch { dial.dragTo(fingerX) }
                        // همسة عند عبور المنتصف — الحاسمة
                        val nowLeft = dial.progressOfHead() < 0.5f
                        if (nowLeft != crossedLeft) {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            crossedLeft = nowLeft
                        }
                        change.consume()
                    }
                }
            }
            .drawBehind {
                if (dial.trackWidth <= 0f || dial.screenWidth <= 0f) return@drawBehind
                val t = frameNanos / 1_000_000_000f
                val y = dial.trackCenterY
                val headXv = dial.headX.value
                val headRv = dial.headR.value * dial.pulse.value
                val tailXv = dial.tailX.floatValue
                val tailRv = dial.tailR.value
                val energy = dial.energy()
                val p = ThemeDialMath.progress(headXv, dial.anchorLeft, dial.anchorRight)

                // ===== 1) كبسولة المسار =====
                val topLeft = Offset(dial.trackLeft, dial.trackTop)
                val capsule = Size(dial.trackWidth, dial.trackHeight)
                drawRoundRect(
                    color = capsuleFill,
                    topLeft = topLeft,
                    size = capsule,
                    cornerRadius = CornerRadius(dial.trackHeight / 2f),
                )
                drawRoundRect(
                    color = capsuleBorder,
                    topLeft = topLeft,
                    size = capsule,
                    cornerRadius = CornerRadius(dial.trackHeight / 2f),
                    style = Stroke(strokePx),
                )

                // ===== 2) توهج الشروق عند حدّ الانقسام =====
                if (dial.splitActive) {
                    val gf = dial.glow.value * ThemeDialMath.glowDuringSweep(dial.sweepFraction())
                    if (gf > 0.004f) {
                        val warm = dial.overlayIsLight
                        val bandColor = if (warm) sunriseBand else moonBand
                        val coreColor = if (warm) sunriseCore else moonCore
                        val profile = ThemeDialMath.glowStops(dial.maskX.value, dial.screenWidth, dial.bandPx)
                        val stops = ArrayList<Pair<Float, Color>>(profile.size / 2)
                        var i = 0
                        while (i < profile.size) {
                            stops.add(profile[i] to bandColor.copy(alpha = 0.42f * gf * profile[i + 1]))
                            i += 2
                        }
                        drawRect(brush = Brush.horizontalGradient(*stops.toTypedArray()))
                        val center = Offset(headXv, y)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    coreColor.copy(alpha = (if (warm) 0.34f else 0.26f) * gf),
                                    Color.Transparent,
                                ),
                                center = center,
                                radius = dial.radialPx,
                            ),
                            radius = dial.radialPx,
                            center = center,
                        )
                    }
                }

                // ===== 3) السائل الحي: ذيل ينقص + رأس + جسر بلا فصل =====
                val blobTop = lerp(moonTop, sunTop, p)
                val blobBottom = lerp(moonBottom, sunBottom, p)
                val blobBrush = Brush.verticalGradient(
                    listOf(blobTop, blobBottom),
                    startY = y - blobR * 1.4f,
                    endY = y + blobR * 1.4f,
                )
                val liquid = Path()
                if (tailRv > GooeyMath.MIN_RADIUS) {
                    addLiquidBlob(
                        liquid,
                        tailXv,
                        y + blobR * GooeyMath.centerDrift(t, 1.7f),
                        tailRv, t, energy, phase = 1.3f,
                    )
                }
                if (headRv > GooeyMath.MIN_RADIUS) {
                    // الرأس يتبع اليد حرفياً أثناء السحب — بلا انزياح رأسي
                    addLiquidBlob(
                        liquid,
                        headXv,
                        if (dial.dragging) y else y + blobR * GooeyMath.centerDrift(t, 0f),
                        headRv, t, energy, phase = 0f,
                    )
                }
                if (tailRv > GooeyMath.MIN_RADIUS && headRv > GooeyMath.MIN_RADIUS) {
                    val bridge = GooeyMath.bridge(
                        tailXv, y, tailRv,
                        headXv, y, headRv,
                        GooeyMath.waistFor(tailRv, blobR),
                    )
                    if (bridge != null) {
                        val sway = blobR * 0.05f * GooeyMath.bridgeSway(t)
                        bridge.qA1y += sway
                        bridge.qA2y += sway * 0.55f
                        bridge.qB1y -= sway * 0.85f
                        bridge.qB2y -= sway * 0.5f
                        addBridgePath(liquid, bridge)
                    }
                }
                if (!liquid.isEmpty) drawPath(liquid, blobBrush)

                // ===== 4) الشمس والقمر فوق السائل — تتقاطفان بموضع اليد =====
                // رسم مباشر داخل اللوحة: محصّن من انعكاس RTL ومتزامن مع السائل بالإطار
                if (headRv > GooeyMath.MIN_RADIUS) {
                    translate(headXv - iconPx / 2f, y - iconPx / 2f) {
                        with(sunPainter) {
                            draw(
                                Size(iconPx, iconPx),
                                alpha = ThemeDialMath.sunAlpha(p),
                                colorFilter = ColorFilter.tint(sunTint),
                            )
                        }
                        with(moonPainter) {
                            draw(
                                Size(iconPx, iconPx),
                                alpha = ThemeDialMath.moonAlpha(p),
                                colorFilter = ColorFilter.tint(moonTint),
                            )
                        }
                    }
                }
            },
    )
}
