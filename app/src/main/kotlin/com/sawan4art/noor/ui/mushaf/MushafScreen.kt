package com.sawan4art.noor.ui.mushaf

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.sawan4art.noor.data.mushaf.MushafDb
import com.sawan4art.noor.domain.mushaf.MushafAyah
import com.sawan4art.noor.domain.mushaf.MushafGeometry
import com.sawan4art.noor.domain.mushaf.MushafJuz
import com.sawan4art.noor.domain.mushaf.MushafPages
import com.sawan4art.noor.domain.mushaf.MushafPlaylist
import com.sawan4art.noor.domain.mushaf.MushafSearch
import com.sawan4art.noor.domain.mushaf.MushafSegment
import com.sawan4art.noor.domain.mushaf.MushafSelection
import com.sawan4art.noor.domain.mushaf.MushafSelectionOps
import com.sawan4art.noor.domain.mushaf.MushafSurahs
import com.sawan4art.noor.domain.mushaf.MushafTransform
import com.sawan4art.noor.ui.icons.NoorIcons
import com.sawan4art.noor.ui.nav.GooeyMath
import com.sawan4art.noor.ui.nav.addLiquidBlob
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * المصحف — صفحات طبعة التجويد الملون المتسعة بكامل الشاشة وتفاعل آيات
 * سائل حي، وكله مرفق داخل التطبيق (alpha53).
 *
 * توجيه المالك ٢٠٢٦-١١-٠٦ بعد alpha52 — ثماني وصايا:
 *
 * 1) «هو في خطأ في شكل الايقونات في المصحف حيث ان النقطه التي في المنتصف
 *    تلتصق مع نقطه اخرى» — جسور السائل من قلب المنغّلة لقطراتها كانت
 *    تلصق الإغلاق بجاره، فأُسقطت الجسور وبُعِّدت الحلقة (٥٦←٦٤dp) وبقيت
 *    القطرة تموج سطحها بلا تلاصق أبداً.
 * 2) «زر تشغيل الصوت لما اضغط عليه للمره الثانيه يتم ايقاف الصوت» —
 *    تلاوة تعمل وضغطة ثانية على الزر تقف فوراً.
 * 3) «في القائمه المنبثقه الخاصه بتشغيل الصوت يظهر خيار اخير يفيد
 *    باغلاق القائمه انا عايز اشيل الخيار ده و احد فقط علامه اكس فوق
 *    يمين القائمه» — القائمة ثلاثة خيارات صافية وإكس صغير أعلى يمينها.
 * 4) «اول ما اضغط على ايقونه المصحف يظهر لي اختيار السوره قائمه مرتبه
 *    بترتيب السور المعتمد» + «فيها اختيار عرض اجزاء القران» — لوحة
 *    اختيار تنبثق عند الدخول: متابعة القراءة، العلامة المرجعية، تبويب
 *    السور ال١١٤ وتبويب الأجزاء ال٣٠، وبحث بكلمة واحدة بلا تشكيل.
 * 5) «صفحات المصحف تظهر علامه تحميل انا مش عايز كده انا عايز التفاسير
 *    و المصحف يكونوا داخل التطبيق بشكل دائم» — الصفحات ال٦٠٤ كلها مرفق
 *    داخل التطبيق (WebP) تُقرأ من ذاكرة الجهاز في لمح البصر، فلا علامة
 *    تحميل تظهر أبداً والتفسير المرفق بقاعدة المصحف كذلك.
 * 6) «استايل الايقونات السائله تمام لاكن اللون غير متناسق مع الثيم فهي
 *    تظهر دائما بلون برتقالي» — المنغّلة صارت بوصفة عائلة السوائل نفسها
 *    (primaryContainer ← secondaryContainer وأيقونتها onPrimaryContainer)
 *    عين شريط التنقل فتتبع الثيم المختار أينما تغيّر.
 * 7) «عايز ايقونه جديده تظهر مع الايقونات السائله المنبثقه في المصحف
 *    تكون خاصه بنظام حفظ القران و عايز نظام حفظ ميسر» — قطرتُ الحفظ
 *    تفتح لوحة الحفظ الميسر: نص الآيات يختبئ وتكشفه ضغطة، تلاوة تكرار
 *    ثلاث مرات للآية تدور على المُدى، وتعليم آية أو المُدى كله محفوظاً
 *    فيُحفظ تعليمه داخل التطبيق.
 * 8) «نظام بحث بالكلمه في ايات المصحف و يكون نظام بحث بدون تشكيل يكفي
 *    فقط كتابه كلمه واحده» — حقل البحث في لوحة الاختيار يطبع تطبيع
 *    التشكيل على الكلمة والنص معاً فيرجع كل الآيات الحاملة لها ولموضعها
 *    سورةً ورقماً وصفحةً تقفز إليها الضغطة.
 */

/** المساحة المحجوزة أسفل الشاشة قبل شريط التنقل السائل (١٨+٧٦dp — alpha45) */
private val NAV_BAR_CLEAR = 94.dp

/** خلفية بطاقات المصحف — دافئة داكنة تليق فوق ورق الطبعة */
private val PanelBg = Color(0xFF1E1814)

/** لون إبراز التحديد — بنفسجي شفاف كما في صورة المالك */
private val SelectionPurple = Color(0xFF7C4DFF).copy(alpha = 0.45f)

/** هندسة المنغّلة السائلة — حلقة سداسية حول قلب الإغلاق (alpha53) */
private val RING_RADIUS = 64.dp
private val ITEM_SIZE = 46.dp
private val CLUSTER_SIZE = 208.dp

