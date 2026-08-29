package com.memento.app.ui

import android.content.res.Configuration
import android.util.TypedValue
import android.view.ContextThemeWrapper
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.memento.app.R
import com.memento.app.MementoEntryContent
import com.memento.app.data.preferences.ThemeMode
import com.memento.app.ui.onboarding.OnboardingContent
import com.memento.app.ui.onboarding.OnboardingScreen
import com.memento.app.ui.settings.BackupNotice
import com.memento.app.ui.settings.SettingsUiState
import com.memento.app.ui.theme.MementoTheme
import com.memento.app.ui.theme.mementoThemeInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class OnboardingComposeTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun firstLaunchShowsOnboardingInsteadOfTheApp() {
        setEntry(onboardingCompleted = false)

        composeRule.onNodeWithText("Empezar").assertIsDisplayed()
        composeRule.onNodeWithText(APP_SENTINEL).assertDoesNotExist()
    }

    @Test
    fun startCompletesTheEntryFlowAndShowsTheApp() {
        lateinit var completed: MutableState<Boolean?>
        composeRule.setContent {
            completed = remember { mutableStateOf(false) }
            MementoTheme(ThemeMode.LIGHT) {
                MementoEntryContent(
                    onboardingCompleted = completed.value,
                    settingsState = SettingsUiState(),
                    onStart = { completed.value = true },
                    onImport = {},
                    onConfirmRestore = {},
                    onCancelRestore = {},
                ) { Text(APP_SENTINEL) }
            }
        }

        composeRule.onNodeWithText("Empezar").performClick()

        composeRule.onNodeWithText(APP_SENTINEL).assertIsDisplayed()
        composeRule.onNodeWithText("Empezar").assertDoesNotExist()
    }

    @Test
    fun existingUserDoesNotSeeOnboarding() {
        setEntry(onboardingCompleted = true)

        composeRule.onNodeWithText(APP_SENTINEL).assertIsDisplayed()
        composeRule.onNodeWithText("Empezar").assertDoesNotExist()
    }

    @Test
    fun restoreRequestDoesNotCompleteOnboardingAndKeepsThePrimaryPathAvailable() {
        var restoreRequests = 0
        var starts = 0
        composeRule.setContent {
            MementoTheme(ThemeMode.LIGHT) {
                OnboardingContent(
                    state = SettingsUiState(),
                    onStart = { starts++ },
                    onRestoreRequested = { restoreRequests++ },
                )
            }
        }

        composeRule.onNodeWithText("Restaurar biblioteca").performClick()

        composeRule.runOnIdle {
            assertEquals(1, restoreRequests)
            assertEquals(0, starts)
        }
        composeRule.onNodeWithText("Empezar").assertIsDisplayed().assertIsEnabled()
    }

    @Test
    fun successfulRestoreCompletesOnboarding() {
        var starts = 0
        composeRule.setContent {
            MementoTheme(ThemeMode.LIGHT) {
                OnboardingScreen(
                    state = SettingsUiState(notice = BackupNotice.RESTORED),
                    onStart = { starts++ },
                    onImport = {},
                    onConfirmRestore = {},
                    onCancelRestore = {},
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.runOnIdle { assertEquals(1, starts) }
    }

    @Test
    fun systemModeTracksDarkAndLightAndroidConfiguration() {
        lateinit var systemDark: MutableState<Boolean>
        var observedDark: Boolean? = null
        composeRule.setContent {
            systemDark = remember { mutableStateOf(true) }
            WithSystemAppearance(dark = systemDark.value) {
                MementoTheme(ThemeMode.SYSTEM) {
                    observedDark = MaterialTheme.mementoThemeInfo.isDark
                    Text("Theme probe")
                }
            }
        }

        composeRule.runOnIdle { assertEquals(true, observedDark) }
        composeRule.runOnIdle { systemDark.value = false }
        composeRule.waitForIdle()
        composeRule.runOnIdle { assertEquals(false, observedDark) }
    }

    @Test
    fun androidRootThemeUsesNightQualifiedSystemBarAppearance() {
        assertTrue(rootThemeUsesLightStatusIcons(dark = false))
        assertFalse(rootThemeUsesLightStatusIcons(dark = true))
    }

    @Test
    fun primaryCtaRemainsReachableOnSmallScreensWithLargeText() {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 1.5f)) {
                MementoTheme(ThemeMode.LIGHT) {
                    Box(Modifier.width(320.dp).height(480.dp)) {
                        OnboardingContent(SettingsUiState(), onStart = {}, onRestoreRequested = {})
                    }
                }
            }
        }

        composeRule.onNodeWithText("Empezar").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun importErrorIsFriendlyAndDoesNotBlockStartingWithoutRestore() {
        composeRule.setContent {
            MementoTheme(ThemeMode.DARK) {
                OnboardingContent(
                    state = SettingsUiState(error = "El archivo no parece un backup válido de Memento."),
                    onStart = {},
                    onRestoreRequested = {},
                )
            }
        }

        composeRule.onNodeWithText("No hemos podido restaurar este backup.").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Empezar").assertExists().assertIsEnabled()
    }

    private fun setEntry(onboardingCompleted: Boolean?) {
        composeRule.setContent {
            MementoTheme(ThemeMode.LIGHT) {
                MementoEntryContent(
                    onboardingCompleted = onboardingCompleted,
                    settingsState = SettingsUiState(),
                    onStart = {},
                    onImport = {},
                    onConfirmRestore = {},
                    onCancelRestore = {},
                ) { Text(APP_SENTINEL) }
            }
        }
    }

    private fun rootThemeUsesLightStatusIcons(dark: Boolean): Boolean {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val configuration = Configuration(context.resources.configuration).apply {
            val nightMode = if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
            uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or nightMode
        }
        val themedContext = ContextThemeWrapper(context.createConfigurationContext(configuration), R.style.Theme_Memento)
        val value = TypedValue()
        check(themedContext.theme.resolveAttribute(android.R.attr.windowLightStatusBar, value, true))
        return value.data != 0
    }

    @Composable
    private fun WithSystemAppearance(dark: Boolean, content: @Composable () -> Unit) {
        val current = LocalConfiguration.current
        val configuration = remember(current, dark) {
            Configuration(current).apply {
                val nightMode = if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
                uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or nightMode
            }
        }
        CompositionLocalProvider(LocalConfiguration provides configuration, content = content)
    }

    private companion object {
        const val APP_SENTINEL = "Memento app content"
    }
}
