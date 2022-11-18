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

import ch.protonmail.android.maildetail.presentation.model.MessageDetailEvent
import ch.protonmail.android.maildetail.presentation.model.MessageDetailMetadataState
import ch.protonmail.android.maildetail.presentation.model.MessageDetailOperation
import ch.protonmail.android.maildetail.presentation.model.MessageViewAction
import ch.protonmail.android.testdata.message.MessageUiModelTestData
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
class MessageDetailMetadataReducerTest(
    private val testInput: TestInput
) {

    private val detailReducer = MessageDetailMetadataReducer()

    @Test
    fun `should produce the expected new state`() {
        val actualState = detailReducer.newStateFrom(testInput.currentState, testInput.operation)

        assertEquals(testInput.expectedState, actualState)
    }

    companion object {

        private val messageUiModel = MessageUiModelTestData.buildMessageUiModel(
            "This email is about subjects"
        )

        private val starredMessageUiModel = messageUiModel.copy(isStarred = true)

        private val transitionsFromLoadingState = listOf(
            TestInput(
                currentState = MessageDetailMetadataState.Loading,
                operation = MessageDetailEvent.MessageWithLabels(messageUiModel),
                expectedState = MessageDetailMetadataState.Data(messageUiModel)
            ).toArray(),
            TestInput(
                currentState = MessageDetailMetadataState.Loading,
                operation = MessageViewAction.Star,
                expectedState = MessageDetailMetadataState.Loading
            ).toArray(),
            TestInput(
                currentState = MessageDetailMetadataState.Loading,
                operation = MessageDetailEvent.ErrorAddingStar,
                expectedState = MessageDetailMetadataState.Loading
            ).toArray(),
            TestInput(
                currentState = MessageDetailMetadataState.Loading,
                operation = MessageDetailEvent.ErrorRemovingStar,
                expectedState = MessageDetailMetadataState.Loading
            ).toArray()
        )

        private val transitionsFromDataState = listOf(
            TestInput(
                currentState = MessageDetailMetadataState.Data(messageUiModel),
                operation = MessageViewAction.Star,
                expectedState = MessageDetailMetadataState.Data(starredMessageUiModel)
            ).toArray(),
            TestInput(
                currentState = MessageDetailMetadataState.Data(starredMessageUiModel),
                operation = MessageDetailEvent.ErrorAddingStar,
                expectedState = MessageDetailMetadataState.Data(messageUiModel)
            ).toArray(),
            TestInput(
                currentState = MessageDetailMetadataState.Data(messageUiModel),
                operation = MessageDetailEvent.ErrorRemovingStar,
                expectedState = MessageDetailMetadataState.Data(starredMessageUiModel)
            ).toArray()
        )

        @JvmStatic
        @Parameterized.Parameters
        fun data(): Collection<Array<TestInput>> = transitionsFromLoadingState + transitionsFromDataState
    }

    class TestInput(
        val currentState: MessageDetailMetadataState,
        val operation: MessageDetailOperation.AffectingMessage,
        val expectedState: MessageDetailMetadataState
    ) {

        fun toArray() = arrayOf(this)
    }
}
