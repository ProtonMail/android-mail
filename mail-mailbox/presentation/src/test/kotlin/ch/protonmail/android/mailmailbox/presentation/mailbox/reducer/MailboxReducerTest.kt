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

package ch.protonmail.android.mailmailbox.presentation.mailbox.reducer

import ch.protonmail.android.mailcommon.domain.sample.LabelIdSample
import ch.protonmail.android.mailcommon.domain.sample.UserSample
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.BottomBarState
import ch.protonmail.android.mailcommon.presentation.model.DialogState
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcommon.presentation.reducer.BottomBarReducer
import ch.protonmail.android.mailcommon.presentation.ui.delete.DeleteDialogState
import ch.protonmail.android.maillabel.domain.model.MailLabel
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.maillabel.presentation.text
import ch.protonmail.android.mailmailbox.domain.model.UserAccountStorageStatus
import ch.protonmail.android.mailmailbox.presentation.R
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxEvent
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxListState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxOperation
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxOperation.AffectingDeleteDialog
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxTopAppBarState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxViewAction
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.StorageLimitState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.UnreadFilterState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.UpgradeStorageState
import ch.protonmail.android.mailmailbox.presentation.mailbox.previewdata.MailboxSearchStateSampleData
import ch.protonmail.android.mailmailbox.presentation.mailbox.previewdata.MailboxStateSampleData
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.LabelAsBottomSheetEntryPoint
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.MailboxUpsellingEntryPoint
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.MoveToBottomSheetEntryPoint
import ch.protonmail.android.mailmessage.presentation.reducer.BottomSheetReducer
import ch.protonmail.android.mailnotifications.presentation.model.NotificationPermissionDialogState
import ch.protonmail.android.mailsettings.domain.model.AutoDeleteSetting
import ch.protonmail.android.mailsettings.presentation.accountsettings.autodelete.AutoDeleteSettingState
import ch.protonmail.android.testdata.label.LabelTestData
import ch.protonmail.android.testdata.mailbox.MailboxItemUiModelTestData
import ch.protonmail.android.testdata.mailbox.UnreadCountersTestData
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import me.proton.core.mailsettings.domain.entity.ViewMode
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
internal class MailboxReducerTest(
    private val testName: String,
    private val testInput: TestInput
) {

    private val mailboxListReducer: MailboxListReducer = mockk {
        every { newStateFrom(any(), any()) } returns reducedState.mailboxListState
    }
    private val topAppBarReducer: MailboxTopAppBarReducer = mockk {
        every { newStateFrom(any(), any()) } returns reducedState.topAppBarState
    }
    private val unreadFilterReducer: MailboxUnreadFilterReducer = mockk {
        every { newStateFrom(any(), any()) } returns reducedState.unreadFilterState
    }
    private val bottomAppBarReducer: BottomBarReducer = mockk {
        every { newStateFrom(any(), any()) } returns reducedState.bottomAppBarState
    }
    private val storageLimitReducer: StorageLimitReducer = mockk {
        every { newStateFrom(any(), any()) } returns reducedState.storageLimitState
    }
    private val upgradeStorageReducer: UpgradeStorageReducer = mockk {
        every { newStateFrom(any()) } returns reducedState.upgradeStorageState
    }
    private val actionMessageReducer: MailboxActionMessageReducer = mockk {
        every { newStateFrom(any()) } returns reducedState.actionResult
    }
    private val deleteDialogReducer: MailboxDeleteDialogReducer = mockk {
        every { newStateFrom(any()) } returns reducedState.deleteDialogState
    }
    private val bottomSheetReducer: BottomSheetReducer = mockk {
        every { newStateFrom(any(), any()) } returns reducedState.bottomSheetState
    }
    private val mailboxReducer = MailboxReducer(
        mailboxListReducer,
        topAppBarReducer,
        unreadFilterReducer,
        bottomAppBarReducer,
        storageLimitReducer,
        upgradeStorageReducer,
        actionMessageReducer,
        deleteDialogReducer,
        bottomSheetReducer
    )

    @Test
    fun `should reduce only the affected parts of the state`() = with(testInput) {
        // When
        val nextState = mailboxReducer.newStateFrom(currentState, operation)

        // Then
        if (shouldReduceMailboxListState) {
            verify {
                mailboxListReducer.newStateFrom(
                    currentState.mailboxListState,
                    operation as MailboxOperation.AffectingMailboxList
                )
            }
        } else {
            assertEquals(currentState.mailboxListState, nextState.mailboxListState, testName)
        }

        if (shouldReduceTopAppBarState) {
            verify {
                topAppBarReducer.newStateFrom(
                    currentState.topAppBarState,
                    operation as MailboxOperation.AffectingTopAppBar
                )
            }
        } else {
            assertEquals(currentState.topAppBarState, nextState.topAppBarState, testName)
        }

        if (shouldReduceUnreadFilterState) {
            verify {
                unreadFilterReducer.newStateFrom(
                    currentState.unreadFilterState,
                    operation as MailboxOperation.AffectingUnreadFilter
                )
            }
        } else {
            assertEquals(currentState.unreadFilterState, nextState.unreadFilterState, testName)
        }

        if (shouldReduceActionMessage) {
            verify { actionMessageReducer.newStateFrom(operation as MailboxOperation.AffectingActionMessage) }
        } else {
            assertEquals(currentState.actionResult, nextState.actionResult)
        }

        if (shouldReduceDeleteDialog) {
            verify { deleteDialogReducer.newStateFrom(operation as AffectingDeleteDialog) }
        } else {
            assertEquals(currentState.deleteDialogState, nextState.deleteDialogState)
        }

        if (shouldReduceBottomAppBarState) {
            verify {
                bottomAppBarReducer.newStateFrom(currentState.bottomAppBarState, any())
            }
        } else {
            assertEquals(currentState.bottomAppBarState, nextState.bottomAppBarState, testName)
        }

        if (shouldReduceBottomSheetState) {
            verify { bottomSheetReducer.newStateFrom(currentState.bottomSheetState, any()) }
        } else {
            assertEquals(currentState.bottomSheetState, nextState.bottomSheetState, testName)
        }

        if (shouldReduceStorageLimitState) {
            verify { storageLimitReducer.newStateFrom(currentState.storageLimitState, any()) }
        } else {
            assertEquals(currentState.storageLimitState, nextState.storageLimitState, testName)
        }

        assertEquals(testInput.deleteAllDialogState, nextState.deleteAllDialogState)

        assertEquals(testInput.showNPSState, nextState.showNPSFeedback)
    }

    companion object {

        private val spamLabel = MailLabel.System(MailLabelId.System.Spam)
        private val currentState = MailboxStateSampleData.Loading
        private val reducedState = MailboxState(
            mailboxListState = MailboxListState.Data.ViewMode(
                currentMailLabel = spamLabel,
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
            ),
            topAppBarState = MailboxTopAppBarState.Data.DefaultMode(
                currentLabelName = spamLabel.text()
            ),
            upgradeStorageState = UpgradeStorageState(false),
            unreadFilterState = UnreadFilterState.Data(
                numUnread = 42,
                isFilterEnabled = false
            ),
            bottomAppBarState = BottomBarState.Loading,
            actionResult = Effect.empty(),
            deleteDialogState = DeleteDialogState.Hidden,
            deleteAllDialogState = DeleteDialogState.Hidden,
            bottomSheetState = null,
            storageLimitState = StorageLimitState.None,
            error = Effect.empty(),
            showRatingBooster = Effect.empty(),
            autoDeleteSettingState = AutoDeleteSettingState.Loading,
            showNPSFeedback = Effect.empty()
        )

        private val actions = listOf(
            TestInput(
                MailboxViewAction.OnItemLongClicked(MailboxItemUiModelTestData.readMailboxItemUiModel),
                shouldReduceMailboxListState = false,
                shouldReduceTopAppBarState = false,
                shouldReduceUnreadFilterState = false,
                shouldReduceBottomAppBarState = false,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = false,
                shouldReduceBottomSheetState = false,
                shouldReduceStorageLimitState = false
            ),
            TestInput(
                MailboxViewAction.OnItemAvatarClicked(MailboxItemUiModelTestData.readMailboxItemUiModel),
                shouldReduceMailboxListState = false,
                shouldReduceTopAppBarState = false,
                shouldReduceUnreadFilterState = false,
                shouldReduceBottomAppBarState = false,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = false,
                shouldReduceBottomSheetState = false,
                shouldReduceStorageLimitState = false
            ),
            TestInput(
                MailboxViewAction.ExitSelectionMode,
                shouldReduceMailboxListState = true,
                shouldReduceTopAppBarState = true,
                shouldReduceUnreadFilterState = false,
                shouldReduceBottomAppBarState = true,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = false,
                shouldReduceBottomSheetState = false,
                shouldReduceStorageLimitState = false
            ),
            TestInput(
                MailboxViewAction.ExitSearchMode,
                shouldReduceMailboxListState = true,
                shouldReduceTopAppBarState = true,
                shouldReduceUnreadFilterState = false,
                shouldReduceBottomAppBarState = true,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = false,
                shouldReduceBottomSheetState = false,
                shouldReduceStorageLimitState = false
            ),
            TestInput(
                MailboxViewAction.ItemClicked(MailboxItemUiModelTestData.readMailboxItemUiModel),
                shouldReduceMailboxListState = false,
                shouldReduceTopAppBarState = false,
                shouldReduceUnreadFilterState = false,
                shouldReduceBottomAppBarState = false,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = false,
                shouldReduceBottomSheetState = false,
                shouldReduceStorageLimitState = false
            ),
            TestInput(
                MailboxViewAction.EnableUnreadFilter,
                shouldReduceMailboxListState = false,
                shouldReduceTopAppBarState = false,
                shouldReduceUnreadFilterState = true,
                shouldReduceBottomAppBarState = false,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = false,
                shouldReduceBottomSheetState = false,
                shouldReduceStorageLimitState = false
            ),
            TestInput(
                MailboxViewAction.DisableUnreadFilter,
                shouldReduceMailboxListState = false,
                shouldReduceTopAppBarState = false,
                shouldReduceUnreadFilterState = true,
                shouldReduceBottomAppBarState = false,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = false,
                shouldReduceBottomSheetState = false,
                shouldReduceStorageLimitState = false
            ),
            TestInput(
                MailboxViewAction.RequestLabelAsBottomSheet,
                shouldReduceMailboxListState = false,
                shouldReduceTopAppBarState = false,
                shouldReduceUnreadFilterState = false,
                shouldReduceBottomAppBarState = false,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = false,
                shouldReduceBottomSheetState = true,
                shouldReduceStorageLimitState = false
            ),
            TestInput(
                MailboxViewAction.SwipeLabelAsAction(userId = UserSample.Primary.userId, itemId = "Item1"),
                shouldReduceMailboxListState = false,
                shouldReduceTopAppBarState = false,
                shouldReduceUnreadFilterState = false,
                shouldReduceBottomAppBarState = false,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = false,
                shouldReduceBottomSheetState = true,
                shouldReduceStorageLimitState = false
            ),
            TestInput(
                MailboxViewAction.DismissBottomSheet,
                shouldReduceMailboxListState = false,
                shouldReduceTopAppBarState = false,
                shouldReduceUnreadFilterState = false,
                shouldReduceBottomAppBarState = false,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = false,
                shouldReduceBottomSheetState = true,
                shouldReduceStorageLimitState = false
            ),
            TestInput(
                MailboxViewAction.LabelAsToggleAction(LabelIdSample.Label2022),
                shouldReduceMailboxListState = false,
                shouldReduceTopAppBarState = false,
                shouldReduceUnreadFilterState = false,
                shouldReduceBottomAppBarState = false,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = false,
                shouldReduceBottomSheetState = true,
                shouldReduceStorageLimitState = false
            ),
            TestInput(
                MailboxViewAction.LabelAsConfirmed(
                    archiveSelected = true,
                    entryPoint = LabelAsBottomSheetEntryPoint.SelectionMode
                ),
                shouldReduceMailboxListState = false,
                shouldReduceTopAppBarState = false,
                shouldReduceUnreadFilterState = false,
                shouldReduceBottomAppBarState = false,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = false,
                shouldReduceBottomSheetState = true,
                shouldReduceStorageLimitState = false
            ),
            TestInput(
                MailboxViewAction.RequestMoveToBottomSheet,
                shouldReduceMailboxListState = false,
                shouldReduceTopAppBarState = false,
                shouldReduceUnreadFilterState = false,
                shouldReduceBottomAppBarState = false,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = false,
                shouldReduceBottomSheetState = true,
                shouldReduceStorageLimitState = false
            ),
            TestInput(
                MailboxViewAction.SwipeMoveToAction(userId = UserSample.Primary.userId, itemId = "Item1"),
                shouldReduceMailboxListState = false,
                shouldReduceTopAppBarState = false,
                shouldReduceUnreadFilterState = false,
                shouldReduceBottomAppBarState = false,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = false,
                shouldReduceBottomSheetState = true,
                shouldReduceStorageLimitState = false
            ),
            TestInput(
                MailboxViewAction.MoveToDestinationSelected(MailLabelId.System.Archive),
                shouldReduceMailboxListState = false,
                shouldReduceTopAppBarState = false,
                shouldReduceUnreadFilterState = false,
                shouldReduceBottomAppBarState = false,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = false,
                shouldReduceBottomSheetState = true,
                shouldReduceStorageLimitState = false
            ),
            TestInput(
                MailboxViewAction.MoveToConfirmed(entryPoint = MoveToBottomSheetEntryPoint.SelectionMode),
                shouldReduceMailboxListState = true,
                shouldReduceTopAppBarState = true,
                shouldReduceUnreadFilterState = false,
                shouldReduceBottomAppBarState = true,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = false,
                shouldReduceBottomSheetState = true,
                shouldReduceStorageLimitState = false
            ),
            TestInput(
                MailboxViewAction.RequestMoreActionsBottomSheet,
                shouldReduceMailboxListState = false,
                shouldReduceTopAppBarState = false,
                shouldReduceUnreadFilterState = false,
                shouldReduceBottomAppBarState = false,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = false,
                shouldReduceBottomSheetState = true,
                shouldReduceStorageLimitState = false
            ),
            TestInput(
                MailboxViewAction.DismissBottomSheet,
                shouldReduceMailboxListState = false,
                shouldReduceTopAppBarState = false,
                shouldReduceUnreadFilterState = false,
                shouldReduceBottomAppBarState = false,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = false,
                shouldReduceBottomSheetState = true,
                shouldReduceStorageLimitState = false
            ),
            TestInput(
                MailboxViewAction.Star,
                shouldReduceMailboxListState = true,
                shouldReduceTopAppBarState = false,
                shouldReduceUnreadFilterState = false,
                shouldReduceBottomAppBarState = false,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = false,
                shouldReduceBottomSheetState = true,
                shouldReduceStorageLimitState = false
            ),
            TestInput(
                MailboxViewAction.UnStar,
                shouldReduceMailboxListState = true,
                shouldReduceTopAppBarState = false,
                shouldReduceUnreadFilterState = false,
                shouldReduceBottomAppBarState = false,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = false,
                shouldReduceBottomSheetState = true,
                shouldReduceStorageLimitState = false
            ),
            TestInput(
                MailboxViewAction.MoveToArchive,
                shouldReduceMailboxListState = true,
                shouldReduceTopAppBarState = true,
                shouldReduceUnreadFilterState = false,
                shouldReduceBottomAppBarState = true,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = false,
                shouldReduceBottomSheetState = true,
                shouldReduceStorageLimitState = false
            ),
            TestInput(
                MailboxViewAction.MoveToSpam,
                shouldReduceMailboxListState = true,
                shouldReduceTopAppBarState = true,
                shouldReduceUnreadFilterState = false,
                shouldReduceBottomAppBarState = true,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = false,
                shouldReduceBottomSheetState = true,
                shouldReduceStorageLimitState = false
            ),
            TestInput(
                MailboxViewAction.StorageLimitConfirmed,
                shouldReduceMailboxListState = false,
                shouldReduceTopAppBarState = false,
                shouldReduceUnreadFilterState = false,
                shouldReduceBottomAppBarState = false,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = false,
                shouldReduceBottomSheetState = false,
                shouldReduceStorageLimitState = true
            ),
            TestInput(
                MailboxViewAction.RequestUpsellingBottomSheet(MailboxUpsellingEntryPoint.Mailbox),
                shouldReduceMailboxListState = false,
                shouldReduceTopAppBarState = false,
                shouldReduceUnreadFilterState = false,
                shouldReduceBottomAppBarState = false,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = false,
                shouldReduceBottomSheetState = true,
                shouldReduceStorageLimitState = false
            ),
            TestInput(
                MailboxViewAction.RequestUpsellingBottomSheet(MailboxUpsellingEntryPoint.AutoDelete),
                shouldReduceMailboxListState = false,
                shouldReduceTopAppBarState = false,
                shouldReduceUnreadFilterState = false,
                shouldReduceBottomAppBarState = false,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = false,
                shouldReduceBottomSheetState = true,
                shouldReduceStorageLimitState = false
            )
        )

        private val events = listOf(
            TestInput(
                MailboxEvent.ItemClicked.ItemDetailsOpenedInViewMode(
                    item = MailboxItemUiModelTestData.readMailboxItemUiModel,
                    preferredViewMode = ViewMode.NoConversationGrouping
                ),
                shouldReduceMailboxListState = true,
                shouldReduceTopAppBarState = false,
                shouldReduceUnreadFilterState = false,
                shouldReduceBottomAppBarState = false,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = false,
                shouldReduceBottomSheetState = false,
                shouldReduceStorageLimitState = false
            ),
            TestInput(
                MailboxEvent.ItemClicked.ItemAddedToSelection(
                    item = MailboxItemUiModelTestData.readMailboxItemUiModel
                ),
                shouldReduceMailboxListState = true,
                shouldReduceTopAppBarState = true,
                shouldReduceUnreadFilterState = false,
                shouldReduceBottomAppBarState = false,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = false,
                shouldReduceBottomSheetState = false,
                shouldReduceStorageLimitState = false
            ),
            TestInput(
                MailboxEvent.ItemClicked.ItemRemovedFromSelection(
                    item = MailboxItemUiModelTestData.readMailboxItemUiModel
                ),
                shouldReduceMailboxListState = true,
                shouldReduceTopAppBarState = true,
                shouldReduceUnreadFilterState = false,
                shouldReduceBottomAppBarState = false,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = false,
                shouldReduceBottomSheetState = false,
                shouldReduceStorageLimitState = false
            ),
            TestInput(
                MailboxEvent.NewLabelSelected(
                    selectedLabel = LabelTestData.systemLabels.first(),
                    selectedLabelCount = UnreadCountersTestData.systemUnreadCounters.first().count
                ),
                shouldReduceMailboxListState = true,
                shouldReduceTopAppBarState = true,
                shouldReduceUnreadFilterState = true,
                shouldReduceBottomAppBarState = false,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = false,
                shouldReduceBottomSheetState = false,
                shouldReduceStorageLimitState = false
            ),
            TestInput(
                MailboxEvent.AutoDeleteStateChanged(
                    isFeatureFlagEnabled = false,
                    autoDeleteSetting = AutoDeleteSetting.Enabled,
                    currentLabelId = MailLabelId.System.Spam
                ),
                shouldReduceMailboxListState = true,
                shouldReduceTopAppBarState = false,
                shouldReduceUnreadFilterState = false,
                shouldReduceBottomAppBarState = false,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = false,
                shouldReduceBottomSheetState = false,
                shouldReduceStorageLimitState = false
            ),
            TestInput(
                MailboxEvent.SelectedLabelChanged(LabelTestData.systemLabels.first()),
                shouldReduceMailboxListState = true,
                shouldReduceTopAppBarState = true,
                shouldReduceUnreadFilterState = false,
                shouldReduceBottomAppBarState = false,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = false,
                shouldReduceBottomSheetState = false,
                shouldReduceStorageLimitState = false
            ),
            TestInput(
                MailboxEvent.SelectedLabelCountChanged(UnreadCountersTestData.systemUnreadCounters.first().count),
                shouldReduceMailboxListState = false,
                shouldReduceTopAppBarState = false,
                shouldReduceUnreadFilterState = true,
                shouldReduceBottomAppBarState = false,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = false,
                shouldReduceBottomSheetState = false,
                shouldReduceStorageLimitState = false
            ),
            TestInput(
                MailboxEvent.EnterSelectionMode(MailboxItemUiModelTestData.readMailboxItemUiModel),
                shouldReduceMailboxListState = true,
                shouldReduceTopAppBarState = true,
                shouldReduceUnreadFilterState = false,
                shouldReduceBottomAppBarState = true,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = false,
                shouldReduceBottomSheetState = false,
                shouldReduceStorageLimitState = false
            ),
            TestInput(
                MailboxEvent.Trash(ViewMode.ConversationGrouping, 5),
                shouldReduceMailboxListState = true,
                shouldReduceTopAppBarState = true,
                shouldReduceUnreadFilterState = false,
                shouldReduceBottomAppBarState = true,
                shouldReduceActionMessage = true,
                shouldReduceDeleteDialog = false,
                shouldReduceBottomSheetState = false,
                shouldReduceStorageLimitState = false
            ),
            TestInput(
                MailboxEvent.Trash(ViewMode.NoConversationGrouping, 5),
                shouldReduceMailboxListState = true,
                shouldReduceTopAppBarState = true,
                shouldReduceUnreadFilterState = false,
                shouldReduceBottomAppBarState = true,
                shouldReduceActionMessage = true,
                shouldReduceDeleteDialog = false,
                shouldReduceBottomSheetState = false,
                shouldReduceStorageLimitState = false
            ),
            TestInput(
                MailboxEvent.Delete(ViewMode.ConversationGrouping, 5),
                shouldReduceMailboxListState = false,
                shouldReduceTopAppBarState = false,
                shouldReduceUnreadFilterState = false,
                shouldReduceBottomAppBarState = false,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = true,
                shouldReduceBottomSheetState = false,
                shouldReduceStorageLimitState = false
            ),
            TestInput(
                MailboxEvent.Delete(ViewMode.NoConversationGrouping, 5),
                shouldReduceMailboxListState = false,
                shouldReduceTopAppBarState = false,
                shouldReduceUnreadFilterState = false,
                shouldReduceBottomAppBarState = false,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = true,
                shouldReduceBottomSheetState = false,
                shouldReduceStorageLimitState = false
            ),
            TestInput(
                MailboxEvent.DeleteConfirmed(ViewMode.ConversationGrouping, 5),
                shouldReduceMailboxListState = true,
                shouldReduceTopAppBarState = true,
                shouldReduceUnreadFilterState = false,
                shouldReduceBottomAppBarState = true,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = false,
                shouldReduceBottomSheetState = false,
                shouldReduceStorageLimitState = false
            ),
            TestInput(
                MailboxEvent.DeleteConfirmed(ViewMode.NoConversationGrouping, 5),
                shouldReduceMailboxListState = true,
                shouldReduceTopAppBarState = true,
                shouldReduceUnreadFilterState = false,
                shouldReduceBottomAppBarState = true,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = false,
                shouldReduceBottomSheetState = false,
                shouldReduceStorageLimitState = false
            ),
            TestInput(
                MailboxViewAction.DeleteDialogDismissed,
                shouldReduceMailboxListState = false,
                shouldReduceTopAppBarState = false,
                shouldReduceUnreadFilterState = false,
                shouldReduceBottomAppBarState = false,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = true,
                shouldReduceBottomSheetState = false,
                shouldReduceStorageLimitState = false
            ),
            TestInput(
                MailboxEvent.DeleteAll(ViewMode.ConversationGrouping, LabelIdSample.Trash),
                shouldReduceMailboxListState = false,
                shouldReduceTopAppBarState = false,
                shouldReduceUnreadFilterState = false,
                shouldReduceBottomAppBarState = false,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = false,
                deleteAllDialogState = DeleteDialogState.Shown(
                    title = TextUiModel(R.string.mailbox_action_clear_trash_dialog_title),
                    message = TextUiModel(R.string.mailbox_action_clear_trash_dialog_body_message)
                ),
                shouldReduceBottomSheetState = false,
                shouldReduceStorageLimitState = false
            ),
            TestInput(
                MailboxEvent.DeleteAll(ViewMode.NoConversationGrouping, LabelIdSample.Trash),
                shouldReduceMailboxListState = false,
                shouldReduceTopAppBarState = false,
                shouldReduceUnreadFilterState = false,
                shouldReduceBottomAppBarState = false,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = false,
                deleteAllDialogState = DeleteDialogState.Shown(
                    title = TextUiModel(R.string.mailbox_action_clear_trash_dialog_title),
                    message = TextUiModel(R.string.mailbox_action_clear_trash_dialog_body_message)
                ),
                shouldReduceBottomSheetState = false,
                shouldReduceStorageLimitState = false
            ),
            TestInput(
                MailboxEvent.DeleteAll(ViewMode.ConversationGrouping, LabelIdSample.Spam),
                shouldReduceMailboxListState = false,
                shouldReduceTopAppBarState = false,
                shouldReduceUnreadFilterState = false,
                shouldReduceBottomAppBarState = false,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = false,
                deleteAllDialogState = DeleteDialogState.Shown(
                    title = TextUiModel(R.string.mailbox_action_clear_spam_dialog_title),
                    message = TextUiModel(R.string.mailbox_action_clear_spam_dialog_body_message)
                ),
                shouldReduceBottomSheetState = false,
                shouldReduceStorageLimitState = false
            ),
            TestInput(
                MailboxEvent.DeleteAll(ViewMode.NoConversationGrouping, LabelIdSample.Spam),
                shouldReduceMailboxListState = false,
                shouldReduceTopAppBarState = false,
                shouldReduceUnreadFilterState = false,
                shouldReduceBottomAppBarState = false,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = false,
                deleteAllDialogState = DeleteDialogState.Shown(
                    title = TextUiModel(R.string.mailbox_action_clear_spam_dialog_title),
                    message = TextUiModel(R.string.mailbox_action_clear_spam_dialog_body_message)
                ),
                shouldReduceBottomSheetState = false,
                shouldReduceStorageLimitState = false
            ),
            TestInput(
                MailboxEvent.DeleteAll(ViewMode.NoConversationGrouping, LabelIdSample.Inbox),
                shouldReduceMailboxListState = false,
                shouldReduceTopAppBarState = false,
                shouldReduceUnreadFilterState = false,
                shouldReduceBottomAppBarState = false,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = false,
                deleteAllDialogState = DeleteDialogState.Hidden,
                shouldReduceBottomSheetState = false,
                shouldReduceStorageLimitState = false
            ),
            TestInput(
                MailboxEvent.DeleteAllConfirmed(ViewMode.ConversationGrouping),
                shouldReduceMailboxListState = false,
                shouldReduceTopAppBarState = false,
                shouldReduceUnreadFilterState = false,
                shouldReduceBottomAppBarState = false,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = false,
                deleteAllDialogState = DeleteDialogState.Hidden,
                shouldReduceBottomSheetState = false,
                shouldReduceStorageLimitState = false
            ),
            TestInput(
                MailboxEvent.DeleteAllConfirmed(ViewMode.NoConversationGrouping),
                shouldReduceMailboxListState = false,
                shouldReduceTopAppBarState = false,
                shouldReduceUnreadFilterState = false,
                shouldReduceBottomAppBarState = false,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = false,
                deleteAllDialogState = DeleteDialogState.Hidden,
                shouldReduceBottomSheetState = false,
                shouldReduceStorageLimitState = false
            ),
            TestInput(
                MailboxViewAction.DeleteAllDialogDismissed,
                shouldReduceMailboxListState = false,
                shouldReduceTopAppBarState = false,
                shouldReduceUnreadFilterState = false,
                shouldReduceBottomAppBarState = false,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = false,
                deleteAllDialogState = DeleteDialogState.Hidden,
                shouldReduceBottomSheetState = false,
                shouldReduceStorageLimitState = false
            ),
            TestInput(
                MailboxEvent.ErrorLabeling,
                shouldReduceMailboxListState = false,
                shouldReduceTopAppBarState = false,
                shouldReduceUnreadFilterState = false,
                shouldReduceBottomAppBarState = false,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = false,
                shouldReduceStorageLimitState = false,
                shouldReduceBottomSheetState = false,
                errorBarState = Effect.of(TextUiModel(R.string.mailbox_action_label_messages_failed))
            ),
            TestInput(
                MailboxEvent.StorageLimitStatusChanged(
                    userAccountStorageStatus = UserAccountStorageStatus(usedSpace = 5_000, maxSpace = 10_000)
                ),
                shouldReduceMailboxListState = false,
                shouldReduceTopAppBarState = false,
                shouldReduceUnreadFilterState = false,
                shouldReduceBottomAppBarState = false,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = false,
                shouldReduceBottomSheetState = false,
                shouldReduceStorageLimitState = true
            ),
            TestInput(
                MailboxEvent.ShowRatingBooster,
                shouldReduceMailboxListState = false,
                shouldReduceTopAppBarState = false,
                shouldReduceUnreadFilterState = false,
                shouldReduceBottomAppBarState = false,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = false,
                shouldReduceBottomSheetState = false,
                shouldReduceStorageLimitState = false,
                showRatingBoosterState = Effect.of(Unit)
            ),
            TestInput(
                MailboxEvent.ShowNPSFeedback,
                shouldReduceMailboxListState = false,
                shouldReduceTopAppBarState = false,
                shouldReduceUnreadFilterState = false,
                shouldReduceBottomAppBarState = false,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = false,
                shouldReduceBottomSheetState = false,
                shouldReduceStorageLimitState = false,
                showNPSState = Effect.of(Unit)
            ),
            TestInput(
                MailboxViewAction.DismissAutoDelete,
                shouldReduceMailboxListState = false,
                shouldReduceTopAppBarState = false,
                shouldReduceUnreadFilterState = false,
                shouldReduceBottomAppBarState = false,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = false,
                shouldReduceBottomSheetState = false,
                shouldReduceStorageLimitState = false,
                autoDeleteSettingState = AutoDeleteSettingState.Data(
                    isEnabled = false,
                    enablingDialogState = DialogState.Hidden,
                    disablingDialogState = DialogState.Hidden
                )
            ),
            TestInput(
                MailboxViewAction.ShowAutoDeleteDialog,
                shouldReduceMailboxListState = false,
                shouldReduceTopAppBarState = false,
                shouldReduceUnreadFilterState = false,
                shouldReduceBottomAppBarState = false,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = false,
                shouldReduceBottomSheetState = false,
                shouldReduceStorageLimitState = false,
                autoDeleteSettingState = AutoDeleteSettingState.Data(
                    isEnabled = false,
                    enablingDialogState = DialogState.Shown(
                        title = TextUiModel(R.string.mail_settings_auto_delete_dialog_enabling_title),
                        message = TextUiModel(R.string.mail_settings_auto_delete_dialog_enabling_text),
                        dismissButtonText = TextUiModel(R.string.mail_settings_auto_delete_dialog_button_cancel),
                        confirmButtonText = TextUiModel(
                            R.string.mail_settings_auto_delete_dialog_enabling_button_confirm
                        )
                    ),
                    disablingDialogState = DialogState.Hidden
                )
            ),
            TestInput(
                MailboxViewAction.AutoDeleteDialogActionSubmitted(enable = true),
                shouldReduceMailboxListState = false,
                shouldReduceTopAppBarState = false,
                shouldReduceUnreadFilterState = false,
                shouldReduceBottomAppBarState = false,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = false,
                shouldReduceBottomSheetState = false,
                shouldReduceStorageLimitState = false,
                autoDeleteSettingState = AutoDeleteSettingState.Data(
                    isEnabled = false,
                    enablingDialogState = DialogState.Hidden,
                    disablingDialogState = DialogState.Hidden
                )
            )
        )

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data(): Collection<Array<Any>> {
            return (actions + events)
                .map { testInput ->
                    val testName = """
                        Operation: ${testInput.operation}
                        
                    """.trimIndent()
                    arrayOf(testName, testInput)
                }
        }
    }

    data class TestInput(
        val operation: MailboxOperation,
        val shouldReduceMailboxListState: Boolean,
        val shouldReduceTopAppBarState: Boolean,
        val shouldReduceUnreadFilterState: Boolean,
        val shouldReduceBottomAppBarState: Boolean,
        val shouldReduceActionMessage: Boolean,
        val shouldReduceDeleteDialog: Boolean,
        val deleteAllDialogState: DeleteDialogState = DeleteDialogState.Hidden,
        val shouldReduceBottomSheetState: Boolean,
        val shouldReduceStorageLimitState: Boolean,
        val errorBarState: Effect<TextUiModel> = Effect.empty(),
        val showRatingBoosterState: Effect<Unit> = Effect.empty(),
        val showNPSState: Effect<Unit> = Effect.empty(),
        val autoDeleteSettingState: AutoDeleteSettingState = AutoDeleteSettingState.Loading,
        val notificationPermissionDialogState: NotificationPermissionDialogState =
            NotificationPermissionDialogState.Hidden
    )
}
