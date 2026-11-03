package com.sawan4art.noor.ui.qibla

import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sawan4art.noor.data.NoorSettingsState
import com.sawan4art.noor.domain.prayer.PrayerMath
import com.sawan4art.noor.domain.qibla.QiblaMath
import com.sawan4art.noor.domain.tasbih.MisbahaMath
import com.sawan4art.noor.domain.tasbih.MisbahaSplashMath
import com.sawan4art.noor.ui.nav.addLiquidBlob
import com.sawan4art.noor.ui.theme.NoorPageTitle
import com.sawan4art.noor.util.MisbahaFeedback
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * بوصلة القبلة — توجيه المالك alpha29: «ابدا في القبله شكل البوصله
 * هيكون نقطه سائله حيه مثل نقطه التسبيح و يكون بداخلها سهم و اتجاهات
 * و رقم درجه الاتجاه و هكذا وكل ما يلزم النقطه هنا هتكون حيه و متفاعله
 * لما المستخدم يتجه الى الاتجاه الصحيح تبدا النقطه الحيه بتناثر اجزاء
 * منها مثل الموجوده في المسبحه».
 *
 * - **القرص حلقة اتجاهات** تدور مع الجهاز: علامات كل ١٥° وكلمات
 *   الشمال والشرق والجنوب والغرب، وعلامة الكعبة السوداء تجلس على
 *   الحلقة عند زاوية القبلة الحقيقية — والحلقة تلاصق النقطة السائلة
 *   (أكبر منها بمقدار ضئيل — توجيه المالك alpha30).
 * - **دقة الاتجاه**: الاتجاه المغناطيسي يُصحّح إلى حقيقي بانحراف
 *   الموقع المغناطيسي (GeomagneticField — alpha30)، والقبلة تُحسب
 *   بالمثلثة الكروية من إحداثيات المستخدم.
 * - **النقطة السائلة الحية** (لغة المسبحة حرفياً — addLiquidBlob) في
 *   المركز، بداخلها **سهم القبلة** المستقيم للأعلى عند المواجهة
 *   و**رقم درجة الاتجاه** من الشمال.
 * - **التفريج**: مواجهة القبلة (± ٤°) تنثر قطرات النقطة كالمسبحة
 *   باستمرار مع اهتزاز الدخول، وتركُها يهدّئها.
 * - **بلا بوصلة حقيقية؟** الوضع اليدوي: شدّ القرص بإصبعك يميناً
 *   ويساراً حتى يستقيم السهم.
 */
