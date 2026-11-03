package com.sawan4art.noor.ui.home

import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sawan4art.noor.domain.prayer.PrayerTimeKey
import com.sawan4art.noor.domain.prayer.PrayerMath
import com.sawan4art.noor.domain.prayer.PrayerTimes
import com.sawan4art.noor.ui.theme.DialTrackLayout
import com.sawan4art.noor.ui.theme.LocalNoorColors
import com.sawan4art.noor.ui.icons.NoorIcons
import com.sawan4art.noor.ui.nav.GooeyMath
import com.sawan4art.noor.ui.nav.addLiquidBlob
import com.sawan4art.noor.util.Hijri
import com.sawan4art.noor.util.TimeFormat
import java.time.Duration
import kotlin.math.cos
import kotlin.math.sin
import java.time.Instant
import java.time.LocalDate
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
    /** الثيم المثبّت المُعاشُ حالياً — قلبه يحرّك دوران جمل التذكير */
    isDark: Boolean,
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
            .padding(horizontal = 12.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        // مسار مزلاج الشمس/القمر — **الوحيد أعلى الشاشة** (بطلب المالك alpha12)
        // والسائل نفسه يُرسم فوق الكل من الجذر. درس alpha18: هذه **فجوة تخطيط
        // فقط** بلا أي قياس — القياس من مسار ثابت على النافذة في ThemeDialOverlay
        // (DialTrackLayout) كي لا يركب المزلاج انزلاق التبويب فيعلق في النص
        Spacer(
            Modifier
                .padding(top = DialTrackLayout.SelfTopPad)
                .fillMaxWidth()
                .height(DialTrackLayout.Height),
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
        Spacer(Modifier.height(8.dp))
        // هيدر التذكيرات — مكان «مواقيت اليوم» المحذوفة (بطلب المالك alpha12)
        // alpha24: يتبدل مع كل قلبٍ للثيم كذلك
        ReminderBadge(isDark = isDark)
        Spacer(Modifier.height(8.dp))
        PrayerTimesGrid(times = times, activeKey = next.key, today = today)
        Spacer(Modifier.height(8.dp))
        // سطر الصدقة الجارية — **جملة ثابتة غير متغيرة** بأمر المالك
        // alpha30: «التذكيرات التي في الاسفل نستبدلها بجمله ثابته غير
        // متغيره الجمله التاليه هي هذا العمل صدقة جارية لي و لأبي و أمي
        // نسألكم صالح الدعاء» — بلا دوران ولا تبديل مع قلب الثيم،
        // تظهر هذه الجملة وحدها دائماً
        Text(
            text = SADAQAH_LINE,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2,
            modifier = Modifier
                .fillMaxWidth()
                .alpha(0.75f),
        )
        Spacer(Modifier.height(112.dp)) // مساحة شريط التنقل الزجاجي العائم — دائماً
    }
}

// جمل التذكير الدوّارة (تجمع السطر السفلي وشريط الهيدر) في ملفٍ نقيّ
// يُختبر آلياً — ReminderRotation.kt (توجيه المالك alpha24)

/**
 * شريط التذكير المتغير — أيقونة التطبيق (الهلال الهندسي بلغة الهوية)
 * وبجانبه تذكير يتبدل مع كل فتح للتطبيق بدل اسم التطبيق (بطلب المالك alpha11)،
 * وأصبح من alpha12 **هيدر التذكيرات** فوق شبكة المواقيت مكان «مواقيت اليوم».
 *
 * **السائل الحي (alpha21 — طلب المالك: «عايز ايقونة الهلال بتاعت التذكيرات
 * تكون داخل سائل متحرك»)**: الصندوق المربع الساكن أُزيل، والهلال الآن يعوم
 * داخل قطرة سائلة حية بلغة مزلاج المظهر نفسها (GooeyMath.addLiquidBlob):
 * سطحها متموج دائماً (موجات الثبات الثلاث)، وتتنفس ببطء، ومركزها ينسيب
 * رأسياً — **والهلال يتبع الانسياب نفسه فيعوم داخل السائل لا فوقه**،
 * وقطرة صغيرة تدور ببطء حوله فتغمر في السطح وتبرز منه كماء حي.
 */
