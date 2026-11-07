package com.sawan4art.noor.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.sawan4art.noor.data.mushaf.MushafDb
import com.sawan4art.noor.data.mushaf.QuranAudioLibrary
import com.sawan4art.noor.data.mushaf.QuranDownloadService
import com.sawan4art.noor.domain.mushaf.MushafSurahs
import com.sawan4art.noor.domain.mushaf.QuranReciters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * قسم «أصوات الشيخ» في الإعدادات — توجيه المالك ٢٠٢٦-١١-٠٦:
 * «اضف عشرين مقرء يتم تحميل اصواتهم من قبل المستخدم نفسه … مع امكانيه
 * تحميل كل سوره على حده او تحميل المصحف كامل لصوت الشيخ المحدد وذالك
 * سيكون في قسم جديد في صفحه الاعدادات خاص باصوات الشيوخ».
 *
 * عشرون شيخاً: العشرة المصريون أولاً ثم العشرة من غيرهم — ضغطة على
 * الشيخ تجعله صوت التلاوة في المصحف، وزر التنزيل يفتح مديرَ صوته:
 * سورةً سورة أو المصحف كاملاً، مع تقدّمٍ حيٍّ وإيقافٍ ومسح، وعدّاد
 * الملفات الحقيقي من القرص هو مصدر العرض.
 */
