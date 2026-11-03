package com.sawan4art.noor.ui.azkar

import android.content.Intent
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sawan4art.noor.domain.azkar.AzkarGroup
import com.sawan4art.noor.domain.azkar.AzkarItem
import com.sawan4art.noor.domain.azkar.AzkarLibrary
import com.sawan4art.noor.domain.azkar.AzkarMath
import com.sawan4art.noor.domain.tasbih.MisbahaMath
import com.sawan4art.noor.ui.theme.NoorPageTitle

/** مدى تكبير خط بطاقة الذكر — أرضية تحمي ضعاف البصر وسقفٌ يبقي النص في الكارت */
private const val FONT_SCALE_MIN = 0.85f
private const val FONT_SCALE_MAX = 2f
private const val FONT_SCALE_STEP = 0.15f

/**
 * شاشة مجموعة أذكار — القراءة بهوية التطبيق نفسها: كل ذكرٍ بطاقةٌ
 * بعدّاد رقمي (alpha28) — نقرة تُصاعد العدّ وبلوغ العدد المسنون
 * يُلون البطاقة مُتمّة كما كان التعليم، والعلامات والعدّادات تتجدد
 * كل يوم (AzkarMath)، ومقدّر التقدّم أعلى القائمة يظهر موضعك في المجموعة.
 *
 * **مكتبة حيّة (alpha44 — بلاغ المالك: «اضف زر في قسم الاذكار يمكن
 * المستخدم من انشاء ذكر اضافي و او حذف اذكار موجوده بالفعل مع طبعا
 * امكانيه استرجاع الاذكار الافتراضيه»)**:
 * - زر «+» في رأس الصفحة يفتح حوار إنشاء ذكرٍ جديد في هذا القسم.
 * - كل بطاقة فيها زر حذف — المضاف يُمحى نهائياً والافتراضي يُسجَّل
 *   محذوفاً فقط، فيظهر شريط الاسترجاع فوق القائمة فيعيد الافتراضي.
 * - المحتوى المعروض ومقدّر التقدم كلاهما من المحتوى الفعلي بعد
 *   الحذف والإضافة (AzkarLibrary.effectiveGroup).
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

    val effective = AzkarLibrary.effectiveGroup(group, state.customs, state.deleted)
    val progress = AzkarMath.groupProgress(state.doneToday, effective)

    var showAddDialog by rememberSaveable { mutableStateOf(false) }

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
            Column(Modifier.weight(1f)) {
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
            // زر إنشاء ذكرٍ إضافي في هذا القسم (alpha44)
            IconButton(onClick = { showAddDialog = true }) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = "إضافة ذكر جديد",
                    tint = scheme.primary,
                    modifier = Modifier.size(28.dp),
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

        // ===== شريط استرجاع الافتراضي — يظهر ما دام في حذفٌ سابق =====
        if (state.deleted.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = scheme.secondaryContainer,
                contentColor = scheme.onSecondaryContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = viewModel::restoreDefaults),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "استرجاع الأذكار الافتراضية المحذوفة (" +
                            MisbahaMath.arabicNumber(state.deleted.size.toLong()) + ")",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        // ===== بطاقات الأذكار — عدّاد رقمي في كل بطاقة (alpha28) =====
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f),
        ) {
            itemsIndexed(effective.items, key = { _, item -> item.id }) { _, item ->
                AzkarRow(
                    item = item,
                    done = item.id in state.doneToday,
                    round = state.roundsToday[item.id] ?: 0,
                    isCustom = AzkarLibrary.isCustom(item.id),
                    onTap = { viewModel.tapCount(item.id, item.count) },
                    onDelete = { viewModel.deleteItem(item.id) },
                )
            }
            item { Spacer(Modifier.height(100.dp)) } // مساحة شريط التنقل الزجاجي
        }
    }

    if (showAddDialog) {
        AddDhikrDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { text, count, source ->
                val ok = viewModel.addCustom(group.id, text, count, source)
                if (ok) showAddDialog = false
            },
        )
    }
}

/** حوار إنشاء ذكرٍ جديد — نصٌ وعددٌ ومصدرٌ اختياري (alpha44) */
@Composable
private fun AddDhikrDialog(
    onDismiss: () -> Unit,
    onConfirm: (text: String, count: Int, source: String) -> Unit,
) {
    var text by rememberSaveable { mutableStateOf("") }
    var countText by rememberSaveable { mutableStateOf("1") }
    var source by rememberSaveable { mutableStateOf("") }
    val count = countText.toIntOrNull() ?: 0
    val valid = text.trim().isNotEmpty() && count >= 1

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "ذكر جديد",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("نص الذكر") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = countText,
                    onValueChange = { v -> countText = v.filter { it.isDigit() }.take(4) },
                    label = { Text("العدد المسنون") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = source,
                    onValueChange = { source = it },
                    label = { Text("المصدر (اختياري)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text, count, source) },
                enabled = valid,
            ) {
                Text("إضافة", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        },
    )
}

/**
 * بطاقة ذكرٍ بعدّاد رقمي — توجيه المالك alpha28: «كروت الاذكار تكون
 * فيها عداد رقمي و عند الانتهاء من العدد المطلوب يتغيير لون الكارت
 * مثل الحالي». كل نقرة تُصاعد العدّ، وشريط رقيق يعرض موضعك من العدد،
 * وبلوغ العدد المسنون يُلون البطاقة بالحالة المُتمّة نفسها (فوق)،
 * ونقرة البطاقة المكتملة تفتح دورةً جديدة من الصفر.
 *
 * **صف الأدوات (alpha44)**: تكبير وتصغير الخط لبطاقة الذكر نفسها،
 * ومشاركة نصّ الذكر بمصدره، وحذف البطاقة — المضاف نهائياً والافتراضي
 * قابلاً للاسترجاع من شريط أعلى القائمة.
 */
@Composable
private fun AzkarRow(
    item: AzkarItem,
    done: Boolean,
    round: Int,
    isCustom: Boolean,
    onTap: () -> Unit,
    onDelete: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    // المعروض في العدّاد: البطاقة المكتملة تظهر العدّ الكامل دائماً
    val shown = if (done) item.count else round.coerceIn(0, item.count)
    val fraction = if (item.count <= 0) 1f else shown.toFloat() / item.count

    // حجم خط هذه البطاقة — يتذكره التطبيق أثناء التنقل (alpha44)
    var fontScale by rememberSaveable { mutableFloatStateOf(1f) }

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
                                modifier = Modifier.size(16.dp),
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
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = (MaterialTheme.typography.bodyLarge.fontSize.value * fontScale).sp,
                ),
                color = if (done) scheme.onPrimaryContainer.copy(alpha = 0.8f) else scheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(10.dp))
            // ===== صف أدوات البطاقة (alpha44) =====
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        fontScale = (fontScale - FONT_SCALE_STEP).coerceAtLeast(FONT_SCALE_MIN)
                    },
                    modifier = Modifier.size(36.dp),
                ) {
                    Text(
                        text = "أ−",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = scheme.onSurfaceVariant,
                    )
                }
                IconButton(
                    onClick = {
                        fontScale = (fontScale + FONT_SCALE_STEP).coerceAtMost(FONT_SCALE_MAX)
                    },
                    modifier = Modifier.size(36.dp),
                ) {
                    Text(
                        text = "أ+",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = scheme.onSurfaceVariant,
                    )
                }
                IconButton(
                    onClick = {
                        val shareText = buildString {
                            append('«').append(item.text).append('»')
                            append("\n\n")
                            append(item.source)
                            append("\n\n")
                            append("من تطبيق نور المسلم")
                        }
                        runCatching {
                            context.startActivity(
                                Intent.createChooser(
                                    Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                    },
                                    "مشاركة الذكر",
                                ),
                            )
                        }
                    },
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        Icons.Filled.Share,
                        contentDescription = "مشاركة الذكر",
                        tint = scheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = if (isCustom) {
                            "حذف الذكر نهائياً"
                        } else {
                            "حذف الذكر — يمكن استرجاعه لاحقاً"
                        },
                        tint = scheme.onSurfaceVariant.copy(alpha = 0.75f),
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(Modifier.weight(1f))
                if (isCustom) {
                    // شارة «مضاف» — تمييز ذكر المستخدم عن الافتراضي
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = scheme.secondaryContainer,
                        contentColor = scheme.onSecondaryContainer,
                    ) {
                        Text(
                            text = "مضاف",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
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
