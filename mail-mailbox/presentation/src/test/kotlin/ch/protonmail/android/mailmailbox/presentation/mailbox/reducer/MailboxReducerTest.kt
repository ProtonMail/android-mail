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

import ch.protonmail.android.mailcategory.presentation.model.CategoryViewState
import ch.protonmail.android.mailcategory.presentation.sample.CategoryItemUiModelSample
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.BottomBarState
import ch.protonmail.android.mailcommon.presentation.model.CappedNumberUiModel
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcommon.presentation.reducer.BottomBarReducer
import ch.protonmail.android.mailcommon.presentation.ui.delete.DeleteDialogState
import ch.protonmail.android.maillabel.domain.model.ViewMode
import ch.protonmail.android.maillabel.domain.sample.LabelIdSample
import ch.protonmail.android.maillabel.presentation.bottomsheet.moveto.MoveToItemId
import ch.protonmail.android.maillabel.presentation.text
import ch.protonmail.android.mailmailbox.domain.model.SpamOrTrash
import ch.protonmail.android.mailmailbox.presentation.R
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.LoadingBarUiState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxComposerNavigationState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxEvent
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxListState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxOperation
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxOperation.AffectingDeleteDialog
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxTopAppBarState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxViewAction
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.ShowSpamTrashIncludeFilterState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.UnreadFilterState
import ch.protonmail.android.mailmailbox.presentation.mailbox.previewdata.MailboxSearchStateSampleData
import ch.protonmail.android.mailmailbox.presentation.mailbox.previewdata.MailboxStateSampleData
import ch.protonmail.android.mailmessage.presentation.model.AvatarImagesUiModel
import ch.protonmail.android.mailmessage.presentation.reducer.BottomSheetReducer
import ch.protonmail.android.testdata.mailbox.MailboxItemUiModelTestData
import ch.protonmail.android.testdata.mailbox.UnreadCountersTestData
import ch.protonmail.android.testdata.maillabel.MailLabelTestData
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
    private val showSpamTrashFilterReducer: MailboxShowSpamTrashFilterReducer = mockk {
        every { newStateFrom(any(), any()) } returns reducedState.showSpamTrashIncludeFilterState
    }
    private val bottomAppBarReducer: BottomBarReducer = mockk {
        every { newStateFrom(any(), any()) } returns reducedState.bottomAppBarState
    }
    private val actionMessageReducer: MailboxActionMessageReducer = mockk {
        every { newStateFrom(any()) } returns reducedState.actionResult
    }
    private val deleteDialogReducer: MailboxDeleteDialogReducer = mockk {
        every { newStateFrom(any()) } returns reducedState.deleteDialogState
    }
    private val clearAllDialogReducer: MailboxClearAllDialogReducer = mockk {
        every { newStateFrom(any()) } returns reducedState.clearAllDialogState
    }
    private val bottomSheetReducer: BottomSheetReducer = mockk {
        every { newStateFrom(any(), any()) } returns reducedState.bottomSheetState
    }

    private val categoryViewReducer: MailboxCategoryViewReducer = mockk {
        every { newStateFrom(any(), any()) } returns reducedState.categoryViewState
    }

    private val mailboxReducer = MailboxReducer(
        mailboxListReducer,
        topAppBarReducer,
        unreadFilterReducer,
        categoryViewReducer,
        showSpamTrashFilterReducer,
        bottomAppBarReducer,
        actionMessageReducer,
        deleteDialogReducer,
        clearAllDialogReducer,
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

        if (shouldReduceSpamTrashFilterState) {
            verify {
                showSpamTrashFilterReducer.newStateFrom(
                    currentState.showSpamTrashIncludeFilterState,
                    operation as MailboxOperation.AffectingShowSpamTrashFilter
                )
            }
        } else {
            assertEquals(
                expected = currentState.showSpamTrashIncludeFilterState,
                actual = nextState.showSpamTrashIncludeFilterState,
                message = testName
            )
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

        if (shouldReduceClearAllDialog) {
            verify { clearAllDialogReducer.newStateFrom(operation as MailboxOperation.AffectingClearAllDialog) }
        } else {
            assertEquals(currentState.clearAllDialogState, nextState.clearAllDialogState)
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
    }

    companion object {

        private val spamLabel = MailLabelTestData.spamSystemLabel
        private val currentState = MailboxStateSampleData.Loading
        private val reducedState = MailboxState(
            mailboxListState = MailboxListState.Data.ViewMode(
                currentMailLabel = spamLabel,
                openItemEffect = Effect.empty(),
                scrollToMailboxTop = Effect.empty(),
                refreshErrorEffect = Effect.empty(),
                refreshOngoing = false,
                swipeActions = null,
                searchState = MailboxSearchStateSampleData.NotSearching,
                shouldShowFab = true,
                avatarImagesUiModel = AvatarImagesUiModel.Empty,
                loadingBarState = LoadingBarUiState.Hide
            ),
            topAppBarState = MailboxTopAppBarState.Data.DefaultMode(
                currentLabelName = spamLabel.text(),
                primaryAvatarItem = null
            ),
            unreadFilterState = UnreadFilterState.Data(
                unreadCount = CappedNumberUiModel.Exact(42),
                isFilterEnabled = false
            ),
            categoryViewState = CategoryViewState.Available.Data(
                categories = CategoryItemUiModelSample.all
            ),
            showSpamTrashIncludeFilterState = ShowSpamTrashIncludeFilterState.Data.Hidden,
            bottomAppBarState = BottomBarState.Loading,
            actionResult = Effect.empty(),
            deleteDialogState = DeleteDialogState.Hidden,
            clearAllDialogState = DeleteDialogState.Hidden,
            bottomSheetState = null,
            composerNavigationState = MailboxComposerNavigationState.Enabled(),
            error = Effect.empty(),
            showRatingBooster = Effect.empty()
        )

        private val actions = listOf(
            TestInput(
                MailboxViewAction.OnItemLongClicked(MailboxItemUiModelTestData.readMailboxItemUiModel),
                shouldReduceMailboxListState = false,
                shouldReduceTopAppBarState = false,
                shouldReduceUnreadFilterState = false,
                shouldReduceSpamTrashFilterState = false,
                shouldReduceBottomAppBarState = false,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = false,
                shouldReduceClearAllDialog = false,
                shouldReduceBottomSheetState = false
            ),
            TestInput(
                MailboxViewAction.OnItemAvatarClicked(MailboxItemUiModelTestData.readMailboxItemUiModel),
                shouldReduceMailboxListState = false,
                shouldReduceTopAppBarState = false,
                shouldReduceUnreadFilterState = false,
                shouldReduceSpamTrashFilterState = false,
                shouldReduceBottomAppBarState = false,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = false,
                shouldReduceClearAllDialog = false,
                shouldReduceBottomSheetState = false
            ),
            TestInput(
                MailboxViewAction.ExitSelectionMode,
                shouldReduceMailboxListState = true,
                shouldReduceTopAppBarState = true,
                shouldReduceUnreadFilterState = false,
                shouldReduceSpamTrashFilterState = false,
                shouldReduceBottomAppBarState = true,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = false,
                shouldReduceClearAllDialog = false,
                shouldReduceBottomSheetState = false
            ),
            TestInput(
                MailboxViewAction.ExitSearchMode,
                shouldReduceMailboxListState = true,
                shouldReduceTopAppBarState = true,
                shouldReduceUnreadFilterState = false,
                shouldReduceSpamTrashFilterState = false,
                shouldReduceBottomAppBarState = true,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = false,
                shouldReduceClearAllDialog = false,
                shouldReduceBottomSheetState = false
            ),
            TestInput(
                MailboxViewAction.ItemClicked(MailboxItemUiModelTestData.readMailboxItemUiModel),
                shouldReduceMailboxListState = false,
                shouldReduceTopAppBarState = false,
                shouldReduceUnreadFilterState = false,
                shouldReduceSpamTrashFilterState = false,
                shouldReduceBottomAppBarState = false,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = false,
                shouldReduceClearAllDialog = false,
                shouldReduceBottomSheetState = false
            ),
            TestInput(
                MailboxViewAction.EnableUnreadFilter,
                shouldReduceMailboxListState = false,
                shouldReduceTopAppBarState = false,
                shouldReduceUnreadFilterState = true,
                shouldReduceSpamTrashFilterState = false,
                shouldReduceBottomAppBarState = false,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = false,
                shouldReduceClearAllDialog = false,
                shouldReduceBottomSheetState = false
            ),
            TestInput(
                MailboxViewAction.DisableUnreadFilter,
                shouldReduceMailboxListState = false,
                shouldReduceTopAppBarState = false,
                shouldReduceUnreadFilterState = true,
                shouldReduceSpamTrashFilterState = false,
                shouldReduceBottomAppBarState = false,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = false,
                shouldReduceClearAllDialog = false,
                shouldReduceBottomSheetState = false
            ),
            TestInput(
                MailboxViewAction.RequestLabelAsBottomSheet,
                shouldReduceMailboxListState = false,
                shouldReduceTopAppBarState = false,
                shouldReduceUnreadFilterState = false,
                shouldReduceSpamTrashFilterState = false,
                shouldReduceBottomAppBarState = false,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = false,
                shouldReduceClearAllDialog = false,
                shouldReduceBottomSheetState = true
            ),
            TestInput(
                MailboxViewAction.DismissBottomSheet,
                shouldReduceMailboxListState = false,
                shouldReduceTopAppBarState = false,
                shouldReduceUnreadFilterState = false,
                shouldReduceSpamTrashFilterState = false,
                shouldReduceBottomAppBarState = false,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = false,
                shouldReduceClearAllDialog = false,
                shouldReduceBottomSheetState = true
            ),
            TestInput(
                MailboxViewAction.RequestMoveToBottomSheet,
                shouldReduceMailboxListState = false,
                shouldReduceTopAppBarState = false,
                shouldReduceUnreadFilterState = false,
                shouldReduceSpamTrashFilterState = false,
                shouldReduceBottomAppBarState = false,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = false,
                shouldReduceClearAllDialog = false,
                shouldReduceBottomSheetState = true
            ),
            TestInput(
                MailboxEvent.MoveToConfirmed.Trash(ViewMode.ConversationGrouping, 1),
                shouldReduceMailboxListState = true,
                shouldReduceTopAppBarState = true,
                shouldReduceUnreadFilterState = false,
                shouldReduceSpamTrashFilterState = false,
                shouldReduceBottomAppBarState = true,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = false,
                shouldReduceClearAllDialog = false,
                shouldReduceBottomSheetState = true
            ),
            TestInput(
                MailboxViewAction.SwipeMoveToAction(itemId = MoveToItemId("Item1")),
                shouldReduceMailboxListState = false,
                shouldReduceTopAppBarState = false,
                shouldReduceUnreadFilterState = false,
                shouldReduceSpamTrashFilterState = false,
                shouldReduceBottomAppBarState = false,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = false,
                shouldReduceClearAllDialog = false,
                shouldReduceBottomSheetState = true
            ),
            TestInput(
                MailboxViewAction.RequestMoreActionsBottomSheet,
                shouldReduceMailboxListState = false,
                shouldReduceTopAppBarState = false,
                shouldReduceUnreadFilterState = false,
                shouldReduceSpamTrashFilterState = false,
                shouldReduceBottomAppBarState = false,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = false,
                shouldReduceClearAllDialog = false,
                shouldReduceBottomSheetState = true
            ),
            TestInput(
                MailboxViewAction.DismissBottomSheet,
                shouldReduceMailboxListState = false,
                shouldReduceTopAppBarState = false,
                shouldReduceUnreadFilterState = false,
                shouldReduceSpamTrashFilterState = false,
                shouldReduceBottomAppBarState = false,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = false,
                shouldReduceClearAllDialog = false,
                shouldReduceBottomSheetState = true
            ),
            TestInput(
                MailboxViewAction.Star,
                shouldReduceMailboxListState = true,
                shouldReduceTopAppBarState = false,
                shouldReduceUnreadFilterState = false,
                shouldReduceSpamTrashFilterState = false,
                shouldReduceBottomAppBarState = false,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = false,
                shouldReduceClearAllDialog = false,
                shouldReduceBottomSheetState = true
            ),
            TestInput(
                MailboxViewAction.UnStar,
                shouldReduceMailboxListState = true,
                shouldReduceTopAppBarState = false,
                shouldReduceUnreadFilterState = false,
                shouldReduceSpamTrashFilterState = false,
                shouldReduceBottomAppBarState = false,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = false,
                shouldReduceClearAllDialog = false,
                shouldReduceBottomSheetState = true
            ),
            TestInput(
                MailboxViewAction.MoveToArchive,
                shouldReduceMailboxListState = true,
                shouldReduceTopAppBarState = true,
                shouldReduceUnreadFilterState = false,
                shouldReduceSpamTrashFilterState = false,
                shouldReduceBottomAppBarState = true,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = false,
                shouldReduceClearAllDialog = false,
                shouldReduceBottomSheetState = true
            ),
            TestInput(
                MailboxViewAction.MoveToSpam,
                shouldReduceMailboxListState = true,
                shouldReduceTopAppBarState = true,
                shouldReduceUnreadFilterState = false,
                shouldReduceSpamTrashFilterState = false,
                shouldReduceBottomAppBarState = true,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = false,
                shouldReduceClearAllDialog = false,
                shouldReduceBottomSheetState = true
            ),
            TestInput(
                MailboxViewAction.MarkAsRead,
                shouldReduceMailboxListState = true,
                shouldReduceTopAppBarState = false,
                shouldReduceUnreadFilterState = false,
                shouldReduceSpamTrashFilterState = false,
                shouldReduceBottomAppBarState = false,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = false,
                shouldReduceClearAllDialog = false,
                shouldReduceBottomSheetState = true
            ),
            TestInput(
                MailboxViewAction.MarkAsUnread,
                shouldReduceMailboxListState = true,
                shouldReduceTopAppBarState = false,
                shouldReduceUnreadFilterState = false,
                shouldReduceSpamTrashFilterState = false,
                shouldReduceBottomAppBarState = false,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = false,
                shouldReduceClearAllDialog = false,
                shouldReduceBottomSheetState = true
            ),
            TestInput(
                MailboxViewAction.ClearAllConfirmed,
                shouldReduceMailboxListState = false,
                shouldReduceTopAppBarState = false,
                shouldReduceUnreadFilterState = false,
                shouldReduceSpamTrashFilterState = false,
                shouldReduceBottomAppBarState = false,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = false,
                shouldReduceClearAllDialog = true,
                shouldReduceBottomSheetState = false
            ),
            TestInput(
                MailboxViewAction.ClearAllDismissed,
                shouldReduceMailboxListState = false,
                shouldReduceTopAppBarState = false,
                shouldReduceUnreadFilterState = false,
                shouldReduceSpamTrashFilterState = false,
                shouldReduceBottomAppBarState = false,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = false,
                shouldReduceClearAllDialog = true,
                shouldReduceBottomSheetState = false
            )
        )

        private val events = listOf(
            TestInput(
                MailboxEvent.ItemClicked.ItemDetailsOpened(
                    item = MailboxItemUiModelTestData.readMailboxItemUiModel,
                    contextLabel = LabelIdSample.RustLabel1,
                    viewModeIsConversationGrouping = true,
                    subitemId = null,
                    openedFromCategory = null
                ),
                shouldReduceMailboxListState = true,
                shouldReduceTopAppBarState = false,
                shouldReduceUnreadFilterState = false,
                shouldReduceSpamTrashFilterState = false,
                shouldReduceBottomAppBarState = false,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = false,
                shouldReduceClearAllDialog = false,
                shouldReduceBottomSheetState = false
            ),
            TestInput(
                MailboxEvent.ItemClicked.ItemAddedToSelection(
                    item = MailboxItemUiModelTestData.readMailboxItemUiModel
                ),
                shouldReduceMailboxListState = true,
                shouldReduceTopAppBarState = true,
                shouldReduceUnreadFilterState = false,
                shouldReduceSpamTrashFilterState = false,
                shouldReduceBottomAppBarState = false,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = false,
                shouldReduceClearAllDialog = false,
                shouldReduceBottomSheetState = false
            ),
            TestInput(
                MailboxEvent.ItemClicked.ItemRemovedFromSelection(
                    item = MailboxItemUiModelTestData.readMailboxItemUiModel
                ),
                shouldReduceMailboxListState = true,
                shouldReduceTopAppBarState = true,
                shouldReduceUnreadFilterState = false,
                shouldReduceSpamTrashFilterState = false,
                shouldReduceBottomAppBarState = false,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = false,
                shouldReduceClearAllDialog = false,
                shouldReduceBottomSheetState = false
            ),
            TestInput(
                MailboxEvent.NewLabelSelected(
                    selectedLabel = MailLabelTestData.dynamicSystemLabels.first(),
                    selectedLabelCount = UnreadCountersTestData.systemUnreadCounters.first().count
                ),
                shouldReduceMailboxListState = true,
                shouldReduceTopAppBarState = true,
                shouldReduceUnreadFilterState = true,
                shouldReduceSpamTrashFilterState = true,
                shouldReduceBottomAppBarState = false,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = false,
                shouldReduceClearAllDialog = false,
                shouldReduceBottomSheetState = false
            ),
            TestInput(
                MailboxEvent.SelectedLabelChanged(MailLabelTestData.dynamicSystemLabels.first()),
                shouldReduceMailboxListState = true,
                shouldReduceTopAppBarState = true,
                shouldReduceUnreadFilterState = false,
                shouldReduceSpamTrashFilterState = false,
                shouldReduceBottomAppBarState = false,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = false,
                shouldReduceClearAllDialog = false,
                shouldReduceBottomSheetState = false
            ),
            TestInput(
                MailboxEvent.SelectedLabelCountChanged(UnreadCountersTestData.systemUnreadCounters.first().count),
                shouldReduceMailboxListState = false,
                shouldReduceTopAppBarState = false,
                shouldReduceUnreadFilterState = true,
                shouldReduceSpamTrashFilterState = false,
                shouldReduceBottomAppBarState = false,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = false,
                shouldReduceClearAllDialog = false,
                shouldReduceBottomSheetState = false
            ),
            TestInput(
                MailboxEvent.EnterSelectionMode(MailboxItemUiModelTestData.readMailboxItemUiModel),
                shouldReduceMailboxListState = true,
                shouldReduceTopAppBarState = true,
                shouldReduceUnreadFilterState = false,
                shouldReduceSpamTrashFilterState = false,
                shouldReduceBottomAppBarState = true,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = false,
                shouldReduceClearAllDialog = false,
                shouldReduceBottomSheetState = false
            ),
            TestInput(
                MailboxEvent.Delete(ViewMode.ConversationGrouping, 5),
                shouldReduceMailboxListState = false,
                shouldReduceTopAppBarState = false,
                shouldReduceUnreadFilterState = false,
                shouldReduceSpamTrashFilterState = false,
                shouldReduceBottomAppBarState = false,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = true,
                shouldReduceClearAllDialog = false,
                shouldReduceBottomSheetState = false
            ),
            TestInput(
                MailboxEvent.Delete(ViewMode.NoConversationGrouping, 5),
                shouldReduceMailboxListState = false,
                shouldReduceTopAppBarState = false,
                shouldReduceUnreadFilterState = false,
                shouldReduceSpamTrashFilterState = false,
                shouldReduceBottomAppBarState = false,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = true,
                shouldReduceClearAllDialog = false,
                shouldReduceBottomSheetState = false
            ),
            TestInput(
                MailboxEvent.DeleteConfirmed(ViewMode.ConversationGrouping, 5),
                shouldReduceMailboxListState = true,
                shouldReduceTopAppBarState = true,
                shouldReduceUnreadFilterState = false,
                shouldReduceSpamTrashFilterState = false,
                shouldReduceBottomAppBarState = true,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = false,
                shouldReduceClearAllDialog = false,
                shouldReduceBottomSheetState = true
            ),
            TestInput(
                MailboxEvent.DeleteConfirmed(ViewMode.NoConversationGrouping, 5),
                shouldReduceMailboxListState = true,
                shouldReduceTopAppBarState = true,
                shouldReduceUnreadFilterState = false,
                shouldReduceSpamTrashFilterState = false,
                shouldReduceBottomAppBarState = true,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = false,
                shouldReduceClearAllDialog = false,
                shouldReduceBottomSheetState = true
            ),
            TestInput(
                MailboxViewAction.DeleteDialogDismissed,
                shouldReduceMailboxListState = false,
                shouldReduceTopAppBarState = false,
                shouldReduceUnreadFilterState = false,
                shouldReduceSpamTrashFilterState = false,
                shouldReduceBottomAppBarState = false,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = true,
                shouldReduceClearAllDialog = false,
                shouldReduceBottomSheetState = false
            ),
            TestInput(
                MailboxEvent.ErrorLabeling,
                shouldReduceMailboxListState = false,
                shouldReduceTopAppBarState = false,
                shouldReduceUnreadFilterState = false,
                shouldReduceSpamTrashFilterState = false,
                shouldReduceBottomAppBarState = false,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = false,
                shouldReduceBottomSheetState = false,
                shouldReduceClearAllDialog = false,
                errorBarState = Effect.of(TextUiModel(R.string.mailbox_action_label_messages_failed))
            ),
            TestInput(
                MailboxEvent.ShowRatingBooster,
                shouldReduceMailboxListState = false,
                shouldReduceTopAppBarState = false,
                shouldReduceUnreadFilterState = false,
                shouldReduceSpamTrashFilterState = false,
                shouldReduceBottomAppBarState = false,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = false,
                shouldReduceBottomSheetState = false,
                shouldReduceClearAllDialog = false,
                showRatingBooster = Effect.of(Unit)
            ),
            TestInput(
                MailboxEvent.ClearAll(SpamOrTrash.Spam),
                shouldReduceMailboxListState = false,
                shouldReduceTopAppBarState = false,
                shouldReduceUnreadFilterState = false,
                shouldReduceSpamTrashFilterState = false,
                shouldReduceBottomAppBarState = false,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = false,
                shouldReduceBottomSheetState = false,
                shouldReduceClearAllDialog = true
            ),
            TestInput(
                MailboxEvent.HideSpamTrashFilter,
                shouldReduceMailboxListState = false,
                shouldReduceTopAppBarState = false,
                shouldReduceUnreadFilterState = false,
                shouldReduceSpamTrashFilterState = true,
                shouldReduceBottomAppBarState = false,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = false,
                shouldReduceBottomSheetState = false,
                shouldReduceClearAllDialog = false
            ),
            TestInput(
                MailboxEvent.ShowSpamTrashFilter,
                shouldReduceMailboxListState = false,
                shouldReduceTopAppBarState = false,
                shouldReduceUnreadFilterState = false,
                shouldReduceSpamTrashFilterState = true,
                shouldReduceBottomAppBarState = false,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = false,
                shouldReduceBottomSheetState = false,
                shouldReduceClearAllDialog = false
            ),
            TestInput(
                MailboxViewAction.EnableShowSpamTrashFilter,
                shouldReduceMailboxListState = false,
                shouldReduceTopAppBarState = false,
                shouldReduceUnreadFilterState = false,
                shouldReduceSpamTrashFilterState = true,
                shouldReduceBottomAppBarState = false,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = false,
                shouldReduceBottomSheetState = false,
                shouldReduceClearAllDialog = false
            ),
            TestInput(
                MailboxViewAction.DisableShowSpamTrashFilter,
                shouldReduceMailboxListState = false,
                shouldReduceTopAppBarState = false,
                shouldReduceUnreadFilterState = false,
                shouldReduceSpamTrashFilterState = true,
                shouldReduceBottomAppBarState = false,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = false,
                shouldReduceBottomSheetState = false,
                shouldReduceClearAllDialog = false
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
        val shouldReduceSpamTrashFilterState: Boolean,
        val shouldReduceBottomAppBarState: Boolean,
        val shouldReduceActionMessage: Boolean,
        val shouldReduceDeleteDialog: Boolean,
        val shouldReduceClearAllDialog: Boolean,
        val shouldReduceBottomSheetState: Boolean,
        val errorBarState: Effect<TextUiModel> = Effect.empty(),
        val showRatingBooster: Effect<Unit> = Effect.empty()
    )
}