/** تكرار تلاوة الحفظ الميسر — كل آية تُتشغَّل ثلاث مرات */
private const val HIFZ_REPEATS = 3

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MushafScreen(
    /** آخر صفحة محفوظة (١..٦٠٤) — نقطة بداية المصحف */
    initialPage: Int,
    /** سورة العلامة المرجعية — صفر يعني لا علامة */
    bookmarkSura: Int,
    /** آية العلامة المرجعية */
    bookmarkAyah: Int,
    /** صفحة العلامة المرجعية — قفزة الضغطة عليها */
    bookmarkPage: Int,
    /** آيات الحفظ المعلَّمة — سلاسل «سورة:آية» */
    hifzMemorized: Set<String>,
    /** حفظ آخر صفحة وصل إليها القارئ */
    onSavePage: (Int) -> Unit,
    /** حفظ علامة مرجعية أو مسحها — سورة صفر للمسح */
    onSetBookmark: (Int, Int, Int) -> Unit,
    /** تعليم آيات محفوظة أو مسح التعليم — الحفظ الميسر */
    onSetHifz: (Int, List<Int>, Boolean) -> Unit,
    /** عودة للرئيسية عبر إيماءة الرجوع النظامية */
    onBack: () -> Unit,
) {
    val context = LocalContext.current

    // كامل الشاشة: شريط الحالة مستخفى أثناء القراءة — السحب يُظهره عابراً
    // ويعود عند مغادرة المصحف، وتشغيل التلاوة يقف مع الخروج
    DisposableEffect(Unit) {
        val controller = context.findActivity()?.window?.let { window ->
            WindowCompat.getInsetsController(window, window.decorView).apply {
                systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                hide(WindowInsetsCompat.Type.statusBars())
            }
        }
        onDispose {
            controller?.show(WindowInsetsCompat.Type.statusBars())
            MushafPlayer.stop()
        }
    }

    val pagerState = rememberPagerState(
        initialPage = MushafPages.clampPage(initialPage) - 1,
        pageCount = { MushafPages.PAGE_COUNT },
    )
    val scope = rememberCoroutineScope()

    // لوحة اختيار السورة تنبثق عند الدخول — توجيه المالك الحرفي: «اول ما
    // اضغط على ايقونه المصحف يظهر لي اختيار السوره»
    var chooserOpen by remember { mutableStateOf(true) }

    // التحديد والمنغّلة وقائمتا التلاوة والحفظ ولوحة التفسير — فوق الصفحات
    var selection by remember { mutableStateOf<MushafSelection?>(null) }
    var menuAt by remember { mutableStateOf<Offset?>(null) }
    var playMenuOpen by remember { mutableStateOf(false) }
    var tafsirOpen by remember { mutableStateOf(false) }
    var hifzOpen by remember { mutableStateOf(false) }
    var rootSize by remember { mutableStateOf(IntSize.Zero) }
    var surahCount by remember { mutableStateOf(0) }

    // الإيماءة الرجوع: تُغلق اللوحات أولاً ثم تلغي التحديد ثم تعود للرئيسية
    BackHandler(enabled = chooserOpen || tafsirOpen || hifzOpen || selection != null) {
        when {
            chooserOpen -> chooserOpen = false
            tafsirOpen -> tafsirOpen = false
            hifzOpen -> hifzOpen = false
            else -> {
                selection = null
                menuAt = null
                playMenuOpen = false
            }
        }
    }

    // حفظ آخر صفحة عند وصول القارئ إليها — المصحف يظل مكمنه الذي تركه
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { page ->
                onSavePage(page + 1)
                menuAt = null // قلب الصفحة يُخفي المنغّلة والتحديد يبقى للمُدى
                playMenuOpen = false
            }
    }

    // عدّ آيات السورة المحددة — لعناوين خيارات التلاوة الثلاثة
    LaunchedEffect(selection?.sura) {
        val sura = selection?.sura ?: return@LaunchedEffect
        surahCount = withContext(Dispatchers.IO) { MushafDb.surahAyahCount(context, sura) }
    }

    val playing by MushafPlayer.playing.collectAsState()

    val density = LocalDensity.current
    val clusterHalfPx = with(density) { CLUSTER_SIZE.toPx() / 2f }

    fun clampMenu(pos: Offset): Offset {
        val w = rootSize.width.toFloat()
        val h = rootSize.height.toFloat()
        return Offset(
            pos.x.coerceIn(clusterHalfPx, (w - clusterHalfPx).coerceAtLeast(clusterHalfPx + 1f)),
            pos.y.coerceIn(clusterHalfPx, (h - clusterHalfPx).coerceAtLeast(clusterHalfPx + 1f)),
        )
    }

    /** قفزة صفحة من اللوحات — تُغلق اللوحة فيصير المصحف مكمن القفزة */
    fun jumpTo(page: Int) {
        scope.launch {
            pagerState.scrollToPage(MushafPages.clampPage(page) - 1)
            chooserOpen = false
        }
    }

    /** تشغيل مُدى آيات متتابع — رابط كل آية من ترتيبها العالمي، والحفظ يكرر كل آية ثلاثاً */
    fun startPlaylist(sura: Int, ayahs: List<Int>, loop: Boolean = false) {
        scope.launch {
            val sequence = if (loop) MushafPlaylist.hifzRepeats(ayahs, HIFZ_REPEATS) else ayahs
            val urls = withContext(Dispatchers.IO) {
                sequence.map { ayah ->
                    val global = MushafDb.globalAyah(context, sura, ayah)
                    "https://cdn.islamic.network/quran/audio/128/ar.alafasy/$global.mp3"
                }
            }
            val ok = withContext(Dispatchers.IO) { MushafPlayer.play(urls, loop) }
            if (!ok) {
                Toast.makeText(context, "التلاوة تحتاج اتصالاً بالإنترنت", Toast.LENGTH_SHORT).show()
            }
            playMenuOpen = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onSizeChanged { rootSize = it },
    ) {
        // مساحة القراءة — تقف قبل شريط التنقل السائل، والصفحة تتسع لكاملها
        HorizontalPager(
            state = pagerState,
            beyondViewportPageCount = 1,
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(bottom = NAV_BAR_CLEAR),
        ) { index ->
            MushafPage(
                page = index + 1,
                selection = selection,
                onLongPressAyah = { ayah, at ->
                    selection = MushafSelectionOps.extend(null, ayah)
                    playMenuOpen = false
                    menuAt = clampMenu(at)
                },
                onTapAyah = { ayah, at ->
                    selection = MushafSelectionOps.extend(selection, ayah)
                    menuAt = clampMenu(at)
                },
            )
        }

        // المنغّلة السائلة أو قائمة التلاوة الثلاثة
        val at = menuAt
        if (at != null) {
            val sel = selection
            if (sel != null) {
                if (playMenuOpen) {
                    PlayOptionsCard(
                        offset = at,
                        rootSize = rootSize,
                        selection = sel,
                        surahCount = surahCount,
                        onPlay = { ayahs -> startPlaylist(sel.sura, ayahs) },
                        onClose = { playMenuOpen = false },
                    )
                } else {
                    AyahMenuCluster(
                        offset = at,
                        playing = playing,
                        bookmarkedHere = bookmarkSura == sel.sura &&
                            bookmarkAyah == MushafSelectionOps.firstOf(sel).ayah,
                        onPlay = {
                            // تلاوة تعمل وضغطة ثانية تقف — توجيه المالك الحرفي
                            if (playing) MushafPlayer.stop() else playMenuOpen = true
                        },
                        onHifz = { hifzOpen = true },
                        onBookmark = {
                            val first = MushafSelectionOps.firstOf(sel)
                            if (bookmarkSura == first.sura && bookmarkAyah == first.ayah) {
                                onSetBookmark(0, 0, 0)
                                Toast.makeText(context, "أُزيلت العلامة المرجعية", Toast.LENGTH_SHORT).show()
                            } else {
                                onSetBookmark(first.sura, first.ayah, pagerState.currentPage + 1)
                                Toast.makeText(
                                    context,
                                    "حُفظت العلامة · ${MushafSurahs.caption(sel)}",
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                            menuAt = null
                        },
                        onTafsir = { tafsirOpen = true },
                        onCopy = {
                            scope.launch {
                                val text = withContext(Dispatchers.IO) {
                                    MushafDb.ayahRangeText(context, sel.sura, sel.from, sel.to)
                                }
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("آيات", text))
                                Toast.makeText(
                                    context,
                                    "نُسخ نص ${MushafSurahs.caption(sel)}",
                                    Toast.LENGTH_SHORT,
                                ).show()
                                menuAt = null
                            }
                        },
                        onShare = {
                            scope.launch {
                                val text = withContext(Dispatchers.IO) {
                                    MushafDb.ayahRangeText(context, sel.sura, sel.from, sel.to)
                                }
                                val send = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, "${MushafSurahs.caption(sel)}\n$text")
                                }
                                context.startActivity(Intent.createChooser(send, "مشاركة الآيات"))
                                menuAt = null
                            }
                        },
                        onClose = { menuAt = null },
                    )
                }
            }
        }

        // لوحة التفسير الميسر — صاعدة من الأسفل فوق كل شيء
        if (tafsirOpen) {
            val sel = selection
            if (sel != null) {
                TafsirPanel(
                    selection = sel,
                    rootSize = rootSize,
                    onDismiss = { tafsirOpen = false },
                )
            }
        }

        // لوحة الحفظ الميسر — نص الآيات يختبئ وتكشفه ضغطة مع تلاوة تكرار وتعليم محفوظ
        if (hifzOpen) {
            val sel = selection
            if (sel != null) {
                HifzPanel(
                    selection = sel,
                    hifzMemorized = hifzMemorized,
                    playing = playing,
                    rootSize = rootSize,
                    onSetHifz = onSetHifz,
                    onPlayRepeated = { ayahs -> startPlaylist(sel.sura, ayahs, loop = true) },
                    onStopPlaying = { MushafPlayer.stop() },
                    onDismiss = { hifzOpen = false },
                )
            }
        }

        // لوحة اختيار السورة — تنبثق عند الدخول وعلى الصفحات كالضباب الخفيف
        if (chooserOpen) {
            SurahChooserPanel(
                rootSize = rootSize,
                bookmarkSura = bookmarkSura,
                bookmarkAyah = bookmarkAyah,
                bookmarkPage = bookmarkPage,
                lastPage = pagerState.currentPage + 1,
                onJump = { jumpTo(it) },
                onDismiss = { chooserOpen = false },
            )
        }

        // العلامة المرجعية — نقطة صغيرة أعلى اليمين تظهر إن وجدت علامة:
        // ضغطة تقفز إلى صفحتها، وضغط مطوّل يمسحها
        if (bookmarkSura > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 10.dp, top = 10.dp)
                    .size(30.dp)
                    .shadow(4.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.38f))
                    .combinedClickable(
                        onClick = {
                            scope.launch {
                                pagerState.scrollToPage(MushafPages.clampPage(bookmarkPage) - 1)
                            }
                            Toast.makeText(
                                context,
                                "العلامة: سورة ${MushafSurahs.name(bookmarkSura)} · آية ${MushafSurahs.arabicDigits(bookmarkAyah)}",
                                Toast.LENGTH_SHORT,
                            ).show()
                        },
                        onLongClick = {
                            onSetBookmark(0, 0, 0)
                            Toast.makeText(context, "أُزيلت العلامة المرجعية", Toast.LENGTH_SHORT).show()
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = NoorIcons.QuranBookmarkOutline,
                    contentDescription = "العلامة المرجعية",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

/**
 * صفحة واحدة من المصحف — صورة الطبعة **المرفقة داخل التطبيق** (alpha53)
 * المقصوصة المتسعة بكامل مساحة القراءة: تُقرأ من مرفقات الجهاز في لمح
 * البصر فلا علامة تحميل تظهر أبداً — توجيه المالك: «انا مش عايز كده انا
 * عايز التفاسير و المصحف يكونوا داخل التطبيق بشكل دائم مش يتم تحميلهم
 * كل مره .. علشان ميظهرش ابدا ان الصفحه بتحمل» — مع إبراز الآيات
 * المحدَّدة وضغطها وشرائح الآيات من قاعدة الطبعة المصاحبة.
 */
@Composable
private fun MushafPage(
    page: Int,
    selection: MushafSelection?,
    onLongPressAyah: (MushafAyah, Offset) -> Unit,
    onTapAyah: (MushafAyah, Offset) -> Unit,
) {
    val context = LocalContext.current

    var bitmap by remember(page) { mutableStateOf<ImageBitmap?>(null) }
    var failed by remember(page) { mutableStateOf(false) }
    var attempt by remember(page) { mutableIntStateOf(0) }

    LaunchedEffect(page, attempt) {
        failed = false
        val bmp = withContext(Dispatchers.IO) {
            runCatching {
                context.assets.open(MushafPages.assetPath(page)).use { input ->
                    BitmapFactory.decodeStream(input)
                }
            }.getOrNull()
        }
        if (bmp != null) {
            bitmap = bmp.asImageBitmap()
        } else {
            bitmap = null
            failed = true
        }
    }

    // شرائح الآيات من قاعدة الطبعة — استعلام صفحة لا يذكر بالذاكرة
    var segments by remember(page) { mutableStateOf<List<MushafSegment>?>(null) }
    LaunchedEffect(page) {
        segments = withContext(Dispatchers.IO) { MushafDb.segments(context, page) }
    }

    var viewSize by remember { mutableStateOf(IntSize.Zero) }
    val transform = remember(page, bitmap, viewSize, segments) {
        val bmp = bitmap ?: return@remember null
        if (viewSize.width == 0 || viewSize.height == 0) return@remember null
        val segs = segments
        val minX = segs?.minOf { it.minX } ?: 0f
        val maxX = segs?.maxOf { it.maxX } ?: MushafGeometry.REF_W
        val crop = MushafGeometry.cropX(page, minX, maxX)
        MushafTransform(
            imgW = bmp.width.toFloat(),
            imgH = bmp.height.toFloat(),
            cropX0 = crop?.first ?: 0f,
            cropX1 = crop?.second ?: MushafGeometry.REF_W,
            viewW = viewSize.width.toFloat(),
            viewH = viewSize.height.toFloat(),
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onSizeChanged { viewSize = it }
            .pointerInput(page, bitmap, segments) {
                detectTapGestures(
                    // الضغط المطوّل يبدأ تحديداً جديداً بالآية تحت الإصبع
                    onLongPress = { pos ->
                        val segs = segments ?: return@detectTapGestures
                        val t = transform ?: return@detectTapGestures
                        val (gx, gy) = t.toGlyph(pos.x, pos.y)
                        MushafSelectionOps.hit(segs, gx, gy)?.let { onLongPressAyah(it, pos) }
                    },
                    // الضغطة التالية تمدّ المُدى حتى الآية الجديدة (٥٠ ← ٥٥)
                    onTap = { pos ->
                        if (selection != null) {
                            val segs = segments ?: return@detectTapGestures
                            val t = transform ?: return@detectTapGestures
                            val (gx, gy) = t.toGlyph(pos.x, pos.y)
                            MushafSelectionOps.hit(segs, gx, gy)?.let { onTapAyah(it, pos) }
                        }
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        val bmp = bitmap
        val t = transform
        if (bmp != null && t != null) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawMushafPage(bmp, t, page, segments, selection)
            }
        }

        if (failed) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { attempt += 1 },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "تعذّر عرض الصفحة $page",
                    color = Color.White.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "اضغط للمحاولة من جديد",
                    color = Color.White.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

/**
 * رسم الصفحة داخل الكانفس: القص الجانبي (بلا تفاسير) ثم تنظيف الزوايا
 * العلوية بلون الورق ثم إبراز المُدى المحدَّد بشريط بنفسجي على أسطره —
 * والامتداد الكامل للمساحة يجري في التحويل نفسه.
 */
private fun DrawScope.drawMushafPage(
    bitmap: ImageBitmap,
    t: MushafTransform,
    page: Int,
    segments: List<MushafSegment>?,
    selection: MushafSelection?,
) {
    drawImage(
        image = bitmap,
        srcOffset = IntOffset(t.srcX0.roundToInt(), 0),
        srcSize = IntSize((t.srcX1 - t.srcX0).roundToInt(), bitmap.height),
        dstOffset = IntOffset(t.dstLeft.roundToInt(), t.dstTop.roundToInt()),
        dstSize = IntSize(t.dstWidth.roundToInt(), t.dstHeight.roundToInt()),
        filterQuality = FilterQuality.High,
    )

    val segs = segments ?: return

    // تنظيف زوايا الإشارات العلوية خارج الإطار — الصفحات المقصوصة فقط،
    // بلون ورق الطبعة نفسه فلا يُعرف الفرق بين المطلوي والمطبوع
    if (page >= MushafGeometry.FIRST_CROPPED_PAGE) {
        val minX = segs.minOf { it.minX }
        val maxX = segs.maxOf { it.maxX }
        val (fx0, fx1) = MushafGeometry.frameX(minX, maxX)
        val paper = Color(MushafGeometry.PAPER_RGB)
        val cornerH = t.toDisplay(0f, MushafGeometry.CORNER_CLEAN_H).second - t.dstTop
        val leftEnd = t.toDisplay(fx0 - 2f, 0f).first
        val rightStart = t.toDisplay(fx1 + 2f, 0f).first
        val rightEnd = t.dstLeft + t.dstWidth
        if (leftEnd > t.dstLeft) {
            drawRect(
                color = paper,
                topLeft = Offset(t.dstLeft, t.dstTop),
                size = Size(leftEnd - t.dstLeft, cornerH),
            )
        }
        if (rightStart < rightEnd) {
            drawRect(
                color = paper,
                topLeft = Offset(rightStart, t.dstTop),
                size = Size(rightEnd - rightStart, cornerH),
            )
        }
    }

    // إبراز المُدى المحدَّد — شريط بنفسجي شفاف على أسطر الآيات كما في
    // صورة المالك، لكل شريحة سطر على حِدة فالسطر المشترك يُبرز نصيبه
    val sel = selection ?: return
    segs.filter { it.sura == sel.sura && it.ayah in sel.ayahs }.forEach { seg ->
        val (x0, y0) = t.toDisplay(seg.minX - 2f, seg.minY - 1f)
        val (x1, y1) = t.toDisplay(seg.maxX + 2f, seg.maxY + 1f)
        drawRoundRect(
            color = SelectionPurple,
            topLeft = Offset(x0, y0),
            size = Size(x1 - x0, y1 - y0),
            cornerRadius = CornerRadius(7f, 7f),
        )
    }
}

/** فعل المنغّلة — أيقونة وعنوان وإجراء */
private data class MenuAction(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
)

/**
 * المنغّلة السائلة الحية — من نفس عائلة شريط التنقل السائل بوصفته
 * اللونية ذاتها (alpha53 بلاغ المالك: «اللون غير متناسق مع الثيم فهي
 * تظهر دائما بلون برتقالي»): القطرة تدرّج primaryContainer ←
 * secondaryContainer وأيقونتها onPrimaryContainer فتتبع الثيم المختار.
 *
 * **ولا التصاق في القلب** — بلاغ المالك: «النقطه التي في المنتصف تلتصق
 * مع نقطه اخرى» — كانت جسور السائل من القلب للقطرات تلصق الإغلاق
 * بجاره؛ أُسقطت الجسور كلها وبُعِّدت الحلقة (٥٦←٦٤dp) حتى مع أعلى
 * تموجٍ للقطرة تظل الأنقاط منفصلة نظيفة.
 *
 * الحلقة سداسية: علامة أعلى، تلاوة يمينَها الأعلى، حفظ يمينَها الأسفل
 * (الجديد بنظمة النجمة)، تفسير أسفل، نسخ أسفل اليسار، مشاركة يسارَها
 * الأعلى — والإغلاق في القلب فوق نقطة الضغط.
 */
@Composable
private fun AyahMenuCluster(
    offset: Offset,
    playing: Boolean,
    bookmarkedHere: Boolean,
    onPlay: () -> Unit,
    onHifz: () -> Unit,
    onBookmark: () -> Unit,
    onTafsir: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onClose: () -> Unit,
) {
    val density = LocalDensity.current
    val ringPx = with(density) { RING_RADIUS.roundToPx() }
    val itemPx = with(density) { ITEM_SIZE.roundToPx() }
    val clusterPx = with(density) { CLUSTER_SIZE.roundToPx() }
    val half = clusterPx / 2f

    // زمن حي لا يتوقف — السائل لا يسكن (تموج ورفرفة في الثبات ذاته)
    var frameNanos by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            withInfiniteAnimationFrameNanos { frameNanos = it }
        }
    }

    // قفزة السائل من نقطة الضغط — نابض بزوائد فيتضخم ويستقر كماء لزج
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        appear.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 340f))
    }
    val timeSec = frameNanos / 1_000_000_000f
    val ap = appear.value
    val grow = ap.coerceIn(0f, 1f)

    // لون عائلة السوائل — تدرّج الحاوين وأيقونة حاوية العنوان عين شريط التنقل
    val blobTop = MaterialTheme.colorScheme.primaryContainer
    val blobBottom = MaterialTheme.colorScheme.secondaryContainer
    val iconOn = MaterialTheme.colorScheme.onPrimaryContainer
    val blobBrush = Brush.verticalGradient(
        listOf(blobTop, blobBottom),
        startY = half - ringPx * 0.7f,
        endY = half + ringPx * 0.7f,
    )

    val actions = listOf(
        -90f to MenuAction("علامة مرجعية", if (bookmarkedHere) NoorIcons.QuranBookmark else NoorIcons.QuranBookmarkOutline, onBookmark),
        -30f to MenuAction("تلاوة", if (playing) NoorIcons.QuranStop else Icons.Filled.PlayArrow, onPlay),
        30f to MenuAction("حفظ", NoorIcons.QuranHifz, onHifz),
        90f to MenuAction("تفسير", NoorIcons.QuranTafsir, onTafsir),
        150f to MenuAction("نسخ", NoorIcons.QuranCopy, onCopy),
        210f to MenuAction("مشاركة", Icons.Filled.Share, onShare),
    )

    Box(modifier = Modifier.offset {
        IntOffset((offset.x - half).roundToInt(), (offset.y - half).roundToInt())
    }) {
        // السائل الموحد — مسار واحد للقطرات الست والقلب بلا جسور فلا التصاق
        Canvas(modifier = Modifier.size(CLUSTER_SIZE)) {
            if (ap <= 0.02f) return@Canvas
            val centerR = itemPx / 2f * grow
            val liquid = Path()
            addLiquidBlob(liquid, half, half, centerR, timeSec, 0.32f, 0f)
            actions.forEachIndexed { i, (deg, _) ->
                val rad = deg * PI.toFloat() / 180f
                val cx = half + ringPx * grow * cos(rad)
                val cy = half + ringPx * grow * sin(rad)
                val wave = 1f + GooeyMath.surfaceWave(rad, timeSec, 0.26f, i * 0.9f)
                addLiquidBlob(liquid, cx, cy, itemPx / 2f * grow * wave, timeSec, 0.22f, i * 0.9f)
            }
            drawPath(liquid, blobBrush)
        }

        // الأيقونات عائمة فوق القطرات — نفس المواضع والتموج بعينه
        actions.forEachIndexed { i, (deg, action) ->
            val rad = deg * PI.toFloat() / 180f
            val cx = half + ringPx * grow * cos(rad)
            val cy = half + ringPx * grow * sin(rad)
            MenuButton(
                icon = action.icon,
                contentDescription = action.label,
                tint = iconOn,
                offsetPx = IntOffset(
                    (cx - itemPx / 2f).roundToInt(),
                    (cy - itemPx / 2f).roundToInt(),
                ),
                appear = ap,
                onClick = action.onClick,
            )
        }
        // الإغلاق في قلب المنغّلة — فوق نقطة الضغط عينها
        MenuButton(
            icon = Icons.Filled.Close,
            contentDescription = "إغلاق",
            tint = iconOn,
            offsetPx = IntOffset(
                (half - itemPx / 2f).roundToInt(),
                (half - itemPx / 2f).roundToInt(),
            ),
            appear = ap,
            onClick = onClose,
        )
    }
}

/**
 * قطرة المنغّلة القابلة للضغط — بلا خلفية ذاتية: السائل المرسوم في
 * الكانفس هو الدائرة المرئية، وأيقونتها بلون الثيم على الحاوين.
 */
@Composable
private fun MenuButton(
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    offsetPx: IntOffset,
    appear: Float,
    onClick: () -> Unit,
) {
    val pop = appear.coerceIn(0.2f, 1.12f)
    Box(
        modifier = Modifier
            .offset { offsetPx }
            .size(ITEM_SIZE)
            .scale(pop)
            .alpha(appear.coerceIn(0f, 1f))
            .clickable(enabled = appear > 0.55f, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(22.dp),
        )
    }
}

/**
 * قائمة التلاوة الثلاثية — توجيه المالك الحرفي: «تشغيل السوره كامله من
 * بدايه التحديد او تشغيل المحدد فقط او تشغيل السوره كامله من بدايتها
 * بغض النظر عن التحديد» — بطاقة دافئة تنبثق مكان المنغّلة، وخيار
 * الإغلاق ذو الأسفل أُسقط بتوجيه المالك: «انا عايز اشيل الخيار ده و
 * احد فقط علامه اكس فوق يمين القائمه المنبثقه» — فالإكس صغير أعلى
 * يمينها هو القافل وحده.
 */
@Composable
private fun PlayOptionsCard(
    offset: Offset,
    rootSize: IntSize,
    selection: MushafSelection,
    surahCount: Int,
    onPlay: (List<Int>) -> Unit,
    onClose: () -> Unit,
) {
    val density = LocalDensity.current
    val cardW = with(density) { 252.dp.roundToPx() }
    val cardH = with(density) { 208.dp.roundToPx() }
    val x = (offset.x.roundToInt() - cardW / 2).coerceIn(8, (rootSize.width - cardW - 8).coerceAtLeast(9))
    val y = (offset.y.roundToInt() - cardH - with(density) { 12.dp.roundToPx() })
        .coerceIn(8, (rootSize.height - cardH - 8).coerceAtLeast(9))

    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        appear.animateTo(1f, spring(dampingRatio = 0.62f, stiffness = 380f))
    }
    val ap = appear.value

    val first = MushafSelectionOps.firstOf(selection).ayah
    val last = selection.ayahs.last
    val span = MushafSurahs.caption(selection)
    val accent = MaterialTheme.colorScheme.primary

    Box(modifier = Modifier.offset { IntOffset(x, y) }) {
        Column(
            modifier = Modifier
                .width(252.dp)
                .scale(0.85f + 0.15f * ap)
                .alpha(ap.coerceIn(0f, 1f))
                .shadow(16.dp, RoundedCornerShape(20.dp))
                .clip(RoundedCornerShape(20.dp))
                .background(PanelBg.copy(alpha = 0.97f))
                .padding(vertical = 4.dp),
        ) {
            // الترويسة والإكس أعلى يمينها — في RTL أول العناصر يظهر يميناً
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                        .clickable(onClick = onClose),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "إغلاق قائمة التلاوة",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(16.dp),
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "التلاوة · ${MushafSurahs.name(selection.sura)}",
                    color = Color.White.copy(alpha = 0.55f),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            PlayOptionRow(
                icon = Icons.Filled.PlayArrow,
                accent = accent,
                title = "تشغيل المحدد فقط",
                subtitle = span,
            ) { onPlay(MushafPlaylist.selected(selection)) }
            PlayOptionRow(
                icon = Icons.Filled.PlayArrow,
                accent = accent,
                title = "تشغيل السورة من بداية التحديد",
                subtitle = "من آية ${MushafSurahs.arabicDigits(first)} إلى آية ${MushafSurahs.arabicDigits(surahCount)}",
            ) { onPlay(MushafPlaylist.surahFromSelection(selection, surahCount)) }
            PlayOptionRow(
                icon = Icons.Filled.PlayArrow,
                accent = accent,
                title = "تشغيل السورة كاملة من بدايتها",
                subtitle = "الآيات ١–${MushafSurahs.arabicDigits(surahCount)} بغض النظر عن التحديد",
            ) { onPlay(MushafPlaylist.surahFull(surahCount)) }
        }
    }
}

