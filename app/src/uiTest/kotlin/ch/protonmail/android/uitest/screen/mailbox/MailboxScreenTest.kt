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

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.maillabel.domain.model.MailLabel
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.mailmailbox.presentation.mailbox.MailboxScreen
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxItemUiModel
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxListState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxState
import ch.protonmail.android.testdata.mailbox.MailboxItemUiModelTestData
import ch.protonmail.android.uitest.robot.mailbox.MailboxRobot
import kotlinx.coroutines.flow.flowOf
import me.proton.core.compose.theme.ProtonTheme
import org.junit.Rule
import org.junit.Test

internal class MailboxScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun whenLoadingThenProgressIsDisplayed() {
        val mailboxState = MailboxState.Loading
        val robot = setupScreen(state = mailboxState)

        robot.verify { listProgressIsDisplayed() }
    }

    @Test
    fun givenLoadingCompletedWhenItemsThenItemsAreDisplayed() {
        val mailboxListState = MailboxListState.Data(
            currentMailLabel = MailLabel.System(MailLabelId.System.Inbox),
            Effect.empty(),
            Effect.empty()
        )
        val mailboxState = MailboxState.Loading.copy(mailboxListState = mailboxListState)
        val items = listOf(MailboxItemUiModelTestData.readMailboxItemUiModel)
        val robot = setupScreen(state = mailboxState, items = items)

        robot.verify { itemWithSubjectIsDisplayed(items.first().subject) }
    }

    @Test
    fun givenLoadingCompletedWhenNoItemThenEmptyMailboxIsDisplayed() {
        val mailboxListState = MailboxListState.Data(
            currentMailLabel = MailLabel.System(MailLabelId.System.Inbox),
            Effect.empty(),
            Effect.empty()
        )
        val mailboxState = MailboxState.Loading.copy(mailboxListState = mailboxListState)
        val robot = setupScreen(state = mailboxState)

        robot.verify { emptyMailboxIsDisplayed() }
    }

    @Test
    fun givenEmptyMailboxIsDisplayedWhenSwipeDownThenRefreshIsTriggered() {
        val mailboxListState = MailboxListState.Data(
            currentMailLabel = MailLabel.System(MailLabelId.System.Inbox),
            Effect.empty(),
            Effect.empty()
        )
        val mailboxState = MailboxState.Loading.copy(mailboxListState = mailboxListState)
        val robot = setupScreen(state = mailboxState)

        robot
            .verify { emptyMailboxIsDisplayed() }
            .pullDownToRefresh()
            .verify { listProgressIsDisplayed() }
    }

    private fun setupScreen(
        state: MailboxState = MailboxState.Loading,
        items: List<MailboxItemUiModel> = emptyList()
    ): MailboxRobot =
        composeTestRule.MailboxRobot {
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

