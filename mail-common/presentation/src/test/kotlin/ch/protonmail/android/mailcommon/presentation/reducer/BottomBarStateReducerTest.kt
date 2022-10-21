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

import ch.protonmail.android.mailcommon.presentation.model.BottomBarEvent
import ch.protonmail.android.mailcommon.presentation.model.BottomBarState
import ch.protonmail.android.testdata.action.ActionUiModelTestData
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
internal class BottomBarStateReducerTest(
    @Suppress("UNUSED_PARAMETER") private val testName: String,
    private val testInput: TestParams.TestInput
) {

    private val reducer = BottomBarStateReducer()

    @Test
    fun `should produce the expected new state`() {
        val actualState = reducer.reduce(testInput.currentState, testInput.event)

        assertEquals(testInput.expectedState, actualState)
    }

    companion object {

        private val actions = listOf(ActionUiModelTestData.markUnread)

        private val transitionsFromRelevantEvents = listOf(
            TestParams(
                "from loading to actions data",
                TestParams.TestInput(
                    currentState = BottomBarState.Loading,
                    event = BottomBarEvent.ActionsData(actions),
                    expectedState = BottomBarState.Data(actions)
                )
            ),
            TestParams(
                "from loading to failed loading actions data",
                TestParams.TestInput(
                    currentState = BottomBarState.Loading,
                    event = BottomBarEvent.ErrorLoadingActions,
                    expectedState = BottomBarState.Error.FailedLoadingActions
                )
            )
        )

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = transitionsFromRelevantEvents
            .map { arrayOf(it.testName, it.testInput) }
    }

    data class TestParams(
        val testName: String,
        val testInput: TestInput
    ) {

        data class TestInput(
            val currentState: BottomBarState,
            val event: BottomBarEvent,
            val expectedState: BottomBarState
        )
    }

}
