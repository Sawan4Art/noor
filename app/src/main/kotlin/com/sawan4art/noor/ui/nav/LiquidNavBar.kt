package com.sawan4art.noor.ui.nav

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * هيكل التنقل الزجاجي السائل — قلب المرحلة الثانية:
 *
 * 1) الزجاج: لمحة حقيقية للمحتوى خلف الشريط (GraphicsLayer + BlurEffect
 *    على أندرويد 12+) مع غسلة زجاجية وحدّ لامع — وعلى الأجهزة الأقدم
 *    يبقى الغسيل الزجاجي بلا ضبابية (بديل أنيق).
 * 2) النقل السائل المتصل (قاعدة المالك 2026-10-30): عند تبديل التبويب يُنقل
 *    السائل كله من الزر القديم إلى الجديد **دون فصل** — مصدر عند القديم ينقص،
 *    ورأس عند الجديد يزيد، بينهما جسر موصول دائماً يضيق مع نفاد المصدر حتى
 *    يُسحب آخر قطرة. والسائل حيّ لا يسكن: تموج سطحي وانسياب ورفرفة مستمرة.
 */

/** مدد النقل بالمللي ثانية: نقص المصدر، رحلة الرأس، وذبول الرجفة بعدها
 *  (alpha4: أبطأ بطلب المالك — نقل ألزق أكثر تمهلًا) */
private const val DRAIN_MS = 1040
private const val HEAD_MS = 800
private const val JIGGLE_MS = 1250

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
    var lastSelectedIndex by remember { mutableIntStateOf(selectedIndex) }

    // حالة النقل السائل: مصدر ينقص عند الزر القديم + رأس يزيد نحو الجديد
    var srcX by remember { mutableFloatStateOf(0f) }
    val srcR = remember { Animatable(0f) }
    val headX = remember { Animatable(0f) }
    val headR = remember { Animatable(0f) }
    // نبضة الامتلاء عند الهبوط + طاقة الرجفة التي تذبل بعده
    val pulse = remember { Animatable(1f) }
    val jiggle = remember { Animatable(0f) }
    val snapshot = rememberGraphicsLayer()

    val blobRadius = with(density) { 25.dp.toPx() }

    // زمن حي لا يتوقف — السائل لا يسكن أبداً (تموج وانسياب في الثبات ذاته)
    var frameNanos by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            withInfiniteAnimationFrameNanos { frameNanos = it }
        }
    }

    fun centerX(index: Int): Float {
        if (barWidth <= 0f) return 0f
        val tabWidth = barWidth / tabs.size
        val rtl = layoutDir == LayoutDirection.Rtl
        return if (rtl) barWidth - (index + 0.5f) * tabWidth else (index + 0.5f) * tabWidth
    }

    // النقل المتصل: نقص من القديم وزيادة في الجديد معاً، وجسر موصول حتى النهاية
    LaunchedEffect(selected, barWidth) {
        if (barWidth <= 0f) return@LaunchedEffect
        val target = centerX(selectedIndex)
        if (!initialized) {
            // محاذاة أولية فورية — بلا قفزة
            headX.snapTo(target)
            headR.snapTo(blobRadius)
            srcX = target
            srcR.snapTo(0f)
            initialized = true
            lastSelectedIndex = selectedIndex
            return@LaunchedEffect
        }
        if (selectedIndex == lastSelectedIndex) {
            // تغيّر القياس فقط (دوران شاشة مثلًا) — إعادة محاذاة بلا حركة
            if (srcR.value <= GooeyMath.MIN_RADIUS) {
                headX.snapTo(target)
                headR.snapTo(blobRadius)
            }
            return@LaunchedEffect
        }
        if (srcR.value <= GooeyMath.MIN_RADIUS) {
            // البدء من وضع الثبات: السائل الحالي عند الزر القديم يصير هو المصدر
            srcX = headX.value
            srcR.snapTo(maxOf(headR.value, blobRadius))
            headX.snapTo(srcX)
            headR.snapTo(0f)
        }
        // وإلا فنقلٌ جارٍ: المصدر يبقى مكانه يُنقص وتُعاد توجيهة الرأس فقط
        lastSelectedIndex = selectedIndex
        launch { srcR.animateTo(0f, tween(DRAIN_MS, easing = FastOutSlowInEasing)) }
        launch { headR.animateTo(blobRadius, tween(DRAIN_MS, easing = FastOutSlowInEasing)) }
        launch { headX.animateTo(target, tween(HEAD_MS, easing = EaseOutBack)) }
        // لحظة الهبوط: رجفة حيّة تذبل تدريجياً + نبضة امتلاء مرنة
        launch {
            delay(HEAD_MS - 40L)
            jiggle.snapTo(1f)
            jiggle.animateTo(0f, tween(JIGGLE_MS, easing = LinearEasing))
        }
        launch {
            delay(HEAD_MS - 40L)
            pulse.snapTo(1.16f)
            pulse.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 280f))
        }
    }

    val glass = glassSpec()

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
        // ===== السائل المتصل: مصدر ينقص + رأس يزيد + جسر موصول دائماً =====
        Box(
            Modifier
                .matchParentSize()
                .drawBehind {
                    if (!initialized || barWidth <= 0f) return@drawBehind
                    val timeSec = frameNanos / 1_000_000_000f
                    val y = barHeight / 2f
                    val energy = jiggle.value
                    val srcRadius = srcR.value
                    val headRadius = headR.value * pulse.value

                    val blobBrush = Brush.verticalGradient(
                        listOf(glass.blobTop, glass.blobBottom),
                        startY = y - blobRadius * 1.4f,
                        endY = y + blobRadius * 1.4f,
                    )

                    // مسار واحد لكل السائل فتتحد الأجزاء بلا خطوط فاصلة حتى مع الشفافية
                    val liquid = Path()

                    // المصدر — السائل الباقي عند الزر القديم: ينقص مع النقل ويعوم دائماً
                    if (srcRadius > GooeyMath.MIN_RADIUS) {
                        addLiquidBlob(
                            liquid,
                            srcX,
                            y + blobRadius * GooeyMath.centerDrift(timeSec, 0.8f),
                            srcRadius, timeSec, energy, phase = 1.3f,
                        )
                    }
                    // الرأس — السائل الواصل للزر الجديد: يزيد مع النقل
                    if (headRadius > GooeyMath.MIN_RADIUS) {
                        addLiquidBlob(
                            liquid,
                            headX.value,
                            y + blobRadius * GooeyMath.centerDrift(timeSec, 2.6f),
                            headRadius, timeSec, energy, phase = 0f,
                        )
                    }
                    // الجسر — موصول دائماً حتى يكتمل النقل (قاعدة المالك: لا فصل)
                    if (srcRadius > GooeyMath.MIN_RADIUS && headRadius > GooeyMath.MIN_RADIUS) {
                        val bridge = GooeyMath.bridge(
                            srcX, y, srcRadius,
                            headX.value, y, headRadius,
                            GooeyMath.waistFor(srcRadius, blobRadius),
                        )
                        if (bridge != null) {
                            // رفرفة حية للرقبة — حافتاها تتمايل بطورين متعاكسين
                            val sway = blobRadius * 0.05f * GooeyMath.bridgeSway(timeSec)
                            bridge.qA1y += sway
                            bridge.qA2y += sway * 0.55f
                            bridge.qB1y -= sway * 0.85f
                            bridge.qB2y -= sway * 0.5f
                            addBridgePath(liquid, bridge)
                        }
                    }
                    if (!liquid.isEmpty) drawPath(liquid, blobBrush)
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
