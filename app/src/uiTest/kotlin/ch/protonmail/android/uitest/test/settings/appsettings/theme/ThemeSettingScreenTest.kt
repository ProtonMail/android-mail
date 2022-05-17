package ch.protonmail.android.uitest.test.settings.appsettings.theme

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChild
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import ch.protonmail.android.mailsettings.domain.model.Theme
import ch.protonmail.android.mailsettings.domain.model.Theme.DARK
import ch.protonmail.android.mailsettings.domain.model.Theme.LIGHT
import ch.protonmail.android.mailsettings.domain.model.Theme.SYSTEM_DEFAULT
import ch.protonmail.android.mailsettings.presentation.R.string
import ch.protonmail.android.mailsettings.presentation.settings.theme.ThemeSettingsScreen
import ch.protonmail.android.mailsettings.presentation.settings.theme.ThemeSettingsState.Data
import ch.protonmail.android.mailsettings.presentation.settings.theme.ThemeUiModel
import ch.protonmail.android.uitest.annotation.SmokeTest
import me.proton.core.compose.theme.ProtonTheme
import org.junit.Rule
import org.junit.Test
import org.junit.experimental.categories.Category
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ThemeSettingScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    @Category(SmokeTest::class)
    fun testOnlySystemDefaultIsSelectedWhenThemeIsSystemDefault() {
        setupScreenWithSystemDefaultTheme()

        composeTestRule
            .onNodeWithText("System default")
            .onChild()
            .assertIsDisplayed()
            .assertIsSelected()

        composeTestRule
            .onNodeWithText("Light")
            .onChild()
            .assertIsDisplayed()
            .assertIsNotSelected()

        composeTestRule
            .onNodeWithText("Dark")
            .onChild()
            .assertIsDisplayed()
            .assertIsNotSelected()
    }

    @Test
    @Category(SmokeTest::class)
    fun testLightIsSelectedWhenThemeIsLight() {
        setupScreenWithLightTheme()

        composeTestRule
            .onNodeWithText("System default")
            .onChild()
            .assertIsDisplayed()
            .assertIsNotSelected()

        composeTestRule
            .onNodeWithText("Light")
            .onChild()
            .assertIsDisplayed()
            .assertIsSelected()

        composeTestRule
            .onNodeWithText("Dark")
            .onChild()
            .assertIsDisplayed()
            .assertIsNotSelected()
    }

    @Test
    @Category(SmokeTest::class)
    fun testDarkIsSelectedWhenThemeIsDark() {
        setupScreenWithDarkTheme()

        composeTestRule
            .onNodeWithText("System default")
            .onChild()
            .assertIsDisplayed()
            .assertIsNotSelected()

        composeTestRule
            .onNodeWithText("Light")
            .onChild()
            .assertIsDisplayed()
            .assertIsNotSelected()

        composeTestRule
            .onNodeWithText("Dark")
            .onChild()
            .assertIsDisplayed()
            .assertIsSelected()
    }

    @Test
    @Category(SmokeTest::class)
    fun testCallbackIsInvokedWithThemeIdWhenAThemeIsSelected() {
        var selectedTheme: Theme? = null
        setupScreenWithSystemDefaultTheme {
            selectedTheme = it
        }
        assertNull(selectedTheme)

        composeTestRule
            .onNodeWithText("System default")
            .onChild()
            .assertIsDisplayed()
            .assertIsSelected()

        composeTestRule
            .onNodeWithText("Dark")
            .performClick()

        assertEquals(DARK, selectedTheme)
    }


    private fun setupScreenWithLightTheme() {
        setupScreenWithState(
            Data(
                buildThemesList(isSystemDefault = false, isLight = true, isDark = false)
            )
        )
    }

    private fun setupScreenWithDarkTheme() {
        setupScreenWithState(
            Data(
                buildThemesList(isSystemDefault = false, isLight = false, isDark = true)
            )
        )
    }

    private fun setupScreenWithSystemDefaultTheme(onThemeSelected: (Theme) -> Unit = {}) {
        setupScreenWithState(
            Data(
                buildThemesList(isSystemDefault = true, isLight = false, isDark = false)
            ),
            onThemeSelected
        )
    }

    private fun buildThemesList(
        isSystemDefault: Boolean,
        isLight: Boolean,
        isDark: Boolean
    ) = listOf(
        ThemeUiModel(SYSTEM_DEFAULT, string.mail_settings_system_default, isSystemDefault),
        ThemeUiModel(LIGHT, string.mail_settings_theme_light, isLight),
        ThemeUiModel(DARK, string.mail_settings_theme_dark, isDark)
    )


    private fun setupScreenWithState(
        state: Data,
        onThemeSelected: (Theme) -> Unit = {}
    ) {
        composeTestRule.setContent {
            ProtonTheme {
                ThemeSettingsScreen(
                    onBackClick = { },
                    onThemeSelected = onThemeSelected,
                    state = state
                )
            }
        }
    }

}
