package com.sawan4art.noor.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sawan4art.noor.domain.prayer.PrayerTimeKey
import com.sawan4art.noor.domain.prayer.PrayerMath
import com.sawan4art.noor.domain.prayer.PrayerTimes
import com.sawan4art.noor.ui.theme.LocalNoorColors
import com.sawan4art.noor.ui.icons.NoorIcons
import com.sawan4art.noor.util.Hijri
import com.sawan4art.noor.util.TimeFormat
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * الشاشة الرئيسية — **بلا تمرير** (بطلب المالك 2026-10-30):
 * تعديلات alpha12 (بطلب المالك حرفياً):
 * - «الحاجة الوحيدة اللي هتبقى فوق هي هيدر تغيير المظهر» — بطاقة المدينة
 *   المستقلة أُزيلت من الأعلى، ومسار المزلاج صار وحده أول عنصر في الشاشة.
 * - المدينة ودبوسها وزر التحديد انتقلوا **داخل الهيدر المربع (بطاقة الهيرو)
 *   تحت التواريخ** مباشرة.
 * - «مواقيت اليوم» أُزيلت ومكانها **هيدر التذكيرات الجديد** (التذكير المتغير
 *   بأيقونة التطبيق) فوق شبكة المواقيت.
 *
 * المزلاج نفسه يُرسم فوق كل الشاشة من جذر التطبيق (ThemeDialOverlay)
 * **في الرئيسية فقط** (بطلب المالك alpha9) — هنا مساره الفارغ كي يستقر السائل.
 */
@Composable
fun HomeScreen(
    state: HomeViewModel.HomeUiState,
    locating: Boolean,
    onDetectLocation: () -> Unit,
    onDialPlaced: (Float, Float, Float, Float) -> Unit,
) {
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
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        // مسار مزلاج الشمس/القمر — **الوحيد أعلى الشاشة** (بطلب المالك alpha12)
        // والسائل نفسه يُرسم فوق الكل من الجذر
        ThemeDialTrack(
            onPlaced = onDialPlaced,
            modifier = Modifier.padding(top = 4.dp),
        )
        Spacer(Modifier.height(4.dp))
        HeroCard(
            next = next,
            now = now,
            city = settings.city,
            locating = locating,
            onDetectLocation = onDetectLocation,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        )
        Spacer(Modifier.height(12.dp))
        // هيدر التذكيرات — مكان «مواقيت اليوم» المحذوفة (بطلب المالك alpha12)
        ReminderBadge()
        Spacer(Modifier.height(10.dp))
        PrayerTimesGrid(times = times, activeKey = next.key)
        Spacer(Modifier.height(10.dp))
        Text(
            text = "هذا التطبيق صدقة جارية على روح والدي أرجو منكم الدعاء له",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier
                .fillMaxWidth()
                .alpha(0.75f),
        )
        Spacer(Modifier.height(112.dp)) // مساحة شريط التنقل الزجاجي العائم — دائماً
    }
}

/**
 * تذكيرات الشريط — نصوص ثابتة موثقة التسند (قرآن وحديث صحيح)،
 * يُختار أحدها **عشوائياً مع كل فتح للتطبيق** (بطلب المالك alpha11):
 * تنبيه بأهمية الصلاة والأذكار وحفظ القرآن.
 * (alpha12: انتقل الشريط إلى مكان «مواقيت اليوم» فوق الشبكة بطلب المالك).
 */
private val HomeReminders = listOf(
    "«أحبّ الأعمال إلى الله الصلاة على وقتها» — متفق عليه",
    "«من صلّى البردين دخل الجنة» — متفق عليه",
    "﴿فاذكروني أذكركم﴾ — البقرة ١٥٢",
    "«خيركم من تعلّم القرآن وعلّمه» — رواه البخاري",
    "«لا يزال لسانك رطبًا بذكر الله» — رواه الترمذي",
    "«سبحان الله وبحمده، سبحان الله العظيم» — كلمتان خفيفتان على اللسان",
    "«من قرأ سورة الكهف يوم الجمعة أضاء له من النور ما بين الجمعتين» — رواه الحاكم",
    "«الصلوات الخمس والجمعة إلى الجمعة كفارةٌ لما بينهن» — رواه مسلم",
    "«الطُّهور شطر الإيمان» — رواه مسلم",
    "﴿إنّ الصلاة تنهى عن الفحشاء والمنكر﴾ — العنكبوت ٤٥",
    "«أكثر من السجود فإنك لا تسجد لله سجدة إلا رفعك الله بها درجة» — رواه مسلم",
    "«من صلّى عليّ صلاة صلّى الله عليه بها عشراً» — رواه مسلم",
)

