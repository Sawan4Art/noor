package com.sawan4art.noor.ui.quran

import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sawan4art.noor.R
import com.sawan4art.noor.data.quran.MushafRepository
import com.sawan4art.noor.domain.quran.MushafDoc
import com.sawan4art.noor.domain.quran.MushafLine
import com.sawan4art.noor.domain.quran.MushafMath
import com.sawan4art.noor.domain.quran.MushafSurah
import com.sawan4art.noor.domain.quran.MushafWord
import com.sawan4art.noor.ui.icons.NoorIcons
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * المصحف — طبعة المدينة بتلوين التجويد (توجيه المالك 2026-11-06:
 * «ابدا شغل في المصحف و هديك صوره لطبعه المصحف عايز زيها»):
 *
 * - **الصفحات**: ٦٠٤ صفحة بأسطرها المطبوعة (١٥ سطراً في الصفحة
 *   الكاملة) بخط مجمع الملك فهد العثماني، والسطر يتمدد عرض الصفحة
 *   كالطبعة تماماً — والصفحة التالية بالسحب.
 * - **التلوين**: قواعد التجويد ملوّنة على الحروف نفسها بدليل ألوان
 *   ثابت أسفل الشاشة (المد أحمر، الغُنّة أخضر، القلقلة برتقالي…).
 * - **الآية المنبثقة**: لمس أي كلمة تُبرز آيتها وتفتح صف الأزرار
 *   البرتقالية: التلاوة • المشاركة • النسخ • العلامة المرجعية • الإغلاق.
 * - **التنقل**: من عنوان السورة قائمة السور كلها، ومقاس النص بأزرار
 *   التصغير والتكبير — وآخر صفحة تُتذكر تلقائياً.
 */

/** خط المصحف العثماني — خط مجمع الملك فهد لطباعة المصحف */
private val MushafFont = FontFamily(Font(R.font.uthmanic_hafs))

/** ألوان قواعد التجويد — بنمط مصحف التجويد المطبوع، ودليلها أسفل الشاشة */
object TajweedPalette {
    data class Group(val rules: Set<String>, val color: Color, val label: String)

    val groups: List<Group> = listOf(
        Group(
            setOf("madd_2", "madd_246", "madd_6", "madd_muttasil", "madd_munfasil"),
            Color(0xFFD32F2F),
            "المد",
        ),
        Group(
            setOf("ghunnah", "ikhfa", "ikhfa_shafawi", "iqlab", "idghaam_ghunnah", "idghaam_shafawi"),
            Color(0xFF2E7D32),
            "الغُنّة",
        ),
        Group(setOf("qalqalah"), Color(0xFFEF6C00), "القلقلة"),
        Group(setOf("lam_shamsiyyah"), Color(0xFF0288D1), "اللام الشمسية"),
        Group(
            setOf("idghaam_no_ghunnah", "idghaam_mutajanisayn", "idghaam_mutaqaribayn"),
            Color(0xFF6A1B9A),
            "الأداء (إدغام بلا غُنّة)",
        ),
        Group(setOf("hamzat_wasl", "silent"), Color(0xFF9E9E9E), "لا تُلفظ"),
    )

    /** لون قاعدة التجويد — وما عُرف فهو بلا تلوين */
    fun colorOf(rule: String): Color? = groups.firstOrNull { rule in it.rules }?.color
}

/** برتقالي أزرار الآية المنبثقة — كالطبعة المرجعية في الصورة */
private val ActionOrange = Color(0xFFE8811A)

