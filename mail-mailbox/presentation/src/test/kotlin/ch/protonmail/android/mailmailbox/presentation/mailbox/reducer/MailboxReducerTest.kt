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
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.BottomBarState
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcommon.presentation.reducer.BottomBarReducer
import ch.protonmail.android.maillabel.domain.model.MailLabel
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.maillabel.presentation.text
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.DeleteDialogState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxEvent
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxListState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxOperation
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxOperation.AffectingDeleteDialog
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxTopAppBarState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxViewAction
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.OnboardingState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.UnreadFilterState
import ch.protonmail.android.mailmailbox.presentation.mailbox.previewdata.MailboxStateSampleData
import ch.protonmail.android.mailmessage.presentation.reducer.BottomSheetReducer
import ch.protonmail.android.testdata.label.LabelTestData
import ch.protonmail.android.testdata.mailbox.MailboxItemUiModelTestData
import ch.protonmail.android.testdata.mailbox.UnreadCountersTestData
import ch.protonmail.android.mailmailbox.presentation.R
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
    private val onboardingReducer: OnboardingReducer = mockk {
        every { newStateFrom(any()) } returns reducedState.onboardingState
    }
    private val actionMessageReducer: MailboxActionMessageReducer = mockk {
        every { newStateFrom(any()) } returns reducedState.actionMessage
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
        onboardingReducer,
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
            assertEquals(currentState.actionMessage, nextState.actionMessage)
        }

        if (shouldReduceDeleteDialog) {
            verify { deleteDialogReducer.newStateFrom(operation as AffectingDeleteDialog) }
        } else {
            assertEquals(currentState.deleteDialogState, nextState.deleteDialogState)
        }

        if (shouldReduceBottomSheetState) {
            verify { bottomSheetReducer.newStateFrom(currentState.bottomSheetState, any()) }
        } else {
            assertEquals(currentState.bottomSheetState, nextState.bottomSheetState, testName)
        }
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
                selectionModeEnabled = false
            ),
            topAppBarState = MailboxTopAppBarState.Data.DefaultMode(
                currentLabelName = spamLabel.text()
            ),
            unreadFilterState = UnreadFilterState.Data(
                numUnread = 42,
                isFilterEnabled = false
            ),
            bottomAppBarState = BottomBarState.Loading,
            onboardingState = OnboardingState.Hidden,
            actionMessage = Effect.empty(),
            deleteDialogState = DeleteDialogState.Hidden,
            bottomSheetState = null,
            error = Effect.empty()
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
                shouldReduceBottomSheetState = false
            ),
            TestInput(
                MailboxViewAction.OnItemAvatarClicked(MailboxItemUiModelTestData.readMailboxItemUiModel),
                shouldReduceMailboxListState = false,
                shouldReduceTopAppBarState = false,
                shouldReduceUnreadFilterState = false,
                shouldReduceBottomAppBarState = false,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = false,
                shouldReduceBottomSheetState = false
            ),
            TestInput(
                MailboxViewAction.ExitSelectionMode,
                shouldReduceMailboxListState = true,
                shouldReduceTopAppBarState = true,
                shouldReduceUnreadFilterState = false,
                shouldReduceBottomAppBarState = true,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = false,
                shouldReduceBottomSheetState = false
            ),
            TestInput(
                MailboxViewAction.ItemClicked(MailboxItemUiModelTestData.readMailboxItemUiModel),
                shouldReduceMailboxListState = false,
                shouldReduceTopAppBarState = false,
                shouldReduceUnreadFilterState = false,
                shouldReduceBottomAppBarState = false,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = false,
                shouldReduceBottomSheetState = false
            ),
            TestInput(
                MailboxViewAction.EnableUnreadFilter,
                shouldReduceMailboxListState = false,
                shouldReduceTopAppBarState = false,
                shouldReduceUnreadFilterState = true,
                shouldReduceBottomAppBarState = false,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = false,
                shouldReduceBottomSheetState = false
            ),
            TestInput(
                MailboxViewAction.DisableUnreadFilter,
                shouldReduceMailboxListState = false,
                shouldReduceTopAppBarState = false,
                shouldReduceUnreadFilterState = true,
                shouldReduceBottomAppBarState = false,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = false,
                shouldReduceBottomSheetState = false
            ),
            TestInput(
                MailboxViewAction.RequestLabelAsBottomSheet,
                shouldReduceMailboxListState = false,
                shouldReduceTopAppBarState = false,
                shouldReduceUnreadFilterState = false,
                shouldReduceBottomAppBarState = false,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = false,
                shouldReduceBottomSheetState = true
            ),
            TestInput(
                MailboxViewAction.LabelAsToggleAction(LabelIdSample.Label2022),
                shouldReduceMailboxListState = false,
                shouldReduceTopAppBarState = false,
                shouldReduceUnreadFilterState = false,
                shouldReduceBottomAppBarState = false,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = false,
                shouldReduceBottomSheetState = true
            ),
            TestInput(
                MailboxViewAction.LabelAsConfirmed(archiveSelected = true),
                shouldReduceMailboxListState = false,
                shouldReduceTopAppBarState = false,
                shouldReduceUnreadFilterState = false,
                shouldReduceBottomAppBarState = false,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = false,
                shouldReduceBottomSheetState = true
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
                shouldReduceBottomSheetState = false
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
                shouldReduceBottomSheetState = false
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
                shouldReduceBottomSheetState = false
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
                shouldReduceBottomSheetState = false
            ),
            TestInput(
                MailboxEvent.SelectedLabelChanged(LabelTestData.systemLabels.first()),
                shouldReduceMailboxListState = true,
                shouldReduceTopAppBarState = true,
                shouldReduceUnreadFilterState = false,
                shouldReduceBottomAppBarState = false,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = false,
                shouldReduceBottomSheetState = false
            ),
            TestInput(
                MailboxEvent.SelectedLabelCountChanged(UnreadCountersTestData.systemUnreadCounters.first().count),
                shouldReduceMailboxListState = false,
                shouldReduceTopAppBarState = false,
                shouldReduceUnreadFilterState = true,
                shouldReduceBottomAppBarState = false,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = false,
                shouldReduceBottomSheetState = false
            ),
            TestInput(
                MailboxEvent.SelectionModeEnabledChanged(selectionModeEnabled = false),
                shouldReduceMailboxListState = true,
                shouldReduceTopAppBarState = false,
                shouldReduceUnreadFilterState = false,
                shouldReduceBottomAppBarState = false,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = false,
                shouldReduceBottomSheetState = false
            ),
            TestInput(
                MailboxEvent.EnterSelectionMode(MailboxItemUiModelTestData.readMailboxItemUiModel),
                shouldReduceMailboxListState = true,
                shouldReduceTopAppBarState = true,
                shouldReduceUnreadFilterState = false,
                shouldReduceBottomAppBarState = true,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = false,
                shouldReduceBottomSheetState = false
            ),
            TestInput(
                MailboxEvent.Trash(5),
                shouldReduceMailboxListState = true,
                shouldReduceTopAppBarState = true,
                shouldReduceUnreadFilterState = false,
                shouldReduceBottomAppBarState = true,
                shouldReduceActionMessage = true,
                shouldReduceDeleteDialog = false,
                shouldReduceBottomSheetState = false
            ),
            TestInput(
                MailboxEvent.Delete(ViewMode.ConversationGrouping, 5),
                shouldReduceMailboxListState = false,
                shouldReduceTopAppBarState = false,
                shouldReduceUnreadFilterState = false,
                shouldReduceBottomAppBarState = false,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = true,
                shouldReduceBottomSheetState = false
            ),
            TestInput(
                MailboxEvent.Delete(ViewMode.NoConversationGrouping, 5),
                shouldReduceMailboxListState = false,
                shouldReduceTopAppBarState = false,
                shouldReduceUnreadFilterState = false,
                shouldReduceBottomAppBarState = false,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = true,
                shouldReduceBottomSheetState = false
            ),
            TestInput(
                MailboxEvent.DeleteConfirmed(ViewMode.ConversationGrouping, 5),
                shouldReduceMailboxListState = true,
                shouldReduceTopAppBarState = true,
                shouldReduceUnreadFilterState = false,
                shouldReduceBottomAppBarState = true,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = false,
                shouldReduceBottomSheetState = false
            ),
            TestInput(
                MailboxEvent.DeleteConfirmed(ViewMode.NoConversationGrouping, 5),
                shouldReduceMailboxListState = true,
                shouldReduceTopAppBarState = true,
                shouldReduceUnreadFilterState = false,
                shouldReduceBottomAppBarState = true,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = false,
                shouldReduceBottomSheetState = false
            ),
            TestInput(
                MailboxViewAction.DeleteDialogDismissed,
                shouldReduceMailboxListState = false,
                shouldReduceTopAppBarState = false,
                shouldReduceUnreadFilterState = false,
                shouldReduceBottomAppBarState = false,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = true,
                shouldReduceBottomSheetState = false
            ),
            TestInput(
                MailboxEvent.ErrorLabelingMessages,
                shouldReduceMailboxListState = false,
                shouldReduceTopAppBarState = false,
                shouldReduceUnreadFilterState = false,
                shouldReduceBottomAppBarState = false,
                shouldReduceActionMessage = false,
                shouldReduceDeleteDialog = false,
                shouldReduceBottomSheetState = false,
                errorBarState = Effect.of(TextUiModel(R.string.mailbox_action_label_messages_failed))
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
        val shouldReduceBottomSheetState: Boolean,
        val errorBarState: Effect<TextUiModel> = Effect.empty()
    )
}
