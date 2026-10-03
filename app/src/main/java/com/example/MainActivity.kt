package com.example

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.BottomDockNav
import com.example.ui.screens.DictionaryScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.NeuralTrainingScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TranslateScreen
import com.example.ui.theme.LangfactoryTheme
import com.example.viewmodel.AppScreen
import com.example.viewmodel.AppThemeMode
import com.example.viewmodel.TranslatorViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: TranslatorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
            window.isStatusBarContrastEnforced = false
        }

        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val isSystemDark = isSystemInDarkTheme()
            val isDark = when (themeMode) {
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
                AppThemeMode.SYSTEM -> isSystemDark
            }

            DisposableEffect(isDark) {
                enableEdgeToEdge(
                    statusBarStyle = if (isDark) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                    },
                    navigationBarStyle = if (isDark) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                    }
                )
                onDispose {}
            }

            LangfactoryTheme(darkTheme = isDark) {
                MainAppContent(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppContent(viewModel: TranslatorViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val isOfflineModelMode by viewModel.isOfflineModelMode.collectAsStateWithLifecycle()

    // If online model is selected and user is on NEURAL_TRAINING, navigate to HOME
    LaunchedEffect(isOfflineModelMode, currentScreen) {
        if (!isOfflineModelMode && currentScreen == AppScreen.NEURAL_TRAINING) {
            viewModel.navigateTo(AppScreen.HOME)
        }
    }

    // BackHandler ensures back gesture/button returns to Home from any secondary tab
    BackHandler(enabled = currentScreen != AppScreen.HOME && currentScreen != AppScreen.ONBOARDING) {
        viewModel.navigateTo(AppScreen.HOME)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Main Screen Content Container
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "screen_transition"
            ) { screen ->
                when (screen) {
                    AppScreen.ONBOARDING -> {
                        OnboardingScreen(
                            viewModel = viewModel,
                            onGetStarted = { viewModel.completeOnboarding() }
                        )
                    }
                    AppScreen.HOME -> {
                        HomeScreen(
                            viewModel = viewModel,
                            onNavigate = { viewModel.navigateTo(it) }
                        )
                    }
                    AppScreen.TRANSLATE -> {
                        TranslateScreen(
                            viewModel = viewModel,
                            onBack = { viewModel.navigateTo(AppScreen.HOME) }
                        )
                    }
                    AppScreen.HISTORY -> {
                        HistoryScreen(
                            viewModel = viewModel,
                            onNavigate = { viewModel.navigateTo(it) }
                        )
                    }
                    AppScreen.NEURAL_TRAINING -> {
                        NeuralTrainingScreen(
                            viewModel = viewModel
                        )
                    }
                    AppScreen.DICTIONARY -> {
                        DictionaryScreen(
                            viewModel = viewModel,
                            onNavigate = { viewModel.navigateTo(it) }
                        )
                    }
                    AppScreen.SETTINGS -> {
                        SettingsScreen(
                            viewModel = viewModel,
                            onNavigate = { viewModel.navigateTo(it) }
                        )
                    }
                }
            }
        }

        // Floating Bottom Dock Navigation
        if (currentScreen != AppScreen.ONBOARDING) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.background.copy(alpha = 0.85f),
                                MaterialTheme.colorScheme.background
                            )
                        )
                    )
                    .navigationBarsPadding()
            ) {
                BottomDockNav(
                    currentScreen = currentScreen,
                    onNavigate = { screen ->
                        if (screen == AppScreen.TRANSLATE) {
                            viewModel.openTranslateFromNavbar()
                        } else {
                            viewModel.navigateTo(screen)
                        }
                    },
                    onVoiceClick = {
                        viewModel.openTranslateFromNavbar()
                    }
                )
            }
        }
    }
}
