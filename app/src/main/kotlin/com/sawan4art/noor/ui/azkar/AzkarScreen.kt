package com.sawan4art.noor.ui.azkar

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sawan4art.noor.domain.azkar.AzkarContent
import com.sawan4art.noor.domain.azkar.AzkarGroup
import com.sawan4art.noor.domain.azkar.AzkarMath
import com.sawan4art.noor.domain.tasbih.MisbahaMath
import com.sawan4art.noor.ui.icons.NoorIcons

/**
 * تبويب الأذكار — القسم الموعود (توجيه المالك: «الصلاه على النبي ده
 * ذكر لوحده .. اما في قسم الاذكار مستقبلا هنحط اذكار كتير»):
 *
 * - **الصلاة على النبي ﷺ** بطاقةٌ كبرى تفتح عدّادها السائل الحرّ.
 * - أذكار الصباح والمساء والنوم — بطاقات مجموعات بمقدّر تقدّم يوميّ
 *   يتجدد كل يوم، تفتح شاشة القراءة.
 *
 * التنقل داخلي بالروابط النصية (الرئيسية/العدّاد/مجموعة) والزر
 * الرجوع يعود خطوةً خطوة — بلا أي مساهمة من معالج NoorApp (المعطّل
 * لهذا التبويب كي لا يقفز للرئيسية فوق التنقل الداخلي).
 */
@Composable
fun AzkarScreen(
    onExitTab: () -> Unit,
    viewModel: AzkarViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // الروابط الداخلية: home ← praise / المعرّف النصي للمجموعة
    var route by rememberSaveable { mutableStateOf("home") }

    BackHandler(enabled = route != "home") { route = "home" }
    // الرئيسية داخل التبويب: الرجوع يخرج للتبويب السابق (المواقيت)
    BackHandler(enabled = route == "home") { onExitTab() }

    when {
        route == "praise" -> PraiseScreen(
            viewModel = viewModel,
            onBack = { route = "home" },
        )
        route != "home" -> GroupScreen(
            viewModel = viewModel,
            group = AzkarContent.groupById(route),
            onBack = { route = "home" },
        )
        else -> AzkarHome(
            state = state,
            onOpenPraise = { route = "praise" },
            onOpenGroup = { id -> route = id },
        )
    }
}

/** قائمة التبويب: بطاقة الصلاة على النبي ثم بطاقات المجموعات الثلاث */
@Composable
private fun AzkarHome(
    state: AzkarState,
    onOpenPraise: () -> Unit,
    onOpenGroup: (String) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.background)
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        Text(
            text = "الأذكار",
            style = MaterialTheme.typography.headlineSmall,
            color = scheme.onBackground,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = "الصلاة على النبي وأذكار الصباح والمساء والنوم — من السنة الصحيحة",
            style = MaterialTheme.typography.labelMedium,
            color = scheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(14.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ===== بطاقة الصلاة على النبي — ذكرٌ مستقل بعدّاد حرّ =====
            Surface(
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenPraise),
            ) {
                Column(
                    modifier = Modifier
                        .background(
                            brush = Brush.linearGradient(
                                listOf(scheme.primaryContainer, scheme.secondaryContainer),
                            ),
                        )
                        .padding(18.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(
                                    color = scheme.surface.copy(alpha = 0.45f),
                                    shape = RoundedCornerShape(14.dp),
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Filled.Favorite,
                                contentDescription = null,
                                tint = scheme.primary,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "الصلاة على النبي",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = scheme.onPrimaryContainer,
                            )
                            Text(
                                text = AzkarContent.PRAISE_VIRTUE,
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onPrimaryContainer.copy(alpha = 0.8f),
                            )
                        }
                    }
                    if (state.praiseCount > 0) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "عدّك الحالي " + MisbahaMath.arabicNumber(state.praiseCount.toLong()),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = scheme.primary,
                        )
                    }
                }
            }

            // ===== بطاقات المجموعات الثلاث =====
            AzkarContent.GROUPS.forEach { group ->
                GroupCard(
                    group = group,
                    icon = when (group.id) {
                        "morning" -> NoorIcons.Sun
                        "evening" -> NoorIcons.Moon
                        else -> Icons.Filled.Star
                    },
                    progress = AzkarMath.groupProgress(state.doneToday, group),
                    onOpen = { onOpenGroup(group.id) },
                )
            }

            Spacer(Modifier.height(112.dp)) // مساحة شريط التنقل الزجاجي — دائماً
        }
    }
}

/** بطاقة مجموعة أذكار — أيقونتها وعنوانها ومقدّر تقدّم اليوم */
@Composable
private fun GroupCard(
    group: AzkarGroup,
    icon: ImageVector,
    progress: AzkarMath.GroupProgress,
    onOpen: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = scheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = scheme.outline.copy(alpha = 0.28f),
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(
                        brush = Brush.linearGradient(
                            listOf(
                                scheme.primaryContainer.copy(alpha = 0.75f),
                                scheme.secondaryContainer.copy(alpha = 0.75f),
                            ),
                        ),
                        shape = RoundedCornerShape(15.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = scheme.primary,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = group.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onSurface,
                )
                Text(
                    text = group.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    maxLines = 2,
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = MisbahaMath.arabicNumber(progress.doneCount.toLong()) + "/" +
                        MisbahaMath.arabicNumber(progress.total.toLong()),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (progress.allDone) scheme.primary else scheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { progress.fraction.coerceIn(0f, 1f) },
                    modifier = Modifier.width(72.dp),
                    color = scheme.primary,
                    trackColor = scheme.surfaceVariant,
                )
            }
        }
    }
}
