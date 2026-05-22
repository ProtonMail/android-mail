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

package ch.protonmail.android.mailcommon.presentation.reducer

import arrow.core.nonEmptyListOf
import ch.protonmail.android.mailcommon.presentation.model.ActionUiModel
import ch.protonmail.android.mailcommon.presentation.model.BottomBarEvent
import ch.protonmail.android.mailcommon.presentation.model.BottomBarState
import ch.protonmail.android.mailcommon.presentation.model.BottomBarTarget
import ch.protonmail.android.testdata.action.ActionUiModelTestData
import kotlinx.collections.immutable.toImmutableList
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
internal class BottomBarReducerTest(
    private val testName: String,
    private val testInput: TestInput
) {

    private val reducer = BottomBarReducer()

    @Test
    fun `should produce the expected new state`() = with(testInput) {
        val actualState = reducer.newStateFrom(currentState, operation)

        assertEquals(expectedState, actualState, testName)
    }

    companion object {

        private val actions = nonEmptyListOf(ActionUiModelTestData.markUnread).toImmutableList()
        private val updatedActions = listOf(ActionUiModelTestData.archive).toImmutableList()
        private val emptyActions = emptyList<ActionUiModel>().toImmutableList()
        private val mailboxTarget = BottomBarTarget.Mailbox
        private val convoTarget = BottomBarTarget.Conversation
        private val messageTarget = BottomBarTarget.Message(id = "messageId")


        private val transitionsFromLoadingState = listOf(
            TestInput(
                currentState = BottomBarState.Loading,
                operation = BottomBarEvent.ActionsData(mailboxTarget, actions),
                expectedState = BottomBarState.Data.Hidden(mailboxTarget, actions)
            ),
            TestInput(
                currentState = BottomBarState.Loading,
                operation = BottomBarEvent.ShowAndUpdateActionsData(convoTarget, actions),
                expectedState = BottomBarState.Data.Shown(convoTarget, actions)
            ),
            TestInput(
                currentState = BottomBarState.Loading,
                operation = BottomBarEvent.ErrorLoadingActions,
                expectedState = BottomBarState.Error.FailedLoadingActions
            ),
            TestInput(
                currentState = BottomBarState.Loading,
                operation = BottomBarEvent.Offline,
                expectedState = BottomBarState.Offline
            ),
            TestInput(
                currentState = BottomBarState.Loading,
                operation = BottomBarEvent.ShowAndUpdateActionsData(convoTarget, emptyActions),
                expectedState = BottomBarState.Data.Hidden(convoTarget, emptyActions)
            )
        )

        private val transitionsFromDataState = listOf(
            TestInput(
                currentState = BottomBarState.Data.Hidden(mailboxTarget, actions),
                operation = BottomBarEvent.ActionsData(mailboxTarget, updatedActions),
                expectedState = BottomBarState.Data.Hidden(mailboxTarget, updatedActions)
            ),
            TestInput(
                currentState = BottomBarState.Data.Shown(mailboxTarget, actions),
                operation = BottomBarEvent.ActionsData(mailboxTarget, updatedActions),
                expectedState = BottomBarState.Data.Shown(mailboxTarget, updatedActions)
            ),
            TestInput(
                currentState = BottomBarState.Data.Hidden(messageTarget, actions),
                operation = BottomBarEvent.ErrorLoadingActions,
                expectedState = BottomBarState.Data.Hidden(messageTarget, actions)
            ),
            TestInput(
                currentState = BottomBarState.Data.Shown(messageTarget, actions),
                operation = BottomBarEvent.ErrorLoadingActions,
                expectedState = BottomBarState.Data.Shown(messageTarget, actions)
            ),
            TestInput(
                currentState = BottomBarState.Data.Hidden(convoTarget, actions),
                operation = BottomBarEvent.ShowBottomSheet,
                expectedState = BottomBarState.Data.Shown(convoTarget, actions)
            ),
            TestInput(
                currentState = BottomBarState.Data.Shown(convoTarget, actions),
                operation = BottomBarEvent.ShowBottomSheet,
                expectedState = BottomBarState.Data.Shown(convoTarget, actions)
            ),
            TestInput(
                currentState = BottomBarState.Data.Shown(messageTarget, actions),
                operation = BottomBarEvent.HideBottomSheet,
                expectedState = BottomBarState.Data.Hidden(messageTarget, actions)
            ),
            TestInput(
                currentState = BottomBarState.Data.Hidden(messageTarget, actions),
                operation = BottomBarEvent.HideBottomSheet,
                expectedState = BottomBarState.Data.Hidden(messageTarget, actions)
            ),
            TestInput(
                currentState = BottomBarState.Data.Hidden(messageTarget, actions),
                operation = BottomBarEvent.Offline,
                expectedState = BottomBarState.Data.Hidden(messageTarget, actions)
            )
        )

        private val transitionsFromErrorState = listOf(
            TestInput(
                currentState = BottomBarState.Error.FailedLoadingActions,
                operation = BottomBarEvent.ActionsData(mailboxTarget, actions),
                expectedState = BottomBarState.Data.Hidden(mailboxTarget, actions)
            ),
            TestInput(
                currentState = BottomBarState.Error.FailedLoadingActions,
                operation = BottomBarEvent.ShowAndUpdateActionsData(messageTarget, actions),
                expectedState = BottomBarState.Data.Shown(messageTarget, actions)
            ),
            TestInput(
                currentState = BottomBarState.Error.FailedLoadingActions,
                operation = BottomBarEvent.ErrorLoadingActions,
                expectedState = BottomBarState.Error.FailedLoadingActions
            ),
            TestInput(
                currentState = BottomBarState.Error.FailedLoadingActions,
                operation = BottomBarEvent.ShowBottomSheet,
                expectedState = BottomBarState.Error.FailedLoadingActions
            ),
            TestInput(
                currentState = BottomBarState.Error.FailedLoadingActions,
                operation = BottomBarEvent.HideBottomSheet,
                expectedState = BottomBarState.Error.FailedLoadingActions
            ),
            TestInput(
                currentState = BottomBarState.Error.FailedLoadingActions,
                operation = BottomBarEvent.Offline,
                expectedState = BottomBarState.Error.FailedLoadingActions
            )
        )

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = (transitionsFromLoadingState + transitionsFromDataState + transitionsFromErrorState)
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
        val currentState: BottomBarState,
        val operation: BottomBarEvent,
        val expectedState: BottomBarState
    )

}
