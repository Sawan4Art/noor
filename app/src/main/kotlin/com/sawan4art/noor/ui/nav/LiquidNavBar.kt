package com.sawan4art.noor.ui.nav

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import kotlin.math.abs
import kotlinx.coroutines.launch

/**
 * هيكل التنقل الزجاجي السائل — قلب المرحلة الثانية:
 *
 * 1) الزجاج: لمحة حقيقية للمحتوى خلف الشريط (GraphicsLayer + BlurEffect
 *    على أندرويد 12+) مع غسلة زجاجية وحدّ لامع — وعلى الأجهزة الأقدم
 *    يبقى الغسيل الزجاجي بلا ضبابية (بديل أنيق).
 * 2) القطرة: مؤشر سائل بفيزياء spring يتمطط نحو الهدف وتنفصل منه
 *    نقطتان تتأخران في الرحلة ثم تندمجان — جسر metaball مقعّر يربطها.
 */
@Composable
fun LiquidNavScaffold(
    selectedTab: NoorTab,
    onSelectTab: (NoorTab) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val contentLayer = rememberGraphicsLayer()
    var barLeft by remember { mutableFloatStateOf(0f) }
    var barTop by remember { mutableFloatStateOf(0f) }
    val supportsBlur = remember { Build.VERSION.SDK_INT >= Build.VERSION_CODES.S }

    Box(modifier.fillMaxSize()) {
        // طبقة المحتوى — تُسجَّل كل إطار ليلُمَس الزجاجُ ضبابيتَها
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    contentLayer.record { this@drawWithContent.drawContent() }
                    drawLayer(contentLayer)
                },
        ) {
            content()
        }

        LiquidNavBar(
            tabs = NoorTab.entries.toList(),
            selected = selectedTab,
            onSelect = onSelectTab,
            contentLayer = contentLayer,
            supportsBlur = supportsBlur,
            onPlaced = { left, top ->
                barLeft = left
                barTop = top
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                .fillMaxWidth()
                .height(76.dp),
        )
    }
}

