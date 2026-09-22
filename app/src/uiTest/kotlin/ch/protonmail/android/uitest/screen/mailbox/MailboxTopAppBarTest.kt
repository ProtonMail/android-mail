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

package ch.protonmail.android.uitest.screen.mailbox

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import ch.protonmail.android.R
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.maillabel.domain.model.MailLabel
import ch.protonmail.android.maillabel.presentation.text
import ch.protonmail.android.mailmailbox.presentation.mailbox.MailboxTopAppBar
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxTopAppBarState.Data
import ch.protonmail.android.test.annotations.suite.RegressionTest
import ch.protonmail.android.testdata.maillabel.MailLabelTestData
import ch.protonmail.android.uitest.util.HiltInstrumentedTest
import ch.protonmail.android.uitest.util.InstrumentationHolder
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test

@Suppress("SameParameterValue") // We want test parameters to be explicit
@RegressionTest
@HiltAndroidTest
internal class MailboxTopAppBarTest : HiltInstrumentedTest() {

    private val context = InstrumentationHolder.instrumentation.targetContext

    @Test
    fun hamburgerIconIsShownInDefaultMode() {
        setupScreenWithDefaultMode(MAIL_LABEL_INBOX)

        composeTestRule
            .onHamburgerIconButton()
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun labelNameIsShownInDefaultMode() {
        setupScreenWithDefaultMode(MAIL_LABEL_INBOX)

        composeTestRule
            .onNodeWithText(MAIL_LABEL_INBOX_TEXT)
            .assertIsDisplayed()
    }

    @Test
    fun backIconIsShownInSelectionMode() {
        setupScreenWithSelectionMode(MAIL_LABEL_INBOX, selectedCount = SELECTED_COUNT_TEN)

        composeTestRule
            .onExitSelectionModeIconButton()
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun correctCountIsShownInSelectionMode() {
        setupScreenWithSelectionMode(MAIL_LABEL_INBOX, selectedCount = SELECTED_COUNT_TEN)

        composeTestRule
            .onNodeWithText(
                context.resources.getQuantityString(
                    R.plurals.mailbox_toolbar_selected_count,
                    SELECTED_COUNT_TEN,
                    SELECTED_COUNT_TEN
                )
            )
            .assertIsDisplayed()
    }

    private fun setupScreenWithState(state: Data) {
        composeTestRule.setContent {
            ProtonTheme {
                MailboxTopAppBar(
                    state = state,
                    actions = MailboxTopAppBar.Actions(
                        onOpenMenu = {},
                        onExitSelectionMode = {},
                        onTitleClick = {},
                        onAccountAvatarClicked = {},
                        onNavigateToUpselling = { _, _ -> }
                    )
                )
            }
        }

        composeTestRule.waitForIdle()
    }

    private fun setupScreenWithDefaultMode(currentMailLabel: MailLabel) {
        val state = Data.DefaultMode(currentLabelName = currentMailLabel.text(), primaryAvatarItem = null)
        setupScreenWithState(state)
    }

    private fun setupScreenWithSelectionMode(currentMailLabel: MailLabel, selectedCount: Int) {
        val state = Data.SelectionMode(
            currentLabelName = currentMailLabel.text(),
            primaryAvatarItem = null,
            selectedCount = selectedCount
        )
        setupScreenWithState(state)
    }

    private fun SemanticsNodeInteractionsProvider.onHamburgerIconButton() =
        onNodeWithContentDescription(context.getString(R.string.mailbox_toolbar_menu_button_content_description))

    private fun SemanticsNodeInteractionsProvider.onExitSelectionModeIconButton() = onNodeWithContentDescription(
        context.getString(R.string.mailbox_toolbar_exit_selection_mode_button_content_description)
    )

    private companion object TestData {

        val MAIL_LABEL_INBOX = MailLabelTestData.inboxSystemLabel
        const val MAIL_LABEL_INBOX_TEXT = "Inbox"
        const val SELECTED_COUNT_TEN = 10
    }
}
