package com.sawan4art.noor.ui.tasbih

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sawan4art.noor.domain.tasbih.MisbahaDhikrList
import com.sawan4art.noor.domain.tasbih.MisbahaMath
import com.sawan4art.noor.domain.tasbih.MisbahaSplashMath
import com.sawan4art.noor.ui.nav.GooeyMath
import com.sawan4art.noor.ui.nav.addLiquidBlob
import com.sawan4art.noor.util.MisbahaFeedback
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharedFlow
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * المسبحة الإلكترونية — **نقطة سائل حيّ** بدل حلقة الخرز
 * (توجيه المالك alpha25: «بدل الدوائر التي يتغير لونها مع كل عد اعمل
 * عد رقمي عادي و اسم التسبيح تحته و كل هذا داخل نقطه سائل حي بنفس
 * التنسيق لاكن طبعا بحجم اكبر بنفس حجم الدائره الموجوده حاليا»).
 *
 * **السائل نفسه بلغة هوية التطبيق حرفياً** (GooeyMath/addLiquidBlob —
 * لغة سائل مزلاج المظهر وقطرة التذكير): سطح متموج دائماً بثلاث موجات
 * ثبات، وتنفّس، وانسياب رأسي — **والعدّ الرقمي واسم التسبيح يعومان
 * داخل السائل باتباع الانسياب نفسه** كما يعوم الهلال في قطرة التذكير.
 *
 * **سائل تفاعلي مع سرعة النقر**: كل ضغطة تضخّ موجة نشاط [energy] في
 * السطح فيزدوج تموّجه، وتتناثر قطرات عشوائية صغيرة ومتوسطة الأحجام
 * من سطح النقطة — وكلما زادت سرعة النقر زادت كمية القطرات
 * (MisbahaSplashMath: دالة رتيبة بسقفٍ محدود). وإتمام دورةٍ رشّة أقصى،
 * والتسرع يرجّح السائل بقوة.
 *
 * **حارس التسرع**: النقر أسرع من نطق الجمل يُنبه «لا تسرع في التسبيح»
 * ويوقف العدّ ثلاث ثوانٍ — ضغطات الإيقاف مُهمَلة بلا عدّ ولا تناثر
 * (MisbahaRushGuard النقيّ المُختبَر).
 */
