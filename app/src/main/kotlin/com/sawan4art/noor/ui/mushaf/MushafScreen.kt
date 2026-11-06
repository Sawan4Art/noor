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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
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
import com.sawan4art.noor.domain.mushaf.MushafSegment
import com.sawan4art.noor.domain.mushaf.MushafSelection
import com.sawan4art.noor.domain.mushaf.MushafSelectionOps
import com.sawan4art.noor.domain.mushaf.MushafSurahs
import com.sawan4art.noor.domain.mushaf.MushafTransform
import com.sawan4art.noor.ui.icons.NoorIcons
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/**
 * المصحف — صفحات طبعة التجويد الملون بكامل الشاشة وتفاعل آيات (alpha51).
 *
 * توجيه المالك ٢٠٢٦-١١-٠٦: «شيل زر الرجوع من المصحف و خلي صفحه المصحف
 * بكامل الشاشه و اخرها لاقى حد قبل شريط التنقل» + «هديك صوره لازرار
 * تفاعليه تظهر عند الضغط المطول و عايز المصحف يكون تفاعلي و يكون الايات
 * قابله للتحديد مع مراعاه تحديد ايات و ترك ايات في المنتصف اذا حدد
 * المستخدم مثلا ايه رقم ٥٠ و جيه يدوس على ايه ٥٥ يبقى اوتماتك الايات
 * التي في المنتصف تحدد» + «في جزء في الصفحات جانبي في تفاسير شيل الجزء
 * ده من كل الصفحات و ابقي فقط على اطار الصفحه المزغرف مع طبعا الجزء
 * السفلي الذي ينبه معنى الالوان»:
 *
 * • بلا زر رجوع: إيماءة الرجوع النظامية تعود للرئيسية، والتحديد النشيط
 *   يأكل الضغطة الأولى فمستهلكان لا يتصادمان.
 * • كامل الشاشة: شريط الحالة مستخفى أثناء قراءة المصحف ويظهر إن غادرت،
 *   وقلب الصفحة يقف قبل شريط التنقل السائل (٩٤dp أسفل الشاشة: هامش ١٨ +
 *   ارتفاع ٧٦ كما ضبطهما المالك في alpha45) فلا يمر الزجاج فوق الآيات.
 * • قصّ التفاسير الجانبية من كل الصفحات (١..٦٠٤) وتبقى الطبعة بعنوانها:
 *   الإطار المزخرف وشريط دليل الألوان السفلي عينهما، والزوايا العلوية
 *   تُنظَّف بلون ورق الطبعة نفسه فلا بقايا مقصوصة.
 * • الآيات قابلة للتحديد بإحداثيات الطبعة نفسها (قاعدة glyphs المرافقة
 *   للصور): ضغط مطوّل يحدّد آية ويفتح المنغّلة البرتقالية حول الإصبع،
 *   وضغطة تالية على آية أخرى تمدّ التحديد لكل الآيات بينهما آلياً،
 *   والإيماءة الرجوع تُلغي التحديد قبل مغادرة المصحف.
 * • المنغّلة البرتقالية: تلاوة (العفاسي أونلاين) • علامة مرجعية • نسخ
 *   • مشاركة • إغلاق — أفعالها على كامل المُدى المحدَّد.
 */

/** المساحة المحجوزة أسفل الشاشة قبل شريط التنقل السائل (١٨+٧٦dp — alpha45) */
private val NAV_BAR_CLEAR = 94.dp

/** برتقالي المنغّلة — عين برتقال الأزرار في صورة المالك */
private val MenuOrange = Color(0xFFF57C00)

