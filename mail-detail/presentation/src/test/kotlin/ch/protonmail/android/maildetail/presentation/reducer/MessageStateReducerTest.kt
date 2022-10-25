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

import ch.protonmail.android.maildetail.presentation.model.Event
import ch.protonmail.android.maildetail.presentation.model.MessageDetailMetadataState
import ch.protonmail.android.maildetail.presentation.model.MessageDetailOperation
import ch.protonmail.android.testdata.message.MessageUiModelTestData
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
class MessageStateReducerTest(
    private val testName: String,
    private val testInput: TestInput
) {

    private val detailReducer = MessageDetailMetadataReducer()

    @Test
    fun `should produce the expected new state`() = with(testInput) {
        val actualState = detailReducer.newStateFrom(currentState, operation)

        assertEquals(expectedState, actualState, testName)
    }

    companion object {

        private val messageUiModel = MessageUiModelTestData.buildMessageUiModel(
            "messageId",
            "This email is about subjects"
        )
        private val updatedMessageUiModel = MessageUiModelTestData.buildMessageUiModel(
            "messageId1",
            "[Re] This email is about subjects"
        )

        private val transitionsFromLoadingState = listOf(
            TestInput(
                currentState = MessageDetailMetadataState.Loading,
                operation = Event.NoPrimaryUser,
                expectedState = MessageDetailMetadataState.Error.NotLoggedIn
            ),
            TestInput(
                currentState = MessageDetailMetadataState.Loading,
                operation = Event.MessageMetadata(messageUiModel),
                expectedState = MessageDetailMetadataState.Data(messageUiModel)
            )
        )

        private val transitionsFromDataState = listOf(
            TestInput(
                currentState = MessageDetailMetadataState.Data(messageUiModel),
                operation = Event.NoPrimaryUser,
                expectedState = MessageDetailMetadataState.Error.NotLoggedIn
            ),
            TestInput(
                currentState = MessageDetailMetadataState.Data(messageUiModel),
                operation = Event.MessageMetadata(updatedMessageUiModel),
                expectedState = MessageDetailMetadataState.Data(updatedMessageUiModel)
            )
        )

        private val transitionsFromErrorState = listOf(
            TestInput(
                currentState = MessageDetailMetadataState.Error.NotLoggedIn,
                operation = Event.NoPrimaryUser,
                expectedState = MessageDetailMetadataState.Error.NotLoggedIn
            ),
            TestInput(
                currentState = MessageDetailMetadataState.Error.NotLoggedIn,
                operation = Event.MessageMetadata(messageUiModel),
                expectedState = MessageDetailMetadataState.Data(messageUiModel)
            )
        )

        @JvmStatic
        @Parameterized.Parameters
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
        val currentState: MessageDetailMetadataState,
        val operation: MessageDetailOperation.AffectingMessage,
        val expectedState: MessageDetailMetadataState
    )
}