@Composable
private fun LiquidNavBar(
    tabs: List<NoorTab>,
    selected: NoorTab,
    onSelect: (NoorTab) -> Unit,
    contentLayer: GraphicsLayer,
    supportsBlur: Boolean,
    onPlaced: (Float, Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val layoutDir = LocalLayoutDirection.current

    var barWidth by remember { mutableFloatStateOf(0f) }
    var barHeight by remember { mutableFloatStateOf(0f) }
    var barLeft by remember { mutableFloatStateOf(0f) }
    var barTop by remember { mutableFloatStateOf(0f) }
    var initialized by remember { mutableStateOf(false) }

    val selectedIndex = tabs.indexOf(selected).coerceAtLeast(0)

    // حالة القطرة: المركز + نقطتان متأخرتان + نبضة الوصول
    val blobX = remember { Animatable(0f) }
    val trailX1 = remember { Animatable(0f) }
    val trailX2 = remember { Animatable(0f) }
    val pulse = remember { Animatable(1f) }
    val snapshot = rememberGraphicsLayer()

    fun centerX(index: Int): Float {
        if (barWidth <= 0f) return 0f
        val tabWidth = barWidth / tabs.size
        val rtl = layoutDir == LayoutDirection.Rtl
        return if (rtl) barWidth - (index + 0.5f) * tabWidth else (index + 0.5f) * tabWidth
    }

    // محاذاة أولية فورية عند أول قياس — بلا قفزة
    LaunchedEffect(barWidth) {
        if (barWidth > 0f && !initialized) {
            val x = centerX(selectedIndex)
            blobX.snapTo(x)
            trailX1.snapTo(x)
            trailX2.snapTo(x)
            initialized = true
        }
    }

    // حركة فينوم: القطرة تتمطط وتندفع، والنقطتان تتأخران ثم تندمجان
    LaunchedEffect(selected, barWidth) {
        if (!initialized || barWidth <= 0f) return@LaunchedEffect
        val target = centerX(selectedIndex)
        launch {
            pulse.snapTo(1.24f)
            pulse.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 420f))
        }
        launch { blobX.animateTo(target, spring(dampingRatio = 0.64f, stiffness = 240f)) }
        launch { trailX1.animateTo(target, spring(dampingRatio = 0.85f, stiffness = 105f)) }
        trailX2.animateTo(target, spring(dampingRatio = 0.9f, stiffness = 62f))
    }

    val glass = glassSpec()

    val blobRadius = with(density) { 25.dp.toPx() }
    val trailBaseRadius = with(density) { 8.5.dp.toPx() }
    val merge1 = (blobRadius + trailBaseRadius) * 1.9f
    val merge2 = (trailBaseRadius * 0.72f + trailBaseRadius) * 1.9f

    Box(
        modifier = modifier
            .onGloballyPositioned { coords ->
                barWidth = coords.size.width.toFloat()
                barHeight = coords.size.height.toFloat()
                barLeft = coords.positionInRoot().x
                barTop = coords.positionInRoot().y
                onPlaced(barLeft, barTop)
            }
            .drawWithCache {
                // ===== طبقة الزجاج =====
                val capsule = Path().apply {
                    addRoundRect(
                        RoundRect(
                            rect = Rect(0f, 0f, size.width, size.height),
                            cornerRadius = CornerRadius(size.height / 2f),
                        ),
                    )
                }
                val blurPx = 18.dp.toPx()
                val blurEffect = if (supportsBlur) BlurEffect(blurPx, blurPx) else null
                val strokePx = 1.dp.toPx()
                onDrawBehind {
                    if (blurEffect != null) {
                        // لمحة مضببة للمحتوى خلف الشريط فقط (مقصوصة على شكل الكبسولة)
                        snapshot.record(contentLayer.size) { drawLayer(contentLayer) }
                        snapshot.renderEffect = blurEffect
                        clipPath(capsule) {
                            translate(-barLeft, -barTop) {
                                drawLayer(snapshot)
                            }
                        }
                        snapshot.renderEffect = null
                    }
                    clipPath(capsule) {
                        // الغسلة الزجاجية
                        drawRect(
                            Brush.verticalGradient(
                                listOf(glass.tintTop, glass.tintBottom),
                            ),
                        )
                        // اللمعة العلوية
                        drawRect(
                            Brush.verticalGradient(
                                0f to glass.sheen,
                                0.45f to Color.Transparent,
                                1f to Color.Transparent,
                            ),
                        )
                    }
                    // الحد الزجاجي اللامع
                    drawPath(capsule, color = glass.border, style = Stroke(strokePx))
                }
            },
    ) {
        // ===== القطرة السائلة =====
        Box(
            Modifier
                .matchParentSize()
                .drawBehind {
                    if (!initialized || barWidth <= 0f) return@drawBehind
                    val y = barHeight / 2f
                    val target = centerX(selectedIndex)
                    val cx = blobX.value
                    val distance = abs(target - cx)
                    val travel = (barWidth / tabs.size) * 0.9f
                    val stretchX = GooeyMath.stretchFactor(distance, travel)
                    val squashY = GooeyMath.squashFactor(stretchX)
                    val mainRadius = blobRadius * pulse.value

                    val blobBrush = Brush.verticalGradient(
                        listOf(glass.blobTop, glass.blobBottom),
                        startY = y - mainRadius,
                        endY = y + mainRadius,
                    )

                    fun drawPair(xa: Float, ra: Float, xb: Float, rb: Float, merge: Float) {
                        val bridge = GooeyMath.bridge(xa, y, ra, xb, y, rb, merge)
                        if (bridge != null) {
                            drawPath(buildBridgePath(bridge), blobBrush)
                        }
                        drawCircle(blobBrush, ra, Offset(xa, y))
                        drawCircle(blobBrush, rb, Offset(xb, y))
                    }

                    // نصف قطر كل نقطة: يكبر كلما اقتربت من الاندماج
                    val r1 = GooeyMath.dropletRadius(
                        trailBaseRadius, abs(trailX1.value - cx), merge1,
                    )
                    val r2 = GooeyMath.dropletRadius(
                        trailBaseRadius * 0.72f, abs(trailX2.value - trailX1.value), merge2,
                    )

                    // الرحلة كاملة: أعمق نقطة ← نقطة وسيطة ← القطرة الرئيسية المتمططة
                    drawPair(trailX1.value, r1, trailX2.value, r2, merge2)
                    drawPair(cx, mainRadius, trailX1.value, r1, merge1)
                    withTransform({ scale(stretchX, squashY, pivot = Offset(cx, y)) }) {
                        drawCircle(blobBrush, mainRadius, Offset(cx, y))
                    }
                },
        )

        // ===== الأيقونات =====
        Row(Modifier.fillMaxSize()) {
            tabs.forEachIndexed { index, tab ->
                val selectedTab = index == selectedIndex
                val iconScale by animateFloatAsState(
                    targetValue = if (selectedTab) 1f else 0.86f,
                    animationSpec = spring(dampingRatio = 0.5f, stiffness = 520f),
                    label = "tabScale",
                )
                val iconTint by animateColorAsState(
                    targetValue = if (selectedTab) glass.iconOn else glass.iconOff,
                    animationSpec = tween(220),
                    label = "tabTint",
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelect(tab)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.label,
                        tint = iconTint,
                        modifier = Modifier
                            .size(26.dp)
                            .graphicsLayer {
                                scaleX = iconScale
                                scaleY = iconScale
                            },
                    )
                }
            }
        }
    }
}

