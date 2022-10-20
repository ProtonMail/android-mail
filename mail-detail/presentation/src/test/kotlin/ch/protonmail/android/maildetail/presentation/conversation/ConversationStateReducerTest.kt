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

package ch.protonmail.android.maildetail.presentation.conversation

import ch.protonmail.android.maildetail.presentation.model.AffectingConversation
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailEvent
import ch.protonmail.android.maildetail.presentation.model.ConversationState
import ch.protonmail.android.maildetail.presentation.reducer.ConversationStateReducer
import ch.protonmail.android.testdata.conversation.ConversationUiModelTestData
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
class ConversationStateReducerTest(
    @Suppress("UNUSED_PARAMETER") private val testName: String,
    private val testInput: TestParams.TestInput
) {

    private val detailReducer = ConversationStateReducer()

    @Test
    fun `should produce the expected new state`() {
        val actualState = detailReducer.reduce(testInput.currentState, testInput.event)

        assertEquals(testInput.expectedState, actualState)
    }

    companion object {

        private val conversationUiModel = ConversationUiModelTestData.buildConversationUiModel(
            "conversationId",
            "This email is about subjects"
        )

        private val transitionsFromLoadingState = listOf(
            TestParams(
                "from loading to no primary user",
                TestParams.TestInput(
                    currentState = ConversationState.Loading,
                    event = ConversationDetailEvent.NoPrimaryUser,
                    expectedState = ConversationState.Error.NotLoggedIn
                )
            ),
            TestParams(
                "from loading to conversation data",
                TestParams.TestInput(
                    currentState = ConversationState.Loading,
                    event = ConversationDetailEvent.ConversationData(conversationUiModel),
                    expectedState = ConversationState.Data(conversationUiModel)
                )
            ),
            TestParams(
                "from loading to error loading conversation",
                TestParams.TestInput(
                    currentState = ConversationState.Loading,
                    event = ConversationDetailEvent.ErrorLoadingConversation,
                    expectedState = ConversationState.Error.FailedLoadingData
                )
            )
        )

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = transitionsFromLoadingState
            .map { arrayOf(it.testName, it.testInput) }
    }

    data class TestParams(
        val testName: String,
        val testInput: TestInput
    ) {

        data class TestInput(
            val currentState: ConversationState,
            val event: AffectingConversation,
            val expectedState: ConversationState
        )
    }

}