@Composable
fun QuranVoicesSection(
    reciterId: String,
    onSetReciter: (String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // عدّ آيات السور من قاعدة المصحف — يُقرأ مرةً عند فتح القسم
    var surahCounts by remember { mutableStateOf<Map<Int, Int>?>(null) }
    LaunchedEffect(Unit) {
        surahCounts = withContext(Dispatchers.IO) {
            (1..114).associateWith { MushafDb.surahAyahCount(context, it) }
        }
    }

    // حالة التنزيلات الحية من المكتبة + نبض قرصٍ يُحدّث عدّادات الملفات
    val downloads by QuranAudioLibrary.downloads.collectAsState()
    var diskTick by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    LaunchedEffect(downloads) {
        // كل تغيّر في التنزيل يعيد قراءة القرص فتُحدَّث العدّادات
        withContext(Dispatchers.IO) { QuranAudioLibrary.totalDownloadedCount(context, reciterId) }
        diskTick++
    }

    // الشيخ الذي فُتح مدير صوته
    var managerFor by remember { mutableStateOf<QuranReciters.Reciter?>(null) }

    val accent = MaterialTheme.colorScheme.primary

    Column {
        Text(
            text = "عشرون شيخاً — العشرة المصريون ثم العشرة من غيرهم. " +
                "اضغط على الشيخ ليصير صوتَ التلاوة في المصحف، وزر التنزيل " +
                "يحفظ صوته في الجهاز: سورةً سورة أو المصحف كاملاً — والتلاوة " +
                "من المحمَّل على جهازك فقط فحمّل السورة قبل الاستماع إليها.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 14.dp),
        )

        val counts = surahCounts
        if (counts == null) {
            Box(
                modifier = Modifier.fillMaxWidth().height(60.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "…",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            ReciterGroup(
                title = "المصريون — العشرة",
                reciters = QuranReciters.ALL.filter { it.origin == QuranReciters.Origin.EGYPTIAN },
                reciterId = reciterId,
                counts = counts,
                tick = diskTick,
                onSelect = onSetReciter,
                onManage = { managerFor = it },
            )
            Spacer(Modifier.height(14.dp))
            ReciterGroup(
                title = "من غير المصريين — العشرة",
                reciters = QuranReciters.ALL.filter { it.origin == QuranReciters.Origin.OTHER },
                reciterId = reciterId,
                counts = counts,
                tick = diskTick,
                onSelect = onSetReciter,
                onManage = { managerFor = it },
            )
        }
    }

    managerFor?.let { reciter ->
        val countsNow = surahCounts ?: emptyMap()
        ReciterDownloadDialog(
            reciter = reciter,
            surahCounts = countsNow,
            onDismiss = { managerFor = null },
        )
    }
}

/** مجموعة شيوخ — عنوان فرعي ثم صفوفهم */
@Composable
private fun ReciterGroup(
    title: String,
    reciters: List<QuranReciters.Reciter>,
    reciterId: String,
    counts: Map<Int, Int>,
    tick: Int,
    onSelect: (String) -> Unit,
    onManage: (QuranReciters.Reciter) -> Unit,
) {
    val context = LocalContext.current
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 6.dp),
    )
    reciters.forEach { reciter ->
        val selected = reciter.id == reciterId
        val downloaded = remember(reciter.id, tick) {
            QuranAudioLibrary.totalDownloadedCount(context, reciter.id)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .clickable { onSelect(reciter.id) }
                .padding(horizontal = 8.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // دائرة الاختيار — مملوءة لمن يُتلى به الآن
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(
                        if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) {
                    Text(
                        text = "✓",
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
                Text(
                    text = reciter.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = reciter.country +
                        if (downloaded > 0) " · محمَّل: ${MushafSurahs.arabicDigits(downloaded)} آية"
                        else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                onClick = { onManage(reciter) },
            ) {
                Text(
                    text = "تنزيل",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                )
            }
        }
    }
}

/**
 * مدير صوت الشيخ — تنزيل سورة أو المصحف كاملاً مع تقدّمٍ حي، وإيقاف
 * ومسح لكل سورة على حدة.
 */
@Composable
private fun ReciterDownloadDialog(
    reciter: QuranReciters.Reciter,
    surahCounts: Map<Int, Int>,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val downloads by QuranAudioLibrary.downloads.collectAsState()
    var tick by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    LaunchedEffect(downloads) {
        withContext(Dispatchers.IO) { QuranAudioLibrary.totalDownloadedCount(context, reciter.id) }
        tick++
    }

    val accent = MaterialTheme.colorScheme.primary
    val whole = downloads["${reciter.id}:0"]

    fun launchDownload(sura: Int) {
        // التنزيل تحت الخدمة الأمامية (alpha57) — العملية تبقى حية ولو
        // أُغلق التطبيق كله من الريسنتس، والإشعار الحي بنسبة التقدم معه
        QuranDownloadService.start(context, reciter.id, sura, surahCounts)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 580.dp),
        ) {
            Column(Modifier.padding(16.dp)) {
                // الترويسة والإكس أعلى يمينها
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.14f))
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "إغلاق",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    Column(Modifier.weight(1f).padding(start = 10.dp)) {
                        Text(
                            text = "صوت الشيخ",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "${reciter.name} · ${reciter.country}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // شريط المصحف كاملاً — تنزيل واحد متتابع لكل السور
                val totalAll = surahCounts.values.sum()
                val downloadedAll = remember(reciter.id, tick) {
                    QuranAudioLibrary.totalDownloadedCount(context, reciter.id)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(accent.copy(alpha = 0.10f))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "تحميل المصحف كاملاً بصوته",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "المحمَّل: ${MushafSurahs.arabicDigits(downloadedAll)} من ${MushafSurahs.arabicDigits(totalAll)} آية" +
                                if (whole?.active == true) " — ${MushafSurahs.arabicDigits((whole.fraction * 100).toInt())}٪ جارٍ"
                                else "",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (whole?.active == true) {
                            LinearProgressIndicator(
                                progress = { whole.fraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp),
                                color = accent,
                                trackColor = accent.copy(alpha = 0.15f),
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (whole?.active == true) {
                            MaterialTheme.colorScheme.error.copy(alpha = 0.14f)
                        } else {
                            accent
                        },
                        onClick = {
                            if (whole?.active == true) {
                                QuranAudioLibrary.cancel(reciter.id, 0)
                            } else {
                                launchDownload(0)
                            }
                        },
                        modifier = Modifier.padding(start = 8.dp),
                    ) {
                        Text(
                            text = if (whole?.active == true) "إيقاف" else "تحميل",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (whole?.active == true) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onPrimary
                            },
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        )
                    }
                }
                if (whole?.error?.isNotEmpty() == true) {
                    Text(
                        text = whole.error,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    text = "أو سورة على حدة",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp),
                )

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    items((1..114).toList()) { sura ->
                        val count = surahCounts[sura] ?: 0
                        val downloaded = remember(reciter.id, sura, tick) {
                            QuranAudioLibrary.surahDownloadedCount(context, reciter.id, sura, count)
                        }
                        val state = downloads["${reciter.id}:$sura"]
                        val complete = count > 0 && downloaded >= count
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (complete) accent.copy(alpha = 0.08f)
                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.06f),
                                )
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(accent.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = MushafSurahs.arabicDigits(sura),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = accent,
                                )
                            }
                            Column(Modifier.weight(1f).padding(start = 8.dp)) {
                                Text(
                                    text = MushafSurahs.name(sura),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = when {
                                        state?.active == true ->
                                            "جارٍ: ${MushafSurahs.arabicDigits(state.done)}/${MushafSurahs.arabicDigits(state.total)}"
                                        complete -> "محمَّلة كاملة ✓"
                                        downloaded > 0 -> "ناقصة: ${MushafSurahs.arabicDigits(downloaded)}/${MushafSurahs.arabicDigits(count)}"
                                        else -> "${MushafSurahs.arabicDigits(count)} آية"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            val action = when {
                                state?.active == true -> "إيقاف" to {
                                    QuranAudioLibrary.cancel(reciter.id, sura)
                                }
                                complete -> "حذف" to {
                                    QuranAudioLibrary.deleteSurah(context, reciter.id, sura, count)
                                }
                                else -> "تحميل" to { launchDownload(sura) }
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = when {
                                    state?.active == true -> MaterialTheme.colorScheme.error.copy(alpha = 0.14f)
                                    complete -> MaterialTheme.colorScheme.outline.copy(alpha = 0.14f)
                                    else -> accent
                                },
                                onClick = action.second,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (state?.active != true && !complete) {
                                        Icon(
                                            imageVector = Icons.Filled.PlayArrow,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier
                                                .padding(start = 8.dp)
                                                .size(14.dp),
                                        )
                                    }
                                    Text(
                                        text = action.first,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = when {
                                            state?.active == true -> MaterialTheme.colorScheme.error
                                            complete -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                            else -> MaterialTheme.colorScheme.onPrimary
                                        },
                                        modifier = Modifier.padding(
                                            horizontal = if (state?.active == true || complete) 12.dp else 6.dp,
                                            vertical = 6.dp,
                                        ),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
