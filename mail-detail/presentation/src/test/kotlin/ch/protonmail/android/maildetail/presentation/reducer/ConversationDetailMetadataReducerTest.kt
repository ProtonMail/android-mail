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
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailMetadataState
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailOperation
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailViewAction
import ch.protonmail.android.testdata.conversation.ConversationUiModelTestData
import ch.protonmail.android.testdata.conversation.ConversationUiModelTestData.conversationUiModelStarred
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
class ConversationDetailMetadataReducerTest(
    private val testName: String,
    private val testInput: TestParams.TestInput
) {

    private val detailReducer = ConversationDetailMetadataReducer()

    @Test
    fun `should produce the expected new state`() {
        val actualState = detailReducer.newStateFrom(testInput.currentState, testInput.event)

        assertEquals(testInput.expectedState, actualState, testName)
    }

    companion object {

        private val conversationUiModel = ConversationUiModelTestData.buildConversationUiModel(
            "conversationId",
            "This email is about subjects"
        )
        private val updatedConversationUiModel = ConversationUiModelTestData.buildConversationUiModel(
            "conversationId1",
            "[Re] This email is about subjects"
        )

        private val transitionsFromLoadingState = listOf(
            TestParams(
                "from loading to conversation data",
                TestParams.TestInput(
                    currentState = ConversationDetailMetadataState.Loading,
                    event = ConversationDetailEvent.ConversationData(conversationUiModel),
                    expectedState = ConversationDetailMetadataState.Data(conversationUiModel)
                )
            ),
            TestParams(
                "from loading to error loading conversation",
                TestParams.TestInput(
                    currentState = ConversationDetailMetadataState.Loading,
                    event = ConversationDetailEvent.ErrorLoadingConversation,
                    expectedState = ConversationDetailMetadataState.Error(
                        message = TextUiModel(string.detail_error_loading_conversation)
                    )
                )
            )
        )

        private val transitionsFromDataState = listOf(
            TestParams(
                "from data to starred conversation data",
                TestParams.TestInput(
                    currentState = ConversationDetailMetadataState.Data(conversationUiModel),
                    event = ConversationDetailViewAction.Star,
                    expectedState = ConversationDetailMetadataState.Data(conversationUiModel.copy(isStarred = true))
                )
            ),
            TestParams(
                testName = "from data to unStarred conversation data",
                TestParams.TestInput(
                    currentState = ConversationDetailMetadataState.Data(conversationUiModelStarred),
                    event = ConversationDetailViewAction.UnStar,
                    expectedState = ConversationDetailMetadataState.Data(
                        conversationUiModelStarred.copy(isStarred = false)
                    )
                )
            ),
            TestParams(
                testName = "from data does not change state on error loading conversation",
                TestParams.TestInput(
                    currentState = ConversationDetailMetadataState.Data(conversationUiModel),
                    event = ConversationDetailEvent.ErrorLoadingConversation,
                    expectedState = ConversationDetailMetadataState.Data(conversationUiModel)
                )
            ),
            TestParams(
                testName = "from data to updated data state on new data",
                TestParams.TestInput(
                    currentState = ConversationDetailMetadataState.Data(conversationUiModel),
                    event = ConversationDetailEvent.ConversationData(updatedConversationUiModel),
                    expectedState = ConversationDetailMetadataState.Data(updatedConversationUiModel)
                )
            )
        )

        private val transitionsFromErrorState = listOf(
            TestParams(
                "from error to conversation data",
                TestParams.TestInput(
                    currentState = ConversationDetailMetadataState.Error(TextUiModel("any error")),
                    event = ConversationDetailEvent.ConversationData(conversationUiModel),
                    expectedState = ConversationDetailMetadataState.Data(conversationUiModel)
                )
            ),
            TestParams(
                "from error to updated error on error",
                TestParams.TestInput(
                    currentState = ConversationDetailMetadataState.Error(
                        message = TextUiModel(string.x_error_not_logged_in)
                    ),
                    event = ConversationDetailEvent.ErrorLoadingConversation,
                    expectedState = ConversationDetailMetadataState.Error(
                        message = TextUiModel(string.detail_error_loading_conversation)
                    )
                )
            )
        )

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = (transitionsFromLoadingState + transitionsFromDataState + transitionsFromErrorState)
            .map { arrayOf(it.testName, it.testInput) }
    }

    data class TestParams(
        val testName: String,
        val testInput: TestInput
    ) {

        data class TestInput(
            val currentState: ConversationDetailMetadataState,
            val event: ConversationDetailOperation.AffectingConversation,
            val expectedState: ConversationDetailMetadataState
        )
    }

}
