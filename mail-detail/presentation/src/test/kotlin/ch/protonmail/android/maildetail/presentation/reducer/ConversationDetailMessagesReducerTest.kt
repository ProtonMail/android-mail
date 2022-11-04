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

import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.maildetail.presentation.R.string
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailEvent
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailOperation
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailsMessagesState
import ch.protonmail.android.maildetail.presentation.sample.ConversationDetailMessageUiModelSample
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.Test
import kotlin.test.assertEquals
import ch.protonmail.android.mailcommon.presentation.R.string as commonString

@RunWith(Parameterized::class)
class ConversationDetailMessagesReducerTest(
    private val testName: String,
    private val input: Input
) {

    @Test
    fun test() {
        val reducer = ConversationDetailMessagesReducer()
        val result = reducer.newStateFrom(input.currentState, input.operation)
        assertEquals(input.expectedState, result)
    }

    data class Input(
        val currentState: ConversationDetailsMessagesState,
        val operation: ConversationDetailOperation.AffectingMessages,
        val expectedState: ConversationDetailsMessagesState
    )

    private companion object {

        private val allMessages = listOf(
            ConversationDetailMessageUiModelSample.AugWeatherForecast,
            ConversationDetailMessageUiModelSample.SepWeatherForecast
        )

        private val fromLoadingState = listOf(

            Input(
                currentState = ConversationDetailsMessagesState.Loading,
                operation = ConversationDetailEvent.ErrorLoadingMessages,
                expectedState = ConversationDetailsMessagesState.Error(
                    message = TextUiModel(string.detail_error_loading_messages)
                )
            ),

            Input(
                currentState = ConversationDetailsMessagesState.Loading,
                operation = ConversationDetailEvent.NoPrimaryUser,
                expectedState = ConversationDetailsMessagesState.Error(
                    message = TextUiModel(commonString.x_error_not_logged_in)
                )
            ),

            Input(
                currentState = ConversationDetailsMessagesState.Loading,
                operation = ConversationDetailEvent.MessagesData(messagesUiModels = allMessages),
                expectedState = ConversationDetailsMessagesState.Data(messages = allMessages)
            )
        )

        private val fromErrorState = listOf(

            Input(
                currentState = ConversationDetailsMessagesState.Error(
                    message = TextUiModel(commonString.x_error_not_logged_in)
                ),
                operation = ConversationDetailEvent.ErrorLoadingMessages,
                expectedState = ConversationDetailsMessagesState.Error(
                    message = TextUiModel(string.detail_error_loading_messages)
                )
            ),

            Input(
                currentState = ConversationDetailsMessagesState.Error(
                    message = TextUiModel(string.detail_error_loading_messages)
                ),
                operation = ConversationDetailEvent.NoPrimaryUser,
                expectedState = ConversationDetailsMessagesState.Error(
                    message = TextUiModel(commonString.x_error_not_logged_in)
                )
            ),

            Input(
                currentState = ConversationDetailsMessagesState.Error(
                    message = TextUiModel(commonString.x_error_not_logged_in)
                ),
                operation = ConversationDetailEvent.MessagesData(messagesUiModels = allMessages),
                expectedState = ConversationDetailsMessagesState.Data(messages = allMessages)
            )
        )

        private val fromSuccessState = listOf(

            Input(
                currentState = ConversationDetailsMessagesState.Data(
                    messages = emptyList()
                ),
                operation = ConversationDetailEvent.ErrorLoadingMessages,
                expectedState = ConversationDetailsMessagesState.Error(
                    message = TextUiModel(string.detail_error_loading_messages)
                )
            ),

            Input(
                currentState = ConversationDetailsMessagesState.Data(messages = allMessages),
                operation = ConversationDetailEvent.NoPrimaryUser,
                expectedState = ConversationDetailsMessagesState.Error(
                    message = TextUiModel(commonString.x_error_not_logged_in)
                )
            ),

            Input(
                currentState = ConversationDetailsMessagesState.Data(messages = emptyList()),
                operation = ConversationDetailEvent.MessagesData(messagesUiModels = allMessages),
                expectedState = ConversationDetailsMessagesState.Data(messages = allMessages)
            )
        )

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = (fromLoadingState + fromErrorState + fromSuccessState)
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
