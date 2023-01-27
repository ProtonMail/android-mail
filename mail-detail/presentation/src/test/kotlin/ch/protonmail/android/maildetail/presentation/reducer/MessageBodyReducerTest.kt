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

import ch.protonmail.android.maildetail.presentation.model.MessageBodyState
import ch.protonmail.android.maildetail.presentation.model.MessageDetailEvent
import ch.protonmail.android.maildetail.presentation.model.MessageDetailOperation
import ch.protonmail.android.maildetail.presentation.model.MessageViewAction
import ch.protonmail.android.testdata.message.MessageBodyUiModelTestData
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
class MessageBodyReducerTest(
    private val testName: String,
    private val testInput: TestInput
) {

    private val messageBodyReducer = MessageBodyReducer()

    @Test
    fun `should produce the expected new state`() = with(testInput) {
        // When
        val newState = messageBodyReducer.newStateFrom(operation)

        // Then
        assertEquals(expectedState, newState, testName)
    }

    companion object {
        private val actions = listOf(
            TestInput(
                MessageViewAction.Reload,
                MessageBodyState.Loading
            )
        )
        private val events = listOf(
            TestInput(
                MessageDetailEvent.MessageBodyEvent(
                    messageBody = MessageBodyUiModelTestData.messageBodyUiModel
                ),
                MessageBodyState.Data(MessageBodyUiModelTestData.messageBodyUiModel)
            ),
            TestInput(
                MessageDetailEvent.ErrorGettingMessageBody(isNetworkError = true),
                MessageBodyState.Error.Data(true)
            ),
            TestInput(
                MessageDetailEvent.ErrorGettingMessageBody(isNetworkError = false),
                MessageBodyState.Error.Data(false)
            ),
            TestInput(
                MessageDetailEvent.ErrorDecryptingMessageBody(MessageBodyUiModelTestData.messageBodyUiModel),
                MessageBodyState.Error.Decryption(MessageBodyUiModelTestData.messageBodyUiModel)
            )
        )

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data(): Collection<Array<Any>> {
            return (actions + events)
                .map { testInput ->
                    val testName = """
                        Operation: ${testInput.operation}
                        
                    """.trimIndent()
                    arrayOf(testName, testInput)
                }
        }
    }

    data class TestInput(
        val operation: MessageDetailOperation.AffectingMessageBody,
        val expectedState: MessageBodyState
    )
}