/** لون إبراز التحديد — بنفسجي شفاف كما في صورة المالك */
private val SelectionPurple = Color(0xFF7C4DFF).copy(alpha = 0.45f)

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

    // التحديد والمنغّلة — يعيشان فوق الصفحات فقلب الصفحة لا يفقد المُدى
    var selection by remember { mutableStateOf<MushafSelection?>(null) }
    var menuAt by remember { mutableStateOf<Offset?>(null) }
    var rootSize by remember { mutableStateOf(IntSize.Zero) }

    // الإيماءة الرجوع تأكل التحديد أولاً ثم تعود للرئيسية (لا زر ظاهر)
    BackHandler(enabled = selection != null) {
        selection = null
        menuAt = null
    }

    // حفظ آخر صفحة عند وصول القارئ إليها — المصحف يظل مكمنه الذي تركه
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { page ->
                onSavePage(page + 1)
                menuAt = null // قلب الصفحة يُخفي المنغّلة والتحديد يبقى للمُدى
            }
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onSizeChanged { rootSize = it },
    ) {
        // مساحة القراءة — تقف قبل شريط التنقل السائل فلا يمر الزجاج فوق الآيات
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
                    menuAt = clampMenu(at)
                },
                onTapAyah = { ayah, at ->
                    selection = MushafSelectionOps.extend(selection, ayah)
                    menuAt = clampMenu(at)
                },
            )
        }

        // المنغّلة البرتقالية حول الإصبع — أفعالها على كامل المُدى المحدَّد
        val at = menuAt
        if (at != null) {
            val sel = selection
            if (sel != null) {
                AyahMenuCluster(
                    offset = at,
                    playing = playing,
                    bookmarkedHere = bookmarkSura == sel.sura &&
                        bookmarkAyah == MushafSelectionOps.firstOf(sel).ayah,
                    onPlay = {
                        val first = MushafSelectionOps.firstOf(sel)
                        scope.launch {
                            if (MushafPlayer.playing.value) {
                                MushafPlayer.stop()
                            } else {
                                val global = withContext(Dispatchers.IO) {
                                    MushafDb.globalAyah(context, first.sura, first.ayah)
                                }
                                val url = "https://cdn.islamic.network/quran/audio/128/ar.alafasy/$global.mp3"
                                val ok = withContext(Dispatchers.IO) { MushafPlayer.play(url) }
                                if (!ok) {
                                    Toast.makeText(
                                        context,
                                        "التلاوة تحتاج اتصالاً بالإنترنت",
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                }
                            }
                        }
                    },
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
 * صفحة واحدة من المصحف — صورة الطبعة المقصوصة (بلا تفاسير جانبية) مع
 * إبراز الآيات المحدَّدة وضغطها: التسلسل الأساسي ← الاحتياطي بصمت ←
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
 * العلوية بلون الورق ثم إبراز المُدى المحدَّد بشريط بنفسجي على أسطره.
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
 * المنغّلة البرتقالية حول الإصبع — خمس دوائر برتقالية بأيقونات بيضاء:
 * أربعة أفعال في الأطراف (علامة أعلى، تلاوة يميناً، نسخ أسفل، مشاركة
 * يساراً) وإغلاق في القلب فوق نقطة الضغط، كما في صورة المالك.
 */
@Composable
private fun AyahMenuCluster(
    offset: Offset,
    playing: Boolean,
    bookmarkedHere: Boolean,
    onPlay: () -> Unit,
    onBookmark: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onClose: () -> Unit,
) {
    val density = LocalDensity.current
    val itemPx = with(density) { 46.dp.roundToPx() }
    val radiusPx = with(density) { 52.dp.roundToPx() }

    val actions = listOf(
        Triple(0f, -1f, MenuAction("علامة مرجعية", if (bookmarkedHere) NoorIcons.QuranBookmark else NoorIcons.QuranBookmarkOutline, onBookmark)),
        Triple(1f, 0f, MenuAction("تلاوة", if (playing) NoorIcons.QuranStop else Icons.Filled.PlayArrow, onPlay)),
        Triple(0f, 1f, MenuAction("نسخ", NoorIcons.QuranCopy, onCopy)),
        Triple(-1f, 0f, MenuAction("مشاركة", Icons.Filled.Share, onShare)),
    )

    Box(modifier = Modifier.offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }) {
        actions.forEach { (fx, fy, action) ->
            MenuButton(
                icon = action.icon,
                contentDescription = action.label,
                offsetPx = IntOffset(
                    (fx * radiusPx).roundToInt() - itemPx / 2,
                    (fy * radiusPx).roundToInt() - itemPx / 2,
                ),
                onClick = action.onClick,
            )
        }
        // الإغلاق في قلب المنغّلة — فوق نقطة الضغط عينها
        MenuButton(
            icon = Icons.Filled.Close,
            contentDescription = "إغلاق",
            offsetPx = IntOffset(-itemPx / 2, -itemPx / 2),
            onClick = onClose,
        )
    }
}

/** دائرة المنغّلة الواحدة — برتقالية ظليلة بأيقونة بيضاء */
@Composable
private fun MenuButton(
    icon: ImageVector,
    contentDescription: String,
    offsetPx: IntOffset,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .offset { offsetPx }
            .size(46.dp)
            .shadow(8.dp, CircleShape)
            .clip(CircleShape)
            .background(MenuOrange)
            .clickable(onClick = onClick),
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

/** مشغّل تلاوة الآية — واحد فقط في آن، ويقف عند مغادرة المصحف */
private object MushafPlayer {

    private val _playing = MutableStateFlow(false)
    val playing: StateFlow<Boolean> = _playing

    @Volatile
    private var player: MediaPlayer? = null

    /** يشغّل رابط الآية ويرجع نجاح بدئه — الفشل يعرض رسالة الاتصال */
    fun play(url: String): Boolean = runCatching {
        stop()
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
        p.setOnCompletionListener {
            _playing.value = false
            runCatching { it.release() }
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
    }
}

/** سياق التطبيق → النشاط الحامل — لطيّ شريط الحالة داخل المصحف */
private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
