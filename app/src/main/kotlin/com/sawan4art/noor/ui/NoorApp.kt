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
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
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
import com.sawan4art.noor.ui.theme.ThemeDialMath
import com.sawan4art.noor.ui.theme.ThemeDialOverlay
import com.sawan4art.noor.ui.theme.ThemeDialState
import com.sawan4art.noor.ui.update.UpdateDialog
import kotlin.math.max
import kotlin.math.min

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

    // مسار قناع الانقسام — يُعاد بناؤه كل إطار فوق المسار نفسه بلا تخصيصات
    val maskPath = remember { Path() }

    // مزلاج المظهر — حالة واحدة تخدم طبقة الانقسام والمزلاج نفسه
    val dial = remember { ThemeDialState() }
    val commitThemeFromDial: (Boolean) -> Unit = { light ->
        viewModel.setThemeMode(if (light) ThemeMode.LIGHT else ThemeMode.DARK)
    }
    val placeDialTrack: (Float, Float, Float, Float) -> Unit = { l, t, w, h ->
        dial.placeTrack(l, t, w, h)
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

            // ===== طبقة المعاينة المقسومة — الثيم الهدف خلف الأيقونة =====
            // الفاتح يسار الحدّ (خلف الشمس المسحوبة يميناً) والمظلم يمينه،
            // والحدّ **قوس منحنٍ ينتفخ حول الأيقونة** (درس المالك: الضوء لا يستقيم)
            // بحافة ريشية مموّهة على شرائح — قناع DstIn من مسارات لا تتباعد أبداً.
            if (dial.splitActive) {
                NoorTheme(
                    themeMode = if (dial.overlayIsLight) ThemeMode.LIGHT else ThemeMode.DARK,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                            .drawWithContent {
                                drawContent()
                                if (dial.screenWidth > 0f && dial.screenHeight > 0f && dial.fadePx > 0f) {
                                    drawArcSplitMask(dial, maskPath)
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

            // ===== المزلاج فوق كل شيء — السائل والتوهج والأيقونة والإيماءة =====
            NoorTheme(themeMode = uiState.settings.themeMode) {
                ThemeDialOverlay(
                    dial = dial,
                    committedDark = darkTheme,
                    onCommitLight = commitThemeFromDial,
                )
            }
        }
    }

    BackHandler(enabled = tab != NoorTab.PRAYER) {
        tab = NoorTab.PRAYER
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
 * قناع الانقسام القوسي — يُرسم على طبقة الثيم الهدف بـ DstIn:
 * جسم المنطقة الهدف كامل حتى أول إزاحة ريشية، ثم شرائح الريش تتناوب
 * بالشفافية من شبه الكامل إلى الصفر حول حدّ القوس — فتبدو الحافّة
 * مموّهة منحنية كشفق لا خط حادّ. الشرائح لا تتراكب (فرق ثابت بين منحنيين
 * متوازيين) فلا تتضاعف شفافية بكسل ولا ثقوب winding.
 */
private fun DrawScope.drawArcSplitMask(dial: ThemeDialState, maskPath: Path) {
    val w = dial.screenWidth
    val h = dial.screenHeight
    val maskX = dial.maskX.value
    val cy = dial.trackCenterY
    val sag = dial.arcSagPx
    val reach = dial.arcReachPx
    val lightOnLeft = dial.overlayIsLight
    val offsets = ThemeDialMath.featherOffsets(dial.fadePx)
    val bands = offsets.size - 1
    if (lightOnLeft) {
        // المنطقة الهدف يسار الحدّ: الجسم الكامل حتى أول إزاحة ريشية
        targetBodyPath(maskPath, maskX, cy, reach, sag, true, offsets[0], w, h)
        drawPath(maskPath, Color.Black, blendMode = BlendMode.DstIn)
        for (k in 0 until bands) {
            featherBandPath(maskPath, maskX, cy, reach, sag, true, offsets[k], offsets[k + 1], h)
            drawPath(maskPath, Color.Black.copy(alpha = ThemeDialMath.featherAlpha(k)), blendMode = BlendMode.DstIn)
        }
    } else {
        // المنطقة الهدف يمين الحدّ: شرائح الريش تنزلق يساراً من الجسم الكامل
        targetBodyPath(maskPath, maskX, cy, reach, sag, false, offsets[bands], w, h)
        drawPath(maskPath, Color.Black, blendMode = BlendMode.DstIn)
        for (k in 0 until bands) {
            featherBandPath(
                maskPath, maskX, cy, reach, sag, false,
                offsets[bands - 1 - k], offsets[bands - k], h,
            )
            drawPath(maskPath, Color.Black.copy(alpha = ThemeDialMath.featherAlpha(k)), blendMode = BlendMode.DstIn)
        }
    }
}

/**
 * جسم المنطقة الهدف: من أول إزاحة ريشية عن حدّ القوس حتى حافة الشاشة المقابلة.
 * الحافة القوسية تُقص على حدود الشاشة (بلا تقاطع ذاتي) فلا ثقوب عند انسحاب الحدّ.
 */
private fun targetBodyPath(
    path: Path,
    maskX: Float,
    centerY: Float,
    reachPx: Float,
    sagPx: Float,
    lightOnLeft: Boolean,
    edgeOffset: Float,
    screenW: Float,
    screenH: Float,
) {
    path.rewind()
    val steps = 24
    var i = 0
    while (i <= steps) {
        val y = screenH * i / steps
        val x = ThemeDialMath.edgeX(y, maskX, centerY, reachPx, sagPx, lightOnLeft) + edgeOffset
        val clamped = if (lightOnLeft) max(x, -1f) else min(x, screenW + 1f)
        if (i == 0) path.moveTo(clamped, y) else path.lineTo(clamped, y)
        i++
    }
    if (lightOnLeft) {
        path.lineTo(-1f, screenH)
        path.lineTo(-1f, 0f)
    } else {
        path.lineTo(screenW + 1f, screenH)
        path.lineTo(screenW + 1f, 0f)
    }
    path.close()
}

/**
 * شريحة ريش بين منحنيين متوازيين لحدّ القوس (فرق ثابت بينهما) —
 * المتوازيان لا يتقاطعان أبداً فالشريحة بسيطة بلا ثقوب winding.
 */
private fun featherBandPath(
    path: Path,
    maskX: Float,
    centerY: Float,
    reachPx: Float,
    sagPx: Float,
    lightOnLeft: Boolean,
    offsetA: Float,
    offsetB: Float,
    screenH: Float,
) {
    path.rewind()
    val steps = 24
    var i = 0
    while (i <= steps) {
        val y = screenH * i / steps
        val x = ThemeDialMath.edgeX(y, maskX, centerY, reachPx, sagPx, lightOnLeft) + offsetA
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        i++
    }
    i = steps
    while (i >= 0) {
        val y = screenH * i / steps
        path.lineTo(ThemeDialMath.edgeX(y, maskX, centerY, reachPx, sagPx, lightOnLeft) + offsetB, y)
        i--
    }
    path.close()
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
