package com.sawan4art.noor.ui.theme

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import com.sawan4art.noor.ui.icons.NoorIcons
import com.sawan4art.noor.ui.nav.GooeyMath
import com.sawan4art.noor.ui.nav.addBridgePath
import com.sawan4art.noor.ui.nav.addLiquidBlob
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * مزلاج الشمس والقمر — طبقة فوق كل شيء (فوق طبقتي الثيم) في الرئيسية فقط:
 * كبسولة المسار + السائل الحي (ذيل ورأس وجسر بلا فصل) + أيقونتا الشمس والقمر
 * تتقاطفان بموضع اليد.
 *
 * درس alpha7 (ملاحظة المالك): الإيماءة كانت على الطبقة الكاملة فصارت حاجز لمس
 * يبتلع ضربات الشريط السفلي، والإمساك بالمسار الفارغ يولّد الانقسام الثقيل (لاج).
 * الإصلاح: الطبقة ترسم فقط بلا أي قبضة لمس، والإيماءة على **صندوق يركب الأيقونة
 * وحدها** — الفراغ يمر للطبقات تحتيه طبيعياً، والانقسام لا يشتغل إلا بسحب فعلي.
 *
 * قرار المالك (alpha11، بلقطة شاشة): الانقسام لم يظهر على جهازه قط — آلية
 * القناع (Offscreen + DstIn) تفشل بصمت على بعض معالجات الرسوميات. الإصلاح:
 * المعاينة قصّ صلب (في NoorApp) وعلى الحدّ **شعاع كشاف** يرسم هنا: هالة عريضة
 * بتدرج أفقي متصل (بلور جامد بلا أي خط) نواتها الحارّة شبه معتمة عند الحدّ،
 * تسقط طويلاً في الجهة المضيئة وقصيراً في المظلمة — «ضوء كشاف ينير جزءاً
 * ويترك جزءاً منفصلاً» (كلام المالك حرفياً). تدرّجات متصلة بلا شرائح
 * ولا أنماط مزج — ترسم على أي جهاز.
 *
 * الاتجاه داخلياً LTR دائماً — الرسم كله بإحداثيات فيزيائية مطلقة محصّنة من RTL.
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
    val beamLitPx = with(density) { ThemeDialMath.BEAM_LIT_FALL_DP.dp.toPx() }
    val beamDarkPx = with(density) { ThemeDialMath.BEAM_DARK_FALL_DP.dp.toPx() }
    val beamCoreLitPx = with(density) { ThemeDialMath.BEAM_CORE_LIT_DP.dp.toPx() }
    val beamCoreDarkPx = with(density) { ThemeDialMath.BEAM_CORE_DARK_DP.dp.toPx() }
    val iconPx = with(density) { 24.dp.toPx() }
    val strokePx = with(density) { 1.dp.toPx() }
    val anchorPad = blobR * 1.6f
    val touchHalf = with(density) { 32.dp.toPx() } // هدف لمس 64dp يركب الأيقونة

    SideEffect {
        dial.configure(
            dial.screenWidth, dial.screenHeight, blobR,
            beamLitPx, beamDarkPx, beamCoreLitPx, beamCoreDarkPx, anchorPad,
        )
    }

    // مزامنة المرسى مع الثيم المثبت (أول قياس + تغيّر الإعدادات + الدوران)
    LaunchedEffect(committedDark, dial.trackWidth) {
        dial.syncTo(committedDark, scope)
    }

    // انزلاق إحداثيات الجذر (عودة الرئيسية منزاحةً أثناء انتقال التبويب):
    // الرأس يُلصق بمرساه كل إطار فلا يتخلف السائل عن كبسولته
    LaunchedEffect(dial.trackLeft, dial.trackTop) {
        dial.followAnchors(currentCommittedDark)
    }

    // زمن حي لا يتوقف — السائل لا يسكن أبداً
    var frameNanos by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            withInfiniteAnimationFrameNanos { frameNanos = it }
        }
    }

    // ألوان الثيم المثبت — الكبسولة والحدود والسائل
    val scheme = MaterialTheme.colorScheme
    val capsuleFill = scheme.surface.copy(alpha = 0.45f)
    val capsuleBorder = scheme.outline.copy(alpha = 0.35f)
    val sunTop = Color(0xFFFFE2AE)
    val sunBottom = Color(0xFFF4A85C)
    val moonTop = Color(0xFFD7E3F4)
    val moonBottom = Color(0xFF8FA6C8)
    val sunTint = Color(0xFF452B10)
    val moonTint = Color(0xFF1A2433)

    val sunPainter = rememberVectorPainter(NoorIcons.Sun)
    val moonPainter = rememberVectorPainter(NoorIcons.Moon)

    // اتجاه داخلي LTR: صندوق الإيماءة يُثبَّت TopStart الفيزيائي (أعلى اليسار)
    // مهما كان اتجاه التطبيق RTL — فالإزاحة الخام = إحداثيات مطلقة صحيحة
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged {
                dial.configure(
                    it.width.toFloat(), it.height.toFloat(), blobR,
                    beamLitPx, beamDarkPx, beamCoreLitPx, beamCoreDarkPx, anchorPad,
                )
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

                // ===== 1) شعاع الكشاف على حدّ الانقسام (تحت الكبسولة والسائل) =====
                // هالة عريضة بتدرج أفقي متصل + نواة حارّة شبه معتمة تخفي حدّ القص،
                // وسقوط طويل في الجهة المضيئة (يسار) وقصير في المظلمة —
                // «ينير جزءاً ويترك جزءاً منفصلاً» — بلا خطوط ولا أنماط مزج.
                if (dial.splitActive) {
                    val power = dial.beam.value
                    if (power > 0.004f) {
                        val edgeX = dial.maskX.value
                        val h = size.height
                        val warm = dial.overlayIsLight
                        val dim = if (warm) 1f else 0.82f // ضوء أكثر هدوءاً والمظلم يتراجع
                        val bandColor = if (warm) Color(0xFFFFC47E) else Color(0xFFB9CDEC)
                        val coreColor = if (warm) Color(0xFFFFF1D6) else Color(0xFFE6EFFB)
                        // الهالة العريضة — الحدّ عند كسر edgeFrac من عرض الشعاع
                        val litFall = dial.beamLitPx
                        val darkFall = dial.beamDarkPx
                        val beamLeft = edgeX - litFall
                        val beamW = litFall + darkFall
                        val edgeFrac = if (beamW > 1f) litFall / beamW else 0.5f
                        val beamStops = arrayOf(
                            0.00f to bandColor.copy(alpha = 0.00f),
                            (edgeFrac * 0.35f) to bandColor.copy(alpha = 0.10f * dim * power),
                            (edgeFrac * 0.70f) to bandColor.copy(alpha = 0.22f * dim * power),
                            edgeFrac to bandColor.copy(alpha = 0.46f * dim * power),
                            (edgeFrac + (1f - edgeFrac) * 0.55f) to bandColor.copy(alpha = 0.16f * dim * power),
                            (edgeFrac + (1f - edgeFrac) * 0.85f) to bandColor.copy(alpha = 0.05f * dim * power),
                            1.00f to bandColor.copy(alpha = 0.00f),
                        )
                        drawRect(
                            brush = Brush.horizontalGradient(
                                *beamStops,
                                startX = beamLeft,
                                endX = edgeX + darkFall,
                            ),
                            topLeft = Offset(beamLeft, 0f),
                            size = Size(beamW, h),
                        )
                        // النواة الحارّة القاتمة على الحدّ نفسه — تخفي حدّ القص تحت النور
                        val coreLit = dial.beamCoreLitPx
                        val coreDark = dial.beamCoreDarkPx
                        val coreLeft = edgeX - coreLit
                        val coreW = coreLit + coreDark
                        val coreFrac = if (coreW > 1f) coreLit / coreW else 0.5f
                        val coreStops = arrayOf(
                            0.00f to coreColor.copy(alpha = 0.00f),
                            (coreFrac * 0.45f) to coreColor.copy(alpha = 0.52f * dim * power),
                            (coreFrac * 0.80f) to coreColor.copy(alpha = 0.86f * dim * power),
                            coreFrac to coreColor.copy(alpha = 0.94f * dim * power),
                            (coreFrac + (1f - coreFrac) * 0.55f) to coreColor.copy(alpha = 0.50f * dim * power),
                            1.00f to coreColor.copy(alpha = 0.00f),
                        )
                        drawRect(
                            brush = Brush.horizontalGradient(
                                *coreStops,
                                startX = coreLeft,
                                endX = coreLeft + coreW,
                            ),
                            topLeft = Offset(coreLeft, 0f),
                            size = Size(coreW, h),
                        )
                        // وهج مصدر الكشاف حول الأيقونة نفسها
                        val lamp = Offset(edgeX, y)
                        val lampR = litFall * 0.55f
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    coreColor.copy(alpha = 0.30f * dim * power),
                                    Color.Transparent,
                                ),
                                center = lamp,
                                radius = lampR,
                            ),
                            radius = lampR,
                            center = lamp,
                        )
                    }
                }

                // ===== 2) كبسولة المسار (زخرفة لا إيماءة) =====
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
    ) {
        // ===== صندوق الإيماءة — ثابت فوق مسار المزلاج كاملاً، والقبض من الأيقونة وحدها =====
        // الطبقة الأم بلا pointerInput فليست حاجز لمس، وهذا الصندوق محصور في فجوة
        // المسار (لا شيء تفاعلي تحتها): اللمس قرب الأيقونة يقبل، وفراغ المسار
        // يُرفض فوراً بلا انقسام ولا لاج (إصلاح ملاحظة المالك)، والشريط السفلي
        // وخارجه يعملان طبيعياً. الصندوق ثابت فالإحداثيات المحلية = إزاحة ثابتة
        // عن الجذر (trackLeft/trackTop) — بلا أي حلقة تغذية راجعة مع حركة الأيقونة.
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset {
                    IntOffset(dial.trackLeft.roundToInt(), dial.trackTop.roundToInt())
                }
                .size(
                    with(density) { dial.trackWidth.toDp() },
                    with(density) { dial.trackHeight.toDp() },
                )
                .semantics {
                    contentDescription =
                        "مزلاج المظهر — اسحب الأيقونة يميناً للوضع الفاتح ويساراً للوضع المظلم، أو انقرها للتبديل"
                }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        if (dial.busy || dial.dragging) {
                            return@awaitEachGesture // الأيقونة مشغولة برحلة — لا قبضة الآن
                        }
                        // القبض من الأيقونة وحدها — الإحداثيات المحلية + أصل المسار الثابت = جذرية
                        val touchX = down.position.x + dial.trackLeft
                        val touchY = down.position.y + dial.trackTop
                        if (!dial.isInsideIcon(touchX, touchY, touchHalf)) {
                            return@awaitEachGesture // فراغ المسار لا يغيّر المظهر ولا يعطل الأيقونة
                        }
                        down.consume()
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        // داخل awaitEachGesture تُمنع التعليقات العامة — تعديلات الحالة
                        // عبر مسرح النطاق الرئيسي فتتسلسل بترتيب الأحداث نفسه
                        var began = false
                        var totalDx = 0f
                        var totalDy = 0f
                        var crossedLeft = dial.progressOfHead() < 0.5f
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) {
                                if (began) {
                                    // الرفع بعد سحب: الضفة التي عبرها الإصبع تحسم
                                    scope.launch { dial.endDrag(currentCommit, scope) }
                                } else {
                                    // نقرة على الأيقونة: تبديل فوري برحلة كاملة
                                    scope.launch { dial.toggle(currentCommittedDark, currentCommit, scope) }
                                }
                                break
                            }
                            val delta = change.positionChange()
                            totalDx += delta.x
                            totalDy += delta.y
                            if (!began && !ThemeDialMath.isTap(totalDx, totalDy)) {
                                // أول تجاوز لحساسية اللمس يولد الانقسام — النقرة الصامتة لا تكلف شيئاً
                                began = true
                                scope.launch { dial.beginDrag(change.position.x + dial.trackLeft, currentCommittedDark) }
                            }
                            if (began) {
                                scope.launch { dial.dragTo(change.position.x + dial.trackLeft) }
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
                },
        )
    }
    }
}