/**
 * شريط التذكير المتغير — أيقونة التطبيق (الهلال الهندسي بلغة الهوية)
 * وبجانبه تذكير يتبدل مع كل فتح للتطبيق بدل اسم التطبيق (بطلب المالك alpha11)،
 * وأصبح من alpha12 **هيدر التذكيرات** فوق شبكة المواقيت مكان «مواقيت اليوم».
 */
@Composable
private fun ReminderBadge(modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val reminder = remember { HomeReminders.random() }
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.linearGradient(
                        listOf(scheme.primaryContainer, scheme.secondaryContainer),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                NoorIcons.Moon,
                contentDescription = null,
                tint = scheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.size(10.dp))
        Text(
            text = reminder,
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant,
            maxLines = 2,
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * مسار مزلاج المظهر — فجوة محجوزة في الهيدر يُوضع فوقها السائل من الجذر.
 * لا ترسم شيئاً هنا؛ موقعها فقط هو ما يهم (بإحداثيات الجذر).
 */
@Composable
private fun ThemeDialTrack(
    onPlaced: (Float, Float, Float, Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
            .onGloballyPositioned { coords ->
                val pos = coords.positionInRoot()
                onPlaced(pos.x, pos.y, coords.size.width.toFloat(), coords.size.height.toFloat())
            },
    )
}

@Composable
private fun HeroCard(
    next: com.sawan4art.noor.domain.prayer.NextPrayer,
    now: ZonedDateTime,
    city: String,
    locating: Boolean,
    onDetectLocation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val noorColors = LocalNoorColors.current
    val gold = MaterialTheme.colorScheme.secondary
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(32.dp))
            .background(noorColors.heroGradient)
            .padding(vertical = 20.dp, horizontal = 24.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize(),
        ) {
            Text(
                text = "الصلاة القادمة",
                style = MaterialTheme.typography.labelLarge,
                color = noorColors.heroMuted,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = next.arabicName,
                style = MaterialTheme.typography.displaySmall,
                color = noorColors.heroContent,
            )
            Text(
                text = TimeFormat.prayerTime(next.target),
                style = MaterialTheme.typography.titleMedium,
                color = noorColors.heroMuted,
            )
            // الحلقة تتكيف مع المساحة المتاحة، ونص العدّاد طبقة مستقلة لا ينكمش
            // أبداً — حتى على أقصر الشاشات يبقى العدّاد مقروءاً بلا تمرير
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                val side = minOf(maxHeight, maxWidth).coerceAtMost(176.dp)
                ProgressRing(
                    progressPercent = next.progressPercent,
                    ringColor = gold,
                    trackColor = Color.White.copy(alpha = 0.12f),
                    modifier = Modifier.size(side),
                )
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
            Spacer(Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HeroChip(text = Hijri.of(now.toLocalDate()).formatted)
                HeroChip(text = Hijri.gregorianArabic(now.toLocalDate()))
            }
            // صف الموقع — انتقل من البطاقة المستقلة إلى **داخل الهيدر المربع
            // تحت التواريخ مباشرة** (بطلب المالك alpha12)
            Spacer(Modifier.height(10.dp))
            HeroLocationRow(
                city = city,
                locating = locating,
                onDetectLocation = onDetectLocation,
            )
        }
    }
}

/**
 * صف الموقع داخل بطاقة الهيرو تحت التواريخ — دبوس المدينة واسمها وزر
 * التحديد التلقائي بألوان بطاقة الهيرو نفسها (بطلب المالك alpha12).
 */
@Composable
private fun HeroLocationRow(
    city: String,
    locating: Boolean,
    onDetectLocation: () -> Unit,
) {
    val noorColors = LocalNoorColors.current
    val gold = MaterialTheme.colorScheme.secondary
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            Icons.Filled.LocationOn,
            contentDescription = null,
            tint = noorColors.heroMuted,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = city,
            style = MaterialTheme.typography.labelLarge,
            color = noorColors.heroContent,
            maxLines = 1,
        )
        if (locating) {
            CircularProgressIndicator(
                modifier = Modifier.size(13.dp),
                strokeWidth = 2.dp,
                color = noorColors.heroContent,
            )
        }
        Spacer(Modifier.size(2.dp))
        Surface(
            onClick = onDetectLocation,
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.10f),
            modifier = Modifier.size(26.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Filled.LocationOn,
                    contentDescription = "تحديد الموقع تلقائياً",
                    tint = gold,
                    modifier = Modifier.size(14.dp),
                )
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
    modifier: Modifier = Modifier,
) {
    val target = (progressPercent.coerceIn(0, 100)) / 100f
    val animated by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = 700),
        label = "prayerProgress",
    )
    Canvas(modifier = modifier) {
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
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 8.dp),
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
