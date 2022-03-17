package ch.protonmail.android.uitest.test.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChild
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import ch.protonmail.android.mailsettings.presentation.accountsettings.AccountSettingScreen
import ch.protonmail.android.mailsettings.presentation.accountsettings.AccountSettingsState.Data
import ch.protonmail.android.mailsettings.presentation.accountsettings.TEST_TAG_ACCOUNT_SETTINGS_LIST
import ch.protonmail.android.uitest.annotation.SmokeTest
import me.proton.core.compose.theme.ProtonTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.experimental.categories.Category
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AccountSettingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val settingsState = Data(
        currentPlan = "Visionary",
        recoveryEmail = "recovery-email@protonmail.com",
        mailboxSize = 20_000,
        mailboxUsedSpace = 15_000,
        defaultEmail = "contact@protonmail.ch",
        isConversationMode = true
    )

    @Before
    fun setUp() {
        composeTestRule.setContent {
            ProtonTheme {
                AccountSettingScreen(
                    onBackClick = {},
                    onSubscriptionClick = {},
                    onPasswordManagementClick = {},
                    onRecoveryEmailClick = {},
                    onConversationModeClick = {},
                    onDefaultEmailAddressClick = {},
                    onDisplayNameClick = {},
                    onPrivacyClick = {},
                    onSearchMessageContentClick = {},
                    onLabelsFoldersClick = {},
                    onLocalStorageClick = {},
                    onSnoozeNotificationsClick = {},
                    state = settingsState
                )
            }
        }
    }

    @Test
    @Category(SmokeTest::class)
    fun testAccountSettingsScreenContainsAllExpectedSections() {
        composeTestRule.onNodeWithText("Account").assertIsDisplayed()
        composeTestRule.onNodeWithText("Addresses").assertIsDisplayed()
        composeTestRule.onNodeWithText("Mailbox").assertIsDisplayed()
        composeTestRule
            .onNodeWithTag(TEST_TAG_ACCOUNT_SETTINGS_LIST)
            .onChild()
            .performScrollToNode(hasText("Snooze"))
            .assertIsDisplayed()
    }

    @Test
    @Category(SmokeTest::class)
    fun testSettingsScreenDisplayStateCorrectly() {
        composeTestRule
            .onNodeWithText("Subscription")
            .assertTextContains("Visionary")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Recovery email")
            .assertTextContains("recovery-email@protonmail.com")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Password management")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Recovery email")
            .assertTextContains("recovery-email@protonmail.com")
            .assertIsDisplayed()

        // Assert values individually as android's `Formatter.formatShortFileSize` method
        // adds many non-printable BiDi chars when executing on some virtual devices
        // so checking for "1 kB / 2 kB" would not find a match
        composeTestRule
            .onNodeWithText("Mailbox size")
            .assertTextContains(value = "15", substring = true)
            .assertTextContains(value = "20", substring = true)
            .assertTextContains(value = "kB", substring = true, ignoreCase = true)
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Conversation mode")
            .assertTextContains("Enabled")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Default email address")
            .assertTextContains("contact@protonmail.ch")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Display name and signature")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithTag(TEST_TAG_ACCOUNT_SETTINGS_LIST)
            .onChild()
            .performScrollToNode(hasText("Privacy"))
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithTag(TEST_TAG_ACCOUNT_SETTINGS_LIST)
            .onChild()
            .performScrollToNode(hasText("Search message content"))
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithTag(TEST_TAG_ACCOUNT_SETTINGS_LIST)
            .onChild()
            .performScrollToNode(hasText("Labels and folders"))
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithTag(TEST_TAG_ACCOUNT_SETTINGS_LIST)
            .onChild()
            .performScrollToNode(hasText("Local storage"))
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithTag(TEST_TAG_ACCOUNT_SETTINGS_LIST)
            .onChild()
            .performScrollToNode(hasText("Snooze notifications"))
            .assertIsDisplayed()
    }

}