@Composable
private fun ReminderBadge(modifier: Modifier = Modifier, isDark: Boolean) {
    val scheme = MaterialTheme.colorScheme
    // عادة alpha11 محفوظة: بداية عشوائية مع كل فتحٍ للتطبيق،
    // ثم **دوران حتمي مع كل قلبٍ للثيم** (توجيه المالك alpha24)
    val reminderIndex = remember { mutableIntStateOf(HomeReminders.indices.random()) }
    var badgeSeenDark by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(isDark) {
        val seen = badgeSeenDark
        if (seen == null) {
            badgeSeenDark = isDark
        } else if (seen != isDark) {
            badgeSeenDark = isDark
            reminderIndex.intValue =
                ReminderRotation.nextIndex(reminderIndex.intValue, HomeReminders.size)
        }
    }
    val reminder = HomeReminders[reminderIndex.intValue]
    val density = LocalDensity.current
    // alpha43 (بلاغ المالك: «الخط و ايقونه السائل الخاصه بالوقت المتبقي
    // صغيره هي كمان») — قطرة التذكير والهلال داخله كبُرا: ١٩←٢٣ و٢٠←٢٦
    val blobR = with(density) { 23.dp.toPx() }
    val iconPx = with(density) { 26.dp.toPx() }
    val moonPainter = rememberVectorPainter(NoorIcons.Moon)

    // زمن حي لا يتوقف — السائل لا يسكن (نفس لغة مزلاج المظهر)
    var frameNanos by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            withInfiniteAnimationFrameNanos { frameNanos = it }
        }
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(with(density) { (blobR * 2.4f).toDp() })
                .drawBehind {
                    val t = frameNanos / 1_000_000_000f
                    val cx = size.width / 2f
                    // الانسياب الرأسي نفسه الذي يعوم به سائل المزلاج —
                    // الهلال يتبعه فلا يثبت فوق السائل بل يعوم داخله
                    val cy = size.height / 2f + blobR * GooeyMath.centerDrift(t, 0.7f)
                    // تنفس بطيء للقطرة كاملة
                    val breathe = 1f + 0.045f * sin(t * 0.9f)
                    val liquid = Path()
                    addLiquidBlob(
                        liquid, cx, cy,
                        blobR * breathe, t, energy = 0f, phase = 0.7f,
                    )
                    // قطرة صغيرة تدور ببطء حول السائل — تغمر في السطح
                    // وتبرز منه (المسار المفلطح يُغرقها أعلى وأسفل)
                    val orbAngle = t * 0.55f
                    val orbDist = blobR * (1.18f + 0.06f * sin(t * 0.8f + 1.1f))
                    val orbX = cx + orbDist * cos(orbAngle)
                    val orbY = cy + orbDist * sin(orbAngle) * 0.72f
                    addLiquidBlob(
                        liquid, orbX, orbY,
                        blobR * 0.16f, t, energy = 0f, phase = 2.1f,
                    )
                    drawPath(
                        liquid,
                        Brush.verticalGradient(
                            listOf(scheme.primaryContainer, scheme.secondaryContainer),
                            startY = cy - blobR * 1.35f,
                            endY = cy + blobR * 1.35f,
                        ),
                    )
                    // الهلال يعوم داخل السائل — بمركز منساب مطابق للقطرة
                    translate(cx - iconPx / 2f, cy - iconPx / 2f) {
                        with(moonPainter) {
                            draw(
                                Size(iconPx, iconPx),
                                colorFilter = ColorFilter.tint(scheme.primary),
                            )
                        }
                    }
                },
        )
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
            .padding(vertical = 22.dp, horizontal = 18.dp),
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
            // الحلقة السائلة تتكيف مع المساحة المتاحة — والعدّاد نفسه
            // يعوم داخل النقطة السائلة فلا ينكمش ولا يخرج عن القطرة أبداً
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                val side = minOf(maxHeight, maxWidth).coerceAtMost(300.dp)
                // alpha44 — سقف الحلقة ٢١٦←٣٠٠dp: النقطة تملأ الهيدر المستطيل
                // (بلاغ المالك: «كبر النقطه خمس اضعاف … هديك صوره لحجم النقطه
                // السائله اللي انا عايزها»)
                // نسبة التقدم اللحظية — الماضي من الفترة ÷ كاملها بقيمة
                // متصلة (لا أعداد صحيحة) كي يتحرك السائل مع كل ثانية:
                // «اجعله يتقدم مع تقدم انتهاء الوقت» — يكتمل عند الأذان
                val windowSec = Duration.between(next.previous, next.target).seconds.coerceAtLeast(1)
                val progressFraction =
                    (windowSec - next.remainingSeconds).coerceIn(0, windowSec).toFloat() / windowSec
                LiquidCountdown(
                    progressFraction = progressFraction,
                    remainingText = TimeFormat.remainingArabic(next.remainingSeconds),
                    sideDp = side,
                    modifier = Modifier.size(side),
                )
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
 * صف الموقع داخل بطاقة الهيرو تحت التواريخ — زر التحديد التلقائي بألوان
 * بطاقة الهيرو نفسها. alpha18: دبوس الخرائط الذي كان يمين اسم الموقع أُزيل
 * (توجيه المالك: «على يمين اسم الموقع في ايقونة الخرائط شيلها») — يكفي زر
 * التحديد جهة اليسار.
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
        modifier = Modifier.fillMaxWidth(),
    ) {
        // السطر الطويل (قرية، مدينة، محافظة) يُقتطع بأمان ولا يزاح زر التحديد
        Text(
            text = city,
            style = MaterialTheme.typography.labelLarge,
            color = noorColors.heroContent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (locating) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = noorColors.heroContent,
            )
        }
        Spacer(Modifier.size(2.dp))
        Surface(
            onClick = onDetectLocation,
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.10f),
            modifier = Modifier.size(30.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Filled.LocationOn,
                    contentDescription = "تحديد الموقع تلقائياً",
                    tint = gold,
                    modifier = Modifier.size(16.dp),
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

/**
 * **مؤقت الصلاة السائل الحي** — طلب المالك alpha31:
 * **alpha44 (بلاغ المالك): «شيل الشريط الملفوف حول النقطه السائله التي
 * تعرض الوقت المتبقي و كبر النقطه خمس اضعاف … بدل الشريط الملتف حول
 * نقطه المتبقي هنخليه شريط عادي ملتف و الوانه متدرجه بنفس الوان النقطه»**:
 * الشريط أصبح حلقةً عادية ملساء (بلا تموج ولا رقبة اتصال) بتدرّج
 * ألوان النقطة نفسها، والنقطة السائلة كبرت لتملأ الحلقة كلها —
 * «الشريط الذي يحيط ب الوقت المتبقي اجعله يتقدم مع تقدم انتهاء الوقت
 * و اجعل الوقت المتبقي داخل سائل حي و يلتف حوله الشريط الدائري الاسبق»
 * و«ممكن نعمله شريط دائري عباره عن سائل حي برضو».
 *
 * التركيب من الداخل للخارج:
 * ١. **النقطة السائلة الحية** في المركز — بلغة المسبحة والقبلة نفسها
 *    (GooeyMath.addLiquidBlob): سطح متموج وتنفس وانسياب رأسي دائم،
 *    والعدّاد التنازلي «٠٣:٢٤:١٥» وتحته «المتبقي» **يعومان داخلها**
 *    يتبعان الانسياب نفسه فلا يثبتان فوق السائل.
 * ٢. **الحلقة السائلة** حوله — الشريط الدائري نفسه ماءً حياً:
 *    أنبوب حافتاه الخارجية والداخلية متموجتان دائماً (بطور معاكس
 *    كي يتنفس الماء بينهما)، ويمتلئ موضعاً موضعاً كالزئبق **مع كل
 *    ثانية** من عمر الفترة حتى يكتمل اللوب لحظة الأذان، وعلى رأس
 *    السائل قطرة قائدة تسبق الامتلاء، وأخرى واقفة عند نقطة البداية
 *    كي يبقى الأنبوب حياً حتى وهو فارغ.
 * ٣. **الشريط جزء من النقطة نفسها** (توجيه المالك alpha32: «سائل حي
 *    محيط بالنقطه الكبيره و يكون خارج من النقطه الكبيره من نقطه
 *    البدايه و عند انتهاء الشريط المحيط يتم التصاقه بالنقطه
 *    الرئيسيه ثم يبدا الانفصال مره اخرى») — **رقبة اتصال** سائلة
 *    عند نقطة البداية (الأعلى) تلحم النقطة بالأنبوب فالسائل يخرج
 *    منها يلتفّ حولها، وعند اكتمال اللوب يعلق الرأس عند نقطة البداية
 *    نفسها **فيلتصق الشريط كله بالنقطة** بدفعة وصول تتنفّس
 *    (CountdownLiquidMath.attachSurge)، ثم تُبتدأ فترة جديدة فينفصل
 *    السائل ويخرج من جديد — دورة خروجٍ والتصاقٍ لا تنقطع.
 *
 * الحلقة الخافتة الكاملة (الأنبوب الفارغ) تُرسم أولاً ثم السائل
 * الممتلئ فوقها — فتُرى نسبة الباقي كزجاج ماء شفيف.
 * الرياضيات كلها نقية في [CountdownLiquidMath] بعقود اختبار.
 */
@Composable
private fun LiquidCountdown(
    progressFraction: Float,
    remainingText: String,
    sideDp: Dp,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val textMeasurer = rememberTextMeasurer()
    // alpha41: مقياس التطبيق الثابت لا يدخل حلقة العدّاد — رياضيات
    // CountdownLiquidMath تضبط حجم النص على قطرة الحلقة بوحدات الكثافة
    // الخالصة (sp = px دون fontScale) بمقاساتها الصريحة المُكبّرة
    // (alpha43: نسبة ٠.٣٦ وسقف ٣٠sp)، فالقياس بكثافة بمقياس ١
    val baseDensity = LocalDensity.current
    val fitDensity = remember(baseDensity) { Density(baseDensity.density, fontScale = 1f) }

    // أبعاد الطبقات بالـ dp — تُحسب مرة واحدة لكل حجم شاشة.
    // alpha44: النقطة بطل الصفحة — الشريط عاديٌ رقيق يلاصق حافتها
    // الخارجية، والنقطة تملأ الدائرة بامتدادها الأقصى المحسوب
    val strokeDp = 7f
    val edgeGapDp = 2f
    val trackDp = CountdownLiquidMath.trackRadius(sideDp.value, strokeDp / 2f + edgeGapDp)
    val ringInnerDp = trackDp - strokeDp / 2f
    val dropDp = CountdownLiquidMath.dropRadius(ringInnerDp, 0f) /
        CountdownLiquidMath.MAX_BLOB_EXTENT
    val fontSizeSp = CountdownLiquidMath.countdownFontSizeSp(dropDp)

    // تخطيط النصوص يُقاس مرة واحدة فقط — لا قياس في كل إطار
    val remainingLayout = remember(remainingText, fontSizeSp, textMeasurer, fitDensity) {
        textMeasurer.measure(
            AnnotatedString(remainingText),
            TextStyle(fontSize = fontSizeSp.sp, fontWeight = FontWeight.Black),
            density = fitDensity,
        )
    }
    val labelLayout = remember(fontSizeSp, textMeasurer, fitDensity) {
        textMeasurer.measure(
            AnnotatedString("المتبقي"),
            TextStyle(fontSize = (fontSizeSp * 0.56f).sp, fontWeight = FontWeight.Bold),
            density = fitDensity,
        )
    }

    // زمن حي لا يتوقف — السائل لا يسكن (نفس لغة المسبحة والقبلة)
    var frameNanos by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            withInfiniteAnimationFrameNanos { frameNanos = it }
        }
    }

    val animated by animateFloatAsState(
        targetValue = progressFraction,
        animationSpec = tween(durationMillis = 600),
        label = "prayerLiquidFill",
    )

    Canvas(modifier = modifier) {
        val t = frameNanos / 1_000_000_000f
        val side = size.minDimension
        val cx = size.width / 2f
        val cy = size.height / 2f

        val stroke = strokeDp.dp.toPx()
        val trackR = CountdownLiquidMath.trackRadius(side, stroke / 2f + edgeGapDp.dp.toPx())
        val dropR = CountdownLiquidMath.dropRadius(trackR - stroke / 2f, 0f) /
            CountdownLiquidMath.MAX_BLOB_EXTENT

        // التنفس والانسياب — لغة السائل الحي نفسها في كل أقسام التطبيق
        val breathe = 1f + CountdownLiquidMath.BREATH_AMPLITUDE * sin(t * 0.85f + 0.9f)
        val drift = GooeyMath.centerDrift(t, 0.9f) * 0.8f
        val dropCy = cy + dropR * drift

        // ١) الشريط العادي الملتف — حلقة ملساء بلا تموّج، تدرّجها ألوان
        //    النقطة نفسها (بلاغ المالك alpha44) — والمسار الفارغ خافتٌ
        //    كزجاج ماء شفيف
        drawCircle(
            color = Color.White.copy(alpha = 0.10f),
            radius = trackR,
            center = Offset(cx, cy),
            style = Stroke(width = stroke),
        )

        // ٢) امتلاء الشريط مع الزمن — يتقدم مع كل ثانية من عمر الفترة
        //    (عقد alpha31 باقٍ: «اجعله يتقدم مع تقدم انتهاء الوقت»)
        val sweep = CountdownLiquidMath.sweepDegrees(animated)
        if (sweep > 0.5f) {
            drawArc(
                brush = Brush.sweepGradient(
                    0f to scheme.primaryContainer,
                    0.5f to scheme.secondaryContainer,
                    1f to scheme.primaryContainer,
                ),
                startAngle = -90f,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = Offset(cx - trackR, cy - trackR),
                size = Size(trackR * 2f, trackR * 2f),
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }

        // ٣) النقطة السائلة الحية — تملأ الحلقة تقريباً (كبر النقطة خمس
        //    أضعافٍ تقريباً بلاغ المالك alpha44): سطحها متموج دائماً بلغة
        //    المسبحة، وامتدادها الأقصى محسوب بـ MAX_BLOB_EXTENT فلا يلمس
        //    موجهاً الشريط العادي الملتف حولها أبداً
        val drop = Path()
        addLiquidBlob(drop, cx, dropCy, dropR * breathe, t, 0f, 1.3f)
        drawPath(
            drop,
            Brush.verticalGradient(
                listOf(scheme.primaryContainer, scheme.secondaryContainer),
                startY = dropCy - dropR * 1.45f,
                endY = dropCy + dropR * 1.2f,
            ),
        )

        // ٤) العدّاد «٠٣:٢٤:١٥» وتحته «المتبقي» — يتبعان الانسياب نفسه
        drawText(
            remainingLayout,
            color = scheme.onPrimaryContainer,
            topLeft = Offset(
                cx - remainingLayout.size.width / 2f,
                dropCy - dropR * 0.10f - remainingLayout.size.height / 2f,
            ),
        )
        drawText(
            labelLayout,
            color = scheme.onPrimaryContainer.copy(alpha = 0.65f),
            topLeft = Offset(
                cx - labelLayout.size.width / 2f,
                dropCy + dropR * 0.34f - labelLayout.size.height / 2f,
            ),
        )
    }
}

@Composable
private fun PrayerTimesGrid(times: PrayerTimes, activeKey: PrayerTimeKey, today: LocalDate) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PrayerTimeKey.entries.toList().chunked(3).forEach { rowKeys ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowKeys.forEach { key ->
                    PrayerCard(
                        // يوم الجمعة: صلاة الظهر تُسمّى الجمعة (توجيه المالك alpha30)
                        name = PrayerMath.displayName(key, today),
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
            modifier = Modifier.padding(vertical = 11.dp, horizontal = 8.dp),
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
