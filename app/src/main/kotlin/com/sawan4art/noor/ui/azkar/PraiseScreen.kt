package com.sawan4art.noor.ui.azkar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.sawan4art.noor.domain.azkar.AzkarContent
import com.sawan4art.noor.domain.tasbih.MisbahaMath
import com.sawan4art.noor.domain.tasbih.MisbahaSplashMath
import com.sawan4art.noor.ui.nav.GooeyMath
import com.sawan4art.noor.ui.nav.addLiquidBlob
import com.sawan4art.noor.ui.theme.NoorPageTitle
import com.sawan4art.noor.util.MisbahaFeedback
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharedFlow
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * عدّاد الصلاة على النبي ﷺ — **ذكرٌ مستقل** (توجيه المالك) بعيدٌ كلياً
 * عن تسبيح ما بعد الصلاة: عدّ حرّ يتصاعد بلا هدف ولا مائات، بنقطة
 * السائل الحيّ نفسها (GooeyMath/addLiquidBlob) وتناثر قطرات MisbahaSplashMath
 * بكثافة سرعة النقر، وكل مائةٍ رشّة احتفال ونبضة واهتزاز أوضح، وحارس
 * التسرع MisbahaRushGuard نفسه: النقر بآلة أسرع من نطق الصلاة ينبّه
 * «لا تسرع» ويوقف العدّ ثلاث ثوانٍ.
 *
 * الصلاة الإبراهيمية الكاملة معروضة أسفل النقطة للقراءة، وفوقها
 * عدّك الحالي محفوظٌ تلقائياً ويصفَّر بإرادتك فقط.
 */
@Composable
fun PraiseScreen(
    viewModel: AzkarViewModel,
    onBack: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pausedUntil by viewModel.pausedUntil.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val feedback = remember { MisbahaFeedback(context) }

    // الاهتزاز — نخلة مع كل صلاة، وموجة أوضح عند كل مائة
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is AzkarEvent.PraiseCounted -> feedback.tap()
                AzkarEvent.PraiseHundredth -> feedback.dhikrCompleted()
                AzkarEvent.RushWarned -> feedback.rush()
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

    var showReset by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.background)
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "رجوع",
                    tint = scheme.primary,
                )
            }
            Column {
                // alpha39: العنوان داخل إطار NoorPageTitle
                NoorPageTitle(text = "الصلاة على النبي")
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "ذكرٌ مستقل — عدّك يُحفظ تلقائياً",
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(8.dp))

        // ===== النقطة السائلة الحية — كل ضغطة صلاة على النبي =====
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .pointerInput(Unit) { detectTapGestures { viewModel.tapPraise() } },
            contentAlignment = Alignment.Center,
        ) {
            val side = minOf(maxHeight, maxWidth)
            val blobR = with(LocalDensity.current) { (side * 0.38f).toPx() }
            LiquidPraise(
                count = state.praiseCount,
                events = viewModel.events,
                blobR = blobR,
                modifier = Modifier.matchParentSize(),
            )
        }

        // ===== تنبيه التسرع — الصلاة تُنطق لا تُنقر =====
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
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(Modifier.size(12.dp))
                    Column {
                        Text(
                            text = "لا تسرع في الصلاة على النبي",
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

        // ===== الصلاة الإبراهيمية الكاملة — نص القراءة =====
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = scheme.surface,
            border = BorderStroke(
                width = 1.dp,
                color = scheme.outline.copy(alpha = 0.3f),
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Text(
                    text = AzkarContent.PRAISE_FULL,
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = AzkarContent.PRAISE_SOURCE + " — " + AzkarContent.PRAISE_VIRTUE,
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
        Spacer(Modifier.height(6.dp))

        // ===== صف الإعادة =====
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { showReset = true }) {
                Icon(
                    Icons.Filled.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = scheme.primary,
                )
                Spacer(Modifier.size(6.dp))
                Text("تصفير العدّ")
            }
        }
        Spacer(Modifier.height(112.dp)) // مساحة شريط التنقل الزجاجي — دائماً
    }

    if (showReset) {
        AlertDialog(
            onDismissRequest = { showReset = false },
            title = { Text("تصفير العدّ؟") },
            text = {
                Text("سيبدأ عدّ الصلاة على النبي من الصفر من جديد.")
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.resetPraise()
                    showReset = false
                }) { Text("تصفير", color = scheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showReset = false }) { Text("إلغاء") }
            },
        )
    }
}

/**
 * النقطة السائلة لعدّ الصلاة — صعوداً لا هبوطاً: العدد الحالي كبيرٌ
 * في قلب السائل ونص الذكر تحته، وبقية الحياة كما في المسبحة: تنفّس
 * وانسياب وقطرة مدارية وقطرات تناثر تعيش بالجاذبية وتتلاشى.
 */
@Composable
private fun LiquidPraise(
    count: Int,
    events: SharedFlow<AzkarEvent>,
    blobR: Float,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme

    var frameNanos by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            withInfiniteAnimationFrameNanos { frameNanos = it }
        }
    }
    val t = frameNanos / 1_000_000_000f

    var energy by remember { mutableFloatStateOf(0f) }
    var lastT by remember { mutableFloatStateOf(0f) }

    val particles = remember { mutableStateListOf<MisbahaSplashMath.Particle>() }

    val pulseScale = remember { Animatable(1f) }

    LaunchedEffect(blobR) {
        events.collect { event ->
            when (event) {
                is AzkarEvent.PraiseCounted -> {
                    energy = MisbahaSplashMath.bumpEnergy(energy)
                    particles.addAll(
                        MisbahaSplashMath.spawn(event.particles, 0f, 0f, blobR),
                    )
                }
                AzkarEvent.PraiseHundredth -> {
                    // مائةٌ كاملة — رشّة أقصى ونبضة توسع
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
                AzkarEvent.RushWarned -> energy = 1f
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

            val iter = particles.iterator()
            while (iter.hasNext()) {
                val p = iter.next()
                MisbahaSplashMath.advance(p, dt, blobR)
                if (p.life <= 0f) iter.remove()
            }

            val cx = size.width / 2f
            val cy = size.height / 2f + blobR * GooeyMath.centerDrift(t, 0.7f)
            val breathe = 1f + 0.045f * sin(t * 0.9f)
            val liquid = Path()
            addLiquidBlob(
                liquid, cx, cy,
                blobR * breathe, t, energy = energy, phase = 0.7f,
            )
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
            for (p in particles) {
                drawCircle(
                    color = scheme.primary.copy(alpha = 0.9f * p.life),
                    radius = p.radius,
                    center = Offset(cx + p.x, cy + p.y),
                )
            }
        }

        // العدّاد الصاعد ونص الذكر — يعومان باتباع الانسياب نفسه
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
                text = AzkarContent.PRAISE_TEXT,
                style = MaterialTheme.typography.bodyLarge,
                color = scheme.primary,
                textAlign = TextAlign.Center,
                maxLines = 3,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "اضغط لإضافة صلاة — وكل مائةٍ موجة",
                style = MaterialTheme.typography.labelMedium,
                color = scheme.primary.copy(alpha = 0.75f),
                textAlign = TextAlign.Center,
            )
        }
    }
}
