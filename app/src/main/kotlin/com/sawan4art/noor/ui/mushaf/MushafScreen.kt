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
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
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
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.window.Dialog
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
import com.sawan4art.noor.data.mushaf.AyahAudioCache
import com.sawan4art.noor.data.mushaf.AyahTimings
import com.sawan4art.noor.data.mushaf.MushafDb
import com.sawan4art.noor.data.mushaf.QuranAudioLibrary
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
import com.sawan4art.noor.domain.mushaf.MushafZoomMath
import com.sawan4art.noor.domain.mushaf.QuranReciters
import com.sawan4art.noor.ui.icons.NoorIcons
import com.sawan4art.noor.ui.nav.GooeyMath
import com.sawan4art.noor.ui.nav.addLiquidBlob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.isActive
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
 *
 * توجيه المالك ٢٠٢٦-١١-٠٦ بعد alpha53 — ست وصايا جديدة:
 *
 * 9) «عايز تحديد الايات لا يقتصر على ايه واحده فقط عايز يبقى في
 *    امكانيه تحديد عده ايات مع بعضها» — الضغط المطوّل ثم السحب على
 *    الأسطر يمدّ المُدى حيّاً آيةً وراء آية بإيماءة واحدة، والنقر على
 *    آيةٍ أخرى يمدّه، والمنغّلة تظهر على نهاية الإيماءة.
 * 10) «شكل التحديد بدل ما هو بنفسجي خليه بالوان تتماشى مع الوان
 *     الثيم و يكون شفاف لدرجه تسمح برؤيه النص المحدد» — الإبراز
 *     من لون الثيم الأساسي بشفافية تُبقي النص مقروءاً.
 * 11) «اضف امكانيه عمل زوم في صفحات المصحف» — القرص بإصبعين يكبّر
 *     حتى ٤٫٥×حول مركز القرص، والنقرة المزدوجة تقلب بين ١× و٢٫٥×،
 *     والإصبع الواحد عند التكبير يحرّك الصفحة (التنقل موقوف) ويعود
 *     عند العودة إلى ١×.
 * 12) «قائمه اختيار السوره والجزء اخر جزء منها تحت شريط التنقل…
 *     خلي حدود القائمه مع اخر حدود شريط التنقل» — كل اللوحات
 *     الصاعدة رُفعت حدّها السفلي ليقف على آخر شريط التنقل فلا
 *     يبقى سطرٌ مخفي تحتَه.
 * 13) «اضف عشرين مقرء يتم تحميل اصواتهم من قبل المستخدم نفسه…» —
 *     التلاوة صارت بصوت الشيخ المختار من قسم «أصوات الشيخ» في
 *     الإعدادات: الملف المحلي المنزَّل يُشغَّل أولاً والغائب يُبثّ
 *     من الشبكة بالمعرّف نفسه.
 */

/** المساحة المحجوزة أسفل الشاشة قبل شريط التنقل السائل (١٨+٧٦dp — alpha45) */
private val NAV_BAR_CLEAR = 94.dp

/**
 * خلفية بطاقات المصحف — من الثيم نفسه (توجيه المالك alpha60: «خلي شكل
 * قائمه اختيار السوره في المصحف بنفس شكل و تصميم متماشي مع التطبيق
 * و بنفس الالوان الثيم») فاللوحات تتبع الثيم الفاتح والمظلم وألوانه
 * المختارة أينما تغيّرت.
 */

/**
 * شفافية إبراز التحديد — توجيه المالك: «بدل ما هو بنفسجي خليه بالوان
 * تتماشى مع الوان الثيم و يكون شفاف لدرجه تسمح برؤيه النص المحدد» —
 * اللون الآن من لون الثيم الأساسي نفسه، والشفافية تُبقي النص مقروءًا
 * تحته، فالإبراز تابعةٌ للثيم المختار أينما تغيّر.
 */
private const val SELECTION_ALPHA = 0.32f

/**
 * حالة تكبير صفحات المصحف — معامل وإزاحة محصورة بحدود [MushafZoomMath]،
 * واحدة لكل المصحف (صفحةٌ ظاهرةٌ واحدة) وتُصفَّر عند قلب الصفحة.
 */
internal class MushafZoomState {
    var scale by androidx.compose.runtime.mutableFloatStateOf(1f)
    var offX by androidx.compose.runtime.mutableFloatStateOf(0f)
    var offY by androidx.compose.runtime.mutableFloatStateOf(0f)

    /** هل الصفحة مكبَّرة الآن؟ (وإذن التنقل موقوف والسحب يحرّك الصفحة) */
    val zoomed: Boolean get() = MushafZoomMath.isZoomed(scale)

    fun set(s: Float, x: Float, y: Float) {
        scale = s
        offX = x
        offY = y
    }

