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

import androidx.compose.runtime.Composable
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import arrow.core.nonEmptyListOf
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.ActionUiModel
import ch.protonmail.android.mailcommon.presentation.model.BottomBarState
import ch.protonmail.android.mailcommon.presentation.ui.delete.DeleteDialogState
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
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.StorageLimitState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.UnreadFilterState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.UpgradeStorageState
import ch.protonmail.android.mailmailbox.presentation.mailbox.previewdata.MailboxSearchStateSampleData
import ch.protonmail.android.mailmailbox.presentation.mailbox.previewdata.MailboxStateSampleData
import ch.protonmail.android.mailnotifications.presentation.model.NotificationPermissionDialogState
import ch.protonmail.android.mailsettings.presentation.accountsettings.autodelete.AutoDeleteSettingState
import ch.protonmail.android.test.annotations.suite.RegressionTest
import ch.protonmail.android.uitest.util.HiltInstrumentedTest
import ch.protonmail.android.testdata.mailbox.MailboxItemUiModelTestData
import ch.protonmail.android.uitest.models.avatar.AvatarInitial
import ch.protonmail.android.uitest.models.folders.MailLabelEntry
import ch.protonmail.android.uitest.models.mailbox.MailboxListItemEntry
import ch.protonmail.android.uitest.models.mailbox.ParticipantEntry
import ch.protonmail.android.uitest.robot.mailbox.MailboxRobot
import ch.protonmail.android.uitest.robot.mailbox.mailboxRobot
import ch.protonmail.android.uitest.robot.mailbox.section.emptyListSection
import ch.protonmail.android.uitest.robot.mailbox.section.listSection
import ch.protonmail.android.uitest.robot.mailbox.section.progressListSection
import ch.protonmail.android.uitest.robot.mailbox.section.verify
import ch.protonmail.android.uitest.util.ManagedState
import ch.protonmail.android.uitest.util.StateManager
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.flowOf
import org.junit.Ignore
import org.junit.Test

@RegressionTest
@HiltAndroidTest
internal class MailboxScreenTest : HiltInstrumentedTest() {

    private val topMailboxItem = MailboxListItemEntry(
        index = 0,
        avatarInitial = AvatarInitial.WithText("T"),
        participants = listOf(ParticipantEntry.NoSender),
        subject = "1",
        date = "10:42"
    )

    @Test
    fun whenLoadingThenProgressIsDisplayed() {
        val mailboxState = MailboxStateSampleData.Loading
        val robot = setupScreen(state = mailboxState)

        robot.progressListSection { verify { isShown() } }
    }

    @Test
    fun whenLoadingCompletedThenItemsAreDisplayed() {
        val mailboxListState = MailboxListState.Data.ViewMode(
            currentMailLabel = MailLabel.System(MailLabelId.System.Inbox),
            openItemEffect = Effect.empty(),
            scrollToMailboxTop = Effect.empty(),
            offlineEffect = Effect.empty(),
            refreshErrorEffect = Effect.empty(),
            refreshRequested = false,
            swipingEnabled = false,
            swipeActions = null,
            searchState = MailboxSearchStateSampleData.NotSearching,
            clearState = MailboxListState.Data.ClearState.Hidden,
            autoDeleteBannerState = MailboxListState.Data.AutoDeleteBannerState.Hidden
        )
        val mailboxState = MailboxStateSampleData.Loading.copy(mailboxListState = mailboxListState)
        val items = listOf(MailboxItemUiModelTestData.readMailboxItemUiModel)
        val robot = setupScreen(state = mailboxState, items = items)

        robot.listSection {
            verify {
                listItemsAreShown(topMailboxItem.copy(subject = items.first().subject))
            }
        }
    }

    @Test
    fun whenLoadingCompletedThenItemsLabelsAreDisplayed() {
        val mailboxListState = MailboxListState.Data.ViewMode(
            currentMailLabel = MailLabel.System(MailLabelId.System.Inbox),
            openItemEffect = Effect.empty(),
            scrollToMailboxTop = Effect.empty(),
            offlineEffect = Effect.empty(),
            refreshErrorEffect = Effect.empty(),
            refreshRequested = false,
            swipingEnabled = false,
            swipeActions = null,
            searchState = MailboxSearchStateSampleData.NotSearching,
            clearState = MailboxListState.Data.ClearState.Hidden,
            autoDeleteBannerState = MailboxListState.Data.AutoDeleteBannerState.Hidden
        )
        val mailboxState = MailboxStateSampleData.Loading.copy(mailboxListState = mailboxListState)
        val label = LabelUiModelSample.News
        val item = MailboxItemUiModelTestData.buildMailboxUiModelItem(
            labels = persistentListOf(label)
        )
        val mailboxItem = topMailboxItem.copy(
            labels = listOf(MailLabelEntry(index = 0, name = label.name)),
            subject = "0"
        )

        val robot = setupScreen(state = mailboxState, items = listOf(item))

        robot.listSection { verify { listItemsAreShown(mailboxItem) } }
    }

