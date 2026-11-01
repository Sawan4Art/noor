package com.sawan4art.noor.ui.tasbih

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sawan4art.noor.domain.tasbih.MisbahaDhikrList
import com.sawan4art.noor.domain.tasbih.MisbahaMath
import com.sawan4art.noor.util.MisbahaFeedback
import kotlin.math.cos
import kotlin.math.sin

/**
 * المسبحة الإلكترونية — تبويب TASBIH يخرج من «قريبًا» إلى عملٍ حيّ
 * (توجيه المالك alpha24: «ابدا شغل في المسبحه»).
 *
 * **حلقة خرز حيّة** بثلاثٍ وثلاثين خرزة (سنة السبحة) تلتفّ كل دورة:
 * خرزةٌ تُضاء بكل تسبيحة، والتسبيحة نفسها ضغطةٌ على أي موضع من
 * الحلقة — بلا زرٍ وسيط يُفصل يدك عن السبحة. العدّ داخل الحلقة
 * بأرقام هندية، وإتمامُ دورةٍ (الهدف) ينبض الحلقة ويهتز مزدوجاً
 * ويعيد العدّ تلقائياً لوردةٍ جديدة، والذهاب (الدورات) يُحفظ لكل ذكر.
 *
 * كل الحسابات في MisbahaMath النقيّ المُختبَر، والحفظ مؤجَّل من
 * MisbahaViewModel كي لا يقرص المخزن مع كل تسبيحة.
 */
@Composable
fun MisbahaScreen(viewModel: MisbahaViewModel = viewModel()) {
    val scheme = MaterialTheme.colorScheme
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val feedback = remember { MisbahaFeedback(context) }

    // نبضة إتمام الدورة — الحلقة تتوسع وتعود بنعومة الربيع
    val pulseScale = remember { Animatable(1f) }

    // الأحداث: نخلة التسبيحة خفيفة، وإتمام الدورة نبضة مزدوجة + نبض الحلقة
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                MisbahaEvent.TAP -> feedback.tap()
                MisbahaEvent.ROUND_COMPLETED -> {
                    feedback.roundCompleted()
                    pulseScale.snapTo(1f)
                    pulseScale.animateTo(1.07f, tween(durationMillis = 110))
                    pulseScale.animateTo(1f, spring(stiffness = 500f))
                }
            }
        }
    }

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
        Spacer(Modifier.height(10.dp))

        // ===== حلقة الخرز — الضغط عليها تسبيحة =====
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        scaleX = pulseScale.value
                        scaleY = pulseScale.value
                    }
                    .pointerInput(Unit) { detectTapGestures { viewModel.tap() } },
            ) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val ringRadius = minOf(size.width, size.height) * 0.42f
                val beadRadius = (2f * Math.PI.toFloat() * ringRadius / MisbahaMath.BEADS) * 0.36f
                val lit = MisbahaMath.beadsLit(state.count)
                val litBrush = Brush.linearGradient(
                    listOf(scheme.primary, scheme.secondary),
                )
                val step = (2f * Math.PI.toFloat() / MisbahaMath.BEADS)
                for (i in 0 until MisbahaMath.BEADS) {
                    // البداية من الأعلى والدوران عكس عقارب السبحة الممسوكة
                    val angle = -Math.PI.toFloat() / 2f + i * step
                    val bead = Offset(
                        center.x + ringRadius * cos(angle),
                        center.y + ringRadius * sin(angle),
                    )
                    when {
                        i < lit -> drawCircle(brush = litBrush, radius = beadRadius, center = bead)
                        i == lit -> drawCircle(
                            color = scheme.primary.copy(alpha = 0.35f),
                            radius = beadRadius,
                            center = bead,
                        )
                        else -> drawCircle(
                            color = scheme.outline.copy(alpha = 0.30f),
                            radius = beadRadius,
                            center = bead,
                        )
                    }
                }
            }
            // العدّ والذكر داخل الحلقة — نصوص حقيقية لا نقشٌ كانفس
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .matchParentSize()
                    .padding(all = 52.dp)
                    .pointerInput(Unit) { detectTapGestures { viewModel.tap() } },
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = MisbahaMath.arabicNumber(state.count.toLong()),
                    style = MaterialTheme.typography.displayLarge,
                    color = scheme.onBackground,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = state.dhikr.text,
                    style = MaterialTheme.typography.titleLarge,
                    color = scheme.primary,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(6.dp))
                val remaining = MisbahaMath.remaining(state.count, state.target)
                Text(
                    text = if (remaining == null) {
                        "الدورات ${MisbahaMath.arabicNumber(state.roundsNow.toLong())} · بلا حدّ"
                    } else {
                        "المتبقي ${MisbahaMath.arabicNumber(remaining.toLong())}" +
                            " · الدورات ${MisbahaMath.arabicNumber(state.roundsNow.toLong())}"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }

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