    fun reset() {
        scale = 1f
        offX = 0f
        offY = 0f
    }
}

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
    /** معرّف الشيخ المختار من قسم «أصوات الشيخ» (alpha54) */
    reciterId: String,
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

    // زوم الصفحات — حالة واحدة لكل المصحف وتُصفَّر عند قلب الصفحة (alpha54)
    val zoom = remember { MushafZoomState() }

    // لون إبراز التحديد من الثيم — يتتبع اللون المختار أينما تغيّر
    val selectionColor = MaterialTheme.colorScheme.primary.copy(alpha = SELECTION_ALPHA)

    // لوحة اختيار السورة تنبثق عند الدخول — توجيه المالك الحرفي: «اول ما
    // اضغط على ايقونه المصحف يظهر لي اختيار السوره»
    var chooserOpen by remember { mutableStateOf(true) }

    // التحديد والمنغّلة وقائمتا التلاوة والحفظ ولوحة التفسير — فوق الصفحات.
    // **التحديد لا يُحذف أبداً** (بلاغ المالك 2026-11-11: «عند التحديد على
    // ايه في المصحف لا يتم حذف التحديد ابدا») — فحالته محفوظةٌ عبر التنقل
    // بين التبويبات والصفحات كي لا يفقد المستخدم تحديده إلا بإزالته بنفسه
    var selection by rememberSaveable(
        stateSaver = listSaver<MushafSelection?, Any>(
            save = { sel ->
                if (sel == null) {
                    emptyList()
                } else {
                    mutableListOf<Any>().apply { add(sel.sura); addAll(sel.ayahs) }
                }
            },
            restore = { list ->
                if (list.size < 2) {
                    null
                } else {
                    MushafSelection(
                        (list[0] as Int),
                        list.drop(1).map { it as Int },
                    )
                }
            },
        ),
    ) { mutableStateOf<MushafSelection?>(null) }
    var menuAt by remember { mutableStateOf<Offset?>(null) }
    var playMenuOpen by remember { mutableStateOf(false) }
    var tafsirOpen by remember { mutableStateOf(false) }
    var hifzOpen by remember { mutableStateOf(false) }
    var rootSize by remember { mutableStateOf(IntSize.Zero) }
    var surahCount by remember { mutableStateOf(0) }

    // **النظام الذكي (بلاغ المالك ٢٠٢٦-١١-١٢)**: التلاوة بلا حوارَ
    // تنزيلٍ ولا انتظارِ سورةٍ كاملة — الآية المطلوبة تُنزَّل وحدها
    // وتُشغَّل فور جاهزيتها، والبقية تجلب للذاكرة خلفيةً أثناء
    // التلاوة، وما حاضرٌ يعمل لحظياً بلا إنترنت
    val playing by MushafPlayer.playing.collectAsState()
    val playState by MushafPlayer.state.collectAsState()

    // عطل التجهيز يُعلن هادئاً — والتشغيل التالي يمسحه
    LaunchedEffect(playState.error) {
        if (playState.error.isNotBlank()) {
            Toast.makeText(context, playState.error, Toast.LENGTH_LONG).show()
        }
    }

    // الإيماءة الرجوع: تُغلق اللوحات أولاً ثم تلغي التحديد ثم تفتح قائمة
    // السور ثم تعود للرئيسية — الرجوع خطوةً خطوة (توجيه المالك alpha56:
    // «عايز لما ارجع خطوه للوراء يتم عرض قائمه اختيار السوره مره اخرى»)
    BackHandler(enabled = true) {
        when {
            tafsirOpen -> tafsirOpen = false
            hifzOpen -> hifzOpen = false
            selection != null || menuAt != null || playMenuOpen -> {
                selection = null
                menuAt = null
                playMenuOpen = false
            }
            chooserOpen -> {
                chooserOpen = false
                onBack()
            }
            else -> chooserOpen = true
        }
    }

    // حفظ آخر صفحة عند وصول القارئ إليها — المصحف يظل مكمنه الذي تركه
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { page ->
                onSavePage(page + 1)
                zoom.reset() // قلب الصفحة يعيد الزوم للعرض الطبيعي
                menuAt = null // قلب الصفحة يُخفي المنغّلة والتحديد يبقى للمُدى
                playMenuOpen = false
            }
    }

    // عدّ آيات السورة المحددة — لعناوين خيارات التلاوة الثلاثة
    LaunchedEffect(selection?.sura) {
        val sura = selection?.sura ?: return@LaunchedEffect
        surahCount = withContext(Dispatchers.IO) { MushafDb.surahAyahCount(context, sura) }
    }

    val density = LocalDensity.current
    val clusterHalfPx = with(density) { CLUSTER_SIZE.toPx() / 2f }

    // الآية الصاخبة الآن — تُبرَز على أسطر المصحف مزامنةً حيّة
    val playingTrack = playState.track?.takeIf { playState.playing }

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

    /** تشغيل مُدى آيات متتابع بصوت الشيخ المختار — **النظام الذكي**
     *  (بلاغ المالك ٢٠٢٦-١١-١٢): لا حوارَ تنزيلٍ ولا انتظارَ سورةٍ
     *  كاملة — الآية الأولى تُنزَّل إن غابت وتُشغَّل فور جاهزيتها
     *  (مئات الكيلوبايت في ثوانٍ)، والبقية تجلب للذاكرة وتُحضَّر
     *  خلفيةً أثناء التلاوة فلا صمتَ ولا فجوة بين الآيات، وما حاضرٌ
     *  يعمل لحظياً بلا إنترنت */
    fun startPlaylist(sura: Int, ayahs: List<Int>, loop: Boolean = false) {
        playMenuOpen = false
        val sequence = if (loop) MushafPlaylist.hifzRepeats(ayahs, HIFZ_REPEATS) else ayahs
        MushafPlayer.play(context, reciterId, sura, sequence, loop)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .onSizeChanged { rootSize = it },
    ) {
        // مساحة القراءة — تقف قبل شريط التنقل السائل، والصفحة تتسع لكاملها.
        // عند التكبير لا يحتاج قلبُ الصفحات مفتاحَ إيقاف خارجيّ — معالج
        // التحريك على الصفحة نفسها يستهلك السحب فلا يبلغ قلب الصفحات
        HorizontalPager(
            state = pagerState,
            beyondViewportPageCount = 1,
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(bottom = NAV_BAR_CLEAR),
        ) { index ->
            MushafPage(
                page = index + 1,
                selection = selection,
                zoom = zoom,
                selectionColor = selectionColor,
                playingAyah = playingTrack,
                onLongPressAyah = { ayah ->
                    // بداية تحديد جديد — والمنغّلة تظهر عند نهاية الإيماءة
                    selection = MushafSelectionOps.extend(null, ayah)
                    playMenuOpen = false
                    menuAt = null
                },
                onClearSelection = {
                    // الضغطة المطوّلة على الآية الوحيدة المحددة تمسح
                    // التحديد كلياً (alpha63) — وقواعد 2026-11-11: مع أكثر
                    // من آيةٍ تمسح الآية وحدها من القائمة
                    selection = null
                    menuAt = null
                    playMenuOpen = false
                },
                onRemoveSingleAyah = { ayah ->
                    // **الضغطة المطوّلة على آيةٍ من مُدىٍ متعددٍ تُسقط
                    // الآية وحدها** (بلاغ المالك 2026-11-11: «لازم
                    // المستخدم يضغط ضغطة طويلة لإلغاء تحديدها»)
                    selection = selection?.let { MushafSelectionOps.removeSingle(it, ayah) }
                },
                onDragExtendAyah = { ayah ->
                    // السحب على الأسطر يمدّ المُدى حيّاً — تحديد آيات مع بعضها
                    selection = selection?.let { MushafSelectionOps.extend(it, ayah) }
                        ?: MushafSelectionOps.extend(null, ayah)
                },
                onDragEndAyah = { at ->
                    menuAt = clampMenu(at)
                },
                onTapAyah = { ayah, at ->
                    // **الضغطة الواحدة على آيةٍ محدّدةٍ في مُدىٍ متعددٍ
                    // تقطع الذيل**: من الآية المضغوطة إلى آخر التحديد
                    // (بلاغ المالك 2026-11-11) — وعلى غير المحددة تمدّ
                    // المُدى كما كان (alpha51)
                    selection = if (selection != null &&
                        MushafSelectionOps.contains(selection, ayah) &&
                        !selection!!.isSingle
                    ) {
                        MushafSelectionOps.truncateFrom(selection!!, ayah)
                    } else {
                        MushafSelectionOps.extend(selection, ayah)
                    }
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
                        reciterName = QuranReciters.byId(reciterId).name,
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
                                    MushafDb.ayahRangeText(context, sel.sura, sel.first, sel.last)
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
                                    MushafDb.ayahRangeText(context, sel.sura, sel.first, sel.last)
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

        // شريط التجهيز الخلفي — الآية الغائبة تُنزَّل خلفيةً بلا انجمادٍ
        // ولا حوارٍ: يُرى هذا اللمح حتى جاهزية الصوت ثم يختفي وحده
        if (playState.loading) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                shadowElevation = 4.dp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = NAV_BAR_CLEAR + 8.dp),
            ) {
                Text(
                    text = "جارٍ تجهيز صوت التلاوة — تُحفظ الآية بالجهاز فتردّ لحظياً بعدما",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
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
                onHighlightAyah = { sura, ayah ->
                    selection = MushafSelection(sura, ayah)
                    menuAt = null
                    playMenuOpen = false
                },
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
 * المحدَّدة بلون الثيم، والزوم بالقرص والنقرة المزدوجة (alpha54)،
 * وشرائح الآيات من قاعدة الطبعة المصاحبة.
 */
@Composable
private fun MushafPage(
    page: Int,
    selection: MushafSelection?,
    zoom: MushafZoomState,
    selectionColor: Color,
    /** الآية الصاخبة الآن — إبرازُ مزامنةٍ حيّ يتبع التلاوة لحظياً */
    playingAyah: MushafPlayer.Track? = null,
    /** بداية تحديد جديد بالضغط المطوّل — بلا منغّلة بعد */
    onLongPressAyah: (MushafAyah) -> Unit,
    /** **مسح التحديد كلياً بالضغطة المطوّلة على الآية الوحيدة المحددة** */
    onClearSelection: () -> Unit,
    /** **إزالة الآية الوحيدة من مُدىٍ متعدد بالضغطة المطوّلة** (بلاغ 2026-11-11) */
    onRemoveSingleAyah: (MushafAyah) -> Unit,
    /** سحب الأسطر يمدّ المُدى حيّاً آيةً وراء آية */
    onDragExtendAyah: (MushafAyah) -> Unit,
    /** نهاية سحب التحديد — موضع ظهور المنغّلة */
    onDragEndAyah: (Offset) -> Unit,
    /** ضغطة على آيةٍ تمدّ المُدى الحالي */
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
            .background(MaterialTheme.colorScheme.surface)
            .onSizeChanged { viewSize = it }
            // الزوم بالقرص أُلغي من أصله (alpha63 — بلاغ المالك: «امنع
            // التكبير و التصغير في صفحات المصحف») — الصفحة تُعرض دوماً
            // بحجمها الكامل ولا إيماءة تكبيرٍ تستقبلها أصلاً.
            // ===== الضغط المطوّل ثم السحب — تحديد آيات متتابعة بإيماءة
            // واحدة: «عايز يبقى في امكانيه تحديد عده ايات مع بعضها» =====
            .pointerInput(page, bitmap, segments, viewSize) {
                var dragging = false
                var lastPos = Offset.Zero
                detectDragGesturesAfterLongPress(
                    onDragStart = { pos ->
                        dragging = false
                        val segs = segments ?: return@detectDragGesturesAfterLongPress
                        val t = transform ?: return@detectDragGesturesAfterLongPress
                        val (gx, gy) = glyphAt(t, pos, zoom, viewSize)
                        MushafSelectionOps.hit(segs, gx, gy)?.let { ayah ->
                            // **قواعد الإزالة (بلاغ المالك 2026-11-11):**
                            // الضغطة المطوّلة على آيةٍ محدّدةٍ:
                            // — آيةً واحدةً محدّدة: يُمسح التحديد كلياً
                            // — من مُدىٍ متعدد: تُسقط الآية وحدها
                            val sel = selection
                            if (MushafSelectionOps.contains(sel, ayah)) {
                                if (sel!!.isSingle) {
                                    onClearSelection()
                                } else {
                                    onRemoveSingleAyah(ayah)
                                }
                                return@detectDragGesturesAfterLongPress
                            }
                            onLongPressAyah(ayah)
                            lastPos = pos
                            dragging = true
                        }
                    },
                    onDrag = { change, _ ->
                        if (!dragging) return@detectDragGesturesAfterLongPress
                        change.consume()
                        lastPos = change.position
                        val segs = segments ?: return@detectDragGesturesAfterLongPress
                        val t = transform ?: return@detectDragGesturesAfterLongPress
                        val (gx, gy) = glyphAt(t, lastPos, zoom, viewSize)
                        MushafSelectionOps.hit(segs, gx, gy)?.let { onDragExtendAyah(it) }
                    },
                    onDragEnd = {
                        if (dragging) {
                            dragging = false
                            onDragEndAyah(lastPos)
                        }
                    },
                    onDragCancel = { dragging = false },
                )
            }
            // ===== النقر يمدّ المُدى الحالي، والنقرة المزدوجة تقلب الزوم
            // بين ١× و٢٫٥× حول نقطة الضغطة =====
            .pointerInput(page, bitmap, segments, viewSize, selection != null) {
                detectTapGestures(
                    // النقرة على آيةٍ تمدّ المُدى حتى الآية الجديدة (٥٠ ← ٥٥)
                    onTap = { pos ->
                        if (selection != null) {
                            val segs = segments ?: return@detectTapGestures
                            val t = transform ?: return@detectTapGestures
                            val (gx, gy) = glyphAt(t, pos, zoom, viewSize)
                            MushafSelectionOps.hit(segs, gx, gy)?.let { onTapAyah(it, pos) }
                        }
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        val bmp = bitmap
        val t = transform
        // لون إبراز التلاوة الجارية — أعمق من شفافية التحديد بنفس لون
        // الثيم، ويُحسب هنا في التركيب لا داخل كانفس الرسم
        val playingColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
        if (bmp != null && t != null) {
            // طبقة الزوم — تكبير حول مركز الصفحة مع الإزاحة المحصورة،
            // والإبرازات داخلها تتكبّر معها لأنها تُرسم في الكانفس نفسه
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = zoom.scale
                        scaleY = zoom.scale
                        translationX = zoom.offX
                        translationY = zoom.offY
                    },
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawMushafPage(
                        bmp, t, page, segments, selection, selectionColor,
                        playingAyah = playingAyah,
                        playingColor = playingColor,
                    )
                }
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
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "اضغط للمحاولة من جديد",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

/**
 * رسم الصفحة داخل الكانفس: القص الجانبي (بلا تفاسير) ثم تنظيف الزوايا
 * العلوية بلون الورق ثم إبراز المُدى المحدَّد بلون الثيم الشفاف على
 * أسطره — والامتداد الكامل للمساحة يجري في التحويل نفسه.
 */
private fun DrawScope.drawMushafPage(
    bitmap: ImageBitmap,
    t: MushafTransform,
    page: Int,
    segments: List<MushafSegment>?,
    selection: MushafSelection?,
    selectionColor: Color,
    /** الآية الصاخبة الآن — تُرسم بعد التحديد فتظهر أعمق فوقه */
    playingAyah: MushafPlayer.Track? = null,
    /** لون إبراز التلاوة الجارية — أعمق من التحديد لنفس لون الثيم */
    playingColor: Color = Color.Transparent,
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

    // إبراز المُدى المحدَّد — شريط بلون الثيم الشفاف على أسطر الآيات
    // (توجيه المالك: «بدل ما هو بنفسجي خليه بالوان تتماشى مع الوان
    // الثيم و يكون شفاف لدرجه تسمح برؤيه النص المحدد») — لكل شريحة
    // سطر على حِدة فالسطر المشترك يُبرز نصيبه
    val sel = selection
    if (sel != null) {
        segs.filter { it.sura == sel.sura && it.ayah in sel.ayahs }.forEach { seg ->
            val (x0, y0) = t.toDisplay(seg.minX - 2f, seg.minY - 1f)
            val (x1, y1) = t.toDisplay(seg.maxX + 2f, seg.maxY + 1f)
            drawRoundRect(
                color = selectionColor,
                topLeft = Offset(x0, y0),
                size = Size(x1 - x0, y1 - y0),
                cornerRadius = CornerRadius(7f, 7f),
            )
        }
    }

    // إبراز الآية الصاخبة الآن — مزامنة التلاوة الحيّة: الشريط الأعمق
    // يسير مع الصوت آيةً آية (ومن توقيتات القاعدة لحظةً بلحظة) فيُرى
    // موضع الصوت على السطر حتى مع مُدىٍ محدَّدٍ أوسع منه
    val sounding = playingAyah
    if (sounding != null) {
        segs.filter { it.sura == sounding.sura && it.ayah == sounding.ayah }.forEach { seg ->
            val (x0, y0) = t.toDisplay(seg.minX - 2f, seg.minY - 1f)
            val (x1, y1) = t.toDisplay(seg.maxX + 2f, seg.maxY + 1f)
            drawRoundRect(
                color = playingColor,
                topLeft = Offset(x0, y0),
                size = Size(x1 - x0, y1 - y0),
                cornerRadius = CornerRadius(7f, 7f),
            )
        }
    }
}

/**
 * نقطة شاشة ← إحداثي فراغ المرجع بالآيات — تمرّ بعكس زوم الصفحة أولاً
 * ثم بعكس تحويل الطبعة، فتبقى المعاينة مليمترية مع الصفحة المكبّرة.
 */
private fun glyphAt(
    t: MushafTransform,
    pos: Offset,
    zoom: MushafZoomState,
    viewSize: IntSize,
): Pair<Float, Float> {
    val (cx, cy) = MushafZoomMath.toContent(
        pos.x, pos.y, zoom.scale, zoom.offX, zoom.offY,
        viewSize.width.toFloat(), viewSize.height.toFloat(),
    )
    return t.toGlyph(cx, cy)
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
    reciterName: String,
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
    val last = selection.last
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
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.97f))
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
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                        .clickable(onClick = onClose),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "إغلاق قائمة التلاوة",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                }
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        text = "التلاوة · ${MushafSurahs.name(selection.sura)}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Text(
                        text = "الشيخ $reciterName",
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
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
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            val lo = selection.first
            val hi = selection.last
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
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                        Text(
                            text = tafsir,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.95f),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp)
                            .height(0.5.dp)
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
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
            val lo = selection.first
            val hi = selection.last
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
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
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
                                    if (memorized) accent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.09f),
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
                                color = if (memorized) accent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                style = MaterialTheme.typography.labelSmall,
                            )
                            if (text.isNotBlank()) {
                                if (visible) {
                                    Text(
                                        text = text,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.95f),
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(top = 4.dp),
                                    )
                                } else {
                                    // النص المختبئ — نقاطٌ بعرضه تقريباً تفاخر
                                    // المختبِر بأن هنا آية لا يحفظها إلا بذكرها
                                    Text(
                                        text = "•".repeat((text.length / 4).coerceIn(8, 64)),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.22f),
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
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
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
    /** إبراز الآية المطلوبة عند الوصول إلى صفحتها (بلاغ 2026-11-11) */
    onHighlightAyah: (Int, Int) -> Unit,
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

    // البحث — كلمة آيةٍ باسم السورة بعدة كلماتٍ أو قفزة آيةٍ «مريم ٥٠»
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<MushafSearch.Hit>?>(null) }
    var searching by remember { mutableStateOf(false) }
    // السور المطابقة لاسم البحث (alpha63) — وقفزة الآية «مريم ٥٠»
    var surahMatches by remember { mutableStateOf<List<Int>>(emptyList()) }
    var surahJump by remember { mutableStateOf<Pair<Int, Int>?>(null) } // (سورة، آية)
    LaunchedEffect(query) {
        val q = query.trim()
        if (q.isEmpty()) {
            results = null
            searching = false
            surahMatches = emptyList()
            surahJump = null
            return@LaunchedEffect
        }
        searching = true
        // أولاً: قفزة «اسم سورة + رقم آية» — بلاغ المالك alpha63
        val jump = MushafSearch.extractSurahJump(q)
        val names = (1..114).map { it to MushafSurahs.name(it) }
        val matched = MushafSearch.matchingSurahs(q, names)
        var jumpResolved: Pair<Int, Int>? = null
        if (jump != null) {
            val target = matched.firstOrNull()
                ?: MushafSearch.matchingSurahs(jump.suraNameQuery, names).firstOrNull()
            if (target != null) jumpResolved = target to jump.ayah
        }
        surahMatches = matched
        surahJump = jumpResolved
        // ثم بحث نصّ الآيات كما هو — كلمة واحدة بلا تشكيل
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
            // الآية تُبرز عند وصول القارئ — بلا بحثٍ ثانٍ عنها
            onHighlightAyah(hit.sura, hit.ayah)
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
            subtitle = "اختر سورة أو جزءاً أو ابحث باسمها أو «مريم ٥٠»",
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
                    text = "ابحث: اسم سورة، أو كلمة من الآية، أو «مريم ٥٠»",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "مسح البحث",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable { query = "" },
                    )
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                cursorColor = MaterialTheme.colorScheme.primary,
            ),
            textStyle = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
        )

        // نتائج البحث تعلو القائمتين: قفزة الآية «مريم ٥٠» ثم السور
        // المطابقة بالاسم ثم آيات الكلمة (alpha63 — بلاغ المالك: «اجعل
        // البحث … يدعم البحث باسم السوره و عده كلمات او كلمه واحده و
        // يدعم البحث برقم الايه … مثلا مريم ٥٠ فتأتي له الايه رقم ٥٠ من
        // سوره مريم و هكذا»)
        when {
            results != null || surahMatches.isNotEmpty() -> {
                val hits = results ?: emptyList()
                if (hits.isEmpty() && surahMatches.isEmpty()) {
                    Text(
                        text = "لا نتائج — جرّب اسم سورة، أو كلمة من الآية " +
                            "بلا تشكيل، أو «اسم السورة ورقم الآية» مثل: مريم ٥٠",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 4.dp),
                    ) {
                        // ===== بطاقة قفزة الآية — الآية ٥٠ من سورة مريم =====
                        val jump = surahJump
                        if (jump != null) {
                            val (jumpSura, jumpAyah) = jump
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp)
                                    .clickable {
                                        scope.launch {
                                            val page = withContext(Dispatchers.IO) {
                                                MushafDb.pageOfAyah(context, jumpSura, jumpAyah)
                                            }
                                            // **القفزة تُبرز الآية نفسها** — بلاغ
                                            // المالك 2026-11-11: «البحث يوديه الى
                                            // سوره الاعراف الايه ٦٠» فلا يُترك
                                            // القارئ يبحث عنها في الصفحة
                                            onHighlightAyah(jumpSura, jumpAyah)
                                            onJump(page)
                                        }
                                    },
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = MushafSurahs.arabicDigits(jumpAyah),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            text = "سورة ${MushafSurahs.name(jumpSura)} — الآية " +
                                                MushafSurahs.arabicDigits(jumpAyah),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                        Text(
                                            text = "اضغط لفتح صفحة الآية",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                        }

                        // ===== السور المطابقة بالاسم — كلمةً أو عدة كلمات =====
                        if (surahMatches.isNotEmpty()) {
                            Text(
                                text = "السور المطابقة",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 2.dp),
                            )
                        }
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp),
                        ) {
                            items(surahMatches) { sura ->
                                SurahRow(
                                    sura = sura,
                                    title = MushafSurahs.name(sura),
                                    detail = "سورة رقم ${MushafSurahs.arabicDigits(sura)}",
                                    accent = MaterialTheme.colorScheme.primary,
                                    onClick = {
                                        // الصفحة غير المحمّلة بعد — أولها من السورة
                                        val start = startPages?.get(sura)
                                        if (start != null) onJump(start)
                                    },
                                )
                            }
                            if (hits.isNotEmpty()) {
                                item {
                                    Text(
                                        text = if (hits.size >= 200) {
                                            "أول ٢٠٠ نتيجة — حدد بحثك بكلمة أدق"
                                        } else {
                                            "آيات تحمل الكلمة: ${MushafSurahs.arabicDigits(hits.size)}"
                                        },
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                                    )
                                }
                            }
                            items(hits) { hit ->
                                SearchHitRow(hit = hit, accent = MaterialTheme.colorScheme.primary) {
                                    jumpToHit(hit)
                                }
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
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            modifier = Modifier.padding(top = 5.dp),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
                .height(0.5.dp)
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
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
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = detail,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
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
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                .clickable(onClick = onClose),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "إغلاق",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
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
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
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
            .background(if (filled) accent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (filled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
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
            color = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
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
                // alpha54 — توجيه المالك: «اخر جزء منها تحت شريط التنقل
                // … خلي حدود القائمه مع اخر حدود شريط التنقل» — اللوحة
                // كلها تُرفع لتقف حدّها السفلي على آخر شريط التنقل فلا
                // يبقى سطرٌ مخفيّ تحتَه ولا خيارٌ مستحيل الضغط
                .padding(bottom = NAV_BAR_CLEAR)
                .height(with(LocalDensity.current) { cardH.toDp() })
                .graphicsLayer { translationY = (1f - slide.value) * cardH }
                .shadow(18.dp, RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                .background(MaterialTheme.colorScheme.surface)
                .clickable(enabled = false) { },
        ) {
            // مقبض السحب
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 10.dp)
                    .size(width = 44.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)),
            )
            content()
        }
    }
}