    @Test
    @Ignore(
        """
            The current version of the paging library doesn't allow us to test this in the same way. 
            Wee need to find an alternative
        """
    ) // MAILANDR-330
    fun givenLoadingCompletedWhenNoItemThenEmptyMailboxIsDisplayed() {
        val mailboxListState = MailboxListState.Data.ViewMode(
            currentMailLabel = MailLabel.System(MailLabelId.System.Inbox),
            openItemEffect = Effect.empty(),
            scrollToMailboxTop = Effect.empty(),
            offlineEffect = Effect.empty(),
            refreshErrorEffect = Effect.empty(),
            refreshRequested = false,
            swipingEnabled = false,
            swipeActions = null,
            searchState = MailboxSearchStateSampleData.NotSearching,
            clearState = MailboxListState.Data.ClearState.Hidden,
            autoDeleteBannerState = MailboxListState.Data.AutoDeleteBannerState.Hidden
        )
        val mailboxState = MailboxStateSampleData.Loading.copy(mailboxListState = mailboxListState)
        val robot = setupScreen(state = mailboxState)

        robot.emptyListSection { verify { isShown() } }
    }

    @Test
    @Ignore("How to verify SwipeRefresh is refreshing?") // MAILANDR-330
    fun givenEmptyMailboxIsDisplayedWhenSwipeDownThenRefreshIsTriggered() {
        val mailboxListState = MailboxListState.Data.ViewMode(
            currentMailLabel = MailLabel.System(MailLabelId.System.Inbox),
            openItemEffect = Effect.empty(),
            scrollToMailboxTop = Effect.empty(),
            offlineEffect = Effect.empty(),
            refreshErrorEffect = Effect.empty(),
            refreshRequested = false,
            swipingEnabled = false,
            swipeActions = null,
            searchState = MailboxSearchStateSampleData.NotSearching,
            clearState = MailboxListState.Data.ClearState.Hidden,
            autoDeleteBannerState = MailboxListState.Data.AutoDeleteBannerState.Hidden
        )
        val mailboxState = MailboxStateSampleData.Loading.copy(mailboxListState = mailboxListState)
        val robot = setupScreen(state = mailboxState)

        // TODO
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
                mailboxListState = MailboxListState.Data.ViewMode(
                    currentMailLabel = MailLabel.System(systemLabel),
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = scrollToTopEffect,
                    offlineEffect = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshRequested = false,
                    swipingEnabled = false,
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    clearState = MailboxListState.Data.ClearState.Hidden,
                    autoDeleteBannerState = MailboxListState.Data.AutoDeleteBannerState.Hidden
                ),
                topAppBarState = MailboxTopAppBarState.Data.DefaultMode(
                    currentLabelName = MailLabel.System(systemLabel).text()
                ),
                upgradeStorageState = UpgradeStorageState(notificationDotVisible = false),
                unreadFilterState = UnreadFilterState.Loading,
                bottomAppBarState = BottomBarState.Data.Hidden(emptyList<ActionUiModel>().toImmutableList()),
                actionResult = Effect.empty(),
                deleteDialogState = DeleteDialogState.Hidden,
                deleteAllDialogState = DeleteDialogState.Hidden,
                storageLimitState = StorageLimitState.HasEnoughSpace,
                bottomSheetState = null,
                error = Effect.empty(),
                showRatingBooster = Effect.empty(),
                autoDeleteSettingState = AutoDeleteSettingState.Loading,
                showNPSFeedback = Effect.empty()
            )
        }

        val stateManager = StateManager.of(states)
        val robot = setupManagedState {
            ManagedState(stateManager = stateManager) { mailboxState ->
                MailboxScreen(
                    mailboxState = mailboxState,
                    mailboxListItems = itemsFlow.collectAsLazyPagingItems(),
                    actions = MailboxScreen.Actions.Empty
                )
            }
        }

        robot.listSection {
            verify { listItemsAreShown(topMailboxItem) }
            scrollToItemAtIndex(99)

            stateManager.emitNext()

            verify { listItemsAreShown(topMailboxItem) }
            scrollToItemAtIndex(99)

            stateManager.emitNext()

            verify { listItemsAreShown(topMailboxItem) }
        }
    }

    private fun setupManagedState(content: @Composable () -> Unit): MailboxRobot = mailboxRobot {
        this@MailboxScreenTest.composeTestRule.setContent(content)
    }

    private fun setupScreen(
        state: MailboxState = MailboxStateSampleData.Loading,
        items: List<MailboxItemUiModel> = emptyList()
    ): MailboxRobot = mailboxRobot {
        this@MailboxScreenTest.composeTestRule.setContent {
            val mailboxItems = flowOf(PagingData.from(items)).collectAsLazyPagingItems()

            MailboxScreen(
                mailboxState = state,
                mailboxListItems = mailboxItems,
                actions = MailboxScreen.Actions.Empty
            )
        }
    }
}