@Composable
fun MushafScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var doc by remember { mutableStateOf<MushafDoc?>(null) }
    LaunchedEffect(Unit) {
        doc = withContext(Dispatchers.IO) { MushafRepository.load(context) }
    }

    // آخر صفحة تُتذكر — وفي كل فتح يرجع المالك لآخر ما كان يقرأه
    var zoom by rememberSaveable { mutableStateOf(1f) }
    var selected by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var showSurahs by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }
    val pagerState = rememberPagerState(
        initialPage = (MushafRepository.lastPage(context) - 1).coerceIn(0, 603),
        pageCount = { 604 },
    )

    val current = doc
    Box(Modifier.fillMaxSize()) {
        if (current == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "… يُفتح المصحف",
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            val page = pagerState.currentPage + 1
            LaunchedEffect(pagerState.currentPage) {
                MushafRepository.saveLastPage(context, pagerState.currentPage + 1)
            }

            Column(Modifier.fillMaxSize()) {
                MushafTopBar(
                    doc = current,
                    page = page,
                    zoom = zoom,
                    onZoom = { delta -> zoom = (zoom * 10 + delta).coerceIn(8f, 22f) / 10f },
                    onSurahClick = { showSurahs = true },
                    onHelpClick = { showHelp = true },
                )

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) { index ->
                    MushafPage(
                        doc = current,
                        pageNumber = index + 1,
                        zoom = zoom,
                        selected = selected,
                        onSelect = { selected = it },
                    )
                }

                TajweedLegend()
            }
        }

        // صف أزرار الآية المنبثقة — فوق دليل الألوان
        selected?.let { sel ->
            AyahActionBar(
                doc = current ?: return@let,
                surah = sel.first,
                ayah = sel.second,
                onClose = { selected = null },
            )
        }
    }

    if (showSurahs && current != null) {
        SurahPickerDialog(
            doc = current,
            onDismiss = { showSurahs = false },
            onPick = { surah ->
                showSurahs = false
                selected = null
                scope.launch {
                    val target = MushafMath.firstPageOf(surah.number, current.pages)
                    pagerState.scrollToPage((target - 1).coerceIn(0, 603))
                }
            },
        )
    }
    if (showHelp) {
        TajweedHelpDialog(onDismiss = { showHelp = false })
    }

    DisposableEffect(Unit) {
        onDispose { MushafPlayer.stop() }
    }
}

/** تشغيل تلاوة الآية — تجويد مشاري العفاسي من شبكة القرآن (يحتاج إنترنت) */
private fun playAyah(context: android.content.Context, doc: MushafDoc, surah: Int, ayah: Int) {
    val global = MushafMath.globalAyah(surah, ayah, doc.surahs)
    val ok = MushafPlayer.play(
        "https://cdn.islamic.network/quran/audio/128/ar.alafasy/$global.mp3",
    )
    if (!ok) {
        Toast.makeText(context, "التلاوة تحتاج اتصالاً بالإنترنت", Toast.LENGTH_SHORT).show()
    }
}

/* ============================ الشريط العلوي ============================ */

@Composable
private fun MushafTopBar(
    doc: MushafDoc,
    page: Int,
    zoom: Float,
    onZoom: (Float) -> Unit,
    onSurahClick: () -> Unit,
    onHelpClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(scheme.surface.copy(alpha = 0.96f))
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // اسم السورة يفتح قائمة السور
        Text(
            text = doc.surahOfPage(page)?.let { doc.surah(it).name } ?: "—",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = scheme.primary,
            modifier = Modifier
                .clickable(onClick = onSurahClick)
                .padding(6.dp),
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = "صفحة $page • الجزء ${MushafMath.juzOf(page)}",
            fontSize = 12.sp,
            color = scheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(8.dp))
        RoundSmallButton("−", onClick = { onZoom(-1f) })
        Spacer(Modifier.width(4.dp))
        RoundSmallButton("+", onClick = { onZoom(1f) })
        Spacer(Modifier.width(4.dp))
        RoundSmallButton("؟", onClick = onHelpClick)
    }
}

@Composable
private fun RoundSmallButton(label: String, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(32.dp)
            .background(scheme.primary.copy(alpha = 0.1f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = scheme.primary)
    }
}

/** السورة السائدة لصفحة — آخر عنوان ظهر حتى صفحتها */
private fun MushafDoc.surahOfPage(page: Int): Int? {
    var last: Int? = null
    for (i in 0 until page.coerceAtMost(pages.size)) {
        for (line in pages[i]) {
            if (line is MushafLine.Header) last = line.surah
        }
    }
    return last
}

/* ============================ صفحة المصحف ============================ */

