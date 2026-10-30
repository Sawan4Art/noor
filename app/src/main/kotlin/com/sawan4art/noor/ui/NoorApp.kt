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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sawan4art.noor.data.ThemeMode
import com.sawan4art.noor.ui.home.HomeScreen
import com.sawan4art.noor.ui.home.HomeViewModel
import com.sawan4art.noor.ui.nav.LiquidNavScaffold
import com.sawan4art.noor.ui.nav.NoorTab
import com.sawan4art.noor.ui.sections.ComingSoonScreen
import com.sawan4art.noor.ui.settings.SettingsScreen
import com.sawan4art.noor.ui.theme.NoorTheme

/** التطبيق عربي أولاً — الاتجاه مثبت يمين-لليسار مهما كانت لغة الجهاز */
private val AppDirection = LayoutDirection.Rtl

@Composable
fun NoorApp(viewModel: HomeViewModel) {
    var tab by rememberSaveable { mutableStateOf(NoorTab.PRAYER) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val locating by viewModel.locating.collectAsStateWithLifecycle()
    // الظلام الفعلي الحالي — مبدّل الهيدر يقلبه (من النظام → عكس المعمول به الآن)
    val darkTheme = when (uiState.settings.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    CompositionLocalProvider(LocalLayoutDirection provides AppDirection) {
        NoorTheme(themeMode = uiState.settings.themeMode) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
            ) {
                LiquidNavScaffold(
                    selectedTab = tab,
                    onSelectTab = { tab = it },
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
                                onDetectLocation = viewModel::detectLocation,
                                onToggleTheme = {
                                    viewModel.setThemeMode(
                                        if (darkTheme) ThemeMode.LIGHT else ThemeMode.DARK,
                                    )
                                },
                            )
                            NoorTab.SETTINGS -> SettingsScreen(
                                settings = uiState.settings,
                                locating = locating,
                                onDetectLocation = viewModel::detectLocation,
                                onSetMethod = viewModel::setMethod,
                                onSetHanafi = viewModel::setHanafi,
                                onSetThemeMode = viewModel::setThemeMode,
                            )
                            else -> ComingSoonScreen(target)
                        }
                    }
                }
            }
        }
    }

    BackHandler(enabled = tab != NoorTab.PRAYER) {
        tab = NoorTab.PRAYER
    }
}
