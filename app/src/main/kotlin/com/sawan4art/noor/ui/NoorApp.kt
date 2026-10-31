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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sawan4art.noor.data.ThemeMode
import com.sawan4art.noor.data.update.UpdateManager
import com.sawan4art.noor.data.update.UpdateUiState
import com.sawan4art.noor.ui.home.HomeScreen
import com.sawan4art.noor.ui.home.HomeViewModel
import com.sawan4art.noor.ui.nav.LiquidNavScaffold
import com.sawan4art.noor.ui.nav.NoorTab
import com.sawan4art.noor.ui.sections.ComingSoonScreen
import com.sawan4art.noor.ui.settings.SettingsScreen
import com.sawan4art.noor.ui.theme.NoorTheme
import com.sawan4art.noor.ui.theme.ThemeDialOverlay
import com.sawan4art.noor.ui.theme.ThemeDialState
import com.sawan4art.noor.ui.update.UpdateDialog

/** التطبيق عربي أولاً — الاتجاه مثبت يمين-لليسار مهما كانت لغة الجهاز */
private val AppDirection = LayoutDirection.Rtl

@Composable
fun NoorApp(viewModel: HomeViewModel) {
    var tab by rememberSaveable { mutableStateOf(NoorTab.PRAYER) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val locating by viewModel.locating.collectAsStateWithLifecycle()
    // نظام التحديثات — حالة واحدة تخدم حوار «ما الجديد» وبطاقة الإعدادات
    val updateState by UpdateManager.state.collectAsStateWithLifecycle()
    val dismissedVersion by UpdateManager.dismissedVersion.collectAsStateWithLifecycle()
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
    // كتابة إحداثيات المسار من الرئيسية **الحاضرة** فقط — الصفحة المغادرة
    // أثناء انزلاق التبويب تُبلّغ بمواضع منزلقة تسمّم المرسى فيُرسم السائل
    // منزاحاً وترجع الأيقونة «معلقة في النص» عند العودة (بلاغ المالك alpha13)
    val placeDialTrack: (Float, Float, Float, Float) -> Unit = { l, t, w, h ->
        if (tab == NoorTab.PRAYER) dial.placeTrack(l, t, w, h)
    }

    CompositionLocalProvider(LocalLayoutDirection provides AppDirection) {
        Box(modifier = Modifier.fillMaxSize()) {
            // ===== الطبقة الأساسية — الثيم المثبت =====
            NoorTheme(themeMode = uiState.settings.themeMode) {
                NoorBody(
                    tab = tab,
                    onTab = { tab = it },
                    uiState = uiState,
                    locating = locating,
                    onDetectLocation = viewModel::detectLocation,
                    onSetMethod = viewModel::setMethod,
                    onSetHanafi = viewModel::setHanafi,
                    onSetThemeMode = viewModel::setThemeMode,
                    updateState = updateState,
                    onCheckUpdates = UpdateManager::checkNow,
                    onInstallUpdate = {
                        (updateState as? UpdateUiState.ReadyToInstall)
                            ?.let { UpdateManager.installApk(it.apk) }
                    },
                    onDialPlaced = placeDialTrack,
                )
            }

            // ===== طبقة المعاينة المقسومة — الثيم الهدف خلف الأيقونة (الرئيسية فقط) =====
            // **قصّ صلب** (قرار alpha11 بعد بلاغ المالك بلقطة شاشة: الانقسام لم
            // يظهر على جهازه قط) — آلية القناع القديمة (Offscreen + DstIn بشرائح
            // ريشية) تفشل بصمت على بعض معالجات الرسوميات فتُخفي المعاينة كاملة.
            // clipRect عمل قصّ أساسي مستحيل الفشل: الفاتح يسار الحدّ والمظلم
            // يمينه دائماً، وحدّ القص يعلوه خط الضوء الحي المرسوم في المزلاج
            // (هالة ضيقة مركّزة على الحدّ حصرياً — توجيه المالك alpha15).
            if (onHome && dial.splitActive) {
                NoorTheme(
                    themeMode = if (dial.overlayIsLight) ThemeMode.LIGHT else ThemeMode.DARK,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .drawWithContent {
                                val edge = dial.maskX.value
                                clipRect(
                                    left = if (dial.overlayIsLight) 0f else edge,
                                    top = 0f,
                                    right = if (dial.overlayIsLight) edge else size.width,
                                    bottom = size.height,
                                ) {
                                    this@drawWithContent.drawContent()
                                }
                            },
                    ) {
                        NoorBody(
                            tab = tab,
                            onTab = { tab = it },
                            uiState = uiState,
                            locating = locating,
                            onDetectLocation = viewModel::detectLocation,
                            onSetMethod = viewModel::setMethod,
                            onSetHanafi = viewModel::setHanafi,
                            onSetThemeMode = viewModel::setThemeMode,
                            updateState = updateState,
                            onCheckUpdates = UpdateManager::checkNow,
                            onInstallUpdate = {
                                (updateState as? UpdateUiState.ReadyToInstall)
                                    ?.let { UpdateManager.installApk(it.apk) }
                            },
                            onDialPlaced = placeDialTrack,
                        )
                    }
                }
            }

            // ===== المزلاج فوق كل شيء — السائل والتوهج الخافت والأيقونة والإيماءة =====
            // في الرئيسية فقط (بطلب المالك) — بقية الصفحات بلا أيقونة ولا طبقة
            if (onHome) {
                NoorTheme(themeMode = uiState.settings.themeMode) {
                    ThemeDialOverlay(
                        dial = dial,
                        committedDark = darkTheme,
                        onCommitLight = commitThemeFromDial,
                    )
                }
            }
        }
    }

    BackHandler(enabled = tab != NoorTab.PRAYER) {
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
    // لذلك الإصدار (التنزيل الجاري يكمل ويُتابَع من بطاقة الإعدادات)
    val dialogVisible = when (val s = updateState) {
        is UpdateUiState.Available -> dismissedVersion != s.info.versionName
        is UpdateUiState.Downloading -> dismissedVersion != s.info.versionName
        is UpdateUiState.ReadyToInstall -> dismissedVersion != s.info.versionName
        is UpdateUiState.Failed -> s.info != null && dismissedVersion != s.info?.versionName
        else -> false
    }
    if (dialogVisible) {
        UpdateDialog(
            state = updateState,
            onDismiss = UpdateManager::dismiss,
            onDownload = UpdateManager::startDownload,
            onInstall = UpdateManager::installApk,
            onRetry = UpdateManager::startDownload,
        )
    }
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
    onDetectLocation: () -> Unit,
    onSetMethod: (com.sawan4art.noor.domain.prayer.CalculationMethod) -> Unit,
    onSetHanafi: (Boolean) -> Unit,
    onSetThemeMode: (ThemeMode) -> Unit,
    updateState: UpdateUiState,
    onCheckUpdates: () -> Unit,
    onInstallUpdate: () -> Unit,
    onDialPlaced: (Float, Float, Float, Float) -> Unit,
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
                        onDialPlaced = onDialPlaced,
                    )
                    NoorTab.SETTINGS -> SettingsScreen(
                        settings = uiState.settings,
                        locating = locating,
                        onDetectLocation = onDetectLocation,
                        onSetMethod = onSetMethod,
                        onSetHanafi = onSetHanafi,
                        onSetThemeMode = onSetThemeMode,
                        updateState = updateState,
                        onCheckUpdates = onCheckUpdates,
                        onInstallUpdate = onInstallUpdate,
                    )
                    else -> ComingSoonScreen(target)
                }
            }
        }
    }
}
