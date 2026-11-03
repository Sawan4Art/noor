package com.sawan4art.noor.ui.azkar

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sawan4art.noor.domain.azkar.AzkarGroup
import com.sawan4art.noor.domain.azkar.AzkarItem
import com.sawan4art.noor.domain.azkar.AzkarMath
import com.sawan4art.noor.domain.tasbih.MisbahaMath
import com.sawan4art.noor.ui.theme.NoorPageTitle

/**
 * شاشة مجموعة أذكار — القراءة بهوية التطبيق نفسها: كل ذكرٍ بطاقةٌ
 * بعدّاد رقمي (alpha28) — نقرة تُصاعد العدّ وبلوغ العدد المسنون
 * يُلون البطاقة مُتمّة كما كان التعليم، والعلامات والعدّادات تتجدد
 * كل يوم (AzkarMath)، ومقدّر التقدّم أعلى القائمة يظهر موضعك في المجموعة.
 */
@Composable
fun GroupScreen(
    viewModel: AzkarViewModel,
    group: AzkarGroup,
    onBack: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val state by viewModel.state.collectAsStateWithLifecycle()

    // فتح الشاشة يحدّث مفتاح اليوم — ليلةٌ مرّت والتطبيق مفتوح تعود العلامات صفراً
    LaunchedEffect(group.id) { viewModel.refreshDay() }

    val progress = AzkarMath.groupProgress(state.doneToday, group)

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
                // alpha39: العنوان داخل إطار NoorPageTitle (توجيه المالك:
                // «عايز كل عناوين الصفحات تكون داخل اطار متماشي مع تصميم التطبيق»)
                NoorPageTitle(text = group.title)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = group.subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(10.dp))

        // ===== مقدّر اليوم =====
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "اليوم: " + MisbahaMath.arabicNumber(progress.doneCount.toLong()) +
                    " من " + MisbahaMath.arabicNumber(progress.total.toLong()) +
                    if (progress.allDone) " — اكتملت الحمد لله" else "",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (progress.allDone) scheme.primary else scheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { progress.fraction.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
            color = scheme.primary,
            trackColor = scheme.surfaceVariant,
        )
        Spacer(Modifier.height(12.dp))

        // ===== بطاقات الأذكار — عدّاد رقمي في كل بطاقة (alpha28) =====
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f),
        ) {
            itemsIndexed(group.items, key = { _, item -> item.id }) { _, item ->
                AzkarRow(
                    item = item,
                    done = item.id in state.doneToday,
                    round = state.roundsToday[item.id] ?: 0,
                    onTap = { viewModel.tapCount(item.id, item.count) },
                )
            }
            item { Spacer(Modifier.height(100.dp)) } // مساحة شريط التنقل الزجاجي
        }
    }
}

/**
 * بطاقة ذكرٍ بعدّاد رقمي — توجيه المالك alpha28: «كروت الاذكار تكون
 * فيها عداد رقمي و عند الانتهاء من العدد المطلوب يتغيير لون الكارت
 * مثل الحالي». كل نقرة تُصاعد العدّ، وشريط رقيق يعرض موضعك من العدد،
 * وبلوغ العدد المسنون يُلون البطاقة بالحالة المُتمّة نفسها (فوق)،
 * ونقرة البطاقة المكتملة تفتح دورةً جديدة من الصفر.
 */
@Composable
private fun AzkarRow(
    item: AzkarItem,
    done: Boolean,
    round: Int,
    onTap: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    // المعروض في العدّاد: البطاقة المكتملة تظهر العدّ الكامل دائماً
    val shown = if (done) item.count else round.coerceIn(0, item.count)
    val fraction = if (item.count <= 0) 1f else shown.toFloat() / item.count

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (done) scheme.primaryContainer.copy(alpha = 0.55f) else scheme.surface,
        contentColor = if (done) scheme.onPrimaryContainer else scheme.onSurface,
        border = BorderStroke(
            width = if (done) 1.5.dp else 1.dp,
            color = if (done) scheme.primary else scheme.outline.copy(alpha = 0.3f),
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onTap),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // ===== العدّاد الرقمي — القراءة الحالية على العدد المسنون =====
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (done) scheme.primary else scheme.surfaceVariant,
                    contentColor = if (done) scheme.onPrimary else scheme.onSurfaceVariant,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        if (done) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = "تمّ",
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                        }
                        Text(
                            text = MisbahaMath.arabicNumber(shown.toLong()) + "/" +
                                MisbahaMath.arabicNumber(item.count.toLong()),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    text = item.source,
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.End,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = item.text,
                style = MaterialTheme.typography.bodyLarge,
                color = if (done) scheme.onPrimaryContainer.copy(alpha = 0.8f) else scheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(10.dp))
            // شريط العدّ الرقيق — موضعك من العدد المسنون بصرياً
            LinearProgressIndicator(
                progress = { fraction.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
                color = scheme.primary,
                trackColor = scheme.surfaceVariant,
            )
        }
    }
}