@Composable
fun QiblaScreen(
    settings: NoorSettingsState,
    viewModel: QiblaViewModel = viewModel(),
) {
    val scheme = MaterialTheme.colorScheme
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val feedback = remember { MisbahaFeedback(context) }

    // المستشعر حياة الشاشة — ظهور تسجّل واختفاء يُلغى (ترشيد بطارية)،
    // والإحداثيان يمرران لحساب الانحراف المغناطيسي وتصحيح الاتجاه (alpha30)
    DisposableEffect(settings.latitude, settings.longitude) {
        viewModel.start(settings.latitude, settings.longitude)
        onDispose { viewModel.stop() }
    }

    val qiblaBearing = PrayerMath.calculateQibla(settings.latitude, settings.longitude).toFloat()
    val distanceKm = PrayerMath.calculateDistanceToKaaba(settings.latitude, settings.longitude)

    val heading = state.heading
    val diff = QiblaMath.signedDiff(qiblaBearing, heading)
    val aligned = QiblaMath.isAligned(diff)

    // اهتزاز دخول الاستقامة — مرة واحدة لكل دخول
    var wasAligned by remember { mutableStateOf(false) }
    LaunchedEffect(aligned) {
        if (aligned && !wasAligned) {
            feedback.dhikrCompleted()
        }
        wasAligned = aligned
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.background)
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        // alpha39: العنوان داخل إطار NoorPageTitle
        NoorPageTitle(text = "بوصلة القبلة")
        Spacer(Modifier.height(6.dp))
        Text(
            text = "وجه جهازك حتى يستقيم السهم لأعلى وتبدأ النقطة بالتناثر",
            style = MaterialTheme.typography.labelMedium,
            color = scheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))

        // ===== شريط الحالة — أنت على القبلة / استدر يميناً أو يساراً =====
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (aligned) scheme.primaryContainer else scheme.surface,
            contentColor = if (aligned) scheme.onPrimaryContainer else scheme.onSurface,
            border = BorderStroke(
                width = 1.dp,
                color = if (aligned) scheme.primary else scheme.outline.copy(alpha = 0.3f),
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(vertical = 10.dp, horizontal = 14.dp),
            ) {
                Text(
                    text = if (aligned) {
                        "أنت على القبلة الآن — تقبل الله"
                    } else {
                        val dir = if (diff > 0) "يميناً" else "يساراً"
                        "استدر " + MisbahaMath.arabicNumber(abs(diff).roundToInt().toLong()) +
                            "° " + dir + " حتى يستقيم السهم"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Spacer(Modifier.height(4.dp))

        if (!state.sensorAvailable) {
            // الوضع اليدوي — جهاز بلا بوصلة: شدّ القرص بإصبعك
            Spacer(Modifier.height(4.dp))
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = scheme.surfaceVariant,
                contentColor = scheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "جهازك لا يحتوي بوصلة حقيقية — شدّ القرص بإصبعك يميناً ويساراً لتقريب السهم من القبلة",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }

        // ===== القرص الحي — النقطة السائلة بسهمها ودرجتها وحلقة الاتجاهات =====
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .pointerInput(state.sensorAvailable) {
                    if (!state.sensorAvailable) {
                        detectHorizontalDragGestures { _, dragAmount ->
                            viewModel.dragHeading(dragAmount * 0.35f)
                        }
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            val side = minOf(maxHeight, maxWidth)
            val blobR = with(LocalDensity.current) {
                // أمر المالك alpha30: «كبره النقطه السائل بتاعت البوصله و
                // صغر حجم دائر الاتجاه التي تحيط بالنقطه السائل لتناسب
                // حجمها و تكون اكبر منها بمقدار ضئيل» — النقطة تكبر وكل
                // الحلقة والعلامات والكلمات تُحتسب داخل نصف الصندوق
                val sidePx = side.toPx()
                ((sidePx / 2f - 31.dp.toPx()) / 1.20f)
                    .coerceIn(sidePx * 0.25f, sidePx * 0.42f)
            }
            LiquidCompass(
                qiblaBearing = qiblaBearing,
                heading = heading,
                aligned = aligned,
                blobR = blobR,
                modifier = Modifier.matchParentSize(),
            )
        }

        // ===== المسافة والزاوية =====
        Spacer(Modifier.height(6.dp))
        Text(
            text = "الكعبة المشرّفة على بعد " + MisbahaMath.arabicNumber(distanceKm.toLong()) +
                " كم — القبلة بزاوية " + MisbahaMath.arabicNumber(qiblaBearing.roundToInt().toLong()) +
                "° من الشمال",
            style = MaterialTheme.typography.labelMedium,
            color = scheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(112.dp)) // مساحة شريط التنقل الزجاجي — دائماً
    }
}

/**
 * القرص السائل الحي — كل الرسم في حلقة إطاراته الخاصة كي لا توقظ بقية
 * الشاشة كل فريم: حلقة الاتجاهات تدور مع الجهاز، النقطة تتنفس وتتموج،
 * السهم يستقيم للأعلى عند مواجهة القبلة، والمواجهة تنثر القطرات.
 */
@Composable
private fun LiquidCompass(
    qiblaBearing: Float,
    heading: Float,
    aligned: Boolean,
    blobR: Float,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val textMeasurer = rememberTextMeasurer()

    // زمن حي لا يتوقف — السائل لا يسكن (نفس لغة المسبحة ومزلاج المظهر)
    var frameNanos by remember { mutableStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            withInfiniteAnimationFrameNanos { frameNanos = it }
        }
    }
    val t = frameNanos / 1_000_000_000f

    // نشاط السائل (0..1) — المواجهة تضخّه وتركها يذبل
    var energy by remember { mutableFloatStateOf(0f) }
    var lastT by remember { mutableFloatStateOf(0f) }
    var lastShedT by remember { mutableFloatStateOf(0f) }
    var prevAligned by remember { mutableStateOf(false) }

    // قطرات التناثر — مواضعها نسبية لمركز النقطة (نفس رياضيات المسبحة)
    val particles = remember { mutableStateListOf<MisbahaSplashMath.Particle>() }

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val dt = (t - lastT).coerceIn(0f, 0.1f)
            lastT = t
            energy = MisbahaSplashMath.decayEnergy(energy, dt)

            val cx = size.width / 2f
            val cy = size.height / 2f

            // ===== تفريج المواجهة — دخول رشّة أقصى وبقاء رشّة مستمرة =====
            val particlesNow = QiblaMath.alignedParticles(prevAligned, aligned)
            prevAligned = aligned
            if (aligned) {
                energy = 1f
                if (particlesNow > MisbahaSplashMath.BASE_PARTICLES ||
                    t - lastShedT >= 0.14f
                ) {
                    particles.addAll(MisbahaSplashMath.spawn(particlesNow, 0f, 0f, blobR))
                    lastShedT = t
                }
            }
            while (particles.size > MisbahaSplashMath.MAX_LIVE_PARTICLES) {
                particles.removeAt(0)
            }

            // ===== حلقة الاتجاهات — تدور مع الجهاز، تلاصق النقطة السائلة =====
            // alpha30: من ١٫٤٢ إلى ١٫٢٠ — الحلقة أكبر من النقطة بمقدار ضئيل،
            // والعلامات قُلبت خارج الحلقة كي لا تُصطدم بسطحها المتموج
            val ringR = blobR * 1.20f
            drawCircle(
                color = scheme.outline.copy(alpha = 0.35f),
                radius = ringR,
                center = Offset(cx, cy),
                style = Stroke(width = 1.5.dp.toPx()),
            )
            // علامات كل ١٥ درجة منبثقة خارج الحلقة — ومواضع الاتجاهات الأصلية
            // بلا علامة (كلمة شمال/شرق/جنوب/غرب هي علامتها الكبرى — لا تصادم)
            var deg = 0
            while (deg < 360) {
                if (deg % 90 != 0) {
                    val screenDeg = deg - heading
                    val rad = Math.toRadians((screenDeg - 90.0))
                    val inner = ringR
                    val outer = ringR + 5.dp.toPx()
                    drawLine(
                        color = scheme.outline.copy(alpha = 0.35f),
                        start = Offset(
                            cx + inner * cos(rad).toFloat(),
                            cy + inner * sin(rad).toFloat(),
                        ),
                        end = Offset(
                            cx + outer * cos(rad).toFloat(),
                            cy + outer * sin(rad).toFloat(),
                        ),
                        strokeWidth = 1.dp.toPx(),
                    )
                }
                deg += 15
            }
            // كلمات الاتجاهات الأربعة — الشمال دائماً أعلى القرص الحقيقي
            val cardinals = listOf(
                "شمال" to 0f, "شرق" to 90f, "جنوب" to 180f, "غرب" to 270f,
            )
            cardinals.forEach { (label, bearing) ->
                val screenDeg = bearing - heading
                val rad = Math.toRadians((screenDeg - 90.0))
                val labelR = ringR + 16.dp.toPx()
                val lx = cx + labelR * cos(rad).toFloat()
                val ly = cy + labelR * sin(rad).toFloat()
                val isNorth = bearing == 0f
                val layout = textMeasurer.measure(
                    AnnotatedString(label),
                    style = TextStyle(
                        color = if (isNorth) scheme.primary else scheme.onSurfaceVariant,
                        fontSize = (if (isNorth) 13 else 11).sp,
                        fontWeight = if (isNorth) FontWeight.Bold else FontWeight.Medium,
                    ),
                )
                drawText(
                    layout,
                    topLeft = Offset(lx - layout.size.width / 2f, ly - layout.size.height / 2f),
                )
            }

            // ===== علامة الكعبة على الحلقة — عند زاوية القبلة الحقيقية =====
            val kaabaScreenDeg = qiblaBearing - heading
            val kaabaRad = Math.toRadians((kaabaScreenDeg - 90.0))
            val kaabaSize = 7.dp.toPx()
            val kx = cx + ringR * cos(kaabaRad).toFloat()
            val ky = cy + ringR * sin(kaabaRad).toFloat()
            drawCircle(
                color = scheme.surface,
                radius = kaabaSize * 0.85f,
                center = Offset(kx, ky),
            )
            drawCircle(
                color = Color(0xFF1B1B1B),
                radius = kaabaSize * 0.62f,
                center = Offset(kx, ky),
            )

            // ===== النقطة السائلة الحية — لغة المسبحة نفسها =====
            // حياة القطرات: طيران وجاذبية وتلاشٍ — والميتة تُرفع
            val iter = particles.iterator()
            while (iter.hasNext()) {
                val p = iter.next()
                MisbahaSplashMath.advance(p, dt, blobR)
                if (p.life <= 0f) iter.remove()
            }

            val breathe = 1f + 0.04f * sin(t * 0.9f)
            val liquid = Path()
            addLiquidBlob(
                liquid, cx, cy,
                blobR * breathe, t, energy = energy, phase = 1.3f,
            )
            drawPath(
                liquid,
                Brush.verticalGradient(
                    listOf(scheme.primaryContainer, scheme.secondaryContainer),
                    startY = cy - blobR * 1.35f,
                    endY = cy + blobR * 1.35f,
                ),
            )
            // قطرات التناثر فوق السائل — ألفاها من عمرها
            for (p in particles) {
                drawCircle(
                    color = scheme.primary.copy(alpha = 0.9f * p.life),
                    radius = p.radius,
                    center = Offset(cx + p.x, cy + p.y),
                )
            }

            // ===== سهم القبلة داخل النقطة — يستقيم للأعلى عند المواجهة =====
            val arrowAngle = QiblaMath.arrowAngle(qiblaBearing, heading)
            val shaft = blobR * 0.40f
            val headW = blobR * 0.24f
            val headH = blobR * 0.32f
            rotate(degrees = arrowAngle, pivot = Offset(cx, cy)) {
                // مركز السهم
                drawCircle(
                    color = scheme.primary,
                    radius = 3.dp.toPx(),
                    center = Offset(cx, cy),
                )
                // ذيل السهم — نقطة لأسفل
                drawCircle(
                    color = scheme.primary.copy(alpha = 0.55f),
                    radius = 4.5.dp.toPx(),
                    center = Offset(cx, cy + shaft * 0.78f),
                )
                // رأس السهم المثلث
                val tip = Offset(cx, cy - shaft)
                val left = Offset(cx - headW / 2f, cy - shaft + headH)
                val right = Offset(cx + headW / 2f, cy - shaft + headH)
                val head = Path().apply {
                    moveTo(tip.x, tip.y)
                    lineTo(left.x, left.y)
                    lineTo(right.x, right.y)
                    close()
                }
                drawPath(head, scheme.primary)
            }

            // ===== رقم درجة الاتجاه — داخل النقطة تحت السهم =====
            val degText = MisbahaMath.arabicNumber(qiblaBearing.roundToInt().toLong()) + "°"
            val degLayout = textMeasurer.measure(
                AnnotatedString(degText),
                style = TextStyle(
                    color = scheme.onPrimaryContainer,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
            drawText(
                degLayout,
                topLeft = Offset(
                    cx - degLayout.size.width / 2f,
                    cy + blobR * 0.52f - degLayout.size.height / 2f,
                ),
            )
            val degLabel = textMeasurer.measure(
                AnnotatedString("اتجاه القبلة"),
                style = TextStyle(
                    color = scheme.primary.copy(alpha = 0.85f),
                    fontSize = 11.sp,
                ),
            )
            drawText(
                degLabel,
                topLeft = Offset(
                    cx - degLabel.size.width / 2f,
                    cy + blobR * 0.52f + degLayout.size.height * 0.7f,
                ),
            )
        }
    }
}