@Composable
fun MisbahaScreen(viewModel: MisbahaViewModel = viewModel()) {
    val scheme = MaterialTheme.colorScheme
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pausedUntil by viewModel.pausedUntil.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val feedback = remember { MisbahaFeedback(context) }

    // الاهتزاز — نخلة مع كل تسبيحة، ونبضة إتمام، وثلاث نخلات على التسرع
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is MisbahaEvent.Counted -> feedback.tap()
                MisbahaEvent.RoundCompleted -> feedback.roundCompleted()
                MisbahaEvent.RushWarned -> feedback.rush()
            }
        }
    }

    // نبض الساعة المحلي لحساب باقي فترة الإيقاف (لا مؤقّتات في العدّ نفسه)
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(pausedUntil) {
        while (System.currentTimeMillis() < pausedUntil) {
            nowMs = System.currentTimeMillis()
            delay(200)
        }
        nowMs = System.currentTimeMillis()
    }
    val paused = nowMs < pausedUntil
    val resumeInSec = if (paused) ((pausedUntil - nowMs + 999) / 1000).toInt() else 0

    var showResetAll by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.background)
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        // هيدر الشاشة — العنوان وإجمالي التسبيح التراكمي (لا يُمسح بإعادة التعيين)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "المسبحة",
                style = MaterialTheme.typography.headlineSmall,
                color = scheme.onBackground,
            )
            Spacer(Modifier.weight(1f))
            Surface(
                shape = RoundedCornerShape(50),
                color = scheme.secondaryContainer,
                contentColor = scheme.onSecondaryContainer,
            ) {
                Text(
                    text = "الإجمالي ${MisbahaMath.arabicNumber(state.lifetimeTotal)}",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                )
            }
        }
        Spacer(Modifier.height(8.dp))

        // ===== النقطة السائلة الحية — الضغط عليها تسبيحة =====
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .pointerInput(Unit) { detectTapGestures { viewModel.tap() } },
            contentAlignment = Alignment.Center,
        ) {
            val side = minOf(maxHeight, maxWidth)
            val blobR = with(LocalDensity.current) { (side * 0.38f).toPx() }
            LiquidCounter(
                count = state.count,
                dhikrText = state.dhikr.text,
                subtitle = run {
                    val remaining = MisbahaMath.remaining(state.count, state.target)
                    val rounds = "الدورات ${MisbahaMath.arabicNumber(state.roundsNow.toLong())}"
                    if (remaining == null) {
                        rounds + " · بلا حدّ"
                    } else {
                        "المتبقي ${MisbahaMath.arabicNumber(remaining.toLong())} · " + rounds
                    }
                },
                events = viewModel.events,
                blobR = blobR,
                modifier = Modifier.matchParentSize(),
            )
        }

        // ===== تنبيه التسرع — «يتوقف العداد عن العد» (توجيه المالك) =====
        AnimatedVisibility(
            visible = paused,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = scheme.errorContainer,
                contentColor = scheme.onErrorContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Icon(
                        Icons.Filled.Info,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.size(12.dp))
                    Column {
                        Text(
                            text = "لا تسرع في التسبيح",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "تمهّل حتى تنطق الكلمات.. يُستأنف العدّ بعد " +
                                MisbahaMath.arabicNumber(resumeInSec.toLong()) + " ثوانٍ",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
        if (!paused) Spacer(Modifier.height(10.dp))

        // ===== صف الهدف =====
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (option in MisbahaDhikrList.TARGET_OPTIONS) {
                val label = if (option == MisbahaMath.TARGET_UNLIMITED) {
                    "بلا حدّ"
                } else {
                    MisbahaMath.arabicNumber(option.toLong())
                }
                TargetChip(
                    label = label,
                    selected = state.target == option,
                    onClick = { viewModel.setTarget(option) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(10.dp))

        // ===== صف الأذكار — عدّاد كل ذكر محفوظ له =====
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
        ) {
            for (dhikr in MisbahaDhikrList.ALL) {
                DhikrChip(
                    text = dhikr.text,
                    count = state.counts[dhikr.id] ?: 0,
                    selected = state.selectedId == dhikr.id,
                    onClick = { viewModel.selectDhikr(dhikr.id) },
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        // سطر فضيلة الذكر الحالي — يقول للمستخدم لماذا هذا الذكر
        Text(
            text = state.dhikr.targetLabel,
            style = MaterialTheme.typography.labelSmall,
            color = scheme.onSurfaceVariant,
            maxLines = 1,
        )
        Spacer(Modifier.height(8.dp))

        // ===== صف الإعادة =====
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = viewModel::resetCurrent) {
                Icon(
                    Icons.Filled.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = scheme.primary,
                )
                Spacer(Modifier.size(6.dp))
                Text("إعادة العدّ")
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { showResetAll = true }) {
                Text("إعادة الكل", color = scheme.error)
            }
        }
        Spacer(Modifier.height(112.dp)) // مساحة شريط التنقل الزجاجي العائم — دائماً
    }

    if (showResetAll) {
        AlertDialog(
            onDismissRequest = { showResetAll = false },
            title = { Text("إعادة تعيين الكل؟") },
            text = {
                Text("تصفير كل عدادات الأذكار وذُهابها مع الاحتفاظ بالإجمالي التراكمي.")
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.resetAll()
                    showResetAll = false
                }) { Text("إعادة", color = scheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showResetAll = false }) { Text("إلغاء") }
            },
        )
    }
}

/**
 * النقطة السائلة الحية — العدّ الرقمي واسم التسبيح داخلها.
 * تعيش في حلقة إطاراتها الخاصة كي لا توقظ بقية الشاشة كل فريم:
 * السطح متموج دائماً ويتنفّس وينساب، وكمية نشاطه [energy] تُضخّ مع كل
 * ضغطة وتذبل زمنياً، وقطرات التناثر تعيش وتسقط بالجاذبية وتتلاشى.
 */
@Composable
private fun LiquidCounter(
    count: Int,
    dhikrText: String,
    subtitle: String,
    events: SharedFlow<MisbahaEvent>,
    blobR: Float,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme

    // زمن حي لا يتوقف — السائل لا يسكن (نفس لغة مزلاج المظهر وقطرة التذكير)
    var frameNanos by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            withInfiniteAnimationFrameNanos { frameNanos = it }
        }
    }
    val t = frameNanos / 1_000_000_000f

    // نشاط السائل (0..1) — يُضخّ مع كل ضغطة ويذبل بنعومة
    var energy by remember { mutableFloatStateOf(0f) }
    var lastT by remember { mutableFloatStateOf(0f) }

    // قطرات التناثر — مواضعها نسبية لمركز النقطة
    val particles = remember { mutableStateListOf<MisbahaSplashMath.Particle>() }

    // نبضة إتمام الدورة — السائل كله يتوسع ويعود
    val pulseScale = remember { Animatable(1f) }

    // التناثر والنبض من أحداث العدّ — مُصغيّ ثانٍ للتيار نفسه
    LaunchedEffect(blobR) {
        events.collect { event ->
            when (event) {
                is MisbahaEvent.Counted -> {
                    energy = MisbahaSplashMath.bumpEnergy(energy)
                    particles.addAll(
                        MisbahaSplashMath.spawn(event.particles, 0f, 0f, blobR),
                    )
                }
                MisbahaEvent.RoundCompleted -> {
                    energy = 1f
                    particles.addAll(
                        MisbahaSplashMath.spawn(
                            MisbahaSplashMath.MAX_TAP_PARTICLES, 0f, 0f, blobR,
                        ),
                    )
                    pulseScale.snapTo(1f)
                    pulseScale.animateTo(1.06f, tween(durationMillis = 110))
                    pulseScale.animateTo(1f, spring(stiffness = 500f))
                }
                MisbahaEvent.RushWarned -> {
                    // ارتجاجة قوية بلا تناثر — لم تُعد شيئاً
                    energy = 1f
                }
            }
            while (particles.size > MisbahaSplashMath.MAX_LIVE_PARTICLES) {
                particles.removeAt(0)
            }
        }
    }

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    scaleX = pulseScale.value
                    scaleY = pulseScale.value
                },
        ) {
            val dt = (t - lastT).coerceIn(0f, 0.1f)
            lastT = t
            energy = MisbahaSplashMath.decayEnergy(energy, dt)

            // حياة القطرات: طيران وجاذبية وتلاشٍ — والميتة تُرفع
            val iter = particles.iterator()
            while (iter.hasNext()) {
                val p = iter.next()
                MisbahaSplashMath.advance(p, dt, blobR)
                if (p.life <= 0f) iter.remove()
            }

            val cx = size.width / 2f
            // الانسياب الرأسي نفسه الذي يعوم به سائل المزلاج — العدّ يتبعه
            val cy = size.height / 2f + blobR * GooeyMath.centerDrift(t, 0.7f)
            val breathe = 1f + 0.045f * sin(t * 0.9f)
            val liquid = Path()
            addLiquidBlob(
                liquid, cx, cy,
                blobR * breathe, t, energy = energy, phase = 0.7f,
            )
            // قطرة صغيرة تدور ببطء حول النقطة — تغمر في السطح وتبرز منه
            val orbAngle = t * 0.55f
            val orbDist = blobR * (1.18f + 0.06f * sin(t * 0.8f + 1.1f))
            val orbX = cx + orbDist * cos(orbAngle)
            val orbY = cy + orbDist * sin(orbAngle) * 0.72f
            addLiquidBlob(
                liquid, orbX, orbY,
                blobR * 0.08f, t, energy = 0f, phase = 2.1f,
            )
            drawPath(
                liquid,
                Brush.verticalGradient(
                    listOf(scheme.primaryContainer, scheme.secondaryContainer),
                    startY = cy - blobR * 1.35f,
                    endY = cy + blobR * 1.35f,
                ),
            )
            // قطرات التناثر فوق السائل — ألفاها من عمرها فتتلاشى وهي تسقط
            for (p in particles) {
                drawCircle(
                    color = scheme.primary.copy(alpha = 0.9f * p.life),
                    radius = p.radius,
                    center = Offset(cx + p.x, cy + p.y),
                )
            }
        }

        // العدّ الرقمي واسم التسبيح — يعومان داخل السائل باتباع الانسياب نفسه
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .matchParentSize()
                .offset { IntOffset(0, (blobR * GooeyMath.centerDrift(t, 0.7f)).roundToInt()) }
                .padding(horizontal = 44.dp),
        ) {
            Text(
                text = MisbahaMath.arabicNumber(count.toLong()),
                style = MaterialTheme.typography.displayLarge,
                color = scheme.onPrimaryContainer,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = dhikrText,
                style = MaterialTheme.typography.titleLarge,
                color = scheme.primary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = scheme.primary.copy(alpha = 0.75f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** خرزة هدف — ٣٣ / ٩٩ / ١٠٠ / بلا حدّ */
@Composable
private fun TargetChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = if (selected) scheme.primaryContainer else scheme.surface,
        contentColor = if (selected) scheme.onPrimaryContainer else scheme.onSurfaceVariant,
        border = androidx.compose.foundation.BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) scheme.primary else scheme.outline.copy(alpha = 0.35f),
        ),
        modifier = modifier,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 10.dp),
        )
    }
}

/** خرزة ذكر — الاسم وعدّه الحالي، والمختار بلون الحاوية الأساسي */
@Composable
private fun DhikrChip(
    text: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (selected) scheme.secondaryContainer else scheme.surface,
        contentColor = if (selected) scheme.onSecondaryContainer else scheme.onSurfaceVariant,
        border = androidx.compose.foundation.BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) scheme.secondary else scheme.outline.copy(alpha = 0.35f),
        ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 14.dp, end = 10.dp, top = 9.dp, bottom = 9.dp),
        ) {
            Text(text = text, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.size(6.dp))
            Surface(
                shape = RoundedCornerShape(50),
                color = if (selected) {
                    scheme.secondary.copy(alpha = 0.22f)
                } else {
                    scheme.outline.copy(alpha = 0.14f)
                },
            ) {
                Text(
                    text = MisbahaMath.arabicNumber(count.toLong()),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                )
            }
        }
    }
}
