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

import ch.protonmail.android.maildetail.presentation.model.MessageBannersState
import ch.protonmail.android.maildetail.presentation.model.MessageDetailEvent
import ch.protonmail.android.maildetail.presentation.model.MessageDetailOperation
import ch.protonmail.android.testdata.maildetail.MessageBannersUiModelTestData
import ch.protonmail.android.testdata.maildetail.MessageDetailHeaderUiModelTestData
import ch.protonmail.android.testdata.message.MessageDetailActionBarUiModelTestData
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
class MessageBannersReducerTest(
    private val testInput: TestInput
) {

    private val detailReducer = MessageBannersReducer()

    @Test
    fun `should produce the expected new state`() {
        val actualState = detailReducer.newStateFrom(testInput.operation)

        assertEquals(testInput.expectedState, actualState)
    }

    companion object {

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = arrayOf(
            TestInput(
                operation = MessageDetailEvent.MessageWithLabelsEvent(
                    MessageDetailActionBarUiModelTestData.uiModel,
                    MessageDetailHeaderUiModelTestData.messageDetailHeaderUiModel,
                    MessageBannersUiModelTestData.messageBannersUiModel
                ),
                expectedState = MessageBannersState.Data(MessageBannersUiModelTestData.messageBannersUiModel)
            )
        )
    }

    data class TestInput(
        val operation: MessageDetailOperation.AffectingMessageBanners,
        val expectedState: MessageBannersState
    )
}
