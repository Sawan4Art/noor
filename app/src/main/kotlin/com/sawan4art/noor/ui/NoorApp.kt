package com.sawan4art.noor.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sawan4art.noor.data.ThemeMode
import com.sawan4art.noor.data.update.DownloadState
import com.sawan4art.noor.data.update.UpdateManager
import com.sawan4art.noor.data.update.UpdateUiState
import com.sawan4art.noor.domain.location.LocationGate
import com.sawan4art.noor.ui.home.HomeScreen
import com.sawan4art.noor.ui.home.HomeViewModel
import com.sawan4art.noor.ui.azkar.AzkarScreen
import com.sawan4art.noor.ui.hadith.HadithScreen
import com.sawan4art.noor.ui.nav.LiquidNavScaffold
import com.sawan4art.noor.ui.nav.NoorTab
import com.sawan4art.noor.ui.notice.NoorNoticeDialog
import com.sawan4art.noor.ui.mushaf.MushafScreen
import com.sawan4art.noor.ui.onboarding.WelcomeScreen
import com.sawan4art.noor.ui.qibla.QiblaScreen
import com.sawan4art.noor.ui.settings.SettingsScreen
import com.sawan4art.noor.ui.tasbih.MisbahaScreen
import com.sawan4art.noor.ui.theme.AccentPalette
import com.sawan4art.noor.ui.theme.NoorTheme
import com.sawan4art.noor.ui.theme.ThemeDialMath
import com.sawan4art.noor.ui.theme.ThemeDialOverlay
import com.sawan4art.noor.ui.theme.ThemeDialState
import com.sawan4art.noor.ui.settings.adhan.AdhanActions
import com.sawan4art.noor.ui.update.UpdateDialog

/** التطبيق عربي أولاً — الاتجاه مثبت يمين-لليسار مهما كانت لغة الجهاز */
private val AppDirection = LayoutDirection.Rtl

/**
 * مقياس خط التطبيق الثابت (alpha43 — بلاغ المالك بعد alpha42: «كبر تاني
 * و راجع كل الخطوط و الأيقونات في التطبيق كله .. راعي النسبه الذهبيه و
 * كمان راعي ان في ناس نظرها ضعيف ف مش هتقدر تقرأ»): المقياس الآن مثبت
 * على **١.٤** — التطبيق ما يزال غير متأثر بإعدادات خط الهاتف (لا أيقونات
 * تختفي ولا عناصر تزحزح) وكل النصوص (sp) تكبر ٤٠٪ فوق مقاس الأساس،
 * ومع السلم الذهبي الجديد في Type.kt أصغر خطٍّ يُرى بحجمٍ مقروء لنظرٍ
 * ضعيف (١٣sp × ١.٤ ≈ ١٨ فوق الأساس).
 */
private const val APP_FONT_SCALE = 1.4f

@Composable
fun NoorApp(viewModel: HomeViewModel) {
    // **ثبات مقاس الخط** (alpha39 — بلاغ المالك: «في مشكله عند تكبير حجم
    // العرض والخط من اعدادات الهاتف يتم ازاله بعض الايقونات انا عايز
    // التطبيق ميتاثرش بحجم الخط في اعداد الهاتف»): مقياس خط النظام
    // (fontScale) يُتجاهَل تماماً — التطبيق يرسم نصوصه بمقاساته الثابتة
    // الخاصة (NoorTypography) فلا تتزحزح عناصر ولا تختفي أيقونات داخل
    // صناديقها مهما كُبّر خط الهاتف، والمقاس الظاهر هو الذي في Type.kt
    // نفسه. ويبقى التأثر بحجم العرض (الكثافة dp) كما هو.
    val system = LocalDensity.current
    val fixed = Density(density = system.density, fontScale = APP_FONT_SCALE)
    CompositionLocalProvider(LocalDensity provides fixed) {
        NoorRoot(viewModel)
    }
}

