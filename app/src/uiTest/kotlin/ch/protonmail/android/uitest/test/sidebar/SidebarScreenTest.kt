/*
 * Copyright (c) 2021 Proton Technologies AG
 * This file is part of Proton Technologies AG and ProtonMail.
 *
 * ProtonMail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonMail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonMail.  If not, see <https://www.gnu.org/licenses/>.
 */

package ch.protonmail.android.uitest.test.sidebar

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChild
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import ch.protonmail.android.mailcommon.domain.AppInformation
import ch.protonmail.android.mailmailbox.domain.model.SidebarLocation.Inbox
import ch.protonmail.android.mailmailbox.presentation.Sidebar
import ch.protonmail.android.mailmailbox.presentation.SidebarState
import ch.protonmail.android.mailmailbox.presentation.TEST_TAG_SIDEBAR_MENU
import me.proton.core.compose.theme.ProtonTheme
import org.junit.Rule
import org.junit.Test

private const val APP_VERSION_FOOTER = "Proton Mail 6.0.0-alpha+test"

class SidebarScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun subscriptionIsShownWhenSidebarStateIsDisplaySubscription() {
        setupScreenWithState(showSubscriptionSidebarState())

        scrollToSidebarBottom()

        composeTestRule
            .onNodeWithText("Subscription")
            .assertIsDisplayed()
    }

    @Test
    fun subscriptionIsHiddenWhenSidebarStateIsHideSubscription() {
        setupScreenWithState(hideSubscriptionSidebarState())

        scrollToSidebarBottom()
        composeTestRule
            .onNodeWithText("Subscription")
            .assertDoesNotExist()
    }

    private fun scrollToSidebarBottom(): SemanticsNodeInteraction {
        return composeTestRule
            .onNodeWithTag(TEST_TAG_SIDEBAR_MENU)
            .onChild()
            .performScrollToNode(hasText(APP_VERSION_FOOTER, true))
    }

    private fun showSubscriptionSidebarState() = buildSidebarState(true)

    private fun hideSubscriptionSidebarState() = buildSidebarState(false)

    private fun buildSidebarState(isSubscriptionVisible: Boolean) = SidebarState(
        selectedLocation = Inbox,
        isSubscriptionVisible = isSubscriptionVisible,
        hasPrimaryAccount = false,
        appInformation = AppInformation(
            appName = "Proton Mail",
            appVersionName = "6.0.0-alpha+test"
        )
    )

    private fun setupScreenWithState(state: SidebarState) {
        composeTestRule.setContent {
            ProtonTheme {
                Sidebar(
                    onRemove = {},
                    onSignOut = {},
                    onSignIn = {},
                    onSwitch = {},
                    onMailLocation = {},
                    onFolder = {},
                    onLabel = {},
                    onSettings = {},
                    onSubscription = {},
                    onReportBug = {},
                    viewState = state
                )
            }
        }
    }
}
