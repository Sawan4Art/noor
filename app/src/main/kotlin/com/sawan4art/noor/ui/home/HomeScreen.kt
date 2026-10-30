package com.sawan4art.noor.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sawan4art.noor.BuildConfig
import com.sawan4art.noor.domain.prayer.PrayerTimeKey
import com.sawan4art.noor.domain.prayer.PrayerMath
import com.sawan4art.noor.domain.prayer.PrayerTimes
import com.sawan4art.noor.ui.theme.LocalNoorColors
import com.sawan4art.noor.util.Hijri
import com.sawan4art.noor.util.TimeFormat
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * الشاشة الرئيسية: تحية + المدينة والتاريخ + بطاقة الهيرو (الصلاة القادمة
 * بعدّاد تنازلي وحلقة تقدم ذهبية) + شبكة مواقيت اليوم + تذييل الصدقة الجارية.
 */
@Composable
fun HomeScreen(
    state: HomeViewModel.HomeUiState,
    locating: Boolean,
    onDetectLocation: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val noorColors = LocalNoorColors.current
    val zone = remember { ZoneId.systemDefault() }
    val now: ZonedDateTime = remember(state.nowMillis) {
        Instant.ofEpochMilli(state.nowMillis).atZone(zone)
    }
    val today = now.toLocalDate()
    val settings = state.settings
    val times: PrayerTimes = remember(
        settings.method, settings.isHanafi, settings.latitude, settings.longitude, today, zone,
    ) {
        PrayerMath.compute(today, settings.latitude, settings.longitude, zone, settings.method, settings.isHanafi)
    }
    val next = remember(times, now) { PrayerMath.nextPrayer(times, now) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Spacer(Modifier.height(12.dp))
        HomeHeader(
            city = settings.city,
            locating = locating,
            onDetectLocation = onDetectLocation,
            onOpenSettings = onOpenSettings,
        )
        Spacer(Modifier.height(20.dp))
        HeroCard(next = next, now = now)
        Spacer(Modifier.height(24.dp))
        Text(
            text = "مواقيت اليوم — ${Hijri.gregorianArabic(today)}",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(12.dp))
        PrayerTimesGrid(times = times, activeKey = next.key)
        Spacer(Modifier.height(28.dp))
        Text(
            text = "هذا التطبيق صدقة جارية على روح والد المالك — رحمه الله رحمة واسعة",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .alpha(0.75f)
                .padding(bottom = 8.dp),
        )
        Text(
            text = "نور المسلم ${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .alpha(0.5f)
                .padding(bottom = 8.dp),
        )
        Spacer(Modifier.height(112.dp)) // مساحة شريط التنقل الزجاجي العائم
    }
}

@Composable
private fun HomeHeader(
    city: String,
    locating: Boolean,
    onDetectLocation: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                text = "السلام عليكم ورحمة الله",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.size(4.dp))
                Text(
                    text = city,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                if (locating) {
                    Spacer(Modifier.size(8.dp))
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
        }
        IconButton(
            onClick = onDetectLocation,
            modifier = Modifier.size(40.dp),
        ) {
            Icon(
                Icons.Filled.LocationOn,
                contentDescription = "تحديد الموقع تلقائياً",
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        IconButton(onClick = onOpenSettings, modifier = Modifier.size(40.dp)) {
            Icon(
                Icons.Filled.Settings,
                contentDescription = "الإعدادات",
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun HeroCard(next: com.sawan4art.noor.domain.prayer.NextPrayer, now: ZonedDateTime) {
    val noorColors = LocalNoorColors.current
    val gold = MaterialTheme.colorScheme.secondary
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(noorColors.heroGradient)
            .padding(vertical = 28.dp, horizontal = 24.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "الصلاة القادمة",
                style = MaterialTheme.typography.labelLarge,
                color = noorColors.heroMuted,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = next.arabicName,
                style = MaterialTheme.typography.displaySmall,
                color = noorColors.heroContent,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = TimeFormat.prayerTime(next.target),
                style = MaterialTheme.typography.titleMedium,
                color = noorColors.heroMuted,
            )
            Spacer(Modifier.height(18.dp))
            ProgressRing(
                progressPercent = next.progressPercent,
                ringColor = gold,
                trackColor = Color.White.copy(alpha = 0.12f),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = TimeFormat.remainingArabic(next.remainingSeconds),
                        style = MaterialTheme.typography.headlineMedium,
                        color = noorColors.heroContent,
                    )
                    Text(
                        text = "المتبقي",
                        style = MaterialTheme.typography.labelMedium,
                        color = noorColors.heroMuted,
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HeroChip(text = Hijri.of(now.toLocalDate()).formatted)
                HeroChip(text = Hijri.gregorianArabic(now.toLocalDate()))
            }
        }
    }
}

@Composable
private fun HeroChip(text: String) {
    Surface(
        color = Color.White.copy(alpha = 0.10f),
        contentColor = Color.White,
        shape = RoundedCornerShape(50),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun ProgressRing(
    progressPercent: Int,
    ringColor: Color,
    trackColor: Color,
    content: @Composable () -> Unit,
) {
    val target = (progressPercent.coerceIn(0, 100)) / 100f
    val animated by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = 700),
        label = "prayerProgress",
    )
    Box(modifier = Modifier.size(176.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val strokePx = 11.dp.toPx()
            val diameter = size.minDimension - strokePx
            val topLeft = androidx.compose.ui.geometry.Offset(
                (size.width - diameter) / 2f,
                (size.height - diameter) / 2f,
            )
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = androidx.compose.ui.geometry.Size(diameter, diameter),
                style = Stroke(width = strokePx, cap = StrokeCap.Round),
            )
            drawArc(
                brush = Brush.sweepGradient(
                    0f to ringColor.copy(alpha = 0.25f),
                    0.5f to ringColor,
                    1f to ringColor.copy(alpha = 0.25f),
                ),
                startAngle = -90f,
                sweepAngle = 360f * animated,
                useCenter = false,
                topLeft = topLeft,
                size = androidx.compose.ui.geometry.Size(diameter, diameter),
                style = Stroke(width = strokePx, cap = StrokeCap.Round),
            )
        }
        content()
    }
}

@Composable
private fun PrayerTimesGrid(times: PrayerTimes, activeKey: PrayerTimeKey) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PrayerTimeKey.entries.toList().chunked(3).forEach { rowKeys ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                rowKeys.forEach { key ->
                    PrayerCard(
                        name = PrayerMath.PRAYER_NAMES_AR.getValue(key),
                        time = TimeFormat.prayerTime(times[key]),
                        active = key == activeKey,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun PrayerCard(name: String, time: String, active: Boolean, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        color = if (active) scheme.secondaryContainer else scheme.surface,
        border = if (active) {
            androidx.compose.foundation.BorderStroke(1.5.dp, scheme.secondary)
        } else {
            androidx.compose.foundation.BorderStroke(1.dp, scheme.outline.copy(alpha = 0.35f))
        },
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 16.dp, horizontal = 8.dp),
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                color = if (active) scheme.onSecondaryContainer else scheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = time,
                style = MaterialTheme.typography.bodyLarge,
                color = if (active) scheme.onSecondaryContainer else scheme.onBackground,
            )
        }
    }
}
