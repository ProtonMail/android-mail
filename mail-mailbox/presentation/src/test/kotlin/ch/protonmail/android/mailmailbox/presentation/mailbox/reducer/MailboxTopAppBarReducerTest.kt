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
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxViewAction
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxTopAppBarState
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
                currentState = MailboxTopAppBarState.Loading(composerDisabled = true),
                operation = MailboxViewAction.EnterSelectionMode,
                expectedState = MailboxTopAppBarState.Loading(composerDisabled = true)
            ),
            TestInput(
                currentState = MailboxTopAppBarState.Loading(composerDisabled = false),
                operation = MailboxViewAction.EnterSelectionMode,
                expectedState = MailboxTopAppBarState.Loading(composerDisabled = false)
            ),
            TestInput(
                currentState = MailboxTopAppBarState.Loading(composerDisabled = true),
                operation = MailboxViewAction.ExitSelectionMode,
                expectedState = MailboxTopAppBarState.Loading(composerDisabled = true)
            ),
            TestInput(
                currentState = MailboxTopAppBarState.Loading(composerDisabled = false),
                operation = MailboxViewAction.ExitSelectionMode,
                expectedState = MailboxTopAppBarState.Loading(composerDisabled = false)
            ),
            TestInput(
                currentState = MailboxTopAppBarState.Loading(composerDisabled = true),
                operation = MailboxEvent.NewLabelSelected(inboxLabel, selectedLabelCount = 42),
                expectedState = MailboxTopAppBarState.Data.DefaultMode(inboxLabel.text(), composerDisabled = true)
            ),
            TestInput(
                currentState = MailboxTopAppBarState.Loading(composerDisabled = false),
                operation = MailboxEvent.NewLabelSelected(inboxLabel, selectedLabelCount = 42),
                expectedState = MailboxTopAppBarState.Data.DefaultMode(inboxLabel.text(), composerDisabled = false)
            ),
            TestInput(
                currentState = MailboxTopAppBarState.Loading(composerDisabled = true),
                operation = MailboxEvent.SelectedLabelChanged(inboxLabel),
                expectedState = MailboxTopAppBarState.Data.DefaultMode(inboxLabel.text(), composerDisabled = true)
            ),
            TestInput(
                currentState = MailboxTopAppBarState.Loading(composerDisabled = false),
                operation = MailboxEvent.SelectedLabelChanged(inboxLabel),
                expectedState = MailboxTopAppBarState.Data.DefaultMode(inboxLabel.text(), composerDisabled = false)
            ),
            TestInput(
                currentState = MailboxTopAppBarState.Loading(composerDisabled = true),
                operation = MailboxEvent.ComposerDisabledChanged(composerDisabled = false),
                expectedState = MailboxTopAppBarState.Loading(composerDisabled = false)
            ),
            TestInput(
                currentState = MailboxTopAppBarState.Loading(composerDisabled = false),
                operation = MailboxEvent.ComposerDisabledChanged(composerDisabled = true),
                expectedState = MailboxTopAppBarState.Loading(composerDisabled = true)
            )
        )

        private val transitionsFromDefaultModeState = listOf(
            TestInput(
                currentState = MailboxTopAppBarState.Data.DefaultMode(inboxLabel.text(), composerDisabled = true),
                operation = MailboxViewAction.EnterSelectionMode,
                expectedState = MailboxTopAppBarState.Data.SelectionMode(
                    inboxLabel.text(),
                    selectedCount = 0,
                    composerDisabled = true
                )
            ),
            TestInput(
                currentState = MailboxTopAppBarState.Data.DefaultMode(inboxLabel.text(), composerDisabled = false),
                operation = MailboxViewAction.EnterSelectionMode,
                expectedState = MailboxTopAppBarState.Data.SelectionMode(
                    inboxLabel.text(),
                    selectedCount = 0,
                    composerDisabled = false
                )
            ),
            TestInput(
                currentState = MailboxTopAppBarState.Data.DefaultMode(inboxLabel.text(), composerDisabled = true),
                operation = MailboxViewAction.ExitSelectionMode,
                expectedState = MailboxTopAppBarState.Data.DefaultMode(inboxLabel.text(), composerDisabled = true)
            ),
            TestInput(
                currentState = MailboxTopAppBarState.Data.DefaultMode(inboxLabel.text(), composerDisabled = false),
                operation = MailboxViewAction.ExitSelectionMode,
                expectedState = MailboxTopAppBarState.Data.DefaultMode(inboxLabel.text(), composerDisabled = false)
            ),
            TestInput(
                currentState = MailboxTopAppBarState.Data.DefaultMode(inboxLabel.text(), composerDisabled = true),
                operation = MailboxEvent.NewLabelSelected(trashLabel, selectedLabelCount = 42),
                expectedState = MailboxTopAppBarState.Data.DefaultMode(trashLabel.text(), composerDisabled = true)
            ),
            TestInput(
                currentState = MailboxTopAppBarState.Data.DefaultMode(inboxLabel.text(), composerDisabled = false),
                operation = MailboxEvent.NewLabelSelected(trashLabel, selectedLabelCount = 42),
                expectedState = MailboxTopAppBarState.Data.DefaultMode(trashLabel.text(), composerDisabled = false)
            ),
            TestInput(
                currentState = MailboxTopAppBarState.Data.DefaultMode(inboxLabel.text(), composerDisabled = true),
                operation = MailboxEvent.SelectedLabelChanged(trashLabel),
                expectedState = MailboxTopAppBarState.Data.DefaultMode(trashLabel.text(), composerDisabled = true)
            ),
            TestInput(
                currentState = MailboxTopAppBarState.Data.DefaultMode(inboxLabel.text(), composerDisabled = false),
                operation = MailboxEvent.SelectedLabelChanged(trashLabel),
                expectedState = MailboxTopAppBarState.Data.DefaultMode(trashLabel.text(), composerDisabled = false)
            ),
            TestInput(
                currentState = MailboxTopAppBarState.Data.DefaultMode(inboxLabel.text(), composerDisabled = true),
                operation = MailboxEvent.ComposerDisabledChanged(composerDisabled = false),
                expectedState = MailboxTopAppBarState.Data.DefaultMode(inboxLabel.text(), composerDisabled = false)
            ),
            TestInput(
                currentState = MailboxTopAppBarState.Data.DefaultMode(inboxLabel.text(), composerDisabled = false),
                operation = MailboxEvent.ComposerDisabledChanged(composerDisabled = true),
                expectedState = MailboxTopAppBarState.Data.DefaultMode(inboxLabel.text(), composerDisabled = true)
            )
        )

        private val transitionsFromSelectionModeState = listOf(
            TestInput(
                currentState = MailboxTopAppBarState.Data.SelectionMode(
                    inboxLabel.text(),
                    selectedCount = 42,
                    composerDisabled = true
                ),
                operation = MailboxViewAction.EnterSelectionMode,
                expectedState = MailboxTopAppBarState.Data.SelectionMode(
                    inboxLabel.text(),
                    selectedCount = 0,
                    composerDisabled = true
                )
            ),
            TestInput(
                currentState = MailboxTopAppBarState.Data.SelectionMode(
                    inboxLabel.text(),
                    selectedCount = 42,
                    composerDisabled = false
                ),
                operation = MailboxViewAction.EnterSelectionMode,
                expectedState = MailboxTopAppBarState.Data.SelectionMode(
                    inboxLabel.text(),
                    selectedCount = 0,
                    composerDisabled = false
                )
            ),
            TestInput(
                currentState = MailboxTopAppBarState.Data.SelectionMode(
                    inboxLabel.text(),
                    selectedCount = 42,
                    composerDisabled = true
                ),
                operation = MailboxViewAction.ExitSelectionMode,
                expectedState = MailboxTopAppBarState.Data.DefaultMode(inboxLabel.text(), composerDisabled = true)
            ),
            TestInput(
                currentState = MailboxTopAppBarState.Data.SelectionMode(
                    inboxLabel.text(),
                    selectedCount = 42,
                    composerDisabled = false
                ),
                operation = MailboxViewAction.ExitSelectionMode,
                expectedState = MailboxTopAppBarState.Data.DefaultMode(inboxLabel.text(), composerDisabled = false)
            ),
            TestInput(
                currentState = MailboxTopAppBarState.Data.SelectionMode(
                    inboxLabel.text(),
                    selectedCount = 42,
                    composerDisabled = true
                ),
                operation = MailboxEvent.NewLabelSelected(trashLabel, selectedLabelCount = 42),
                expectedState = MailboxTopAppBarState.Data.SelectionMode(
                    trashLabel.text(),
                    selectedCount = 42,
                    composerDisabled = true
                )
            ),
            TestInput(
                currentState = MailboxTopAppBarState.Data.SelectionMode(
                    inboxLabel.text(),
                    selectedCount = 42,
                    composerDisabled = false
                ),
                operation = MailboxEvent.NewLabelSelected(trashLabel, selectedLabelCount = 42),
                expectedState = MailboxTopAppBarState.Data.SelectionMode(
                    trashLabel.text(),
                    selectedCount = 42,
                    composerDisabled = false
                )
            ),
            TestInput(
                currentState = MailboxTopAppBarState.Data.SelectionMode(
                    inboxLabel.text(),
                    selectedCount = 42,
                    composerDisabled = true
                ),
                operation = MailboxEvent.SelectedLabelChanged(trashLabel),
                expectedState = MailboxTopAppBarState.Data.SelectionMode(
                    trashLabel.text(),
                    selectedCount = 42,
                    composerDisabled = true
                )
            ),
            TestInput(
                currentState = MailboxTopAppBarState.Data.SelectionMode(
                    inboxLabel.text(),
                    selectedCount = 42,
                    composerDisabled = false
                ),
                operation = MailboxEvent.SelectedLabelChanged(trashLabel),
                expectedState = MailboxTopAppBarState.Data.SelectionMode(
                    trashLabel.text(),
                    selectedCount = 42,
                    composerDisabled = false
                )
            ),
            TestInput(
                currentState = MailboxTopAppBarState.Data.SelectionMode(
                    inboxLabel.text(),
                    selectedCount = 42,
                    composerDisabled = true
                ),
                operation = MailboxEvent.ComposerDisabledChanged(composerDisabled = false),
                expectedState = MailboxTopAppBarState.Data.SelectionMode(
                    inboxLabel.text(),
                    selectedCount = 42,
                    composerDisabled = false
                )
            ),
            TestInput(
                currentState = MailboxTopAppBarState.Data.SelectionMode(
                    inboxLabel.text(),
                    selectedCount = 42,
                    composerDisabled = false
                ),
                operation = MailboxEvent.ComposerDisabledChanged(composerDisabled = true),
                expectedState = MailboxTopAppBarState.Data.SelectionMode(
                    inboxLabel.text(),
                    selectedCount = 42,
                    composerDisabled = true
                )
            )
        )

        private val transitionsFromSearchModeState = listOf(
            TestInput(
                currentState = MailboxTopAppBarState.Data.SearchMode(
                    inboxLabel.text(),
                    searchQuery = EMPTY_STRING,
                    composerDisabled = true
                ),
                operation = MailboxViewAction.EnterSelectionMode,
                expectedState = MailboxTopAppBarState.Data.SelectionMode(
                    inboxLabel.text(),
                    selectedCount = 0,
                    composerDisabled = true
                )
            ),
            TestInput(
                currentState = MailboxTopAppBarState.Data.SearchMode(
                    inboxLabel.text(),
                    searchQuery = EMPTY_STRING,
                    composerDisabled = false
                ),
                operation = MailboxViewAction.EnterSelectionMode,
                expectedState = MailboxTopAppBarState.Data.SelectionMode(
                    inboxLabel.text(),
                    selectedCount = 0,
                    composerDisabled = false
                )
            ),
            TestInput(
                currentState = MailboxTopAppBarState.Data.SearchMode(
                    inboxLabel.text(),
                    searchQuery = EMPTY_STRING,
                    composerDisabled = true
                ),
                operation = MailboxViewAction.ExitSelectionMode,
                expectedState = MailboxTopAppBarState.Data.DefaultMode(inboxLabel.text(), composerDisabled = true)
            ),
            TestInput(
                currentState = MailboxTopAppBarState.Data.SearchMode(
                    inboxLabel.text(),
                    searchQuery = EMPTY_STRING,
                    composerDisabled = false
                ),
                operation = MailboxViewAction.ExitSelectionMode,
                expectedState = MailboxTopAppBarState.Data.DefaultMode(inboxLabel.text(), composerDisabled = false)
            ),
            TestInput(
                currentState = MailboxTopAppBarState.Data.SearchMode(
                    inboxLabel.text(),
                    searchQuery = EMPTY_STRING,
                    composerDisabled = true
                ),
                operation = MailboxEvent.NewLabelSelected(trashLabel, selectedLabelCount = 42),
                expectedState = MailboxTopAppBarState.Data.SearchMode(
                    trashLabel.text(),
                    searchQuery = EMPTY_STRING,
                    composerDisabled = true
                )
            ),
            TestInput(
                currentState = MailboxTopAppBarState.Data.SearchMode(
                    inboxLabel.text(),
                    searchQuery = EMPTY_STRING,
                    composerDisabled = false
                ),
                operation = MailboxEvent.NewLabelSelected(trashLabel, selectedLabelCount = 42),
                expectedState = MailboxTopAppBarState.Data.SearchMode(
                    trashLabel.text(),
                    searchQuery = EMPTY_STRING,
                    composerDisabled = false
                )
            ),
            TestInput(
                currentState = MailboxTopAppBarState.Data.SearchMode(
                    inboxLabel.text(),
                    searchQuery = EMPTY_STRING,
                    composerDisabled = true
                ),
                operation = MailboxEvent.SelectedLabelChanged(trashLabel),
                expectedState = MailboxTopAppBarState.Data.SearchMode(
                    trashLabel.text(),
                    searchQuery = EMPTY_STRING,
                    composerDisabled = true
                )
            ),
            TestInput(
                currentState = MailboxTopAppBarState.Data.SearchMode(
                    inboxLabel.text(),
                    searchQuery = EMPTY_STRING,
                    composerDisabled = false
                ),
                operation = MailboxEvent.SelectedLabelChanged(trashLabel),
                expectedState = MailboxTopAppBarState.Data.SearchMode(
                    trashLabel.text(),
                    searchQuery = EMPTY_STRING,
                    composerDisabled = false
                )
            ),
            TestInput(
                currentState = MailboxTopAppBarState.Data.SearchMode(
                    inboxLabel.text(),
                    searchQuery = EMPTY_STRING,
                    composerDisabled = true
                ),
                operation = MailboxEvent.ComposerDisabledChanged(composerDisabled = false),
                expectedState = MailboxTopAppBarState.Data.SearchMode(
                    inboxLabel.text(),
                    searchQuery = EMPTY_STRING,
                    composerDisabled = false
                )
            ),
            TestInput(
                currentState = MailboxTopAppBarState.Data.SearchMode(
                    inboxLabel.text(),
                    searchQuery = EMPTY_STRING,
                    composerDisabled = false
                ),
                operation = MailboxEvent.ComposerDisabledChanged(composerDisabled = true),
                expectedState = MailboxTopAppBarState.Data.SearchMode(
                    inboxLabel.text(),
                    searchQuery = EMPTY_STRING,
                    composerDisabled = true
                )
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
