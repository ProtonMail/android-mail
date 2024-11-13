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
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import ch.protonmail.android.R
import ch.protonmail.android.maillabel.domain.model.MailLabel
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.maillabel.presentation.text
import ch.protonmail.android.mailmailbox.presentation.mailbox.MailboxTopAppBar
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxTopAppBarState.Data
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.UpgradeStorageState
import ch.protonmail.android.test.annotations.suite.RegressionTest
import ch.protonmail.android.uitest.util.HiltInstrumentedTest
import ch.protonmail.android.uitest.util.InstrumentationHolder
import dagger.hilt.android.testing.HiltAndroidTest
import me.proton.core.compose.theme.ProtonTheme
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
    fun actionsAreShownInDefaultMode() {
        setupScreenWithDefaultMode(MAIL_LABEL_INBOX)

        composeTestRule
            .onSearchIconButton()
            .assertIsDisplayed()
            .assertHasClickAction()

        composeTestRule
            .onComposeIconButton()
            .assertIsDisplayed()
            .assertHasClickAction()
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

    @Test
    fun actionsAreHiddenInSelectionMode() {
        setupScreenWithSelectionMode(MAIL_LABEL_INBOX, selectedCount = SELECTED_COUNT_TEN)

        composeTestRule
            .onSearchIconButton()
            .assertDoesNotExist()

        composeTestRule
            .onComposeIconButton()
            .assertDoesNotExist()
    }

    @Test
    fun exitSearchModeButtonShownInSearchMode() {
        setupScreenWithSearchMode(MAIL_LABEL_INBOX, searchQuery = "query")

        composeTestRule
            .onExitSearchIconButton()
            .assertIsDisplayed()
            .assertHasClickAction()

    }

    @Test
    fun clearSearchQueryButtonShownInSearchModeWhenQueryEntered() {
        setupScreenWithSearchMode(MAIL_LABEL_INBOX, searchQuery = "query")

        composeTestRule
            .onClearSearchQueryIconButton()
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun clearSearchQueryButtonHiddenInSearchModeWhenQueryIsEmpty() {
        setupScreenWithSearchMode(MAIL_LABEL_INBOX, searchQuery = "")

        composeTestRule
            .onClearSearchQueryIconButton()
            .assertDoesNotExist()
    }

    @Test
    fun searchTextFieldShouldHaveFocusWhenSearchModeStarted() {
        setupScreenWithSearchMode(MAIL_LABEL_INBOX, searchQuery = "")

        composeTestRule
            .onNodeWithText("")
            .assertIsDisplayed()
            .assertIsFocused()
    }

    @Test
    fun searchTextFieldShouldNotHaveFocusWhenSearchModeStartedWithExistingQuery() {
        setupScreenWithSearchMode(MAIL_LABEL_INBOX, searchQuery = "search query")

        composeTestRule
            .onNodeWithText("search query")
            .assertIsDisplayed()
            .assertIsNotFocused()
    }

    @Test
    fun clearSearchQueryButtonClearsSearchTextField() {
        // Given
        setupScreenWithSearchMode(MAIL_LABEL_INBOX, searchQuery = "non-empty query")
        composeTestRule
            .onNodeWithText("non-empty query")
            .assertIsDisplayed()

        // When
        composeTestRule
            .onClearSearchQueryIconButton()
            .performClick()

        // Then
        composeTestRule
            .onNodeWithText("")
            .assertIsDisplayed()
    }

    @Test
    fun clearSearchQueryButtonBecomeVisibleAfterEnteringQuery() {
        // Given
        setupScreenWithSearchMode(MAIL_LABEL_INBOX, searchQuery = "")
        composeTestRule
            .onClearSearchQueryIconButton()
            .assertDoesNotExist()

        // When
        composeTestRule
            .onNodeWithText("")
            .performTextInput("some query")

        // Then
        composeTestRule
            .onClearSearchQueryIconButton()
            .assertIsDisplayed()
    }

    private fun setupScreenWithState(state: Data) {
        composeTestRule.setContent {
            ProtonTheme {
                MailboxTopAppBar(
                    state = state,
                    upgradeStorageState = UpgradeStorageState(notificationDotVisible = false),
                    actions = MailboxTopAppBar.Actions(
                        onOpenMenu = {},
                        onExitSelectionMode = {},
                        onExitSearchMode = {},
                        onTitleClick = {},
                        onEnterSearchMode = {},
                        onSearch = {},
                        onOpenComposer = {},
                        onNavigateToStandaloneUpselling = {},
                        onOpenUpsellingPage = {},
                        onCloseUpsellingPage = {}
                    )
                )
            }
        }

        composeTestRule.waitForIdle()
    }

    private fun setupScreenWithDefaultMode(currentMailLabel: MailLabel) {
        val state = Data.DefaultMode(currentLabelName = currentMailLabel.text())
        setupScreenWithState(state)
    }

    private fun setupScreenWithSelectionMode(currentMailLabel: MailLabel, selectedCount: Int) {
        val state = Data.SelectionMode(
            currentLabelName = currentMailLabel.text(),
            selectedCount = selectedCount
        )
        setupScreenWithState(state)
    }

    private fun setupScreenWithSearchMode(currentMailLabel: MailLabel, searchQuery: String) {
        val state = Data.SearchMode(
            currentLabelName = currentMailLabel.text(),
            searchQuery = searchQuery
        )
        setupScreenWithState(state)
    }

    private fun SemanticsNodeInteractionsProvider.onHamburgerIconButton() =
        onNodeWithContentDescription(context.getString(R.string.mailbox_toolbar_menu_button_content_description))

    private fun SemanticsNodeInteractionsProvider.onExitSelectionModeIconButton() = onNodeWithContentDescription(
        context.getString(R.string.mailbox_toolbar_exit_selection_mode_button_content_description)
    )

    private fun SemanticsNodeInteractionsProvider.onSearchIconButton() =
        onNodeWithContentDescription(context.getString(R.string.mailbox_toolbar_search_button_content_description))

    private fun SemanticsNodeInteractionsProvider.onComposeIconButton() =
        onNodeWithContentDescription(context.getString(R.string.mailbox_toolbar_compose_button_content_description))

    private fun SemanticsNodeInteractionsProvider.onExitSearchIconButton() =
        onNodeWithContentDescription(context.getString(R.string.mailbox_toolbar_exit_search_mode_content_description))

    private fun SemanticsNodeInteractionsProvider.onClearSearchQueryIconButton() = onNodeWithContentDescription(
        context.getString(R.string.mailbox_toolbar_searchview_clear_search_query_content_description)
    )

    private companion object TestData {

        val MAIL_LABEL_INBOX = MailLabel.System(MailLabelId.System.Inbox)
        const val MAIL_LABEL_INBOX_TEXT = "Inbox"
        const val SELECTED_COUNT_TEN = 10
    }
}
