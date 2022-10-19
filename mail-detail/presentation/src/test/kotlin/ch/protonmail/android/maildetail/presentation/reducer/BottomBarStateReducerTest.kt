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

package ch.protonmail.android.maildetail.presentation.reducer

import ch.protonmail.android.maildetail.domain.Action
import ch.protonmail.android.maildetail.presentation.R
import ch.protonmail.android.maildetail.presentation.model.ActionUiModel
import ch.protonmail.android.maildetail.presentation.model.BottomBarState
import ch.protonmail.android.maildetail.presentation.model.MessageDetailEvent
import ch.protonmail.android.mailmessage.domain.entity.MessageWithBody
import ch.protonmail.android.testdata.message.MessageTestData
import ch.protonmail.android.testdata.message.MessageUiModelTestData
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

        private val actions = listOf(ActionUiModel(Action.MarkUnread, R.drawable.ic_proton_envelope_dot))

        private val transitionsFromRelevantEvents = listOf(
            TestParams(
                "from loading to actions data",
                TestParams.TestInput(
                    currentState = BottomBarState.Loading,
                    event = MessageDetailEvent.MessageActionsData(actions),
                    expectedState = BottomBarState.Data(actions)
                )
            ),
            TestParams(
                "from loading to failed loading actions data",
                TestParams.TestInput(
                    currentState = BottomBarState.Loading,
                    event = MessageDetailEvent.ErrorLoadingActions,
                    expectedState = BottomBarState.Error.FailedLoadingActions
                )
            )
        )
        private val noTransitionFromIgnoredEvents = listOf(
            TestParams(
                "current state is unchanged when no primary user",
                TestParams.TestInput(
                    currentState = BottomBarState.Loading,
                    event = MessageDetailEvent.NoPrimaryUser,
                    expectedState = BottomBarState.Loading
                )
            ),
            TestParams(
                "current state is unchanged when message body",
                TestParams.TestInput(
                    currentState = BottomBarState.Data(actions),
                    event = MessageDetailEvent.MessageBody(MessageWithBody(MessageTestData.message, null)),
                    expectedState = BottomBarState.Data(actions)
                )
            ),
            TestParams(
                "current state is unchanged when message metadata",
                TestParams.TestInput(
                    currentState = BottomBarState.Error.FailedLoadingActions,
                    event = MessageDetailEvent.MessageMetadata(MessageUiModelTestData.uiModel),
                    expectedState = BottomBarState.Error.FailedLoadingActions
                )
            ),
            TestParams(
                "current state is unchanged when no primary user",
                TestParams.TestInput(
                    currentState = BottomBarState.Loading,
                    event = MessageDetailEvent.NoPrimaryUser,
                    expectedState = BottomBarState.Loading
                )
            )
        )

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = (transitionsFromRelevantEvents + noTransitionFromIgnoredEvents)
            .map { arrayOf(it.testName, it.testInput) }
    }

    data class TestParams(
        val testName: String,
        val testInput: TestInput
    ) {

        data class TestInput(
            val currentState: BottomBarState,
            val event: MessageDetailEvent,
            val expectedState: BottomBarState
        )
    }

}
