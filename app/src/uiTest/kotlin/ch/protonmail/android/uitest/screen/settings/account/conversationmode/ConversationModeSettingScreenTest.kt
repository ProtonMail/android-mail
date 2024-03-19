/*
 * Copyright (c) 2022 Proton Technologies AG
 * This file is part of Proton Technologies AG and Proton Mail.
 *
 * Proton Mail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Mail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Mail. If not, see <https://www.gnu.org/licenses/>.
 */

package ch.protonmail.android.uitest.screen.settings.account.conversationmode

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.isToggleable
import ch.protonmail.android.mailsettings.presentation.accountsettings.conversationmode.ConversationModeSettingScreen
import ch.protonmail.android.mailsettings.presentation.accountsettings.conversationmode.ConversationModeSettingState.Data
import ch.protonmail.android.test.annotations.suite.RegressionTest
import ch.protonmail.android.uitest.util.HiltInstrumentedTest
import dagger.hilt.android.testing.HiltAndroidTest
import me.proton.core.compose.theme.ProtonTheme
import org.junit.Test

@RegressionTest
@HiltAndroidTest
internal class ConversationModeSettingScreenTest : HiltInstrumentedTest() {

    @Test
    fun testConversationModeToggleIsOnWhenStateIsTrue() {
        setupScreenWithState(Data(true))

        composeTestRule
            .onNode(isToggleable())
            .assertIsDisplayed()
            .assertIsEnabled()
            .assertIsOn()
    }

    @Test
    fun testConversationModeToggleIsOffWhenStateIsFalse() {
        setupScreenWithState(Data(false))

        composeTestRule
            .onNode(isToggleable())
            .assertIsDisplayed()
            .assertIsEnabled()
            .assertIsOff()
    }

    @Test
    fun testConversationModeToggleIsOffWhenStateIsInvalid() {
        setupScreenWithState(Data(null))

        composeTestRule
            .onNode(isToggleable())
            .assertIsDisplayed()
            .assertIsOff()
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
