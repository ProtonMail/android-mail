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

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.maillabel.domain.model.MailLabel
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.mailmailbox.presentation.mailbox.MailboxScreen
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxItemUiModel
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxListState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxState
import ch.protonmail.android.uitest.util.awaitDisplayed
import kotlinx.coroutines.flow.flowOf
import me.proton.core.compose.component.PROTON_PROGRESS_TEST_TAG
import me.proton.core.compose.theme.ProtonTheme
import org.junit.Rule
import org.junit.Test

internal class MailboxScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun whenNotLoadingAndNoItemThenEmptyMailboxIsDisplayed() {
        val mailboxListState = MailboxListState.Data(
            currentMailLabel = MailLabel.System(MailLabelId.System.Inbox),
            Effect.empty(),
            Effect.empty()
        )
        val mailboxState = MailboxState.Loading.copy(mailboxListState = mailboxListState)
        setupScreen(state = mailboxState, items = emptyList())

        composeTestRule
            .onEmptyMailbox()
            .awaitDisplayed(composeTestRule)
            .assertIsDisplayed()
    }

    @Test
    fun givenEmptyMailboxIsDisplayedWhenSwipeDownThenRefreshIsTriggered() {
        val mailboxListState = MailboxListState.Data(
            currentMailLabel = MailLabel.System(MailLabelId.System.Inbox),
            Effect.empty(),
            Effect.empty()
        )
        val mailboxState = MailboxState.Loading.copy(mailboxListState = mailboxListState)
        setupScreen(state = mailboxState, items = emptyList())

        composeTestRule
            .onEmptyMailbox()
            .awaitDisplayed(composeTestRule)

        composeTestRule
            .onEmptyMailbox()
            .performTouchInput { swipeDown() }

        composeTestRule
            .onNodeWithTag(PROTON_PROGRESS_TEST_TAG)
            .assertIsDisplayed()
    }

    private fun ComposeContentTestRule.onEmptyMailbox(): SemanticsNodeInteraction =
        onNodeWithText("Empty mailbox")

    private fun setupScreen(
        state: MailboxState = MailboxState.Loading,
        items: List<MailboxItemUiModel> = emptyList()
    ) {
        composeTestRule.setContent {
            val mailboxItems = flowOf(PagingData.from(items)).collectAsLazyPagingItems()

            ProtonTheme {
                MailboxScreen(
                    mailboxState = state,
                    mailboxListItems = mailboxItems,
                    actions = MailboxScreen.Actions.Empty
                )
            }
        }
    }
}

