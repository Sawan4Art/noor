package com.sawan4art.noor.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.animation.AnimatedContent
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
import com.sawan4art.noor.ui.home.HomeScreen
import com.sawan4art.noor.ui.home.HomeViewModel
import com.sawan4art.noor.ui.settings.SettingsScreen
import com.sawan4art.noor.ui.theme.NoorTheme

/** التطبيق عربي أولاً — الاتجاه مثبت يمين-لليسار مهما كانت لغة الجهاز */
enum class NoorScreen { HOME, SETTINGS }

@Composable
fun NoorApp(viewModel: HomeViewModel) {
    var screen by rememberSaveable { mutableStateOf(NoorScreen.HOME) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val locating by viewModel.locating.collectAsStateWithLifecycle()

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        NoorTheme(themeMode = uiState.settings.themeMode) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
            ) {
                AnimatedContent(
                    targetState = screen,
                    transitionSpec = {
                        (fadeIn(tween(260)) + scaleIn(initialScale = 0.97f, animationSpec = tween(260)))
                            .togetherWith(fadeOut(tween(180)))
                    },
                    label = "noorNavigation",
                ) { target ->
                    when (target) {
                        NoorScreen.HOME -> HomeScreen(
                            state = uiState,
                            locating = locating,
                            onDetectLocation = viewModel::detectLocation,
                            onOpenSettings = { screen = NoorScreen.SETTINGS },
                        )
                        NoorScreen.SETTINGS -> SettingsScreen(
                            settings = uiState.settings,
                            locating = locating,
                            onBack = { screen = NoorScreen.HOME },
                            onDetectLocation = viewModel::detectLocation,
                            onSetMethod = viewModel::setMethod,
                            onSetHanafi = viewModel::setHanafi,
                            onSetThemeMode = viewModel::setThemeMode,
                        )
                    }
                }
            }
        }
    }

    BackHandler(enabled = screen == NoorScreen.SETTINGS) {
        screen = NoorScreen.HOME
    }
}
