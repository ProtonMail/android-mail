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
import ch.protonmail.android.mailmailbox.presentation.mailbox.AffectingTopAppBar
import ch.protonmail.android.mailmailbox.presentation.mailbox.Event
import ch.protonmail.android.mailmailbox.presentation.mailbox.ViewAction
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxTopAppBarState
import me.proton.core.util.kotlin.EMPTY_STRING
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
internal class MailboxTopAppBarReducerTest(
    private val testInput: TestInput
) {

    private val topAppBarReducer = MailboxTopAppBarReducer()

    @Test
    fun `should produce the expected new state`() {
        val actualState = topAppBarReducer.newStateFrom(testInput.currentState, testInput.operation)

        assertEquals(testInput.expectedState, actualState)
    }

    companion object {

        private val inboxLabel = MailLabel.System(MailLabelId.System.Inbox)
        private val trashLabel = MailLabel.System(MailLabelId.System.Trash)

        private val transitionsFromLoadingState = listOf(
            TestInput(
                currentState = MailboxTopAppBarState.Loading,
                operation = ViewAction.EnterSelectionMode,
                expectedState = MailboxTopAppBarState.Loading
            ).toArray(),
            TestInput(
                currentState = MailboxTopAppBarState.Loading,
                operation = ViewAction.ExitSelectionMode,
                expectedState = MailboxTopAppBarState.Loading
            ).toArray(),
            TestInput(
                currentState = MailboxTopAppBarState.Loading,
                operation = Event.LabelSelected(inboxLabel, currentLabelCount = 42),
                expectedState = MailboxTopAppBarState.Data.DefaultMode(inboxLabel.text())
            ).toArray(),
            TestInput(
                currentState = MailboxTopAppBarState.Loading,
                operation = Event.SelectedLabelChanged(inboxLabel),
                expectedState = MailboxTopAppBarState.Data.DefaultMode(inboxLabel.text())
            ).toArray()
        )

        private val transitionsFromDefaultModeState = listOf(
            TestInput(
                currentState = MailboxTopAppBarState.Data.DefaultMode(inboxLabel.text()),
                operation = ViewAction.EnterSelectionMode,
                expectedState = MailboxTopAppBarState.Data.SelectionMode(
                    inboxLabel.text(),
                    selectedCount = 0
                )
            ).toArray(),
            TestInput(
                currentState = MailboxTopAppBarState.Data.DefaultMode(inboxLabel.text()),
                operation = ViewAction.ExitSelectionMode,
                expectedState = MailboxTopAppBarState.Data.DefaultMode(inboxLabel.text())
            ).toArray(),
            TestInput(
                currentState = MailboxTopAppBarState.Data.DefaultMode(inboxLabel.text()),
                operation = Event.LabelSelected(trashLabel, currentLabelCount = 42),
                expectedState = MailboxTopAppBarState.Data.DefaultMode(trashLabel.text())
            ).toArray(),
            TestInput(
                currentState = MailboxTopAppBarState.Data.DefaultMode(inboxLabel.text()),
                operation = Event.SelectedLabelChanged(trashLabel),
                expectedState = MailboxTopAppBarState.Data.DefaultMode(trashLabel.text())
            ).toArray()
        )

        private val transitionsFromSelectionModeState = listOf(
            TestInput(
                currentState = MailboxTopAppBarState.Data.SelectionMode(inboxLabel.text(), selectedCount = 42),
                operation = ViewAction.EnterSelectionMode,
                expectedState = MailboxTopAppBarState.Data.SelectionMode(
                    inboxLabel.text(),
                    selectedCount = 0
                )
            ).toArray(),
            TestInput(
                currentState = MailboxTopAppBarState.Data.SelectionMode(inboxLabel.text(), selectedCount = 42),
                operation = ViewAction.ExitSelectionMode,
                expectedState = MailboxTopAppBarState.Data.DefaultMode(inboxLabel.text())
            ).toArray(),
            TestInput(
                currentState = MailboxTopAppBarState.Data.SelectionMode(inboxLabel.text(), selectedCount = 42),
                operation = Event.LabelSelected(trashLabel, currentLabelCount = 42),
                expectedState = MailboxTopAppBarState.Data.SelectionMode(trashLabel.text(), selectedCount = 42)
            ).toArray(),
            TestInput(
                currentState = MailboxTopAppBarState.Data.SelectionMode(inboxLabel.text(), selectedCount = 42),
                operation = Event.SelectedLabelChanged(trashLabel),
                expectedState = MailboxTopAppBarState.Data.SelectionMode(trashLabel.text(), selectedCount = 42)
            ).toArray()
        )

        private val transitionsFromSearchModeState = listOf(
            TestInput(
                currentState = MailboxTopAppBarState.Data.SearchMode(inboxLabel.text(), searchQuery = EMPTY_STRING),
                operation = ViewAction.EnterSelectionMode,
                expectedState = MailboxTopAppBarState.Data.SelectionMode(
                    inboxLabel.text(),
                    selectedCount = 0
                )
            ).toArray(),
            TestInput(
                currentState = MailboxTopAppBarState.Data.SearchMode(inboxLabel.text(), searchQuery = EMPTY_STRING),
                operation = ViewAction.ExitSelectionMode,
                expectedState = MailboxTopAppBarState.Data.DefaultMode(inboxLabel.text())
            ).toArray(),
            TestInput(
                currentState = MailboxTopAppBarState.Data.SearchMode(inboxLabel.text(), searchQuery = EMPTY_STRING),
                operation = Event.LabelSelected(trashLabel, currentLabelCount = 42),
                expectedState = MailboxTopAppBarState.Data.SearchMode(trashLabel.text(), searchQuery = EMPTY_STRING)
            ).toArray(),
            TestInput(
                currentState = MailboxTopAppBarState.Data.SearchMode(inboxLabel.text(), searchQuery = EMPTY_STRING),
                operation = Event.SelectedLabelChanged(trashLabel),
                expectedState = MailboxTopAppBarState.Data.SearchMode(trashLabel.text(), searchQuery = EMPTY_STRING)
            ).toArray()
        )

        @JvmStatic
        @Parameterized.Parameters
        fun data(): Collection<Array<TestInput>> {
            return transitionsFromLoadingState +
                transitionsFromDefaultModeState +
                transitionsFromSelectionModeState +
                transitionsFromSearchModeState
        }
    }

    class TestInput(
        val currentState: MailboxTopAppBarState,
        val operation: AffectingTopAppBar,
        val expectedState: MailboxTopAppBarState
    ) {
        fun toArray() = arrayOf(this)
    }
}
