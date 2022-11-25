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

import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.BottomBarEvent
import ch.protonmail.android.mailcommon.presentation.model.BottomBarState
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcommon.presentation.reducer.BottomBarReducer
import ch.protonmail.android.maildetail.presentation.model.MessageDetailActionBarUiModel
import ch.protonmail.android.maildetail.presentation.model.MessageDetailEvent
import ch.protonmail.android.maildetail.presentation.model.MessageDetailOperation
import ch.protonmail.android.maildetail.presentation.model.MessageDetailState
import ch.protonmail.android.maildetail.presentation.model.MessageMetadataState
import ch.protonmail.android.maildetail.presentation.model.MessageViewAction
import ch.protonmail.android.mailmessage.domain.entity.MessageWithBody
import ch.protonmail.android.testdata.action.ActionUiModelTestData
import ch.protonmail.android.testdata.maildetail.MessageDetailHeaderUiModelTestData
import ch.protonmail.android.testdata.message.MessageDetailActionBarUiModelTestData
import ch.protonmail.android.testdata.message.MessageTestData
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(Parameterized::class)
class MessageDetailReducerTest(
    private val testName: String,
    private val testInput: TestInput
) {

    private val messageMetadataReducer: MessageDetailMetadataReducer = mockk {
        every { newStateFrom(any(), any()) } returns reducedState.messageMetadataState
    }

    private val bottomBarReducer: BottomBarReducer = mockk {
        every { newStateFrom(any(), any()) } returns reducedState.bottomBarState
    }

    private val detailReducer = MessageDetailReducer(
        messageMetadataReducer,
        bottomBarReducer
    )

    @Test
    fun `should reduce only the affected parts of the state`() = with(testInput) {
        val nextState = detailReducer.newStateFrom(currentState, operation)

        if (shouldReduceMessageMetadataState) {
            verify {
                messageMetadataReducer.newStateFrom(
                    currentState.messageMetadataState,
                    operation as MessageDetailOperation.AffectingMessage
                )
            }
        } else {
            assertEquals(currentState.messageMetadataState, nextState.messageMetadataState, testName)
        }

        if (shouldReduceBottomBarState) {
            verify {
                bottomBarReducer.newStateFrom(
                    currentState.bottomBarState,
                    (operation as MessageDetailEvent.MessageBottomBarEvent).bottomBarEvent
                )
            }
        } else {
            assertEquals(currentState.bottomBarState, nextState.bottomBarState, testName)
        }

        if (shouldReduceToDismissEffect) {
            assertEquals(Effect.of(Unit), nextState.dismiss)
        } else {
            assertEquals(currentState.dismiss, nextState.dismiss, testName)
        }

        if (shouldReduceToErrorEffect) {
            assertTrue(nextState.error.consume() is TextUiModel.TextRes)
        } else {
            assertEquals(currentState.error, nextState.error, testName)
        }
    }

    companion object {

        private val detailHeaderUiModel = MessageDetailHeaderUiModelTestData.messageDetailHeaderUiModel
        private val actionBarUiModel = MessageDetailActionBarUiModelTestData.uiModel
        private val currentState = MessageDetailState.Loading
        private val reducedState = MessageDetailState(
            messageMetadataState = MessageMetadataState.Data(actionBarUiModel, detailHeaderUiModel),
            bottomBarState = BottomBarState.Data(listOf(ActionUiModelTestData.markUnread)),
            dismiss = Effect.empty(),
            error = Effect.empty()
        )

        private val actions = listOf(
            TestInput(
                MessageViewAction.Star,
                shouldReduceMessageMetadataState = true,
                shouldReduceBottomBarState = false,
                shouldReduceToDismissEffect = false,
                shouldReduceToErrorEffect = false
            ),
            TestInput(
                MessageViewAction.UnStar,
                shouldReduceMessageMetadataState = true,
                shouldReduceBottomBarState = false,
                shouldReduceToDismissEffect = false,
                shouldReduceToErrorEffect = false
            ),
            TestInput(
                MessageViewAction.MarkUnread,
                shouldReduceMessageMetadataState = false,
                shouldReduceBottomBarState = false,
                shouldReduceToDismissEffect = true,
                shouldReduceToErrorEffect = false
            ),
            TestInput(
                MessageViewAction.Trash,
                shouldReduceMessageMetadataState = false,
                shouldReduceBottomBarState = false,
                shouldReduceToDismissEffect = true,
                shouldReduceToErrorEffect = false
            )
        )

        private val events = listOf(
            TestInput(
                MessageDetailEvent.MessageWithLabelsEvent(
                    MessageDetailActionBarUiModel("subject", false),
                    detailHeaderUiModel
                ),
                shouldReduceMessageMetadataState = true,
                shouldReduceBottomBarState = false,
                shouldReduceToDismissEffect = false,
                shouldReduceToErrorEffect = false
            ),
            TestInput(
                MessageDetailEvent.MessageBody(MessageWithBody(MessageTestData.message, null)),
                shouldReduceMessageMetadataState = true,
                shouldReduceBottomBarState = false,
                shouldReduceToDismissEffect = false,
                shouldReduceToErrorEffect = false
            ),
            TestInput(
                MessageDetailEvent.NoCachedMetadata,
                shouldReduceMessageMetadataState = true,
                shouldReduceBottomBarState = false,
                shouldReduceToDismissEffect = false,
                shouldReduceToErrorEffect = false
            ),
            TestInput(
                MessageDetailEvent.MessageBottomBarEvent(BottomBarEvent.ErrorLoadingActions),
                shouldReduceMessageMetadataState = false,
                shouldReduceBottomBarState = true,
                shouldReduceToDismissEffect = false,
                shouldReduceToErrorEffect = false
            ),
            TestInput(
                MessageDetailEvent.ErrorAddingStar,
                shouldReduceMessageMetadataState = true,
                shouldReduceBottomBarState = false,
                shouldReduceToDismissEffect = false,
                shouldReduceToErrorEffect = true
            ),
            TestInput(
                MessageDetailEvent.ErrorRemovingStar,
                shouldReduceMessageMetadataState = true,
                shouldReduceBottomBarState = false,
                shouldReduceToDismissEffect = false,
                shouldReduceToErrorEffect = true
            ),
            TestInput(
                MessageDetailEvent.ErrorMarkingUnread,
                shouldReduceMessageMetadataState = false,
                shouldReduceBottomBarState = false,
                shouldReduceToDismissEffect = false,
                shouldReduceToErrorEffect = true
            ),
            TestInput(
                MessageDetailEvent.ErrorMovingToTrash,
                shouldReduceMessageMetadataState = false,
                shouldReduceBottomBarState = false,
                shouldReduceToDismissEffect = false,
                shouldReduceToErrorEffect = true
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
        val operation: MessageDetailOperation,
        val shouldReduceMessageMetadataState: Boolean,
        val shouldReduceBottomBarState: Boolean,
        val shouldReduceToDismissEffect: Boolean,
        val shouldReduceToErrorEffect: Boolean
    )
}
