package ch.protonmail.android.uitest.test.settings.appsettings

import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChild
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import ch.protonmail.android.mailcommon.domain.AppInformation
import ch.protonmail.android.mailsettings.domain.model.AppSettings
import ch.protonmail.android.mailsettings.presentation.settings.AccountInfo
import ch.protonmail.android.mailsettings.presentation.settings.MainSettingsScreen
import ch.protonmail.android.mailsettings.presentation.settings.SettingsState.Data
import ch.protonmail.android.mailsettings.presentation.settings.TEST_TAG_SETTINGS_LIST
import ch.protonmail.android.uitest.annotation.SmokeTest
import me.proton.core.compose.theme.ProtonTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.experimental.categories.Category

class SettingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val settingsState = Data(
        AccountInfo("ProtonTest", "user-test@proton.ch"),
        AppSettings(
            hasAutoLock = false,
            hasAlternativeRouting = true,
            customAppLanguage = null,
            hasCombinedContacts = true
        ),
        AppInformation(appVersionName = "6.0.0-alpha-adf8373a")
    )

    @Before
    fun setUp() {
        composeTestRule.setContent {
            ProtonTheme {
                MainSettingsScreen(
                    state = settingsState,
                    onAccountClick = {},
                    onThemeClick = {},
                    onPushNotificationsClick = {},
                    onAutoLockClick = {},
                    onAlternativeRoutingClick = {},
                    onAppLanguageClick = {},
                    onCombinedContactsClick = {},
                    onSwipeActionsClick = {},
                    onBackClick = {}
                )
            }
        }
    }

    @Test
    @Category(SmokeTest::class)
    fun testSettingsScreenContainsAllExpectedSections() {
        composeTestRule.onNodeWithText("Account settings").assertIsDisplayed()
        composeTestRule.onNodeWithText("App settings").assertIsDisplayed()
        composeTestRule
            .onNodeWithTag(TEST_TAG_SETTINGS_LIST)
            .onChild()
            .performScrollToNode(hasText("App Information"))
            .assertIsDisplayed()
    }

    @Test
    @Category(SmokeTest::class)
    fun testSettingsScreenDisplayStateCorrectly() {
        composeTestRule
            .onNodeWithText("ProtonTest")
            .assertTextContains("user-test@proton.ch")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Theme")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Push notifications")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Auto Lock")
            .assertTextContains("Disabled")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Alternative routing")
            .assertTextContains("Allowed")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("App language")
            .assertTextContains("Auto-detect")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Combined contacts")
            .assertTextContains("Enabled")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithTag(TEST_TAG_SETTINGS_LIST)
            .onChild()
            .performScrollToNode(hasText("Swipe actions"))
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithTag(TEST_TAG_SETTINGS_LIST)
            .onChild()
            .performScrollToNode(hasText("App version"))

        composeTestRule
            .onNodeWithText("6.0.0-alpha-adf8373a")
            .assertHasNoClickAction()
            .assertIsDisplayed()
    }

}
