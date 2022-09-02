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

package ch.protonmail.android.maildetail.presentation.message

import ch.protonmail.android.maildetail.presentation.message.model.MessageDetailEvent
import ch.protonmail.android.maildetail.presentation.message.model.MessageDetailState
import ch.protonmail.android.testdata.message.MessageUiModelTestData
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
class MessageDetailReducerTest(
    private val testInput: TestInput
) {

    private val detailReducer = MessageDetailReducer()

    @Test
    fun `should produce the expected new state`() {
        val actualState = detailReducer.reduce(testInput.currentState, testInput.event)

        assertEquals(testInput.expectedState, actualState)
    }

    companion object {

        private val messageUiModel = MessageUiModelTestData.buildMessageUiModel(
            "messageId",
            "This email is about subjects"
        )

        private val transitionsFromLoadingState = listOf(
            TestInput(
                currentState = MessageDetailState.Loading,
                event = MessageDetailEvent.NoMessageIdProvided,
                expectedState = MessageDetailState.Error.NoMessageIdProvided
            ).toArray(),
            TestInput(
                currentState = MessageDetailState.Loading,
                event = MessageDetailEvent.NoPrimaryUser,
                expectedState = MessageDetailState.Error.NotLoggedIn
            ).toArray(),
            TestInput(
                currentState = MessageDetailState.Loading,
                event = MessageDetailEvent.MessageMetadata(messageUiModel),
                expectedState = MessageDetailState.Data(messageUiModel)
            ).toArray(),
        )

        @JvmStatic
        @Parameterized.Parameters
        fun data(): Collection<Array<TestInput>> {
            return transitionsFromLoadingState
        }
    }

    class TestInput(
        val currentState: MessageDetailState,
        val event: MessageDetailEvent,
        val expectedState: MessageDetailState
    ) {

        fun toArray() = arrayOf(this)
    }
}