@Composable
private fun MushafPage(
    doc: MushafDoc,
    pageNumber: Int,
    zoom: Float,
    selected: Pair<Int, Int>?,
    onSelect: (Pair<Int, Int>?) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val lines = doc.pages.getOrNull(pageNumber - 1).orEmpty()

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .clickable { onSelect(null) }
            .padding(horizontal = 10.dp),
    ) {
        val textLines = lines.count { it is MushafLine.Text }
        val ornamentLines = lines.count { it !is MushafLine.Text }
        // مقاس السطر يملا الصفحة عمودياً كالطبعة — والعنوان والبسملة أعلى
        val units = (textLines + ornamentLines * 2.2f).coerceAtLeast(1f)
        val lineHeightDp = (maxHeight / units) * zoom
        val fontSizeDp = lineHeightDp * 0.72f
        val density = LocalDensity.current
        val fontSize: TextUnit = with(density) { fontSizeDp.toSp() }

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 4.dp),
        ) {
            for (line in lines) {
                when (line) {
                    is MushafLine.Header -> LineHeader(line)
                    is MushafLine.Basmala ->
                        MushafLineRow(line.words, fontSize, lineHeightDp, selected, onSelect)
                    is MushafLine.Text ->
                        MushafLineRow(line.words, fontSize, lineHeightDp, selected, onSelect)
                }
            }
        }

        // رقم الصفحة أسفل الوسط كالطبعة
        Text(
            text = "$pageNumber",
            fontSize = 10.sp,
            color = scheme.onSurfaceVariant,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 2.dp),
        )
    }
}

@Composable
private fun LineHeader(line: MushafLine.Header) {
    val scheme = MaterialTheme.colorScheme
    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .background(scheme.primary.copy(alpha = 0.08f), RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = line.text,
            fontFamily = MushafFont,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = scheme.primary,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 3.dp),
        )
    }
}

/** سطر نص — الكلمات تتمدد على عرض الصفحة كالطبعة المطبوعة */
@Composable
private fun MushafLineRow(
    words: List<MushafWord>,
    fontSize: TextUnit,
    lineHeightDp: androidx.compose.ui.unit.Dp,
    selected: Pair<Int, Int>?,
    onSelect: (Pair<Int, Int>?) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(lineHeightDp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (word in words) {
            if (word.ayah == 0) {
                // كلمات البسملة لا تحمل آية — تُعرض بلا تحديد
                Text(
                    text = word.toAnnotated(),
                    fontFamily = MushafFont,
                    fontSize = fontSize,
                    color = scheme.onBackground,
                )
                continue
            }
            val isSelected =
                selected != null && selected.first == word.surah && selected.second == word.ayah
            Text(
                text = word.toAnnotated(),
                fontFamily = MushafFont,
                fontSize = fontSize,
                color = scheme.onBackground,
                style = if (isSelected) {
                    androidx.compose.ui.text.TextStyle(
                        background = scheme.primary.copy(alpha = 0.18f),
                    )
                } else {
                    androidx.compose.ui.text.TextStyle()
                },
                modifier = Modifier.clickable { onSelect(word.surah to word.ayah) },
            )
        }
    }
}

/** نص الكلمة بتلوين التجويد على الحروف نفسها */
private fun MushafWord.toAnnotated(): AnnotatedString {
    val builder = AnnotatedString.Builder(text)
    for (span in spans) {
        TajweedPalette.colorOf(span.rule)?.let { color ->
            builder.addStyle(SpanStyle(color = color), span.start, span.end)
        }
    }
    return builder.toAnnotatedString()
}

/* ============================ الآية المنبثقة ============================ */

