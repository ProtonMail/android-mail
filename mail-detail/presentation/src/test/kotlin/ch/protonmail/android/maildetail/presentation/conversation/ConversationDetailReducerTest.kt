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

import ch.protonmail.android.maildetail.presentation.conversation.model.ConversationDetailEvent
import ch.protonmail.android.maildetail.presentation.conversation.model.ConversationDetailState
import ch.protonmail.android.testdata.conversation.ConversationUiModelTestData
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
class ConversationDetailReducerTest (
    private val testInput: TestInput
) {

    private val detailReducer = ConversationDetailReducer()

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
            TestInput(
                currentState = ConversationDetailState.Loading,
                event = ConversationDetailEvent.NoConversationIdProvided,
                expectedState = ConversationDetailState.Error.NoConversationIdProvided
            ).toArray(),
            TestInput(
                currentState = ConversationDetailState.Loading,
                event = ConversationDetailEvent.NoPrimaryUser,
                expectedState = ConversationDetailState.Error.NotLoggedIn
            ).toArray(),
            TestInput(
                currentState = ConversationDetailState.Loading,
                event = ConversationDetailEvent.ConversationData(conversationUiModel),
                expectedState = ConversationDetailState.Data(conversationUiModel)
            ).toArray()
        )

        @JvmStatic
        @Parameterized.Parameters
        fun data(): Collection<Array<TestInput>> {
            return transitionsFromLoadingState
        }
    }

    class TestInput(
        val currentState: ConversationDetailState,
        val event: ConversationDetailEvent,
        val expectedState: ConversationDetailState
    ) {

        fun toArray() = arrayOf(this)
    }
}
