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

import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxEvent
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxOperation
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxViewAction
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.UnreadFilterState
import ch.protonmail.android.testdata.label.LabelTestData
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
internal class MailboxUnreadFilterReducerTest(
    private val testName: String,
    private val testInput: TestInput
) {

    private val unreadFilterReducer = MailboxUnreadFilterReducer()

    @Test
    fun `should produce the expected new state`() = with(testInput) {
        val actualState = unreadFilterReducer.newStateFrom(currentState, operation)

        assertEquals(expectedState, actualState, testName)
    }

    companion object {

        private const val DEFAULT_UNREAD_COUNT = 0
        private const val INITIAL_UNREAD_COUNT = 42
        private const val UPDATED_UNREAD_COUNT = 24

        private val transitionsFromLoadingState = listOf(
            TestInput(
                currentState = UnreadFilterState.Loading,
                operation = MailboxEvent.SelectedLabelCountChanged(INITIAL_UNREAD_COUNT),
                expectedState = UnreadFilterState.Data(INITIAL_UNREAD_COUNT, isFilterEnabled = false)
            ),
            TestInput(
                currentState = UnreadFilterState.Loading,
                operation = MailboxEvent.NewLabelSelected(LabelTestData.systemLabels[0], INITIAL_UNREAD_COUNT),
                expectedState = UnreadFilterState.Data(INITIAL_UNREAD_COUNT, isFilterEnabled = false)
            ),
            TestInput(
                currentState = UnreadFilterState.Loading,
                operation = MailboxEvent.NewLabelSelected(LabelTestData.systemLabels[0], selectedLabelCount = null),
                expectedState = UnreadFilterState.Data(DEFAULT_UNREAD_COUNT, isFilterEnabled = false)
            ),
            TestInput(
                currentState = UnreadFilterState.Loading,
                operation = MailboxViewAction.DisableUnreadFilter,
                expectedState = UnreadFilterState.Loading
            ),
            TestInput(
                currentState = UnreadFilterState.Loading,
                operation = MailboxViewAction.EnableUnreadFilter,
                expectedState = UnreadFilterState.Loading
            )
        )

        private val transitionsFromDataState = listOf(
            TestInput(
                currentState = UnreadFilterState.Data(INITIAL_UNREAD_COUNT, isFilterEnabled = false),
                operation = MailboxEvent.SelectedLabelCountChanged(UPDATED_UNREAD_COUNT),
                expectedState = UnreadFilterState.Data(UPDATED_UNREAD_COUNT, isFilterEnabled = false)
            ),
            TestInput(
                currentState = UnreadFilterState.Data(INITIAL_UNREAD_COUNT, isFilterEnabled = true),
                operation = MailboxEvent.SelectedLabelCountChanged(UPDATED_UNREAD_COUNT),
                expectedState = UnreadFilterState.Data(UPDATED_UNREAD_COUNT, isFilterEnabled = true)
            ),
            TestInput(
                currentState = UnreadFilterState.Data(INITIAL_UNREAD_COUNT, isFilterEnabled = false),
                operation = MailboxEvent.NewLabelSelected(LabelTestData.systemLabels[0], UPDATED_UNREAD_COUNT),
                expectedState = UnreadFilterState.Data(UPDATED_UNREAD_COUNT, isFilterEnabled = false)
            ),
            TestInput(
                currentState = UnreadFilterState.Data(INITIAL_UNREAD_COUNT, isFilterEnabled = true),
                operation = MailboxEvent.NewLabelSelected(LabelTestData.systemLabels[0], UPDATED_UNREAD_COUNT),
                expectedState = UnreadFilterState.Data(UPDATED_UNREAD_COUNT, isFilterEnabled = true)
            ),
            TestInput(
                currentState = UnreadFilterState.Data(INITIAL_UNREAD_COUNT, isFilterEnabled = false),
                operation = MailboxEvent.NewLabelSelected(LabelTestData.systemLabels[0], selectedLabelCount = null),
                expectedState = UnreadFilterState.Data(DEFAULT_UNREAD_COUNT, isFilterEnabled = false)
            ),
            TestInput(
                currentState = UnreadFilterState.Data(INITIAL_UNREAD_COUNT, isFilterEnabled = true),
                operation = MailboxEvent.NewLabelSelected(LabelTestData.systemLabels[0], selectedLabelCount = null),
                expectedState = UnreadFilterState.Data(DEFAULT_UNREAD_COUNT, isFilterEnabled = true)
            ),
            TestInput(
                currentState = UnreadFilterState.Data(INITIAL_UNREAD_COUNT, isFilterEnabled = false),
                operation = MailboxViewAction.EnableUnreadFilter,
                expectedState = UnreadFilterState.Data(INITIAL_UNREAD_COUNT, isFilterEnabled = true)
            ),
            TestInput(
                currentState = UnreadFilterState.Data(INITIAL_UNREAD_COUNT, isFilterEnabled = true),
                operation = MailboxViewAction.EnableUnreadFilter,
                expectedState = UnreadFilterState.Data(INITIAL_UNREAD_COUNT, isFilterEnabled = true)
            ),
            TestInput(
                currentState = UnreadFilterState.Data(INITIAL_UNREAD_COUNT, isFilterEnabled = false),
                operation = MailboxViewAction.DisableUnreadFilter,
                expectedState = UnreadFilterState.Data(INITIAL_UNREAD_COUNT, isFilterEnabled = false)
            ),
            TestInput(
                currentState = UnreadFilterState.Data(INITIAL_UNREAD_COUNT, isFilterEnabled = true),
                operation = MailboxViewAction.DisableUnreadFilter,
                expectedState = UnreadFilterState.Data(INITIAL_UNREAD_COUNT, isFilterEnabled = false)
            )
        )

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data(): Collection<Array<Any>> = (transitionsFromLoadingState + transitionsFromDataState)
            .map { testInput ->
                val testName = """
                    Current state: ${testInput.currentState}
                    Operation: ${testInput.operation}
                    Next state: ${testInput.expectedState}    
                """.trimIndent()
                arrayOf(testName, testInput)
            }
    }

    data class TestInput(
        val currentState: UnreadFilterState,
        val operation: MailboxOperation.AffectingUnreadFilter,
        val expectedState: UnreadFilterState
    )
}