@Composable
private fun NoorRoot(viewModel: HomeViewModel) {
    var tab by rememberSaveable { mutableStateOf(NoorTab.PRAYER) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val locating by viewModel.locating.collectAsStateWithLifecycle()
    // شاشة الترحيب — أول تشغيلٍ فقط (null = ما زال يُتحقّق)
    val onboardingDone by viewModel.onboardingDone.collectAsStateWithLifecycle()
    // تفضيلات الأذان والإقامة — القسم الجديد في الإعدادات
    val adhanPrefs by viewModel.adhanPrefs.collectAsStateWithLifecycle()
    // نظام التحديثات — حالة واحدة تخدم حوار «ما الجديد» وبطاقة الإعدادات
    val updateState by UpdateManager.state.collectAsStateWithLifecycle()
    val dismissedVersion by UpdateManager.dismissedVersion.collectAsStateWithLifecycle()
    // حالة تنزيل التحديث داخل التطبيق (alpha48 — بلا متصفح)
    val downloadState by UpdateManager.download.collectAsStateWithLifecycle()
    // فحص الفتح — بسقف 3 ساعات، والفحص اليدوي من الإعدادات بلا سقف
    LaunchedEffect(Unit) { UpdateManager.onAppStart() }
    // الظلام الفعلي الحالي — مزلاج الشمس/القمر يقلبه (من النظام → عكس المعمول به الآن)
    val darkTheme = when (uiState.settings.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    // مزلاج المظهر — حالة واحدة تخدم طبقة الانقسام والمزلاج نفسه
    // (الطبقتان في الرئيسية فقط — بطلب المالك: الأيقونة لا تظهر في كل الصفحات)
    val onHome = tab == NoorTab.PRAYER
    val dial = remember { ThemeDialState() }
    val commitThemeFromDial: (Boolean) -> Unit = { light ->
        viewModel.setThemeMode(if (light) ThemeMode.LIGHT else ThemeMode.DARK)
    }
    // لون التطبيق المختار من الإعدادات (alpha18) — يغذّي الثيمات الثلاث كلها
    val accent = remember(uiState.settings.accentId) {
        AccentPalette.byId(uiState.settings.accentId)
    }
    // **اللون الأساسي المستقل** (alpha32) — فارغ = يتبع لون التطبيق الحالي
    val primaryAccent = remember(uiState.settings.primaryId) {
        uiState.settings.primaryId.takeIf { it.isNotBlank() }?.let { AccentPalette.byId(it) }
    }
    // بوابات الموقع — حوار الرسالة مع إجراء فتح الإعدادات المناسب (alpha18)
    val locationGate by viewModel.locationGate.collectAsStateWithLifecycle()
    val context = LocalContext.current

    CompositionLocalProvider(LocalLayoutDirection provides AppDirection) {
        Box(modifier = Modifier.fillMaxSize()) {
            // ===== الطبقة الأساسية — الثيم المثبت =====
            NoorTheme(
                themeMode = uiState.settings.themeMode,
                accent = accent,
                primaryAccent = primaryAccent,
            ) {
                NoorBody(
                    tab = tab,
                    onTab = { tab = it },
                    uiState = uiState,
                    locating = locating,
                    isDark = darkTheme,
                    onDetectLocation = viewModel::detectLocation,
                    onSetMethod = viewModel::setMethod,
                    onSetHanafi = viewModel::setHanafi,
                    onSetAccent = viewModel::setAccent,
                    onSetPrimary = viewModel::setPrimary,
                    adhanPrefs = adhanPrefs,
                    adhanActions = viewModel,
                    updateState = updateState,
                    downloadState = downloadState,
                    onCheckUpdates = UpdateManager::checkNow,
                    onDownloadUpdate = {
                        (updateState as? UpdateUiState.Available)?.info
                            ?.let { UpdateManager.downloadAndInstall(it) }
                    },
                    onSaveMushafPage = viewModel::setMushafPage,
                    onSetMushafBookmark = viewModel::setMushafBookmark,
                    hifzMemorized = uiState.settings.mushafHifz,
                    onSetMushafHifz = viewModel::setMushafHifz,
                    reciterId = uiState.settings.quranReciter,
                    onSetReciter = viewModel::setQuranReciter,
                    khutbahTitle = uiState.settings.khutbahTitle,
                    khutbahText = uiState.settings.khutbahText,
                    khutbahImages = uiState.settings.khutbahImages,
                    onSetKhutbah = viewModel::setKhutbah,
                    onImportKhutbahImage = viewModel::importKhutbahImage,
                    onDeleteKhutbahImage = viewModel::deleteKhutbahImage,
                    khutbahEnabled = uiState.settings.khutbahButtonEnabled,
                    onSetKhutbahEnabled = viewModel::setKhutbahButtonEnabled,
                )
            }

            // ===== طبقة المعاينة المقسومة — الثيم الهدف خلف الأيقونة (الرئيسية فقط) =====
            // **تدرّج مزج ناعم** (توجيه المالك alpha23 بعد قصّ alpha22 الصلب:
            // «الفاصل بين اللونين حالياً يظهر كخط مستقيم حاد بدلاً من دمج
            // ومزج اللونين معاً بسلاسة أثناء حركة السحب») — بدل قطع الخلفية
            // بحدّ واحد، طبقة المعاينة تُرسم كاملة داخل saveLayer معزول ثم
            // يُقنَّع ألفاها بقناع تدرّج خطي (BlendMode.DstIn) مركزه يركب
            // الحدّ لحظة بلحظة: الحزام ±15% من عرض الشاشة حول الحدّ بمنحنى
            // smoothstep (وقفاته من ThemeDialMath مصدر واحد للرسم والاختبارات
            // والمرآة) — مزج 50/50 عند الحدّ نفسه وشفافية كاملة خارجه،
            // فتتداخل الثيمتان بمرونة تامة مع حركة الإصبع بلا حواف حادة.
            // **بلا أي عنصر ضوئي** (حكم alpha22 قائم): القناع شفافية ألفا
            // خالصة بلا أي لون زائد. وقناع DstIn بهذا الشكل حاضر ومجرَّب
            // على جهاز المالك نفسه (حقول alpha17-21 حُكم عليها زخرفةً لا
            // عطلاً — كانت تُرى وتُرَفض بصرياً)، وTileMode.Clamp يمدّ طرفي
            // المنحدر خارج الحزام فيبقى الشفافية الكاملة عبر الشاشة كلها
            // حتى خارجها عند المسح النهائي (sweepTargetX يشمل الحزام —
            // عقد الومضة كامل بلا حزام متبقٍّ لحظة القلب).
            if (onHome && dial.splitActive) {
                // وقفات القناع ثابتة (الشكل لا يتحرك — الحزام كله يزحف مع الحدّ)
                // فتُحسب مرة واحدة: نفس blendMaskAlpha حرفياً بوقفات BLEND_STOPS
                val lightStops = remember {
                    Array(ThemeDialMath.BLEND_STOPS) { i ->
                        val s = i / (ThemeDialMath.BLEND_STOPS - 1).toFloat()
                        s to Color.Black.copy(alpha = 1f - ThemeDialMath.smoothstep(s))
                    }
                }
                val darkStops = remember {
                    Array(ThemeDialMath.BLEND_STOPS) { i ->
                        val s = i / (ThemeDialMath.BLEND_STOPS - 1).toFloat()
                        s to Color.Black.copy(alpha = ThemeDialMath.smoothstep(s))
                    }
                }
                NoorTheme(
                    themeMode = if (dial.overlayIsLight) ThemeMode.LIGHT else ThemeMode.DARK,
                    accent = accent,
                    primaryAccent = primaryAccent,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .drawWithContent {
                                val edge = dial.maskX.value
                                val w = size.width
                                val h = size.height
                                val half = ThemeDialMath.blendHalfSpan(w)
                                drawIntoCanvas { canvas ->
                                    val bounds = Rect(0f, 0f, w, h)
                                    canvas.saveLayer(bounds, Paint())
                                    this@drawWithContent.drawContent()
                                    // قناع الألفا المتدرّج: الفاتح يسار الحدّ
                                    // والمظلم يمينه — ثابت فيزيائياً بلا RTL،
                                    // ومركز التدرّج يركب الحدّ كل إطار
                                    drawRect(
                                        brush = Brush.linearGradient(
                                            *(if (dial.overlayIsLight) lightStops else darkStops),
                                            start = Offset(edge - half, 0f),
                                            end = Offset(edge + half, 0f),
                                            tileMode = TileMode.Clamp,
                                        ),
                                        size = size,
                                        blendMode = BlendMode.DstIn,
                                    )
                                    canvas.restore()
                                }
                            },
                    ) {
                        NoorBody(
                            tab = tab,
                            onTab = { tab = it },
                            uiState = uiState,
                            locating = locating,
                            // نسخة المعاينة تتلقى الثيم **المثبّت** لا هدف المعاينة
                            // كي لا تتقدم دورة الجمل مرتين للقلب الواحد (الطبقة
                            // تظهر بالثيم الهدف ثم يصل المثبّت — الجملة تتقدم
                            // عند التثبيت فقط لأن الدوران مرتبط بالثيم المثبّت)
                            isDark = darkTheme,
                            onDetectLocation = viewModel::detectLocation,
                            onSetMethod = viewModel::setMethod,
                            onSetHanafi = viewModel::setHanafi,
                            onSetAccent = viewModel::setAccent,
                            onSetPrimary = viewModel::setPrimary,
                            adhanPrefs = adhanPrefs,
                            adhanActions = viewModel,
                            updateState = updateState,
                            downloadState = downloadState,
                            onCheckUpdates = UpdateManager::checkNow,
                            onDownloadUpdate = {
                                (updateState as? UpdateUiState.Available)?.info
                                    ?.let { UpdateManager.downloadAndInstall(it) }
                            },
                            onSaveMushafPage = viewModel::setMushafPage,
                            onSetMushafBookmark = viewModel::setMushafBookmark,
                            hifzMemorized = uiState.settings.mushafHifz,
                            onSetMushafHifz = viewModel::setMushafHifz,
                            reciterId = uiState.settings.quranReciter,
                            onSetReciter = viewModel::setQuranReciter,
                            khutbahTitle = uiState.settings.khutbahTitle,
                            khutbahText = uiState.settings.khutbahText,
                            khutbahImages = uiState.settings.khutbahImages,
                            onSetKhutbah = viewModel::setKhutbah,
                            onImportKhutbahImage = viewModel::importKhutbahImage,
                            onDeleteKhutbahImage = viewModel::deleteKhutbahImage,
                            khutbahEnabled = uiState.settings.khutbahButtonEnabled,
                            onSetKhutbahEnabled = viewModel::setKhutbahButtonEnabled,
                        )
                    }
                }
            }

            // ===== المزلاج فوق كل شيء — السائل والتوهج الخافت والأيقونة والإيماءة =====
            // في الرئيسية فقط (بطلب المالك) — بقية الصفحات بلا أيقونة ولا طبقة
            if (onHome) {
                NoorTheme(
                    themeMode = uiState.settings.themeMode,
                    accent = accent,
                    primaryAccent = primaryAccent,
                ) {
                    ThemeDialOverlay(
                        dial = dial,
                        committedDark = darkTheme,
                        onCommitLight = commitThemeFromDial,
                    )
                }
            }
        }
    }

    // زر الرجوع العام — شاشات القسم الواحد فقط. تبويب الأذكار يدير رجوعه
    // داخلياً (مجموعة/عدّاد ← قائمة التبويب ← المواقيت) فلا يقفز فوقه معالجٌ
    // آخر (آخر معالج مُركَّب هو الأسبق — ولو بقي هذا مفعّلاً لأكل ضغطات
    // التنقل الداخلي وقفز للرئيسية مباشرة)
    BackHandler(enabled = tab != NoorTab.PRAYER && tab != NoorTab.ADHKAR) {
        tab = NoorTab.PRAYER
    }

    // مغادرة الرئيسية (أو تغيّر الثيم بعيداً عنها) تُطبّع حالة المزلاج كاملة —
    // الطبقة أُزيلت فنطاقها مات وأنيميشناتها أُلغيت؛ التطبيع **بلا شرط** (درس
    // alpha13: الإحباط المشروط كان يترك عالقاً خارج شرطه فترجع الأيقونة
    // «معلقة في النص») — كي تعود الرئيسية نظيفة عند العودة دائماً
    LaunchedEffect(tab, darkTheme) {
        if (tab != NoorTab.PRAYER) dial.abort(darkTheme)
    }

    // قاتل الومضة (بلاغ المالك alpha12: «ومضة بعد ما الوضع يتفعل سواء المظلم
    // أو الفاتح»): عند نهاية الرحلة يُطلب تثبيت الثيم فيصل عبر DataStore بعد
    // إطار أو أكثر — الإسقاط الفوري لغطاء المعاينة كان يكشف الثيم القديم
    // خلال تلك الفجوة = الومضة. الآن الغطاء (صورة كاملة للثيم الجديد والحدي
    // مسوق خارج الشاشة) يبقى معلقاً ويُسقط هنا في اللحظة نفسها التي يصل فيها
    // الثيم المثبت إلى هدفه — الغطاء والأساس يعرضان الثيم الجديد معاً فلا ومضة.
    LaunchedEffect(darkTheme) {
        if (dial.splitActive && darkTheme != dial.overlayIsLight) {
            dial.dropPreview()
        }
    }

    // حوار «ما الجديد» — يظهر مرة لكل نسخة، والاستبعاد يخفي كل حالاتها
    // لذلك الإصدار (alpha48: زر التحديث ينزّل من داخل التطبيق ويفتح مثبّت النظام)
    val dialogVisible = when (val s = updateState) {
        is UpdateUiState.Available -> dismissedVersion != s.info.versionName
        is UpdateUiState.Failed -> s.info != null && dismissedVersion != s.info?.versionName
        else -> false
    }
    if (dialogVisible) {
        UpdateDialog(
            state = updateState,
            download = downloadState,
            onDismiss = UpdateManager::dismiss,
            onUpdate = { _ ->
                updateInfo(updateState)?.let { UpdateManager.downloadAndInstall(it) }
            },
            onRetry = UpdateManager::checkNow,
        )
    }

    // حوار بوابات الموقع (توجيه المالك alpha18): «لا تظهر رسالة تفيد بتفعيل
    // الموقع والانترنت» — الرسالة الآن إلزامية على كل بوابة مقفولة، مع زر
    // يفتح الإعدادات المناسبة مباشرة، وزر إغلاق دائم.
    // alpha19: الحوار عبر **القالب الموحد** NoorNoticeDialog — نفس لون
    // الثيم ونفس تصميم حوار التحديث (بلاغ المالك: الإشعارات ليست بنفس
    // لون الثيم ولا بنفس التصميم).
    locationGate?.let { gate ->
        NoorNoticeDialog(
            title = "تحديد الموقع",
            message = gate.message.orEmpty(),
            icon = Icons.Filled.LocationOn,
            iconDescription = "الموقع",
            actionLabel = if (gate.action != LocationGate.Action.NONE) gate.actionLabel else null,
            onAction = if (gate.action != LocationGate.Action.NONE) {
                {
                    viewModel.dismissLocationGate()
                    // فشل فتح الإعدادات على أجهزة غريبة لا يكسر التطبيق
                    runCatching {
                        val intent = when (gate.action) {
                            LocationGate.Action.OPEN_APP_SETTINGS ->
                                android.content.Intent(
                                    android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    android.net.Uri.fromParts("package", context.packageName, null),
                                )
                            LocationGate.Action.OPEN_LOCATION_SETTINGS ->
                                android.content.Intent(
                                    android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS,
                                )
                            LocationGate.Action.OPEN_INTERNET_SETTINGS ->
                                android.content.Intent(
                                    android.provider.Settings.ACTION_WIRELESS_SETTINGS,
                                )
                            LocationGate.Action.NONE -> null
                        }?.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        intent?.let(context::startActivity)
                    }
                }
            } else {
                null
            },
            onDismiss = viewModel::dismissLocationGate,
        )
    }
}

/**
 * بيانات التحديث المعروضة من الحالة — null بلا تحديثٍ أو فشلٍ ببيانات
 */
private fun updateInfo(state: UpdateUiState): com.sawan4art.noor.data.update.UpdateInfo? =
    when (state) {
        is UpdateUiState.Available -> state.info
        is UpdateUiState.Failed -> state.info
        else -> null
    }

/**
 * جسد التطبيق المشترك — يُرسم مرة بالثيم المثبت (الأساس) ومرة بالثيم الهدف
 * (المعاينة المقسومة أثناء السحب) بنفس الحالة تماماً؛ كل آثار جانبية التطبيق
 * (فحص التحديثات، الشبكة، الموقع) تعيش خارج هذا الجسد فلا تتضاعف.
 */
@Composable
private fun NoorBody(
    tab: NoorTab,
    onTab: (NoorTab) -> Unit,
    uiState: HomeViewModel.HomeUiState,
    locating: Boolean,
    /** الثيم المثبّت الحالي — ينزل للرئيسية كي تدور جمل التذكير مع قلبه */
    isDark: Boolean,
    onDetectLocation: () -> Unit,
    onSetMethod: (com.sawan4art.noor.domain.prayer.CalculationMethod) -> Unit,
    onSetHanafi: (Boolean) -> Unit,
    onSetAccent: (String) -> Unit,
    /** اللون الأساسي — نص فارغ يعني «الحالي» (يتبع لون التطبيق) */
    onSetPrimary: (String) -> Unit,
    /** حفظ آخر صفحة مصحف — المصحف يفتح مكمنه الذي تركه القارئ (alpha49) */
    onSaveMushafPage: (Int) -> Unit,
    /** حفظ علامة مرجعية على آية أو مسحها — سورة صفر للمسح (alpha51) */
    onSetMushafBookmark: (Int, Int, Int) -> Unit,
    /** آيات الحفظ الميسر المعلّمة — سلاسل «سورة:آية» (alpha53) */
    hifzMemorized: Set<String>,
    /** تعليم آيات محفوظة أو مسح التعليم — الحفظ الميسر (alpha53) */
    onSetMushafHifz: (Int, List<Int>, Boolean) -> Unit,
    /** خطبة الجمعة — العنوان والنص والصور المحفوظة (alpha54) */
    khutbahTitle: String,
    khutbahText: String,
    khutbahImages: List<String>,
    /** حفظ خطبة الجمعة المكتوبة */
    onSetKhutbah: (String, String) -> Unit,
    /** استيراد صورة إلى الخطبة */
    onImportKhutbahImage: (android.net.Uri) -> Unit,
    /** حذف صورة من الخطبة */
    onDeleteKhutbahImage: (String) -> Unit,
    /** زر خطبة الجمعة في الرئيسية مفعّل؟ — قسم «خطبة الجمعة» (alpha56) */
    khutbahEnabled: Boolean,
    /** تفعيل/إيقاف زر خطبة الجمعة */
    onSetKhutbahEnabled: (Boolean) -> Unit,
    /** معرّف الشيخ المختار — قسم «أصوات الشيخ» في الإعدادات */
    reciterId: String,
    /** تغيير الشيخ المختار */
    onSetReciter: (String) -> Unit,
    adhanPrefs: com.sawan4art.noor.data.AdhanPrefs,
    adhanActions: AdhanActions,
    updateState: UpdateUiState,
    /** حالة تنزيل التحديث داخل التطبيق (alpha48) */
    downloadState: DownloadState,
    onCheckUpdates: () -> Unit,
    onDownloadUpdate: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        LiquidNavScaffold(
            selectedTab = tab,
            onSelectTab = onTab,
        ) {
            // الانزلاق الاتجاهي: التبويب «الأبعد يمينًا» في RTL يعني انزلاقًا لليسار
            AnimatedContent(
                targetState = tab,
                transitionSpec = {
                    val rtl = AppDirection == LayoutDirection.Rtl
                    val forward = targetState.ordinal > initialState.ordinal
                    val visual = if (forward != rtl) 1 else -1
                    (slideInHorizontally(tween(300)) { it / 3 * visual } +
                        fadeIn(tween(240))) togetherWith (
                        slideOutHorizontally(tween(260)) { -it / 4 * visual } +
                            fadeOut(tween(180))
                        )
                },
                label = "noorNavigation",
            ) { target ->
                when (target) {
                    NoorTab.PRAYER -> HomeScreen(
                        state = uiState,
                        locating = locating,
                        onDetectLocation = onDetectLocation,
                        isDark = isDark,
                        khutbahEnabled = khutbahEnabled,
                        khutbahTitle = khutbahTitle,
                        khutbahText = khutbahText,
                        khutbahImages = khutbahImages,
                        onSetKhutbah = onSetKhutbah,
                        onImportKhutbahImage = onImportKhutbahImage,
                        onDeleteKhutbahImage = onDeleteKhutbahImage,
                    )
                    NoorTab.TASBIH -> MisbahaScreen()
                    NoorTab.ADHKAR -> AzkarScreen(onExitTab = { onTab(NoorTab.PRAYER) })
                    NoorTab.HADITH -> HadithScreen()
                    NoorTab.SETTINGS -> SettingsScreen(
                        settings = uiState.settings,
                        locating = locating,
                        onDetectLocation = onDetectLocation,
                        onSetMethod = onSetMethod,
                        onSetHanafi = onSetHanafi,
                        onSetAccent = onSetAccent,
                        onSetPrimary = onSetPrimary,
                        adhanPrefs = adhanPrefs,
                        adhanActions = adhanActions,
                        updateState = updateState,
                        downloadState = downloadState,
                        onCheckUpdates = onCheckUpdates,
                        onDownloadUpdate = onDownloadUpdate,
                        reciterId = reciterId,
                        onSetReciter = onSetReciter,
                        khutbahEnabled = khutbahEnabled,
                        onSetKhutbahEnabled = onSetKhutbahEnabled,
                    )
                    NoorTab.QIBLA -> QiblaScreen(settings = uiState.settings)
                    NoorTab.QURAN -> MushafScreen(
                        initialPage = uiState.settings.mushafPage,
                        bookmarkSura = uiState.settings.mushafBookmarkSura,
                        bookmarkAyah = uiState.settings.mushafBookmarkAyah,
                        bookmarkPage = uiState.settings.mushafBookmarkPage,
                        reciterId = reciterId,
                        hifzMemorized = hifzMemorized,
                        onSavePage = onSaveMushafPage,
                        onSetBookmark = onSetMushafBookmark,
                        onSetHifz = onSetMushafHifz,
                        onBack = { onTab(NoorTab.PRAYER) },
                    )
                }
            }
        }
    }
}