/** بناء مسار الجسر السائل من نقاط الرياضيات — قبتان دقيقتان وحافتا بيزيه مقعّرتان */
private fun buildBridgePath(b: GooeyBridge): Path = Path().apply {
    moveTo(b.p1x, b.p1y)
    cubicTo(b.qA1x, b.qA1y, b.qA2x, b.qA2y, b.p3x, b.p3y)
    arcTo(Rect(b.bx - b.br, b.by - b.br, b.bx + b.br, b.by + b.br), -90f, 180f, false)
    cubicTo(b.qB2x, b.qB2y, b.qB1x, b.qB1y, b.p2x, b.p2y)
    arcTo(Rect(b.ax - b.ar, b.ay - b.ar, b.ax + b.ar, b.ay + b.ar), 90f, 180f, false)
    close()
}

/** درجات الزجاج والقطرة للوضعين الليلي والنهاري — مشتقة من ثيم «نور» */
private data class GlassSpec(
    val tintTop: Color,
    val tintBottom: Color,
    val sheen: Color,
    val border: Color,
    val blobTop: Color,
    val blobBottom: Color,
    val iconOn: Color,
    val iconOff: Color,
)

@Composable
private fun glassSpec(): GlassSpec {
    val scheme = MaterialTheme.colorScheme
    val dark = scheme.background.luminance() < 0.5f
    return if (dark) {
        GlassSpec(
            tintTop = Color(0xFF121E1A).copy(alpha = 0.62f),
            tintBottom = Color(0xFF121E1A).copy(alpha = 0.42f),
            sheen = Color.White.copy(alpha = 0.10f),
            border = Color.White.copy(alpha = 0.16f),
            blobTop = Color(0xFF9BDCC7),
            blobBottom = scheme.primary,
            iconOn = scheme.onPrimary,
            iconOff = Color(0xFFB9CEC4).copy(alpha = 0.70f),
        )
    } else {
        GlassSpec(
            tintTop = Color.White.copy(alpha = 0.58f),
            tintBottom = Color.White.copy(alpha = 0.32f),
            sheen = Color.White.copy(alpha = 0.40f),
            border = Color.White.copy(alpha = 0.62f),
            blobTop = Color(0xFF3B8272),
            blobBottom = scheme.primary,
            iconOn = scheme.onPrimary,
            iconOff = Color(0xFF17251F).copy(alpha = 0.55f),
        )
    }
}
