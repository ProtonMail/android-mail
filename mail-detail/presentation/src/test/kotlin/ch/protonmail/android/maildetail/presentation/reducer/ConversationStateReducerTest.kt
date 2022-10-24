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

import ch.protonmail.android.maildetail.presentation.model.ConversationDetailEvent
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailOperation
import ch.protonmail.android.maildetail.presentation.model.ConversationState
import ch.protonmail.android.testdata.conversation.ConversationUiModelTestData
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
class ConversationStateReducerTest(
    private val testName: String,
    private val testInput: TestInput
) {

    private val detailReducer = ConversationStateReducer()

    @Test
    fun `should produce the expected new state`() = with(testInput) {
        val actualState = detailReducer.newStateFrom(currentState, operation)

        assertEquals(expectedState, actualState, testName)
    }

    companion object {

        private val conversationUiModel = ConversationUiModelTestData.buildConversationUiModel(
            "conversationId",
            "This email is about subjects"
        )

        private val transitionsFromLoadingState = listOf(
            TestInput(
                currentState = ConversationState.Loading,
                operation = ConversationDetailEvent.NoPrimaryUser,
                expectedState = ConversationState.Error.NotLoggedIn
            ),
            TestInput(
                currentState = ConversationState.Loading,
                operation = ConversationDetailEvent.ConversationData(conversationUiModel),
                expectedState = ConversationState.Data(conversationUiModel)
            ),
            TestInput(
                currentState = ConversationState.Loading,
                operation = ConversationDetailEvent.ErrorLoadingConversation,
                expectedState = ConversationState.Error.FailedLoadingData
            )
        )

        private val transitionsFromDataState = listOf(
            TestInput(
                currentState = ConversationState.Data(conversationUiModel),
                operation = ConversationDetailEvent.NoPrimaryUser,
                expectedState = ConversationState.Error.NotLoggedIn
            ),
            TestInput(
                currentState = ConversationState.Data(conversationUiModel),
                operation = ConversationDetailEvent.ConversationData(conversationUiModel),
                expectedState = ConversationState.Data(conversationUiModel)
            ),
            TestInput(
                currentState = ConversationState.Data(conversationUiModel),
                operation = ConversationDetailEvent.ErrorLoadingConversation,
                expectedState = ConversationState.Error.FailedLoadingData
            )
        )

        private val transitionsFromErrorState = listOf(
            TestInput(
                currentState = ConversationState.Error.FailedLoadingData,
                operation = ConversationDetailEvent.NoPrimaryUser,
                expectedState = ConversationState.Error.NotLoggedIn
            ),
            TestInput(
                currentState = ConversationState.Error.FailedLoadingData,
                operation = ConversationDetailEvent.ConversationData(conversationUiModel),
                expectedState = ConversationState.Data(conversationUiModel)
            ),
            TestInput(
                currentState = ConversationState.Error.FailedLoadingData,
                operation = ConversationDetailEvent.ErrorLoadingConversation,
                expectedState = ConversationState.Error.FailedLoadingData
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
        val currentState: ConversationState,
        val operation: ConversationDetailOperation.AffectingConversation,
        val expectedState: ConversationState
    )

}