/** سطر خيار واحد في بطاقة التلاوة — أيقونة بلون الثيم وعنوان ووصفه */
@Composable
private fun PlayOptionRow(
    icon: ImageVector,
    accent: Color,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(22.dp),
        )
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(
                text = title,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

/**
 * لوحة التفسير الميسر — صاعدة من الأسفل فوق صفحة المصحف: لكل آية في
 * المُدى المحدد نصها العثماني وتفسيرها الميسر، ومصدرها مرفق التطبيق نفسه
 * فتعمل بلا إنترنت. ضغطة العاتم أو الإيماءة الرجوع أو زر الإغلاق يقفلها.
 */
@Composable
private fun TafsirPanel(
    selection: MushafSelection,
    rootSize: IntSize,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current

    // محتوى المُدى: (الآية، نصها العثماني، تفسيرها) — قراءة واحدة خفيفة
    var entries by remember(selection) {
        mutableStateOf<List<Triple<Int, String, String>>?>(null)
    }
    LaunchedEffect(selection) {
        entries = withContext(Dispatchers.IO) {
            val lo = minOf(selection.from, selection.to)
            val hi = maxOf(selection.from, selection.to)
            MushafDb.tafsirRange(context, selection.sura, lo, hi).map { (ayah, tafsir) ->
                Triple(ayah, MushafDb.ayahText(context, selection.sura, ayah) ?: "", tafsir)
            }
        }
    }

    val accent = MaterialTheme.colorScheme.primary
    BottomSheet(
        rootSize = rootSize,
        fraction = 0.72f,
        onDismiss = onDismiss,
    ) {
        SheetHeader(
            icon = NoorIcons.QuranTafsir,
            accent = accent,
            title = "التفسير الميسر",
            subtitle = MushafSurahs.caption(selection),
            onClose = onDismiss,
        )

        val content = entries
        if (content == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(26.dp),
                    color = accent,
                    strokeWidth = 2.dp,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp),
            ) {
                items(content) { (ayah, text, tafsir) ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 10.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(accent.copy(alpha = 0.16f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                            ) {
                                Text(
                                    text = "آية ${MushafSurahs.arabicDigits(ayah)}",
                                    color = accent,
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                        }
                        if (text.isNotBlank()) {
                            Text(
                                text = text,
                                color = Color.White.copy(alpha = 0.72f),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                        Text(
                            text = tafsir,
                            color = Color.White.copy(alpha = 0.95f),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp)
                            .height(0.5.dp)
                            .background(Color.White.copy(alpha = 0.1f)),
                    )
                }
            }
        }
    }
}

/**
 * لوحة الحفظ الميسر — توجيه المالك: «عايز ايقونه جديده تظهر مع
 * الايقونات السائله المنبثقه في المصحف تكون خاصه بنظام حفظ القران و
 * عايز نظام حفظ ميسر». سهلٌ على المقصود به:
 *
 * • النص يختبئ افتراضياً وضغطة على الآية تكشفه — اختبار النفس بنظرة.
 * • «تشغيل بالتكرار» — كل آية تُتشغَّل ثلاث مرات ثم تليها الأخرى
 *   وتعود الدورة على المُدى حتى يقف المستخدم (طريقة الحفظ بالتكرار).
 * • ✓ تعليم الآية محفوظة ضغطة على نقطة الدائرة، أو تعليم المُدى كله
 *   من زر الترويسة — والتعليم يبقى محفوظاً داخل التطبيق.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HifzPanel(
    selection: MushafSelection,
    hifzMemorized: Set<String>,
    playing: Boolean,
    rootSize: IntSize,
    onSetHifz: (Int, List<Int>, Boolean) -> Unit,
    onPlayRepeated: (List<Int>) -> Unit,
    onStopPlaying: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current

    // محتوى المُدى: (الآية، نصها العثماني) — قراءة واحدة خفيفة
    var entries by remember(selection) {
        mutableStateOf<List<Pair<Int, String>>?>(null)
    }
    LaunchedEffect(selection) {
        entries = withContext(Dispatchers.IO) {
            val lo = minOf(selection.from, selection.to)
            val hi = maxOf(selection.from, selection.to)
            (lo..hi).map { ayah -> ayah to (MushafDb.ayahText(context, selection.sura, ayah) ?: "") }
        }
    }

    // النص مختبئ افتراضياً — وضغطة الآية تكشفه. الكشوف المحلية تُنسى
    // بين الفتحتين فتبدأ اللوحة كل مرة اختباراً نظيفاً
    var hiddenAll by remember(selection) { mutableStateOf(true) }
    var revealed by remember(selection) { mutableStateOf(setOf<Int>()) }

    val accent = MaterialTheme.colorScheme.primary
    val ayahs = selection.ayahs.toList()
    val memorizedInRange = ayahs.count { hifzMemorized.contains("${selection.sura}:$it") }
    val allMemorized = ayahs.isNotEmpty() && memorizedInRange == ayahs.size

    BottomSheet(
        rootSize = rootSize,
        fraction = 0.72f,
        onDismiss = onDismiss,
    ) {
        SheetHeader(
            icon = NoorIcons.QuranHifz,
            accent = accent,
            title = "الحفظ الميسر",
            subtitle = MushafSurahs.caption(selection),
            onClose = onDismiss,
        )

        // صف الأدوات: تكرار + إظهار/إخفاء + تعليم المُدى — وعدّ المحفوظ
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SheetChip(
                text = if (playing) "إيقاف التكرار" else "تشغيل بالتكرار",
                filled = playing,
                accent = accent,
                onClick = { if (playing) onStopPlaying() else onPlayRepeated(ayahs) },
            )
            SheetChip(
                text = if (hiddenAll) "إظهار الكل" else "إخفاء الكل",
                filled = false,
                accent = accent,
                onClick = {
                    hiddenAll = !hiddenAll
                    revealed = emptySet()
                },
            )
            SheetChip(
                text = if (allMemorized) "إلغاء تعليم المُدى" else "تعليم المُدى محفوظاً",
                filled = allMemorized,
                accent = accent,
                onClick = { onSetHifz(selection.sura, ayahs, !allMemorized) },
            )
        }
        Text(
            text = "المحفوظ في هذا المُدى: ${MushafSurahs.arabicDigits(memorizedInRange)} من ${MushafSurahs.arabicDigits(ayahs.size)}" +
                " — وضغطة على الآية تكشف نصها للاختبار",
            color = Color.White.copy(alpha = 0.5f),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp),
        )

        val content = entries
        if (content == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(26.dp),
                    color = accent,
                    strokeWidth = 2.dp,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp),
            ) {
                items(content) { (ayah, text) ->
                    val key = "${selection.sura}:$ayah"
                    val memorized = hifzMemorized.contains(key)
                    val visible = !hiddenAll || revealed.contains(ayah)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = {
                                    revealed = if (visible) {
                                        revealed - ayah
                                    } else {
                                        revealed + ayah
                                    }
                                },
                            )
                            .padding(horizontal = 18.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        // نقطة الحفظ — ضغطة تعلّم الآية محفوظة أو تمسح التعليم
                        Box(
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(
                                    if (memorized) accent else Color.White.copy(alpha = 0.09f),
                                )
                                .combinedClickable(
                                    onClick = { onSetHifz(selection.sura, listOf(ayah), !memorized) },
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (memorized) {
                                Text(
                                    text = "✓",
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(
                                text = "آية ${MushafSurahs.arabicDigits(ayah)}" +
                                    if (memorized) " · محفوظة" else "",
                                color = if (memorized) accent else Color.White.copy(alpha = 0.5f),
                                style = MaterialTheme.typography.labelSmall,
                            )
                            if (text.isNotBlank()) {
                                if (visible) {
                                    Text(
                                        text = text,
                                        color = Color.White.copy(alpha = 0.95f),
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(top = 4.dp),
                                    )
                                } else {
                                    // النص المختبئ — نقاطٌ بعرضه تقريباً تفاخر
                                    // المختبِر بأن هنا آية لا يحفظها إلا بذكرها
                                    Text(
                                        text = "•".repeat((text.length / 4).coerceIn(8, 64)),
                                        color = Color.White.copy(alpha = 0.22f),
                                        style = MaterialTheme.typography.bodyMedium,
                                        letterSpacing = 2.5.sp,
                                        modifier = Modifier.padding(top = 6.dp),
                                    )
                                }
                            }
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp)
                            .height(0.5.dp)
                            .background(Color.White.copy(alpha = 0.1f)),
                    )
                }
            }
        }
    }
}

/**
 * لوحة اختيار السورة — تنبثق عند الدخول للمصحف بتوجيه المالك: «اول ما
 * اضغط على ايقونه المصحف يظهر لي اختيار السوره يعني قائمه مرتبه بترتيم
 * السور المعتمد» + «عايزها تكون فيها اختيار عرض اجزاء القران».
 *
 * صفّا متابعة القراءة والعلامة أولاً، ثم تبويب السور والأجزاء، وحقل
 * بحثٍ بكلمة واحدة بلا تشكيل يجعل النتائج تعلو القائمتين — كل نتيجة
 * تحمل سورتها ورقم آيتها وضغطة تقفز إلى صفحتها في المصحف.
 */
@Composable
private fun SurahChooserPanel(
    rootSize: IntSize,
    bookmarkSura: Int,
    bookmarkAyah: Int,
    bookmarkPage: Int,
    lastPage: Int,
    onJump: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current

    // فهارس الاختيار — صفحات بداية السور وعدّ آياتها وأول سورة في الأجزاء
    var startPages by remember { mutableStateOf<Map<Int, Int>?>(null) }
    var surahCounts by remember { mutableStateOf<Map<Int, Int>?>(null) }
    var juzFirstSura by remember { mutableStateOf<List<Int>?>(null) }
    LaunchedEffect(Unit) {
        val pages = withContext(Dispatchers.IO) { MushafDb.surahStartPages(context) }
        val counts = withContext(Dispatchers.IO) {
            (1..114).associateWith { MushafDb.surahAyahCount(context, it) }
        }
        val juzSura = withContext(Dispatchers.IO) {
            MushafJuz.START_PAGES.map { MushafDb.firstSurahOnPage(context, it) }
        }
        startPages = pages
        surahCounts = counts
        juzFirstSura = juzSura
    }

    // تبويب القائمة: سور أو أجزاء
    var tab by remember { mutableIntStateOf(0) }

    // البحث بكلمة واحدة — كل النصوص تُقرأ مرة عند أول بحث فالباقي فوري
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<MushafSearch.Hit>?>(null) }
    var searching by remember { mutableStateOf(false) }
    LaunchedEffect(query) {
        val q = query.trim()
        if (q.isEmpty()) {
            results = null
            searching = false
            return@LaunchedEffect
        }
        delay(180)
        searching = true
        val hits = withContext(Dispatchers.IO) {
            MushafSearch.search(q, MushafDb.allAyahs(context))
        }
        results = hits
        searching = false
    }

    // قفزة نتيجة بحث — إلى أول صفحة تظهر فيها الآية
    val scope = rememberCoroutineScope()
    fun jumpToHit(hit: MushafSearch.Hit) {
        scope.launch {
            val page = withContext(Dispatchers.IO) {
                MushafDb.pageOfAyah(context, hit.sura, hit.ayah)
            }
            onJump(page)
        }
    }

    BottomSheet(
        rootSize = rootSize,
        fraction = 0.86f,
        onDismiss = onDismiss,
    ) {
        SheetHeader(
            icon = NoorIcons.Mushaf,
            accent = MaterialTheme.colorScheme.primary,
            title = "المصحف",
            subtitle = "اختر سورة أو جزءاً أو ابحث بكلمة من الآية",
            onClose = onDismiss,
        )

        // متابعة القراءة والعلامة المرجعية — قفزتان سريعتان قبل القائمة
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SheetChip(
                text = "متابعة القراءة · صفحة ${MushafSurahs.arabicDigits(lastPage)}",
                filled = true,
                accent = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
                onClick = { onJump(lastPage) },
            )
            if (bookmarkSura > 0) {
                SheetChip(
                    text = "العلامة: ${MushafSurahs.name(bookmarkSura)} ${MushafSurahs.arabicDigits(bookmarkAyah)}",
                    filled = false,
                    accent = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                    onClick = { onJump(bookmarkPage) },
                )
            }
        }

        // حقل البحث — كلمة واحدة بلا تشكيل
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            placeholder = {
                Text(
                    text = "ابحث بكلمة من الآية — بلا تشكيل",
                    color = Color.White.copy(alpha = 0.4f),
                    style = MaterialTheme.typography.bodySmall,
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "مسح البحث",
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier
                            .size(20.dp)
                            .clickable { query = "" },
                    )
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = MaterialTheme.colorScheme.primary,
            ),
            textStyle = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
        )

        // نتائج البحث تعلو القائمتين متى وُجدت كلمة مبحوثة
        when {
            results != null -> {
                val hits = results!!
                if (hits.isEmpty()) {
                    Text(
                        text = "لا آية تحمل هذه الكلمة — تأكد من كتابتها بلا تشكيل",
                        color = Color.White.copy(alpha = 0.55f),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                    )
                } else {
                    Text(
                        text = if (hits.size >= 200) {
                            "أول ٢٠٠ نتيجة — حدد بحثك بكلمة أدق"
                        } else {
                            "النتائج: ${MushafSurahs.arabicDigits(hits.size)} آية"
                        },
                        color = Color.White.copy(alpha = 0.45f),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 2.dp),
                    )
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp),
                    ) {
                        items(hits) { hit ->
                            SearchHitRow(hit = hit, accent = MaterialTheme.colorScheme.primary) {
                                jumpToHit(hit)
                            }
                        }
                    }
                }
            }
            else -> {
                // تبويب السور والأجزاء + القائمة
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                ) {
                    SegmentedTab(
                        text = "السور",
                        selected = tab == 0,
                        accent = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                        onClick = { tab = 0 },
                    )
                    SegmentedTab(
                        text = "أجزاء القرآن",
                        selected = tab == 1,
                        accent = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                        onClick = { tab = 1 },
                    )
                }

                val pages = startPages
                val juzSura = juzFirstSura
                if (pages == null || juzSura == null) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(26.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp,
                        )
                    }
                } else if (tab == 0) {
                    // السور ال١١٤ بترتيبها المعتمد
                    val counts = surahCounts
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp),
                    ) {
                        items((1..114).toList()) { sura ->
                            val start = pages[sura] ?: 1
                            val count = counts?.get(sura) ?: 0
                            SurahRow(
                                sura = sura,
                                title = MushafSurahs.name(sura),
                                detail = "${MushafSurahs.arabicDigits(count)} آية · الصفحة ${MushafSurahs.arabicDigits(start)}",
                                accent = MaterialTheme.colorScheme.primary,
                                onClick = { onJump(start) },
                            )
                        }
                    }
                } else {
                    // الأجزاء الثلاثون من المصحف المدني
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp),
                    ) {
                        items((1..MushafJuz.COUNT).toList()) { juz ->
                            val start = MushafJuz.startPage(juz)
                            val sura = juzSura.getOrNull(juz - 1) ?: 1
                            SurahRow(
                                sura = juz,
                                title = "الجزء ${MushafSurahs.arabicDigits(juz)}",
                                detail = "من سورة ${MushafSurahs.name(sura)} · الصفحة ${MushafSurahs.arabicDigits(start)}",
                                accent = MaterialTheme.colorScheme.primary,
                                onClick = { onJump(start) },
                            )
                        }
                    }
                }
            }
        }
    }
}