/**
 * مشغّل التلاوة الذكي — توجيه المالك: «امكانيه تحديد عده ايات مع بعض
 * لتشغيلها» — آية وراء آية حتى آخر المُدى المختار، وواحد فقط في آن،
 * ويقف مع مغادرة المصحف أو الضغطة على زر الإيقاف — وفي الحفظ الميسر
 * يدور على المُدى من جديد حتى يقف المستخدم (loop).
 *
 * **الذكاء الجديد** (بلاغ المالك ٢٠٢٦-١١-١٢: نظام تحميلٍ وتشغيلٍ
 * وتخزينٍ ذكيٍّ للآيات):
 * • **التنزيل والتحضير كلهما خلفيةٌ خالصة** — لا `prepare()` متزامناً
 *   على خيط الواجهة قط: كل مشغّل MediaPlayer يُبنى ويُحضَّر في
 *   Dispatchers.IO ثم يُسلَّم جاهزاً تحت القفل، فلا تجميداً للواجهة
 *   ولا تقطيعاً مهما نزلت آياتٌ جديدة.
 * • **الضغطة تُشغّل فوراً** — لا حوارَ تنزيلٍ ولا انتظارَ سورةٍ
 *   كاملة: الآية المطلوبة وحدها تُنزَّل إن غابت ([AyahAudioCache.ensureAyah]
 *   بمسارها الثلاثي: everyayah ← مرآتنا ← islamic.network) وتُحفَظ
 *   بالجهاز، وطلبُها التالي يعود **لحظياً من الذاكرة بلا إنترنت**.
 * • **التحميل المسبق** — أثناء سماع الآية تُجلَب تاليتها للذاكرة
 *   (نافذة [AyahAudioCache.PRELOAD_AHEAD]) وتُحضَّر مسبقاً وتُربط
 *   بـ MediaPlayer.setNextMediaPlayer فيبدأها النظام لحظة نهاية
 *   سابقتها مباشرة — قفزةٌ بلا صمتٍ ولا فجوة، وإن تأخر تحضيرُ
 *   تالٍ فالمتابعة تُبدأه فور جاهزيته.
 * • **مزامنة الآية النشطة لحظياً** — [activeAyah] تُحدَّث كل ١٠٠
 *   مللي: مع قاعدة التوقيتات [AyahTimings] يُحسب الموضع من مرساة
 *   بداية الآية زائد موضع المشغّل فيها فتُحدَّد الآية النشطة قبل
 *   نهاية ملفها، وبغياب القاعدة تُحدَّث من انتقال المسارات وهي أدق
 *   ساعةٍ — وفي الحالين بلا قراءةِ قاعدةٍ ولا تخصيصِ كائنات.
 * • **الفشل مهذب** — آيةٌ تالفةٌ تُتخطى والتسلسل يكمل، وثلاثٌ
 *   متتالية تقف برسالةٍ واضحة بدل حلقةٍ صامتة.
 */
