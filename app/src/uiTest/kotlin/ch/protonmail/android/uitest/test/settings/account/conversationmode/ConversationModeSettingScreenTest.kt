package ch.protonmail.android.uitest.test.settings.account.conversationmode

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import ch.protonmail.android.mailsettings.presentation.accountsettings.conversationmode.ConversationModeSettingScreen
import ch.protonmail.android.mailsettings.presentation.accountsettings.conversationmode.ConversationModeSettingState.Data
import ch.protonmail.android.uitest.annotation.SmokeTest
import me.proton.core.compose.theme.ProtonTheme
import org.junit.Rule
import org.junit.Test
import org.junit.experimental.categories.Category

class ConversationModeSettingScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    @Category(SmokeTest::class)
    fun testConversationModeToggleIsOnWhenStateIsTrue() {
        setupScreenWithState(Data(true))

        composeTestRule
            .onNode(isToggleable())
            .assertIsDisplayed()
            .assertIsEnabled()
            .assertIsOn()
    }

    @Test
    @Category(SmokeTest::class)
    fun testConversationModeToggleIsOffWhenStateIsFalse() {
        setupScreenWithState(Data(false))

        composeTestRule
            .onNode(isToggleable())
            .assertIsDisplayed()
            .assertIsEnabled()
            .assertIsOff()
    }

    @Test
    @Category(SmokeTest::class)
    fun testConversationModeToggleIsDisabledWhenStateIsInvalid() {
        setupScreenWithState(Data(null))

        composeTestRule
            .onNode(isToggleable())
            .assertIsDisplayed()
            .assertIsOff()
            .assertIsNotEnabled()
    }

    private fun setupScreenWithState(state: Data) {
        composeTestRule.setContent {
            ProtonTheme {
                ConversationModeSettingScreen(
                    onBackClick = { },
                    onConversationModeToggled = { },
                    state = state
                )
            }
        }
    }

}
