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
import arrow.core.nonEmptyListOf
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.maillabel.domain.model.MailLabel
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.maillabel.presentation.sample.LabelUiModelSample
import ch.protonmail.android.maillabel.presentation.text
import ch.protonmail.android.mailmailbox.domain.model.MailboxItemType
import ch.protonmail.android.mailmailbox.presentation.mailbox.MailboxScreen
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxItemUiModel
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxListState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxTopAppBarState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.UnreadFilterState
import ch.protonmail.android.mailmailbox.presentation.mailbox.previewdata.MailboxStateSampleData
import ch.protonmail.android.test.annotations.suite.SmokeExtendedTest
import ch.protonmail.android.testdata.mailbox.MailboxItemUiModelTestData
import ch.protonmail.android.uitest.robot.mailbox.MailboxRobot
import ch.protonmail.android.uitest.util.ManagedState
import ch.protonmail.android.uitest.util.StateManager
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.flowOf
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

@SmokeExtendedTest
internal class MailboxScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun whenLoadingThenProgressIsDisplayed() {
        val mailboxState = MailboxStateSampleData.Loading
        val robot = setupScreen(state = mailboxState)

        robot.verify { listProgressIsDisplayed() }
    }

    @Test
    fun whenLoadingCompletedThenItemsAreDisplayed() {
        val mailboxListState = MailboxListState.Data(
            currentMailLabel = MailLabel.System(MailLabelId.System.Inbox),
            openItemEffect = Effect.empty(),
            scrollToMailboxTop = Effect.empty()
        )
        val mailboxState = MailboxStateSampleData.Loading.copy(mailboxListState = mailboxListState)
        val items = listOf(MailboxItemUiModelTestData.readMailboxItemUiModel)
        val robot = setupScreen(state = mailboxState, items = items)

        robot.verify { itemWithSubjectIsDisplayed(items.first().subject) }
    }

    @Test
    fun whenLoadingCompletedThenItemsLabelsAreDisplayed() {
        val mailboxListState = MailboxListState.Data(
            currentMailLabel = MailLabel.System(MailLabelId.System.Inbox),
            openItemEffect = Effect.empty(),
            scrollToMailboxTop = Effect.empty()
        )
        val mailboxState = MailboxStateSampleData.Loading.copy(mailboxListState = mailboxListState)
        val label = LabelUiModelSample.News
        val item = MailboxItemUiModelTestData.buildMailboxUiModelItem(
            labels = persistentListOf(label)
        )
        val robot = setupScreen(state = mailboxState, items = listOf(item))

        robot.verify { itemLabelIsDisplayed(label.name) }
    }

    @Test
    @Ignore(
        """
            The current version of the paging library doesn't allow us to test this in the same way. 
            Wee need to find an alternative
        """
    ) // MAILANDR-330
    fun givenLoadingCompletedWhenNoItemThenEmptyMailboxIsDisplayed() {
        val mailboxListState = MailboxListState.Data(
            currentMailLabel = MailLabel.System(MailLabelId.System.Inbox),
            openItemEffect = Effect.empty(),
            scrollToMailboxTop = Effect.empty()
        )
        val mailboxState = MailboxStateSampleData.Loading.copy(mailboxListState = mailboxListState)
        val robot = setupScreen(state = mailboxState)

        robot.verify { emptyMailboxIsDisplayed() }
    }

    @Test
    @Ignore("How to verify SwipeRefresh is refreshing?") // MAILANDR-330
    fun givenEmptyMailboxIsDisplayedWhenSwipeDownThenRefreshIsTriggered() {
        val mailboxListState = MailboxListState.Data(
            currentMailLabel = MailLabel.System(MailLabelId.System.Inbox),
            openItemEffect = Effect.empty(),
            scrollToMailboxTop = Effect.empty()
        )
        val mailboxState = MailboxStateSampleData.Loading.copy(mailboxListState = mailboxListState)
        val robot = setupScreen(state = mailboxState)

        robot
            .verify { emptyMailboxIsDisplayed() }
            .pullDownToRefresh()
            .verify { swipeRefreshProgressIsDisplayed() }
    }

    @Test
    fun givenDataIsLoadedWhenCurrentLabelChangesThenScrollToTop() {
        val items = (1..100).map { index ->
            MailboxItemUiModelTestData.buildMailboxUiModelItem(
                id = index.toString(),
                type = MailboxItemType.Message
            )
        }
        val itemsFlow = flowOf(PagingData.from(items))
        val states = nonEmptyListOf(
            MailLabelId.System.Trash to false,
            MailLabelId.System.AllMail to true,
            MailLabelId.System.Trash to true
        ).map { (systemLabel, shouldScrollToTop) ->
            val scrollToTopEffect: Effect<MailLabelId> =
                if (shouldScrollToTop) Effect.of(systemLabel) else Effect.empty()
            MailboxState(
                mailboxListState = MailboxListState.Data(
                    currentMailLabel = MailLabel.System(systemLabel),
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = scrollToTopEffect
                ),
                topAppBarState = MailboxTopAppBarState.Data.DefaultMode(
                    currentLabelName = MailLabel.System(systemLabel).text(),
                    composerDisabled = false
                ),
                unreadFilterState = UnreadFilterState.Loading,
                networkStatusEffect = Effect.empty()
            )
        }

        val stateManager = StateManager.of(states)
        val robot = composeTestRule.MailboxRobot {
            ManagedState(stateManager = stateManager) { mailboxState ->
                MailboxScreen(
                    mailboxState = mailboxState,
                    mailboxListItems = itemsFlow.collectAsLazyPagingItems(),
                    actions = MailboxScreen.Actions.Empty
                )
            }
        }

        robot
            .verify { itemWithSubjectIsDisplayed(subject = "1") }
            .scrollToItem(index = 99)

        stateManager.emitNext()

        robot
            .verify { itemWithSubjectIsDisplayed(subject = "1") }
            .scrollToItem(index = 99)

        stateManager.emitNext()

        robot
            .verify { itemWithSubjectIsDisplayed(subject = "1") }
    }

    private fun setupScreen(
        state: MailboxState = MailboxStateSampleData.Loading,
        items: List<MailboxItemUiModel> = emptyList()
    ): MailboxRobot =
        composeTestRule.MailboxRobot {
            val mailboxItems = flowOf(PagingData.from(items)).collectAsLazyPagingItems()

            MailboxScreen(
                mailboxState = state,
                mailboxListItems = mailboxItems,
                actions = MailboxScreen.Actions.Empty
            )
        }
}

