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
import ch.protonmail.android.mailcommon.presentation.model.BottomBarEvent
import ch.protonmail.android.mailcommon.presentation.model.BottomBarState
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

        private val transitionsFromLoadingState = listOf(
            TestInput(
                currentState = BottomBarState.Loading,
                operation = BottomBarEvent.ActionsData(actions),
                expectedState = BottomBarState.Data.Hidden(actions)
            ),
            TestInput(
                currentState = BottomBarState.Loading,
                operation = BottomBarEvent.ShowAndUpdateActionsData(actions),
                expectedState = BottomBarState.Data.Shown(actions)
            ),
            TestInput(
                currentState = BottomBarState.Loading,
                operation = BottomBarEvent.ErrorLoadingActions,
                expectedState = BottomBarState.Error.FailedLoadingActions
            )
        )

        private val transitionsFromDataState = listOf(
            TestInput(
                currentState = BottomBarState.Data.Hidden(actions),
                operation = BottomBarEvent.ActionsData(updatedActions),
                expectedState = BottomBarState.Data.Hidden(updatedActions)
            ),
            TestInput(
                currentState = BottomBarState.Data.Shown(actions),
                operation = BottomBarEvent.ActionsData(updatedActions),
                expectedState = BottomBarState.Data.Shown(updatedActions)
            ),
            TestInput(
                currentState = BottomBarState.Data.Hidden(actions),
                operation = BottomBarEvent.ErrorLoadingActions,
                expectedState = BottomBarState.Data.Hidden(actions)
            ),
            TestInput(
                currentState = BottomBarState.Data.Shown(actions),
                operation = BottomBarEvent.ErrorLoadingActions,
                expectedState = BottomBarState.Data.Shown(actions)
            ),
            TestInput(
                currentState = BottomBarState.Data.Hidden(actions),
                operation = BottomBarEvent.ShowBottomSheet,
                expectedState = BottomBarState.Data.Shown(actions)
            ),
            TestInput(
                currentState = BottomBarState.Data.Shown(actions),
                operation = BottomBarEvent.ShowBottomSheet,
                expectedState = BottomBarState.Data.Shown(actions)
            ),
            TestInput(
                currentState = BottomBarState.Data.Shown(actions),
                operation = BottomBarEvent.HideBottomSheet,
                expectedState = BottomBarState.Data.Hidden(actions)
            ),
            TestInput(
                currentState = BottomBarState.Data.Hidden(actions),
                operation = BottomBarEvent.HideBottomSheet,
                expectedState = BottomBarState.Data.Hidden(actions)
            )
        )

        private val transitionsFromErrorState = listOf(
            TestInput(
                currentState = BottomBarState.Error.FailedLoadingActions,
                operation = BottomBarEvent.ActionsData(actions),
                expectedState = BottomBarState.Data.Hidden(actions)
            ),
            TestInput(
                currentState = BottomBarState.Error.FailedLoadingActions,
                operation = BottomBarEvent.ShowAndUpdateActionsData(actions),
                expectedState = BottomBarState.Data.Shown(actions)
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
