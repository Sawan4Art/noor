package com.sawan4art.noor.ui.mushaf

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.sawan4art.noor.data.mushaf.MushafDb
import com.sawan4art.noor.domain.mushaf.MushafAyah
import com.sawan4art.noor.domain.mushaf.MushafGeometry
import com.sawan4art.noor.domain.mushaf.MushafPages
import com.sawan4art.noor.domain.mushaf.MushafPlaylist
import com.sawan4art.noor.domain.mushaf.MushafSegment
import com.sawan4art.noor.domain.mushaf.MushafSelection
import com.sawan4art.noor.domain.mushaf.MushafSelectionOps
import com.sawan4art.noor.domain.mushaf.MushafSurahs
import com.sawan4art.noor.domain.mushaf.MushafTransform
import com.sawan4art.noor.ui.icons.NoorIcons
import com.sawan4art.noor.ui.nav.GooeyMath
import com.sawan4art.noor.ui.nav.addBridgePath
import com.sawan4art.noor.ui.nav.addLiquidBlob
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * المصحف — صفحات طبعة التجويد الملون المتسعة بكامل الشاشة وتفاعل آيات
 * سائل حي (alpha52).
 *
 * توجيه المالك ٢٠٢٦-١١-٠٦ بعد alpha51: «صفحه المصحف نفسها في فراغ فوقها و
 * تحتها يعني عايز الصفحه تتسع لكامل الشاشه من اولها من فوق الى اخر حدود شريط
 * التنقل» و «عايز امكانيه تحديد عده ايات مع بعض لتشغيلها» و «عايز لما ادوس
 * على زر تشغيل الصوت يظهر قائمه اختار منها تشغيل السوره كامله من بدايه
 * التحديد او تشغيل المحدد فقط او تشغيل السوره كامله من بدايتها بغض النظر عن
 * التحديد» و «عايز استايل الايقونات الدائريه المنبثقه تكون بشكل سائل حي زي
 * باقي الموجود في التطبيق و عايز ايقونه جديده تظهر مع الايقونات المنبثقه
 * تكون خاصه بالتفاسير»:
 *
 * • الصفحة تتسع على كل محور على حدة فتملأ المساحة من أول الشاشة فوق إلى
 *   آخر حدود شريط التنقل السائل بلا أي فراغ — عين ما تفعل تطبيقات المصحف
 *   المرجعية، ومحاذاة الضغط والإبراز تظل مليمترية رغم الامتداد.
 * • المُدى المحدد يُتشغَّل كله متتابعاً آية وراء آية، وزر التلاوة يفتح
 *   قائمة الاختيارات الثلاثة: المحدد فقط، السورة من بداية التحديد،
 *   السورة كاملة من بدايتها بغض النظر عن التحديد.
 * • المنغّلة البرتقالية صارت سائلة حية من نفس عائلة شريط التنقل: ست
 *   قطرات متموجة بتموج GooeyMath وجسورها الموصولة بلا فصل، تقفز من نقطة
 *   الضغط بنابض وترفرف في الثبات ذاته، وفي قلبها زر التفسير الجديد
 *   (كتاب مفتوح مرسوم بنمط التطبيق).
 * • زر التفسير يفتح لوحة التفسير الميسر كاملاً (بلا إنترنت — مرفقة
 *   بقاعدة المصحف) لكل آيات المُدى المحدد نصاً وتفسيراً.
 */

/** المساحة المحجوزة أسفل الشاشة قبل شريط التنقل السائل (١٨+٧٦dp — alpha45) */
private val NAV_BAR_CLEAR = 94.dp

/** برتقالي المنغّلة السائلة — عين برتقال الأزرار في صورة المالك */
private val MenuOrange = Color(0xFFF57C00)

/** خلفية بطاقتَي التلاوة والتفسير — دافئة داكنة تليق فوق ورق الطبعة */
private val PanelBg = Color(0xFF1E1814)

/** لون إبراز التحديد — بنفسجي شفاف كما في صورة المالك */
private val SelectionPurple = Color(0xFF7C4DFF).copy(alpha = 0.45f)