/** سطر نتيجة بحث — «سورة X · آية N» ونص الآية وضغطة تقفز لصفحتها */
@Composable
private fun SearchHitRow(
    hit: MushafSearch.Hit,
    accent: Color,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 8.dp),
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(accent.copy(alpha = 0.16f))
                .padding(horizontal = 10.dp, vertical = 3.dp),
        ) {
            Text(
                text = "سورة ${MushafSurahs.name(hit.sura)} · آية ${MushafSurahs.arabicDigits(hit.ayah)}",
                color = accent,
                style = MaterialTheme.typography.labelMedium,
            )
        }
        Text(
            text = hit.text,
            color = Color.White.copy(alpha = 0.85f),
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            modifier = Modifier.padding(top = 5.dp),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
                .height(0.5.dp)
                .background(Color.White.copy(alpha = 0.1f)),
        )
    }
}

/** سطر قائمة السور والأجزاء — مربع رقم وعنوان وتفصيل الصفحة */
@Composable
private fun SurahRow(
    sura: Int,
    title: String,
    detail: String,
    accent: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // مربع الرقم مائل الزوايا — يلفت كعلامة الموضع في الصفحات
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(accent.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = MushafSurahs.arabicDigits(sura),
                color = accent,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
        }
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Text(
                text = title,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = detail,
                color = Color.White.copy(alpha = 0.5f),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(top = 1.dp),
            )
        }
    }
}

