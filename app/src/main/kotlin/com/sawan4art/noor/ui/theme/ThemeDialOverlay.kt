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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
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
 * شكل الفاصل — توجيه المالك alpha17 (2026-10-31 ليلًا، بعد alpha16): «كبر عرض
 * الخط شويه وشيل الخط وخلّي التوهج فقط» — الفاصل لم يعد يضم أي خط خالص:
 * **حقل توهج واحد ناعم أعرض من قبل** (30dp بدل 16dp) لا حافة له ولا خيط —
 * طيف نهاري دافئ بسبع وقفات بطول الشاشة (ذهب الصباح ← قلب أبيض ساطع ←
 * ذهبي ← كهرماني ← وردة الغروب — روح alpha16 كما هي) يُقنّع أفقيّاً بتلاشٍ
 * جرسّي متماثل حول حدّ الانقسام (saveLayer + BlendMode.DstIn): أسطع نقطة
 * على الحدّ نفسه تتلاشى ناعمة في الاتجاهين — إضاءة خالصة لا جسم مرسوم.
 * شدة الإضاءة كلها **ديناميكية مع السحب** (ThemeDialMath.beamIntensity):
 * تبدأ خافتة وتزداد تدريجياً حتى شدة إضاءة النهار الكاملة لحظة غمر الشاشة،
 * والعكس نحو المظلم تبدأ عالية وتنحدر حتى الانطفاء. يُرسم فوق حدّ القص
 * فيُغطي أي ميز قطع بالمحتوى.
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
    val iconPx = with(density) { 24.dp.toPx() }
    val strokePx = with(density) { 1.dp.toPx() }
    val anchorPad = blobR * 1.6f
    val touchHalf = with(density) { 32.dp.toPx() } // هدف لمس 64dp يركب الأيقونة
    // توهج الضوء الحي (alpha17): حقل توهج طيفي ناعم بلا خط — أعرض شويه من
    // هالة alpha16 (30dp بدل 16dp). الامتداد يُمرر للمزلاج كي يُساق كل الرسم
    // خارج الشاشة عند المسح دون أي بقايا توهج
    val glowHalfOuter = with(density) { 15.dp.toPx() }

    SideEffect {
        dial.configure(
            dial.screenWidth, dial.screenHeight, blobR,
            anchorPad, glowHalfOuter,
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

    // توهج الضوء الحي — طيف نهاري كامل (روح توجيه المالك alpha16 كما هي):
    // «متدرج بالوان كثيرة... كأن الوضع النهاري يضيء الليلي بسلاسة وشفافية» —
    // سبعة ألوان دافئة من أعلى الشاشة لأسفلها، وأسطع نقطة في قلبها
    val beamSpectrum = listOf(
        0.00f to Color(0xFFFFDFA0), // ذهب الصباح
        0.16f to Color(0xFFFFF1C8), // كريمي دافئ
        0.34f to Color(0xFFFFFFFF), // القلب الساطع
        0.52f to Color(0xFFFFF3CE), // كريمي
        0.70f to Color(0xFFFFDC96), // ذهبي
        0.86f to Color(0xFFFFC084), // كهرماني
        1.00f to Color(0xFFFFA97C), // وردة الغروب
    )

    // اتجاه داخلي LTR: صندوق الإيماءة يُثبَّت TopStart الفيزيائي (أعلى اليسار)
    // مهما كان اتجاه التطبيق RTL — فالإزاحة الخام = إحداثيات مطلقة صحيحة
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged {
                dial.configure(
                    it.width.toFloat(), it.height.toFloat(), blobR,
                    anchorPad, glowHalfOuter,
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

                // ===== 1) توهج الضوء الحي على حدّ الانقسام (تحت الكبسولة والسائل) =====
                // توجيه المالك alpha17: «شيل الخط وخلّي التوهج فقط» — لا خيط
                // ولا هالة منفصلة بعد اليوم: **حقل توهج واحد ناعم أعرض من
                // قبل** (30dp). الطيف الرأسي متعدد الألوان (روح alpha16)
                // يُقنّع أفقيّاً بتلاشٍ جرسّي متماثل حول الحدّ داخل طبقة
                // معزولة (saveLayer + BlendMode.DstIn) — أسطع نقطة على الحدّ
                // نفسه تتلاشى ناعمة في الاتجاهين بلا أي حافة ولا خيط صلب.
                // الشدة ديناميكية = نسبة النهار على الشاشة (ThemeDialMath.
                // beamIntensity): السحب نحو الفاتح يبدأ خافتاً ويزداد مع كل
                // بكسل حتى شدة إضاءة النهار الكاملة لحظة الغمر، والسحب نحو
                // المظلم يبدأ ساطعاً وينحدر حتى الانطفاء («والعكس صحيح»).
                if (dial.splitActive) {
                    val visibility = dial.beam.value
                    val dayLight = ThemeDialMath.beamIntensity(dial.maskX.value, dial.screenWidth)
                    val power = visibility * dayLight
                    if (power > 0.004f) {
                        val edgeX = dial.maskX.value
                        val glowLeft = edgeX - glowHalfOuter
                        val glowWidth = glowHalfOuter * 2f
                        val gradH = if (size.height > 1f) size.height else 1f
                        drawIntoCanvas { canvas ->
                            canvas.saveLayer(
                                Rect(glowLeft, 0f, glowLeft + glowWidth, size.height),
                                Paint(),
                            )
                            // الطيف الرأسي بألوان كثيرة بطول الشاشة — يُرسم
                            // معتماً ثم يُخفّف بالقناع الأفقي في الخطوة التالية
                            drawRect(
                                brush = Brush.verticalGradient(
                                    *beamSpectrum.map { (stop, c) -> stop to c }.toTypedArray(),
                                    startY = 0f,
                                    endY = gradH,
                                ),
                                topLeft = Offset(glowLeft, 0f),
                                size = Size(glowWidth, size.height),
                            )
                            // قناع التلاشٍ الأفقي — جرس ناعم متماثل حول الحدّ:
                            // هذا ما يجعله «توهجاً فقط» بلا خط، فلا حافة صلبة
                            // عند الطرفين ولا قلب خيطي عند المركز (DstIn يضرب
                            // شفافية الطيف بمنحنى التلاشٍ)
                            drawRect(
                                brush = Brush.horizontalGradient(
                                    0.00f to Color.Black.copy(alpha = 0f),
                                    0.10f to Color.Black.copy(alpha = 0.06f * power),
                                    0.25f to Color.Black.copy(alpha = 0.20f * power),
                                    0.38f to Color.Black.copy(alpha = 0.44f * power),
                                    0.50f to Color.Black.copy(alpha = 0.72f * power),
                                    0.62f to Color.Black.copy(alpha = 0.44f * power),
                                    0.75f to Color.Black.copy(alpha = 0.20f * power),
                                    0.90f to Color.Black.copy(alpha = 0.06f * power),
                                    1.00f to Color.Black.copy(alpha = 0f),
                                    startX = glowLeft,
                                    endX = glowLeft + glowWidth,
                                ),
                                topLeft = Offset(glowLeft, 0f),
                                size = Size(glowWidth, size.height),
                                blendMode = BlendMode.DstIn,
                            )
                            canvas.restore()
                        }
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
