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

import ch.protonmail.android.maillabel.domain.model.ViewMode
import ch.protonmail.android.maillabel.presentation.text
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxEvent
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxItemUiModel
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxOperation
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxTopAppBarState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxTopAppBarState.Data.SelectionMode
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxViewAction
import ch.protonmail.android.testdata.mailbox.MailboxItemUiModelTestData.readMailboxItemUiModel
import ch.protonmail.android.testdata.maillabel.MailLabelTestData
import me.proton.android.core.accountmanager.domain.model.CoreAccountAvatarItem
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

        private val avatarItem = CoreAccountAvatarItem()
        private val inboxLabel = MailLabelTestData.inboxSystemLabel
        private val trashLabel = MailLabelTestData.trashSystemLabel

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
                expectedState = MailboxTopAppBarState.Data.DefaultMode(inboxLabel.text(), null)
            ),
            TestInput(
                currentState = MailboxTopAppBarState.Loading,
                operation = MailboxEvent.SelectedLabelChanged(inboxLabel),
                expectedState = MailboxTopAppBarState.Data.DefaultMode(inboxLabel.text(), null)
            )
        )

        private val transitionsFromDefaultModeState = listOf(
            TestInput(
                currentState = MailboxTopAppBarState.Data.DefaultMode(inboxLabel.text(), avatarItem),
                operation = MailboxEvent.EnterSelectionMode(readMailboxItemUiModel),
                expectedState = SelectionMode(
                    inboxLabel.text(),
                    avatarItem,
                    selectedCount = 1
                )
            ),
            TestInput(
                currentState = MailboxTopAppBarState.Data.DefaultMode(inboxLabel.text(), avatarItem),
                operation = MailboxViewAction.ExitSelectionMode,
                expectedState = MailboxTopAppBarState.Data.DefaultMode(inboxLabel.text(), avatarItem)
            ),
            TestInput(
                currentState = MailboxTopAppBarState.Data.DefaultMode(inboxLabel.text(), avatarItem),
                operation = MailboxEvent.NewLabelSelected(trashLabel, selectedLabelCount = 42),
                expectedState = MailboxTopAppBarState.Data.DefaultMode(trashLabel.text(), avatarItem)
            ),
            TestInput(
                currentState = MailboxTopAppBarState.Data.DefaultMode(inboxLabel.text(), avatarItem),
                operation = MailboxEvent.SelectedLabelChanged(trashLabel),
                expectedState = MailboxTopAppBarState.Data.DefaultMode(trashLabel.text(), avatarItem)
            )
        )

        private val transitionsFromSelectionModeState = listOf(
            TestInput(
                currentState = SelectionMode(
                    inboxLabel.text(),
                    avatarItem,
                    selectedCount = 42
                ),
                operation = MailboxEvent.EnterSelectionMode(readMailboxItemUiModel),
                expectedState = SelectionMode(
                    inboxLabel.text(),
                    avatarItem,
                    selectedCount = 1
                )
            ),
            TestInput(
                currentState = SelectionMode(
                    inboxLabel.text(),
                    avatarItem,
                    selectedCount = 42
                ),
                operation = MailboxViewAction.ExitSelectionMode,
                expectedState = MailboxTopAppBarState.Data.DefaultMode(inboxLabel.text(), avatarItem)
            ),
            TestInput(
                currentState = SelectionMode(
                    inboxLabel.text(),
                    avatarItem,
                    selectedCount = 42
                ),
                operation = MailboxEvent.ItemClicked.ItemAddedToSelection(readMailboxItemUiModel),
                expectedState = SelectionMode(
                    inboxLabel.text(),
                    avatarItem,
                    selectedCount = 43
                )
            ),
            TestInput(
                currentState = SelectionMode(
                    inboxLabel.text(),
                    avatarItem,
                    selectedCount = 42
                ),
                operation = MailboxEvent.ItemClicked.ItemRemovedFromSelection(readMailboxItemUiModel),
                expectedState = SelectionMode(
                    inboxLabel.text(),
                    avatarItem,
                    selectedCount = 41
                )
            ),
            TestInput(
                currentState = SelectionMode(
                    inboxLabel.text(),
                    avatarItem,
                    selectedCount = 42
                ),
                operation = MailboxEvent.NewLabelSelected(trashLabel, selectedLabelCount = 42),
                expectedState = SelectionMode(
                    trashLabel.text(),
                    avatarItem,
                    selectedCount = 42
                )
            ),
            TestInput(
                currentState = SelectionMode(
                    inboxLabel.text(),
                    avatarItem,
                    selectedCount = 42
                ),
                operation = MailboxEvent.SelectedLabelChanged(trashLabel),
                expectedState = SelectionMode(
                    trashLabel.text(),
                    avatarItem,
                    selectedCount = 42
                )
            ),
            TestInput(
                currentState = SelectionMode(
                    inboxLabel.text(),
                    avatarItem,
                    selectedCount = 42
                ),
                operation = MailboxEvent.MoveToConfirmed.Trash(ViewMode.ConversationGrouping, 42),
                expectedState = MailboxTopAppBarState.Data.DefaultMode(inboxLabel.text(), avatarItem)
            ),
            TestInput(
                currentState = SelectionMode(
                    inboxLabel.text(),
                    avatarItem,
                    selectedCount = 42
                ),
                operation = MailboxEvent.DeleteConfirmed(ViewMode.ConversationGrouping, 42),
                expectedState = MailboxTopAppBarState.Data.DefaultMode(inboxLabel.text(), avatarItem)
            ),
            TestInput(
                currentState = SelectionMode(
                    inboxLabel.text(),
                    avatarItem,
                    selectedCount = 42
                ),
                operation = MailboxEvent.DeleteConfirmed(ViewMode.NoConversationGrouping, 42),
                expectedState = MailboxTopAppBarState.Data.DefaultMode(inboxLabel.text(), avatarItem)
            ),
            TestInput(
                currentState = SelectionMode(
                    inboxLabel.text(),
                    avatarItem,
                    selectedCount = 42
                ),
                operation = MailboxEvent.ItemsRemovedFromSelection(itemIds = listOf("1", "2", "3")),
                expectedState = SelectionMode(
                    inboxLabel.text(),
                    avatarItem,
                    selectedCount = 39
                )
            ),
            TestInput(
                currentState = SelectionMode(
                    inboxLabel.text(),
                    avatarItem,
                    selectedCount = 42
                ),
                operation = MailboxEvent.MoveToConfirmed.Trash(ViewMode.NoConversationGrouping, 42),
                expectedState = MailboxTopAppBarState.Data.DefaultMode(inboxLabel.text(), avatarItem)
            ),
            TestInput(
                currentState = SelectionMode(
                    inboxLabel.text(),
                    avatarItem,
                    selectedCount = 42
                ),
                operation = MailboxViewAction.MoveToArchive,
                expectedState = MailboxTopAppBarState.Data.DefaultMode(inboxLabel.text(), avatarItem)
            ),
            TestInput(
                currentState = SelectionMode(
                    inboxLabel.text(),
                    avatarItem,
                    selectedCount = 42
                ),
                operation = MailboxViewAction.MoveToSpam,
                expectedState = MailboxTopAppBarState.Data.DefaultMode(inboxLabel.text(), avatarItem)
            ),
            TestInput(
                currentState = SelectionMode(
                    inboxLabel.text(),
                    avatarItem,
                    selectedCount = 42
                ),
                operation = MailboxEvent.AllItemsSelected(
                    mutableListOf<MailboxItemUiModel>().apply {
                        for (i in 0..200) {
                            add(readMailboxItemUiModel.copy(id = i.toString()))
                        }
                    }
                ),
                expectedState = SelectionMode(inboxLabel.text(), avatarItem, selectedCount = 100)
            ),
            TestInput(
                currentState = SelectionMode(
                    inboxLabel.text(),
                    avatarItem,
                    selectedCount = 42
                ),
                operation = MailboxViewAction.SnoozeDismissed,
                expectedState = MailboxTopAppBarState.Data.DefaultMode(inboxLabel.text(), avatarItem)
            )
        )

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data(): Collection<Array<Any>> {
            return (
                transitionsFromLoadingState +
                    transitionsFromDefaultModeState +
                    transitionsFromSelectionModeState
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