/** رأس أي لوحة مصحف — أيقونة وعنوان وسطر تفصيل وإكس أعلى اليمين */
@Composable
private fun SheetHeader(
    icon: ImageVector,
    accent: Color,
    title: String,
    subtitle: String,
    onClose: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // الإكس أعلى يمين اللوحة — أول العناصر في RTL يظهر يميناً
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f))
                .clickable(onClick = onClose),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "إغلاق",
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(16.dp),
            )
        }
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accent,
            modifier = Modifier
                .padding(start = 12.dp)
                .size(22.dp),
        )
        Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
            Text(
                text = title,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = subtitle,
                color = Color.White.copy(alpha = 0.55f),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

/** رقاقة زر صغيرة في لوحات المصحف — filled يبرزها بلون الثيم */
@Composable
private fun SheetChip(
    text: String,
    filled: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (filled) accent else Color.White.copy(alpha = 0.08f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (filled) MaterialTheme.colorScheme.onPrimary else Color.White.copy(alpha = 0.85f),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
        )
    }
}

/** تبويب مقسوم — السور/الأجزاء في لوحة الاختيار */
@Composable
private fun SegmentedTab(
    text: String,
    selected: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) accent.copy(alpha = 0.2f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (selected) accent else Color.White.copy(alpha = 0.6f),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

/**
 * اللوحة الصاعدة الموحدة لكل نوافذ المصحف — عاتم وصعود نابض من الأسفل
 * ومقبض سحب، والمحتوى يمرَّر إليها كي تشترك اللوحات في عين الصعود.
 */
@Composable
private fun BottomSheet(
    rootSize: IntSize,
    fraction: Float,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    // صعود اللوحة — نابض هادئ من أسفل الشاشة
    val slide = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        slide.animateTo(1f, spring(dampingRatio = 0.8f, stiffness = 420f))
    }
    val cardH = (rootSize.height * fraction)
        .coerceAtLeast(320f)
        .coerceAtMost(rootSize.height - with(LocalDensity.current) { 90.dp.roundToPx() }.toFloat())

    Box(modifier = Modifier.fillMaxSize()) {
        // العاتم — ضغطة تقفل
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f * slide.value.coerceIn(0f, 1f)))
                .clickable(onClick = onDismiss),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(with(LocalDensity.current) { cardH.toDp() })
                .graphicsLayer { translationY = (1f - slide.value) * cardH }
                .shadow(18.dp, RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                .background(PanelBg)
                .clickable(enabled = false) { },
        ) {
            // مقبض السحب
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 10.dp)
                    .size(width = 44.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.25f)),
            )
            content()
        }
    }
}

