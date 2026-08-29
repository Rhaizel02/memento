package com.memento.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.memento.app.ui.navigation.MementoApp
import com.memento.app.ui.onboarding.OnboardingScreen
import com.memento.app.ui.settings.SettingsViewModel
import com.memento.app.ui.theme.MementoTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val themeMode by settingsViewModel.themeMode.collectAsStateWithLifecycle()
            val themePalette by settingsViewModel.themePalette.collectAsStateWithLifecycle()
            val onboardingCompleted by settingsViewModel.onboardingCompleted.collectAsStateWithLifecycle()
            val settingsState by settingsViewModel.state.collectAsStateWithLifecycle()

            MementoTheme(themeMode = themeMode, palette = themePalette) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    when (onboardingCompleted) {
                        null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                        false -> OnboardingScreen(
                            state = settingsState,
                            onStart = settingsViewModel::completeOnboarding,
                            onImport = settingsViewModel::prepareImport,
                            onConfirmRestore = settingsViewModel::confirmRestore,
                            onCancelRestore = settingsViewModel::cancelRestore,
                        )
                        true -> MementoApp(
                            themeMode = themeMode,
                            themePalette = themePalette,
                            onThemeModeChanged = settingsViewModel::setThemeMode,
                            onThemePaletteChanged = settingsViewModel::setThemePalette,
                        )
                    }
                }
            }
        }
    }
}