/** هندسة المنغّلة السائلة — حلقة خماسية حول قلب الإغلاق */
private val RING_RADIUS = 56.dp
private val ITEM_SIZE = 46.dp
private val CLUSTER_SIZE = 172.dp

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
    /** حفظ آخر صفحة وصل إليها القارئ */
    onSavePage: (Int) -> Unit,
    /** حفظ علامة مرجعية أو مسحها — سورة صفر للمسح */
    onSetBookmark: (Int, Int, Int) -> Unit,
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

    // التحديد والمنغّلة وقائمتها ولوحة التفسير — فوق الصفحات فقلب الصفحة لا يفقد المُدى
    var selection by remember { mutableStateOf<MushafSelection?>(null) }
    var menuAt by remember { mutableStateOf<Offset?>(null) }
    var playMenuOpen by remember { mutableStateOf(false) }
    var tafsirOpen by remember { mutableStateOf(false) }
    var rootSize by remember { mutableStateOf(IntSize.Zero) }
    var surahCount by remember { mutableStateOf(0) }

    // الإيماءة الرجوع: تُغلق لوحة التفسير أولاً ثم تلغي التحديد ثم تعود للرئيسية
    BackHandler(enabled = tafsirOpen || selection != null) {
        if (tafsirOpen) {
            tafsirOpen = false
        } else {
            selection = null
            menuAt = null
            playMenuOpen = false
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

    fun clampMenu(pos: Offset): Offset {
        val w = rootSize.width.toFloat()
        val h = rootSize.height.toFloat()
        return Offset(
            pos.x.coerceIn(96f, (w - 96f).coerceAtLeast(97f)),
            pos.y.coerceIn(160f, (h - 160f).coerceAtLeast(161f)),
        )
    }

    /** تشغيل مُدى آيات متتابع — رابط كل آية من ترتيبها العالمي */
    fun startPlaylist(sura: Int, ayahs: List<Int>) {
        scope.launch {
            val urls = withContext(Dispatchers.IO) {
                ayahs.map { ayah ->
                    val global = MushafDb.globalAyah(context, sura, ayah)
                    "https://cdn.islamic.network/quran/audio/128/ar.alafasy/$global.mp3"
                }
            }
            val ok = withContext(Dispatchers.IO) { MushafPlayer.play(urls) }
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
                        onCancel = { playMenuOpen = false },
                    )
                } else {
                    AyahMenuCluster(
                        offset = at,
                        playing = playing,
                        bookmarkedHere = bookmarkSura == sel.sura &&
                            bookmarkAyah == MushafSelectionOps.firstOf(sel).ayah,
                        onPlay = { playMenuOpen = true },
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
 * صفحة واحدة من المصحف — صورة الطبعة المقصوصة المتسعة بكامل مساحة القراءة
 * مع إبراز الآيات المحدَّدة وضغطها: التسلسل الأساسي ← الاحتياطي بصمت ←
 * رسالة إعادة المحاولة، وشرائح الآيات من قاعدة الطبعة المصاحبة للصور.
 */
@Composable
private fun MushafPage(
    page: Int,
    selection: MushafSelection?,
    onLongPressAyah: (MushafAyah, Offset) -> Unit,
    onTapAyah: (MushafAyah, Offset) -> Unit,
) {
    val context = LocalContext.current

    // الصورة — ٠ = الرابط الأساسي، ١ = الاحتياطي، ٢+ = إعادة محاولة بالتناوب
    var attempt by remember(page) { mutableIntStateOf(0) }
    var bitmap by remember(page) { mutableStateOf<ImageBitmap?>(null) }
    var loading by remember(page) { mutableStateOf(true) }
    var failed by remember(page) { mutableStateOf(false) }

    LaunchedEffect(page, attempt) {
        loading = true
        failed = false
        val url = if (attempt % 2 == 0) MushafPages.primaryUrl(page) else MushafPages.fallbackUrl(page)
        val request = ImageRequest.Builder(context)
            .data(url)
            // صورة قابلة للرسم داخل كانفس القص — لا عتاد إذن
            .allowHardware(false)
            // معامل المحاولة يفصل مفتاح الطلب فتعيد الضغطة الشبكة فعلاً
            .setParameter("attempt", attempt)
            .build()
        val bmp = runCatching {
            (context.imageLoader.execute(request) as? SuccessResult)
                ?.drawable as? BitmapDrawable
        }.getOrNull()?.bitmap?.asImageBitmap()
        when {
            bmp != null -> {
                bitmap = bmp
                loading = false
            }
            attempt == 0 -> attempt = 1 // فشل الأساسي → الاحتياطي فوراً بصمت
            else -> {
                failed = true
                loading = false
            }
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

        if (loading && !failed) {
            CircularProgressIndicator(
                modifier = Modifier.size(26.dp),
                color = Color.White.copy(alpha = 0.75f),
                strokeWidth = 2.dp,
            )
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
                    text = "تعذّر تحميل الصفحة $page",
                    color = Color.White.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "تأكد من الاتصال ثم اضغط للمحاولة من جديد",
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
 * المنغّلة السائلة الحية — من نفس عائلة شريط التنقل السائل: ست قطرات
 * برتقالية متموجة سطحها بتموج GooeyMath، وجسور موصولة من القلب لكل قطرة
 * بأي زاوية (إطار مُدار حول القلب فيدور الجسر معها)، تقفز من نقطة الضغط
 * بنابض زائد وترفرف في الثبات ذاته — والقطرات نفسها هي مقابض الضغط
 * فالأيقونات عائمة فوق سائل يعيش.
 *
 * الحلقة خماسية: علامة أعلى، تلاوة يميناً، تفسير أسفل اليمين (الجديد)،
 * نسخ أسفل اليسار، مشاركة يساراً — وإغلاق في القلب فوق نقطة الضغط.
 */
@Composable
private fun AyahMenuCluster(
    offset: Offset,
    playing: Boolean,
    bookmarkedHere: Boolean,
    onPlay: () -> Unit,
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

    val actions = listOf(
        -90f to MenuAction("علامة مرجعية", if (bookmarkedHere) NoorIcons.QuranBookmark else NoorIcons.QuranBookmarkOutline, onBookmark),
        -18f to MenuAction("تلاوة", if (playing) NoorIcons.QuranStop else Icons.Filled.PlayArrow, onPlay),
        54f to MenuAction("تفسير", NoorIcons.QuranTafsir, onTafsir),
        126f to MenuAction("نسخ", NoorIcons.QuranCopy, onCopy),
        198f to MenuAction("مشاركة", Icons.Filled.Share, onShare),
    )

    Box(modifier = Modifier.offset {
        IntOffset((offset.x - half).roundToInt(), (offset.y - half).roundToInt())
    }) {
        // السائل الموحد — مسار واحد لكل القطرات والجسور فتتحد بلا فواصل
        Canvas(modifier = Modifier.size(CLUSTER_SIZE)) {
            if (ap <= 0.02f) return@Canvas
            val centerR = itemPx / 2f * grow
            val liquid = Path()
            addLiquidBlob(liquid, half, half, centerR, timeSec, 0.32f, 0f)
            val drops = actions.mapIndexed { i, (deg, _) ->
                val rad = deg * PI.toFloat() / 180f
                val cx = half + ringPx * grow * cos(rad)
                val cy = half + ringPx * grow * sin(rad)
                val wave = 1f + GooeyMath.surfaceWave(rad, timeSec, 0.3f, i * 0.9f)
                addLiquidBlob(liquid, cx, cy, itemPx / 2f * grow * wave, timeSec, 0.26f, i * 0.9f)
                Triple(cx, cy, wave)
            }
            // جسور السائل من القلب لكل قطرة — بأي زاوية: الإطار يدور حول
            // القلب فيصير الخط أفقياً فيعمل جسر GooeyMath ثم يعود بالدوران
            drops.forEach { (cx, cy, _) ->
                val dx = cx - half
                val dy = cy - half
                val d = sqrt(dx * dx + dy * dy)
                if (d > 1f) {
                    rotate(atan2(dy, dx) * 180f / PI.toFloat(), pivot = Offset(half, half)) {
                        val bridge = GooeyMath.bridge(
                            half, half, centerR,
                            half + d, half, itemPx / 2f * grow,
                            waist = 0.45f,
                        )
                        if (bridge != null) {
                            val sway = itemPx * 0.05f * GooeyMath.bridgeSway(timeSec, dx * 0.013f)
                            bridge.qA1y += sway
                            bridge.qA2y += sway * 0.55f
                            bridge.qB1y -= sway * 0.85f
                            bridge.qB2y -= sway * 0.5f
                            addBridgePath(liquid, bridge)
                        }
                    }
                }
            }
            drawPath(liquid, MenuOrange)
        }

        // الأيقونات عائمة فوق القطرات — نفس المواضع والتموج بعينه
        actions.forEachIndexed { i, (deg, action) ->
            val rad = deg * PI.toFloat() / 180f
            val cx = half + ringPx * grow * cos(rad)
            val cy = half + ringPx * grow * sin(rad)
            MenuButton(
                icon = action.icon,
                contentDescription = action.label,
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
 * الكانفس هو الدائرة المرئية، وهنا أيقونة بيضاء عائمة تظهر مع النمو.
 */
@Composable
private fun MenuButton(
    icon: ImageVector,
    contentDescription: String,
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
            tint = Color.White,
            modifier = Modifier.size(22.dp),
        )
    }
}

/**
 * قائمة التلاوة الثلاثية — توجيه المالك الحرفي: «تشغيل السوره كامله من
 * بدايه التحديد او تشغيل المحدد فقط او تشغيل السوره كامله من بدايتها بغض
 * النظر عن التحديد» — بطاقة دافئة تنبثق مكان المنغّلة بسنام نابض، وكل
 * سطر يحمل عنوانه ومتى يبدأ وينتهي بعينه فلا التباس.
 */
@Composable
private fun PlayOptionsCard(
    offset: Offset,
    rootSize: IntSize,
    selection: MushafSelection,
    surahCount: Int,
    onPlay: (List<Int>) -> Unit,
    onCancel: () -> Unit,
) {
    val density = LocalDensity.current
    val cardW = with(density) { 252.dp.roundToPx() }
    val cardH = with(density) { 236.dp.roundToPx() }
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
            Text(
                text = "التلاوة · ${MushafSurahs.name(selection.sura)}",
                color = Color.White.copy(alpha = 0.55f),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            PlayOptionRow(
                icon = Icons.Filled.PlayArrow,
                title = "تشغيل المحدد فقط",
                subtitle = span,
            ) { onPlay(MushafPlaylist.selected(selection)) }
            PlayOptionRow(
                icon = Icons.Filled.PlayArrow,
                title = "تشغيل السورة من بداية التحديد",
                subtitle = "من آية ${MushafSurahs.arabicDigits(first)} إلى آية ${MushafSurahs.arabicDigits(surahCount)}",
            ) { onPlay(MushafPlaylist.surahFromSelection(selection, surahCount)) }
            PlayOptionRow(
                icon = Icons.Filled.PlayArrow,
                title = "تشغيل السورة كاملة من بدايتها",
                subtitle = "الآيات ١–${MushafSurahs.arabicDigits(surahCount)} بغض النظر عن التحديد",
            ) { onPlay(MushafPlaylist.surahFull(surahCount)) }
            PlayOptionRow(
                icon = Icons.Filled.Close,
                title = "رجوع إلى المنغّلة",
                subtitle = null,
            ) { onCancel() }
        }
    }
}

/** سطر خيار واحد في بطاقة التلاوة — أيقونة برتقالية وعنوان ووصفه */
@Composable
private fun PlayOptionRow(
    icon: ImageVector,
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
            tint = MenuOrange,
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

    // صعود اللوحة — نابض هادئ من أسفل الشاشة
    val slide = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        slide.animateTo(1f, spring(dampingRatio = 0.8f, stiffness = 420f))
    }
    val cardH = (rootSize.height * 0.72f).coerceAtLeast(320f)
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
            // مقبض السحب + الترويسة
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 10.dp)
                    .size(width = 44.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.25f)),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = NoorIcons.QuranTafsir,
                    contentDescription = null,
                    tint = MenuOrange,
                    modifier = Modifier.size(22.dp),
                )
                Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
                    Text(
                        text = "التفسير الميسر",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = MushafSurahs.caption(selection),
                        color = Color.White.copy(alpha = 0.55f),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "إغلاق التفسير",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            val content = entries
            if (content == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(26.dp),
                        color = MenuOrange,
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
                                        .background(MenuOrange.copy(alpha = 0.16f))
                                        .padding(horizontal = 10.dp, vertical = 4.dp),
                                ) {
                                    Text(
                                        text = "آية ${MushafSurahs.arabicDigits(ayah)}",
                                        color = MenuOrange,
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
}

/**
 * مشغّل التلاوة بقائمة آيات متتابعة — توجيه المالك: «امكانيه تحديد عده
 * ايات مع بعض لتشغيلها» — آية وراء آية حتى آخر المُدى المختار، وواحد فقط
 * في آن، ويقف مع مغادرة المصحف أو الضغطة على زر الإيقاف.
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

    /** تشغيل قائمة روابط آيات بالتتابع — يرجع نجاح بدء الأولى */
    fun play(urls: List<String>): Boolean {
        stop()
        if (urls.isEmpty()) return false
        queue = urls
        queueIndex = 0
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
    }
}

/** سياق التطبيق → النشاط الحامل — لطيّ شريط الحالة داخل المصحف */
private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
