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

import ch.protonmail.android.maillabel.domain.model.MailLabel
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.maillabel.presentation.text
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxEvent
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxOperation
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxTopAppBarState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxViewAction
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.MoveToBottomSheetEntryPoint
import ch.protonmail.android.testdata.mailbox.MailboxItemUiModelTestData.readMailboxItemUiModel
import me.proton.core.mailsettings.domain.entity.ViewMode
import me.proton.core.util.kotlin.EMPTY_STRING
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
internal class MailboxTopAppBarReducerTest(
    private val testName: String,
    private val testInput: TestInput
) {

    private val topAppBarReducer = MailboxTopAppBarReducer()

    @Test
    fun `should produce the expected new state`() = with(testInput) {
        val actualState = topAppBarReducer.newStateFrom(currentState, operation)

        assertEquals(expectedState, actualState, testName)
    }

    companion object {

        private val inboxLabel = MailLabel.System(MailLabelId.System.Inbox)
        private val trashLabel = MailLabel.System(MailLabelId.System.Trash)

        private val transitionsFromLoadingState = listOf(
            TestInput(
                currentState = MailboxTopAppBarState.Loading,
                operation = MailboxEvent.EnterSelectionMode(readMailboxItemUiModel),
                expectedState = MailboxTopAppBarState.Loading
            ),
            TestInput(
                currentState = MailboxTopAppBarState.Loading,
                operation = MailboxViewAction.ExitSelectionMode,
                expectedState = MailboxTopAppBarState.Loading
            ),
            TestInput(
                currentState = MailboxTopAppBarState.Loading,
                operation = MailboxEvent.ItemClicked.ItemAddedToSelection(readMailboxItemUiModel),
                expectedState = MailboxTopAppBarState.Loading
            ),
            TestInput(
                currentState = MailboxTopAppBarState.Loading,
                operation = MailboxEvent.ItemClicked.ItemRemovedFromSelection(readMailboxItemUiModel),
                expectedState = MailboxTopAppBarState.Loading
            ),
            TestInput(
                currentState = MailboxTopAppBarState.Loading,
                operation = MailboxEvent.NewLabelSelected(inboxLabel, selectedLabelCount = 42),
                expectedState = MailboxTopAppBarState.Data.DefaultMode(inboxLabel.text())
            ),
            TestInput(
                currentState = MailboxTopAppBarState.Loading,
                operation = MailboxEvent.SelectedLabelChanged(inboxLabel),
                expectedState = MailboxTopAppBarState.Data.DefaultMode(inboxLabel.text())
            ),
            TestInput(
                currentState = MailboxTopAppBarState.Loading,
                operation = MailboxViewAction.EnterSearchMode,
                expectedState = MailboxTopAppBarState.Loading
            ),
            TestInput(
                currentState = MailboxTopAppBarState.Loading,
                operation = MailboxViewAction.ExitSearchMode,
                expectedState = MailboxTopAppBarState.Loading
            )
        )

        private val transitionsFromDefaultModeState = listOf(
            TestInput(
                currentState = MailboxTopAppBarState.Data.DefaultMode(inboxLabel.text()),
                operation = MailboxEvent.EnterSelectionMode(readMailboxItemUiModel),
                expectedState = MailboxTopAppBarState.Data.SelectionMode(inboxLabel.text(), selectedCount = 1)
            ),
            TestInput(
                currentState = MailboxTopAppBarState.Data.DefaultMode(inboxLabel.text()),
                operation = MailboxViewAction.ExitSelectionMode,
                expectedState = MailboxTopAppBarState.Data.DefaultMode(inboxLabel.text())
            ),
            TestInput(
                currentState = MailboxTopAppBarState.Data.DefaultMode(inboxLabel.text()),
                operation = MailboxEvent.NewLabelSelected(trashLabel, selectedLabelCount = 42),
                expectedState = MailboxTopAppBarState.Data.DefaultMode(trashLabel.text())
            ),
            TestInput(
                currentState = MailboxTopAppBarState.Data.DefaultMode(inboxLabel.text()),
                operation = MailboxEvent.SelectedLabelChanged(trashLabel),
                expectedState = MailboxTopAppBarState.Data.DefaultMode(trashLabel.text())
            ),
            TestInput(
                currentState = MailboxTopAppBarState.Data.DefaultMode(inboxLabel.text()),
                operation = MailboxViewAction.EnterSearchMode,
                expectedState = MailboxTopAppBarState.Data.SearchMode(inboxLabel.text(), searchQuery = EMPTY_STRING)
            ),
            TestInput(
                currentState = MailboxTopAppBarState.Data.DefaultMode(inboxLabel.text()),
                operation = MailboxViewAction.ExitSearchMode,
                expectedState = MailboxTopAppBarState.Data.DefaultMode(inboxLabel.text())
            )
        )

        private val transitionsFromSelectionModeState = listOf(
            TestInput(
                currentState = MailboxTopAppBarState.Data.SelectionMode(inboxLabel.text(), selectedCount = 42),
                operation = MailboxEvent.EnterSelectionMode(readMailboxItemUiModel),
                expectedState = MailboxTopAppBarState.Data.SelectionMode(inboxLabel.text(), selectedCount = 1)
            ),
            TestInput(
                currentState = MailboxTopAppBarState.Data.SelectionMode(inboxLabel.text(), selectedCount = 42),
                operation = MailboxViewAction.ExitSelectionMode,
                expectedState = MailboxTopAppBarState.Data.DefaultMode(inboxLabel.text())
            ),
            TestInput(
                currentState = MailboxTopAppBarState.Data.SelectionMode(inboxLabel.text(), selectedCount = 42),
                operation = MailboxEvent.ItemClicked.ItemAddedToSelection(readMailboxItemUiModel),
                expectedState = MailboxTopAppBarState.Data.SelectionMode(inboxLabel.text(), selectedCount = 43)
            ),
            TestInput(
                currentState = MailboxTopAppBarState.Data.SelectionMode(inboxLabel.text(), selectedCount = 42),
                operation = MailboxEvent.ItemClicked.ItemRemovedFromSelection(readMailboxItemUiModel),
                expectedState = MailboxTopAppBarState.Data.SelectionMode(inboxLabel.text(), selectedCount = 41)
            ),
            TestInput(
                currentState = MailboxTopAppBarState.Data.SelectionMode(inboxLabel.text(), selectedCount = 42),
                operation = MailboxEvent.NewLabelSelected(trashLabel, selectedLabelCount = 42),
                expectedState = MailboxTopAppBarState.Data.SelectionMode(trashLabel.text(), selectedCount = 42)
            ),
            TestInput(
                currentState = MailboxTopAppBarState.Data.SelectionMode(inboxLabel.text(), selectedCount = 42),
                operation = MailboxEvent.SelectedLabelChanged(trashLabel),
                expectedState = MailboxTopAppBarState.Data.SelectionMode(trashLabel.text(), selectedCount = 42)
            ),
            TestInput(
                currentState = MailboxTopAppBarState.Data.SelectionMode(inboxLabel.text(), selectedCount = 42),
                operation = MailboxEvent.Trash(ViewMode.ConversationGrouping, 42),
                expectedState = MailboxTopAppBarState.Data.DefaultMode(inboxLabel.text())
            ),
            TestInput(
                currentState = MailboxTopAppBarState.Data.SelectionMode(inboxLabel.text(), selectedCount = 42),
                operation = MailboxEvent.Trash(ViewMode.NoConversationGrouping, 42),
                expectedState = MailboxTopAppBarState.Data.DefaultMode(inboxLabel.text())
            ),
            TestInput(
                currentState = MailboxTopAppBarState.Data.SelectionMode(inboxLabel.text(), selectedCount = 42),
                operation = MailboxEvent.DeleteConfirmed(ViewMode.ConversationGrouping, 42),
                expectedState = MailboxTopAppBarState.Data.DefaultMode(inboxLabel.text())
            ),
            TestInput(
                currentState = MailboxTopAppBarState.Data.SelectionMode(inboxLabel.text(), selectedCount = 42),
                operation = MailboxEvent.DeleteConfirmed(ViewMode.NoConversationGrouping, 42),
                expectedState = MailboxTopAppBarState.Data.DefaultMode(inboxLabel.text())
            ),
            TestInput(
                currentState = MailboxTopAppBarState.Data.SelectionMode(inboxLabel.text(), selectedCount = 42),
                operation = MailboxEvent.ItemsRemovedFromSelection(itemIds = listOf("1", "2", "3")),
                expectedState = MailboxTopAppBarState.Data.SelectionMode(inboxLabel.text(), selectedCount = 39)
            ),
            TestInput(
                currentState = MailboxTopAppBarState.Data.SelectionMode(inboxLabel.text(), selectedCount = 42),
                operation = MailboxViewAction.MoveToConfirmed(MoveToBottomSheetEntryPoint.SelectionMode),
                expectedState = MailboxTopAppBarState.Data.DefaultMode(inboxLabel.text())
            ),
            TestInput(
                currentState = MailboxTopAppBarState.Data.SelectionMode(inboxLabel.text(), selectedCount = 42),
                operation = MailboxViewAction.MoveToArchive,
                expectedState = MailboxTopAppBarState.Data.DefaultMode(inboxLabel.text())
            ),
            TestInput(
                currentState = MailboxTopAppBarState.Data.SelectionMode(inboxLabel.text(), selectedCount = 42),
                operation = MailboxViewAction.MoveToSpam,
                expectedState = MailboxTopAppBarState.Data.DefaultMode(inboxLabel.text())
            ),
            TestInput(
                currentState = MailboxTopAppBarState.Data.SelectionMode(inboxLabel.text(), selectedCount = 42),
                operation = MailboxViewAction.EnterSearchMode,
                expectedState = MailboxTopAppBarState.Data.SelectionMode(inboxLabel.text(), selectedCount = 42)
            ),
            TestInput(
                currentState = MailboxTopAppBarState.Data.SelectionMode(inboxLabel.text(), selectedCount = 42),
                operation = MailboxViewAction.ExitSearchMode,
                expectedState = MailboxTopAppBarState.Data.SelectionMode(inboxLabel.text(), selectedCount = 42)
            )
        )

        private val transitionsFromSearchModeState = listOf(
            TestInput(
                currentState = MailboxTopAppBarState.Data.SearchMode(inboxLabel.text(), searchQuery = EMPTY_STRING),
                operation = MailboxEvent.EnterSelectionMode(readMailboxItemUiModel),
                expectedState = MailboxTopAppBarState.Data.SearchMode(inboxLabel.text(), searchQuery = EMPTY_STRING)
            ),
            TestInput(
                currentState = MailboxTopAppBarState.Data.SearchMode(inboxLabel.text(), searchQuery = EMPTY_STRING),
                operation = MailboxViewAction.ExitSelectionMode,
                expectedState = MailboxTopAppBarState.Data.SearchMode(inboxLabel.text(), searchQuery = EMPTY_STRING)
            ),
            TestInput(
                currentState = MailboxTopAppBarState.Data.SearchMode(inboxLabel.text(), searchQuery = EMPTY_STRING),
                operation = MailboxEvent.NewLabelSelected(trashLabel, selectedLabelCount = 42),
                expectedState = MailboxTopAppBarState.Data.SearchMode(trashLabel.text(), searchQuery = EMPTY_STRING)
            ),
            TestInput(
                currentState = MailboxTopAppBarState.Data.SearchMode(inboxLabel.text(), searchQuery = EMPTY_STRING),
                operation = MailboxEvent.SelectedLabelChanged(trashLabel),
                expectedState = MailboxTopAppBarState.Data.SearchMode(trashLabel.text(), searchQuery = EMPTY_STRING)
            ),
            TestInput(
                currentState = MailboxTopAppBarState.Data.SearchMode(inboxLabel.text(), searchQuery = EMPTY_STRING),
                operation = MailboxViewAction.EnterSearchMode,
                expectedState = MailboxTopAppBarState.Data.SearchMode(inboxLabel.text(), searchQuery = EMPTY_STRING)
            ),
            TestInput(
                currentState = MailboxTopAppBarState.Data.SearchMode(inboxLabel.text(), searchQuery = EMPTY_STRING),
                operation = MailboxViewAction.ExitSearchMode,
                expectedState = MailboxTopAppBarState.Data.DefaultMode(inboxLabel.text())
            )
        )

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data(): Collection<Array<Any>> {
            return (
                transitionsFromLoadingState +
                    transitionsFromDefaultModeState +
                    transitionsFromSelectionModeState +
                    transitionsFromSearchModeState
                )
                .map { testInput ->
                    val testName = """
                        Current state: ${testInput.currentState}
                        Operation: ${testInput.operation}
                        Next state: ${testInput.expectedState}

                    """.trimIndent()
                    arrayOf(testName, testInput)
                }
        }
    }

    data class TestInput(
        val currentState: MailboxTopAppBarState,
        val operation: MailboxOperation.AffectingTopAppBar,
        val expectedState: MailboxTopAppBarState
    )
}