/** صف أزرار الآية — دوائر برتقالية كالطبعة المرجعية في الصورة */
@Composable
private fun AyahActionBar(
    doc: MushafDoc,
    surah: Int,
    ayah: Int,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var marked by remember(surah, ayah) {
        mutableStateOf(MushafRepository.isBookmarked(context, surah, ayah))
    }

    Box(Modifier.fillMaxSize()) {
        // عنوان الآية المختارة فوق صف الأزرار
        Text(
            text = "سورة ${doc.surah(surah).name} — الآية $ayah",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 116.dp)
                .background(ActionOrange.copy(alpha = 0.95f), RoundedCornerShape(20.dp))
                .padding(horizontal = 14.dp, vertical = 5.dp),
        )
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 52.dp)
                .background(Color.White.copy(alpha = 0.97f), RoundedCornerShape(30.dp))
                .border(1.5.dp, ActionOrange.copy(alpha = 0.5f), RoundedCornerShape(30.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ActionCircle(
                icon = if (MushafPlayer.playing) NoorIcons.QuranStop else Icons.Filled.PlayArrow,
                label = if (MushafPlayer.playing) "إيقاف" else "تلاوة",
                onClick = {
                    if (MushafPlayer.playing) MushafPlayer.stop()
                    else playAyah(context, doc, surah, ayah)
                },
            )
            ActionCircle(
                icon = Icons.Filled.Share,
                label = "مشاركة",
                onClick = {
                    runCatching {
                        context.startActivity(
                            Intent.createChooser(
                                Intent(Intent.ACTION_SEND)
                                    .setType("text/plain")
                                    .putExtra(Intent.EXTRA_TEXT, ayahShareText(doc, surah, ayah)),
                                "مشاركة الآية",
                            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    }
                },
            )
            ActionCircle(
                icon = NoorIcons.QuranCopy,
                label = "نسخ",
                onClick = {
                    clipboard.setText(AnnotatedString(ayahShareText(doc, surah, ayah)))
                    Toast.makeText(context, "نُسخت الآية", Toast.LENGTH_SHORT).show()
                },
            )
            ActionCircle(
                icon = if (marked) NoorIcons.QuranBookmark else NoorIcons.QuranBookmarkOutline,
                label = if (marked) "محفوظة" else "علامة",
                filled = marked,
                onClick = {
                    MushafRepository.toggleBookmark(context, surah, ayah)
                    marked = !marked
                    Toast.makeText(
                        context,
                        if (marked) "عُلِّمت الآية" else "أُزيلت العلامة",
                        Toast.LENGTH_SHORT,
                    ).show()
                },
            )
            ActionCircle(
                icon = Icons.Filled.Close,
                label = "إغلاق",
                onClick = onClose,
            )
        }
    }
}

@Composable
private fun ActionCircle(
    icon: ImageVector,
    label: String,
    filled: Boolean = false,
    onClick: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(ActionOrange.copy(alpha = if (filled) 1f else 0.92f), CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.height(2.dp))
        Text(label, fontSize = 10.sp, color = ActionOrange, fontWeight = FontWeight.Bold)
    }
}

/** نص المشاركة والنسخ — نص الآية verbatim ثم تعريفها */
private fun ayahShareText(doc: MushafDoc, surah: Int, ayah: Int): String {
    val text = doc.ayahText(surah, ayah)
    return "﴿$text﴾\n[سورة ${doc.surah(surah).name} — الآية $ayah]"
}

/* ============================ دليل الألوان ============================ */

@Composable
private fun TajweedLegend() {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .background(scheme.surface.copy(alpha = 0.96f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (group in TajweedPalette.groups) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(10.dp)
                        .background(group.color, CircleShape),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = group.label,
                    fontSize = 10.sp,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun TajweedHelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("حسناً") } },
        title = { Text("دليل ألوان التجويد", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (group in TajweedPalette.groups) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(12.dp)
                                .background(group.color, CircleShape),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(group.label, fontSize = 13.sp)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "الألوان على الحروف نفسها كطبعة مصحف التجويد المطبوع — ودليلها ثابت أسفل صفحة القراءة. لمس أي كلمة يُبرز آيتها ويفتح أزرارها.",
                    fontSize = 12.sp,
                )
            }
        },
    )
}

/* ============================ قائمة السور ============================ */

@Composable
private fun SurahPickerDialog(
    doc: MushafDoc,
    onDismiss: () -> Unit,
    onPick: (MushafSurah) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("إغلاق") } },
        title = { Text("اذهب إلى سورة", fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                for (surah in doc.surahs) {
                    Text(
                        text = "${surah.number}. ${surah.name} — ${surah.ayahCount} آية",
                        fontSize = 14.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(surah) }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                    )
                }
            }
        },
    )
}

/* ============================ مشغّل التلاوة ============================ */

/** مشغّل تلاوة بسيط — آية واحدة كل مرة، ويتوقف عند خروج الشاشة */
private object MushafPlayer {
    private var player: MediaPlayer? = null

    /** حالة حية للواجهة — تتغير فتتبدل أيقونة الزر */
    var playing by mutableStateOf(false)
        private set

    /** يرجع true إن بدأ التشغيل فعلاً */
    fun play(url: String): Boolean = runCatching {
        stop()
        val p = MediaPlayer()
        p.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build(),
        )
        p.setDataSource(url)
        p.setOnCompletionListener { stop() }
        p.setOnErrorListener { _, _, _ ->
            stop()
            true
        }
        p.setOnPreparedListener {
            it.start()
            playing = true
        }
        p.prepareAsync()
        player = p
        true
    }.getOrDefault(false)

    fun stop() {
        playing = false
        runCatching {
            player?.let { p ->
                if (p.isPlaying) p.stop()
                p.release()
            }
        }
        player = null
    }
}