/**
 * مشغّل التلاوة بقائمة آيات متتابعة — توجيه المالك: «امكانيه تحديد عده
 * ايات مع بعض لتشغيلها» — آية وراء آية حتى آخر المُدى المختار، وواحد فقط
 * في آن، ويقف مع مغادرة المصحف أو الضغطة على زر الإيقاف — وفي الحفظ
 * الميسر يدور على المُدى من جديد حتى يقف المستخدم (loop).
 */
private object MushafPlayer {

    private val _playing = MutableStateFlow(false)
    val playing: StateFlow<Boolean> = _playing

    @Volatile
    private var player: MediaPlayer? = null

    /** قائمة روابط الآيات وما بقي منها */
    @Volatile
    private var queue: List<String> = emptyList()
    @Volatile
    private var queueIndex = 0

    /** دور الحفظ الميسر — بعد آخر آية تعود الدورة من أول المُدى */
    @Volatile
    private var loop = false

    /** تشغيل قائمة روابط آيات بالتتابع — يرجع نجاح بدء الأولى */
    fun play(urls: List<String>, loop: Boolean = false): Boolean {
        stop()
        if (urls.isEmpty()) return false
        queue = urls
        queueIndex = 0
        this.loop = loop
        return startCurrent()
    }

    /** تحضير الآية الحالية وتشغيلها — فشل التحضير يوقف الكل برسالة صاحبها */
    private fun startCurrent(): Boolean = runCatching {
        val url = queue[queueIndex]
        val p = MediaPlayer()
        p.setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build(),
        )
        p.setDataSource(url)
        p.setOnPreparedListener {
            it.start()
            _playing.value = true
        }
        p.setOnCompletionListener { done ->
            runCatching { done.release() }
            player = null
            val next = queueIndex + 1
            if (next < queue.size) {
                queueIndex = next
                if (!startCurrent()) _playing.value = false
            } else if (loop && queue.isNotEmpty()) {
                // الحفظ الميسر — الدورة تعود من أول المُدى حتى يقف المستخدم
                queueIndex = 0
                if (!startCurrent()) _playing.value = false
            } else {
                _playing.value = false
                queue = emptyList()
                queueIndex = 0
            }
        }
        p.setOnErrorListener { _, _, _ ->
            _playing.value = false
            true
        }
        p.prepareAsync()
        player = p
        true
    }.getOrDefault(false)

    fun stop() {
        player?.let { p ->
            runCatching {
                if (p.isPlaying) p.stop()
                p.release()
            }
        }
        player = null
        _playing.value = false
        queue = emptyList()
        queueIndex = 0
        loop = false
    }
}

/** سياق التطبيق → النشاط الحامل — لطيّ شريط الحالة داخل المصحف */
private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