private object MushafPlayer {

    /** الآية الواحدة في قائمة التلاوة */
    data class Track(val sura: Int, val ayah: Int)

    /** حالة التلاوة الكاملة — ما يُسمع الآن وما يجري خلفه */
    data class State(
        /** هل صوتٌ يُسمَع الآن */
        val playing: Boolean = false,
        /** تجهيزٌ جارٍ خلفياً — تنزيل الآية أو تحضير مشغّلها */
        val loading: Boolean = false,
        /** الآية التي يتعامل معها المشغّل الآن */
        val track: Track? = null,
        /** رسالة عطلٍ تُعرض للمستخدم — التشغيل التالي يمسحها */
        val error: String = "",
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state

    private val _playing = MutableStateFlow(false)
    val playing: StateFlow<Boolean> = _playing

    /** الآية النشطة لحظياً بالمزامنة — انظر توثيق الكائن */
    private val _activeAyah = MutableStateFlow<Track?>(null)
    val activeAyah: StateFlow<Track?> = _activeAyah

    private const val PREPARE_ERROR =
        "تعذّر تجهيز صوت التلاوة — تحقق من الاتصال ثم أعد المحاولة"
    private const val CORRUPT_ERROR =
        "تعذّر تشغيل التلاوة — أعد تنزيل صوت السورة من قسم «أصوات الشيخ»"

    /** سقف الأخطاء المتتالية قبل وقفٍ مهذبٍ برسالةٍ بدل حلقةٍ صامتة */
    private const val MAX_CONSECUTIVE_ERRORS = 3

    // قائمة الآيات ودورها وشيخها
    @Volatile
    private var queue: List<Track> = emptyList()
    @Volatile
    private var playingIndex = 0
    @Volatile
    private var loop = false
    @Volatile
    private var reciterId = ""
    private var appContext: Context? = null

    private val lock = Any()
    private var current: MediaPlayer? = null
    private var next: MediaPlayer? = null
    private var nextIndex = -1

    /** تحضيراتٌ جاريةٌ بمؤشراتها — حارسٌ واحدٌ فلا تحضيرٌ مزدوجاً قط */
    private val preparing: MutableSet<Int> =
        java.util.Collections.newSetFromMap(java.util.concurrent.ConcurrentHashMap())

    /** جيل التشغيل — يبطل مستقبلات وتدفقات الجيل القديم بعد إيقافٍ */
    @Volatile
    private var generation = 0L
    @Volatile
    private var consecutiveErrors = 0

    /**
     * مجال التلاوة — أحداث المستمعات تصل هنا والتنزيل والتحضير في
     * Dispatchers.IO كاملاً.
     */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /** بدء تلاوة مُدى آيات — التنزيل والتحضير خلفيةٌ كاملة والبداية فورية */
    fun play(context: Context, reciterId: String, sura: Int, ayahs: List<Int>, loop: Boolean = false) {
        val tracks = ayahs.filter { it >= 1 }.map { Track(sura, it) }
        if (tracks.isEmpty()) return
        synchronized(lock) {
            stopInternal()
            appContext = context.applicationContext
            this.reciterId = reciterId
            queue = tracks
            playingIndex = 0
            this.loop = loop
            consecutiveErrors = 0
            val gen = ++generation
            _state.value = State(loading = true, track = tracks.first())
            _activeAyah.value = tracks.first()
            val ctx = appContext ?: return@synchronized
            // تدفئة توقيتات السورة في الذاكرة مبكراً — مزامنةٌ بلا أول انتظار
            scope.launch { runCatching { AyahTimings.suraTimings(ctx, sura) } }
            scope.launch { prepareAndRun(gen) }
        }
    }

    fun stop() {
        synchronized(lock) { stopInternal() }
    }

    private fun stopInternal() {
        generation++
        releaseAll()
        queue = emptyList()
        playingIndex = 0
        loop = false
        preparing.clear()
        consecutiveErrors = 0
        _playing.value = false
        _state.value = State()
        _activeAyah.value = null
        AyahAudioCache.protectedPaths = emptySet()
    }

    private fun releaseAll() {
        current?.let { p ->
            runCatching { p.setNextMediaPlayer(null) }
            runCatching { if (p.isPlaying) p.stop() }
            runCatching { p.release() }
        }
        runCatching { next?.release() }
        current = null
        next = null
        nextIndex = -1
    }

    /**
     * بناء مشغّل آيةٍ بالمؤشر **كاملاً في الخلفية**: تنزيلٌ إن غاب
     * صوتها ثم فتحٌ وتحضير — ويعود جاهزاً للربط أو البدء، أو لا شيء
     * إن تعذّر تجهيزها. لا خيطَ واجهةٍ يلمس هذه العملية قط.
     */
    private suspend fun buildPlayer(index: Int, gen: Long): MediaPlayer? {
        val ctx = appContext ?: return null
        val track = queue.getOrNull(index) ?: return null
        val path = AyahAudioCache.ensureAyah(ctx, reciterId, track.sura, track.ayah)
            ?: return null
        return withContext(Dispatchers.IO) {
            runCatching {
                MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build(),
                    )
                    setDataSource(path)
                    setOnCompletionListener { done -> onCompletedLocked(done, gen) }
                    setOnErrorListener { _, _, _ ->
                        onErrorLocked(gen)
                        true
                    }
                    prepare()
                }
            }.getOrNull()
        }
    }

    /** أول الآيات: تُحضَّر خلفيةً ثم تُبدأ — ومنها تُطلَق بقية الآليات */
    private suspend fun prepareAndRun(gen: Long) {
        val first = buildPlayer(0, gen)
        synchronized(lock) {
            if (gen != generation) {
                runCatching { first?.release() }
                return
            }
            if (first == null) {
                _state.value = State(error = PREPARE_ERROR)
                _playing.value = false
                queue = emptyList()
                return
            }
            current = first
            val started = runCatching { first.start() }.isSuccess
            consecutiveErrors = if (started) 0 else 1
            _playing.value = started
            _state.value = _state.value.copy(playing = started, loading = !started)
            if (!started) {
                runCatching { first.release() }
                current = null
            }
        }
        refreshProtection()
        advance(gen)
        startTicker(gen)
    }

    /** مؤشر الآية التالية بدور الحفظ — لا تالٍ بعد الأخير بلا دورٍ */
    private fun nextIndexOf(index: Int): Int? {
        val size = queue.size
        if (size == 0) return null
        var idx = index + 1
        if (idx >= size) {
            if (!loop) return null
            idx = 0
        }
        return idx
    }

    /**
     * محرّك التسلسل — يُنادى بعد كل تغيّرٍ حالة: الجاريةُ الغائبة
     * تُحضَّر وتُبدأ فور جاهزيتها، والحاضرةُ تاليتها تُحضَّر وتُربط —
     * وكلٌّ في خلفيته، بلا ازدواجٍ (حارس [preparing]) وبلا إعادةِ
     * تحضيرٍ لما حاضرٌ أصلاً.
     */
    private fun advance(gen: Long) {
        var index = -1
        synchronized(lock) {
            if (gen != generation) return
            val target = if (current == null) playingIndex else nextIndexOf(playingIndex)
            if (target == null) return
            if (target == nextIndex && next != null) return
            if (target == playingIndex && current != null) return
            if (target in preparing) return
            preparing += target
            index = target
        }
        if (index < 0) return
        scope.launch {
            val p = try {
                buildPlayer(index, gen)
            } finally {
                preparing.remove(index)
            }
            if (p == null) {
                onPrepareFailed(gen, index)
                return@launch
            }
            var failedStart = false
            synchronized(lock) {
                if (gen != generation) {
                    runCatching { p.release() }
                    return@synchronized
                }
                when {
                    // الجارية انتظرت التحضير — تُبدأ لحظة جاهزيتها
                    index == playingIndex && current == null -> {
                        current = p
                        val started = runCatching { p.start() }.isSuccess
                        if (started) {
                            consecutiveErrors = 0
                            _playing.value = true
                            _state.value = _state.value.copy(
                                playing = true, loading = false,
                                track = queue.getOrNull(index), error = "",
                            )
                        } else {
                            consecutiveErrors++
                            failedStart = true
                        }
                    }
                    // تاليةٌ جاهزة — تُربط بالجارية فتبدأ لحظة نهايتها
                    index != playingIndex -> {
                        runCatching { next?.release() }
                        next = p
                        nextIndex = index
                        runCatching { current?.setNextMediaPlayer(p) }
                    }
                    // تحضيرٌ عدّت مدّته — المستمع أهدره بأدب
                    else -> runCatching { p.release() }
                }
            }
            if (failedStart) {
                runCatching { p.release() }
                synchronized(lock) {
                    if (gen == generation) current = null
                }
                skipForwardOrStop(gen)
                return@launch
            }
            refreshProtection()
            advance(gen)
        }
    }

    /**
     * تعذّر تجهيز آيةٍ كانت الجارية بانتظارها — تُتخطى والتسلسل
     * يكمل بالتالية، وسقفُ الأخطاء المتتالية يقف برسالةٍ واضحة.
     * وتالٍ فاشلٌ لا يُقطع شيئاً: عند نهاية جاريتها يُعاد طلبُها
     * فرصةً أخرى (فشلُ الشبكة عابرٌ لا أبديّ).
     */
    private fun onPrepareFailed(gen: Long, index: Int) {
        synchronized(lock) {
            if (gen != generation) return
            if (index == playingIndex && current == null) {
                consecutiveErrors++
                val nxt = nextIndexOf(playingIndex)
                if (nxt == null || consecutiveErrors >= MAX_CONSECUTIVE_ERRORS) {
                    _state.value = _state.value.copy(
                        playing = false, loading = false, error = PREPARE_ERROR,
                    )
                    _playing.value = false
                    queue = emptyList()
                    _activeAyah.value = null
                    AyahAudioCache.protectedPaths = emptySet()
                    return
                }
                playingIndex = nxt
                _state.value = _state.value.copy(
                    playing = false, loading = true, track = queue.getOrNull(nxt),
                )
                _activeAyah.value = queue.getOrNull(nxt)
            }
        }
        refreshProtection()
        advance(gen)
    }

    /**
     * فشل بدءٍ أو خطأ مشغّل — الآية الفاشلة تُتخطى والتسلسل يكمل،
     * وسقفُ الأخطاء المتتالية يقف مهذباً بدل حلقةٍ صامتة.
     */
    private fun skipForwardOrStop(gen: Long) {
        synchronized(lock) {
            if (gen != generation) return
            if (consecutiveErrors >= MAX_CONSECUTIVE_ERRORS) {
                _state.value = _state.value.copy(
                    playing = false, loading = false, error = CORRUPT_ERROR,
                )
                _playing.value = false
                releaseAll()
                queue = emptyList()
                _activeAyah.value = null
                AyahAudioCache.protectedPaths = emptySet()
                return
            }
            val nxt = nextIndexOf(playingIndex)
            if (nxt == null) {
                // انتهى المُدى بعد فشلٍ — وقفٌ نظيف
                queue = emptyList()
                playingIndex = 0
                loop = false
                _playing.value = false
                _state.value = State()
                _activeAyah.value = null
                AyahAudioCache.protectedPaths = emptySet()
                return
            }
            playingIndex = nxt
            _state.value = _state.value.copy(
                playing = false, loading = true, track = queue.getOrNull(nxt),
            )
            _activeAyah.value = queue.getOrNull(nxt)
        }
        refreshProtection()
        advance(gen)
    }

    /** نهاية آية — التالية المحضَّرة تبدأ لحظياً ونحضّر بعدها في السرّاء */
    private fun onCompletedLocked(done: MediaPlayer, gen: Long) {
        synchronized(lock) {
            if (gen != generation) {
                runCatching { done.release() }
                return
            }
            if (done !== current) {
                // مستمع مشغّلٍ سبق استبداله — يُهمل ويُحرَّر
                runCatching { done.release() }
                return
            }
            val ready = next
            runCatching { done.setNextMediaPlayer(null) }
            runCatching { done.release() }
            if (ready != null) {
                // بدأها النظام لحظة نهاية سابقتها — قفزةٌ بلا صمتٍ
                current = ready
                playingIndex = nextIndex
                next = null
                nextIndex = -1
                consecutiveErrors = 0
                _state.value = _state.value.copy(
                    playing = true, loading = false,
                    track = queue.getOrNull(playingIndex), error = "",
                )
            } else {
                // لا تالٍ محضَّراً بعد — الجارية الجديدة تُحضَّر فوراً
                current = null
                val nxt = nextIndexOf(playingIndex)
                if (nxt == null) {
                    queue = emptyList()
                    playingIndex = 0
                    loop = false
                    _playing.value = false
                    _state.value = State()
                    _activeAyah.value = null
                    AyahAudioCache.protectedPaths = emptySet()
                    return
                }
                playingIndex = nxt
                _state.value = _state.value.copy(
                    playing = false, loading = true, track = queue.getOrNull(nxt),
                )
            }
            _activeAyah.value = queue.getOrNull(playingIndex)
        }
        refreshProtection()
        advance(gen)
    }

    /** خطأ مشغّل — الفاشلة تُتخطى والتسلسل يكمل بأدب */
    private fun onErrorLocked(gen: Long) {
        synchronized(lock) {
            if (gen != generation) return
            current?.let { p ->
                runCatching { p.setNextMediaPlayer(null) }
                runCatching { p.release() }
            }
            runCatching { next?.release() }
            current = null
            next = null
            nextIndex = -1
            consecutiveErrors++
        }
        skipForwardOrStop(gen)
    }

    /** حماية آيات السير من سياسة الإخلاء — الجارية والمحضَّرة تاليةً */
    private fun refreshProtection() {
        val ctx = appContext ?: return
        val paths = mutableSetOf<String>()
        queue.getOrNull(playingIndex)?.let {
            paths += QuranAudioLibrary.ayahFile(ctx, reciterId, it.sura, it.ayah).absolutePath
        }
        if (nextIndex >= 0) {
            queue.getOrNull(nextIndex)?.let {
                paths += QuranAudioLibrary.ayahFile(ctx, reciterId, it.sura, it.ayah).absolutePath
            }
        }
        AyahAudioCache.protectedPaths = paths
    }

    /**
     * نابض المزامنة اللحظية — كل ١٠٠ مللي يُحسب موضع الصوت الحالي:
     * مع توقيتات القاعدة يكون من المرساة (بداية الآية في التوقيتات)
     * زائد موضع المشغّل داخلها فتُحدَّد الآية النشطة قبل نهاية ملفها
     * إن اقترب زمن تاليتها، وبغياب التوقيتات يكون من انتقال المسارات
     * نفسه. مصفوفاتٌ في الذاكرة وبحثٌ ثنائيٌّ — لا قاعدةَ ولا تخصيصٍ
     * في المسار الحي، والنابض يموت وحده بتغيّر الجيل.
     */
    private fun startTicker(gen: Long) {
        scope.launch {
            while (isActive && gen == generation) {
                delay(100L)
                val p = current ?: continue
                val tr = _state.value.track ?: continue
                val sounding = runCatching { p.isPlaying }.getOrDefault(false)
                if (!sounding) continue
                val timings = AyahTimings.cachedSuraTimings(tr.sura)
                val ayah = if (timings != null) {
                    val anchor = timings.startTimeOf(tr.ayah)
                    if (anchor >= 0L) {
                        AyahTimings.ayahAt(timings, anchor + p.currentPosition.coerceAtLeast(0))
                    } else {
                        tr.ayah
                    }
                } else {
                    tr.ayah
                }
                val cur = _activeAyah.value
                if (cur?.sura != tr.sura || cur?.ayah != ayah) {
                    _activeAyah.value = Track(tr.sura, ayah)
                }
            }
        }
    }
}

/** سياق التطبيق → النشاط الحامل — لطيّ شريط الحالة داخل المصحف */
private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
