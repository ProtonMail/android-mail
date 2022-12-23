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
import ch.protonmail.android.maildetail.presentation.R.string
import ch.protonmail.android.maildetail.presentation.model.BottomSheetEvent
import ch.protonmail.android.maildetail.presentation.model.BottomSheetState
import ch.protonmail.android.maildetail.presentation.model.MessageBodyState
import ch.protonmail.android.maildetail.presentation.model.MessageDetailActionBarUiModel
import ch.protonmail.android.maildetail.presentation.model.MessageDetailEvent
import ch.protonmail.android.maildetail.presentation.model.MessageDetailOperation
import ch.protonmail.android.maildetail.presentation.model.MessageDetailState
import ch.protonmail.android.maildetail.presentation.model.MessageMetadataState
import ch.protonmail.android.maildetail.presentation.model.MessageViewAction
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.testdata.action.ActionUiModelTestData
import ch.protonmail.android.testdata.maildetail.MessageDetailHeaderUiModelTestData
import ch.protonmail.android.testdata.maillabel.MailLabelUiModelTestData
import ch.protonmail.android.testdata.message.MessageBodyUiModelTestData
import ch.protonmail.android.testdata.message.MessageDetailActionBarUiModelTestData
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(Parameterized::class)
class MessageDetailReducerTest(
    private val testName: String,
    private val testInput: TestInput
) {

    private val messageMetadataReducer: MessageDetailMetadataReducer = mockk {
        every { newStateFrom(any(), any()) } returns reducedState.messageMetadataState
    }

    private val messageBodyReducer: MessageBodyReducer = mockk {
        every { newStateFrom(any()) } returns reducedState.messageBodyState
    }

    private val bottomBarReducer: BottomBarReducer = mockk {
        every { newStateFrom(any(), any()) } returns reducedState.bottomBarState
    }

    private val bottomSheetReducer: BottomSheetReducer = mockk {
        every { newStateFrom(any(), any()) } returns reducedState.bottomSheetState
    }

    private val detailReducer = MessageDetailReducer(
        messageMetadataReducer,
        messageBodyReducer,
        bottomBarReducer,
        bottomSheetReducer
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

        if (shouldReduceMessageBodyState) {
            verify {
                messageBodyReducer.newStateFrom(
                    operation as MessageDetailOperation.AffectingMessageBody
                )
            }
        } else {
            assertEquals(currentState.messageBodyState, nextState.messageBodyState, testName)
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

        if (shouldReduceBottomSheetState) {
            verify {
                bottomSheetReducer.newStateFrom(
                    currentState.bottomSheetState,
                    any()
                )
            }
        } else {
            assertEquals(currentState.bottomSheetState, nextState.bottomSheetState, testName)
        }

        if (shouldReduceToErrorEffect) {
            assertTrue(nextState.error.consume() is TextUiModel.TextRes)
        } else {
            assertEquals(currentState.error, nextState.error, testName)
        }

        if (shouldReduceExitEffect) {
            assertNotNull(nextState.exitScreenEffect.consume(), testName)
        } else {
            assertEquals(currentState.exitScreenEffect, nextState.exitScreenEffect, testName)
        }

        if (exitMessage != null) {
            assertEquals(exitMessage, nextState.exitScreenWithMessageEffect.consume())
        }
    }

    companion object {

        private val detailHeaderUiModel = MessageDetailHeaderUiModelTestData.messageDetailHeaderUiModel
        private val messageBodyUiModel = MessageBodyUiModelTestData.messageBodyUiModel
        private val actionBarUiModel = MessageDetailActionBarUiModelTestData.uiModel
        private val currentState = MessageDetailState.Loading
        private val reducedState = MessageDetailState(
            messageMetadataState = MessageMetadataState.Data(actionBarUiModel, detailHeaderUiModel),
            messageBodyState = MessageBodyState.Data(messageBodyUiModel),
            bottomBarState = BottomBarState.Data(listOf(ActionUiModelTestData.markUnread)),
            bottomSheetState = BottomSheetState.Data(MailLabelUiModelTestData.spamAndCustomFolder, null),
            exitScreenEffect = Effect.empty(),
            exitScreenWithMessageEffect = Effect.empty(),
            error = Effect.empty()
        )

        private val actions = listOf(
            TestInput(
                MessageViewAction.Star,
                shouldReduceMessageMetadataState = true,
                shouldReduceMessageBodyState = false,
                shouldReduceBottomBarState = false,
                shouldReduceExitEffect = false,
                shouldReduceToErrorEffect = false,
                shouldReduceBottomSheetState = false
            ),
            TestInput(
                MessageViewAction.UnStar,
                shouldReduceMessageMetadataState = true,
                shouldReduceMessageBodyState = false,
                shouldReduceBottomBarState = false,
                shouldReduceExitEffect = false,
                shouldReduceToErrorEffect = false,
                shouldReduceBottomSheetState = false
            ),
            TestInput(
                MessageViewAction.MarkUnread,
                shouldReduceMessageMetadataState = false,
                shouldReduceMessageBodyState = false,
                shouldReduceBottomBarState = false,
                shouldReduceExitEffect = true,
                shouldReduceToErrorEffect = false,
                shouldReduceBottomSheetState = false
            ),
            TestInput(
                MessageViewAction.Trash,
                shouldReduceMessageMetadataState = false,
                shouldReduceMessageBodyState = false,
                shouldReduceBottomBarState = false,
                shouldReduceExitEffect = false,
                exitMessage = TextUiModel(string.message_moved_to_trash),
                shouldReduceToErrorEffect = false,
                shouldReduceBottomSheetState = false
            ),
            TestInput(
                MessageViewAction.MoveToDestinationSelected(MailLabelId.System.Spam),
                shouldReduceMessageMetadataState = false,
                shouldReduceMessageBodyState = false,
                shouldReduceBottomBarState = false,
                shouldReduceExitEffect = false,
                shouldReduceToErrorEffect = false,
                shouldReduceBottomSheetState = true
            )
        )

        private val events = listOf(
            TestInput(
                MessageDetailEvent.MessageWithLabelsEvent(
                    MessageDetailActionBarUiModel("subject", false),
                    detailHeaderUiModel
                ),
                shouldReduceMessageBodyState = false,
                shouldReduceMessageMetadataState = true,
                shouldReduceBottomBarState = false,
                shouldReduceExitEffect = false,
                shouldReduceToErrorEffect = false,
                shouldReduceBottomSheetState = false
            ),
            TestInput(
                MessageDetailEvent.MessageBodyEvent(
                    MessageBodyUiModelTestData.messageBodyUiModel
                ),
                shouldReduceMessageMetadataState = false,
                shouldReduceMessageBodyState = true,
                shouldReduceBottomBarState = false,
                shouldReduceExitEffect = false,
                shouldReduceToErrorEffect = false,
                shouldReduceBottomSheetState = false
            ),
            TestInput(
                MessageDetailEvent.NoCachedMetadata,
                shouldReduceMessageMetadataState = true,
                shouldReduceMessageBodyState = false,
                shouldReduceBottomBarState = false,
                shouldReduceExitEffect = false,
                shouldReduceToErrorEffect = false,
                shouldReduceBottomSheetState = false
            ),
            TestInput(
                MessageDetailEvent.MessageBottomBarEvent(BottomBarEvent.ErrorLoadingActions),
                shouldReduceMessageMetadataState = false,
                shouldReduceMessageBodyState = false,
                shouldReduceBottomBarState = true,
                shouldReduceExitEffect = false,
                shouldReduceToErrorEffect = false,
                shouldReduceBottomSheetState = false
            ),
            TestInput(
                MessageDetailEvent.ErrorAddingStar,
                shouldReduceMessageMetadataState = true,
                shouldReduceMessageBodyState = false,
                shouldReduceBottomBarState = false,
                shouldReduceExitEffect = false,
                shouldReduceToErrorEffect = true,
                shouldReduceBottomSheetState = false
            ),
            TestInput(
                MessageDetailEvent.ErrorRemovingStar,
                shouldReduceMessageMetadataState = true,
                shouldReduceMessageBodyState = false,
                shouldReduceBottomBarState = false,
                shouldReduceExitEffect = false,
                shouldReduceToErrorEffect = true,
                shouldReduceBottomSheetState = false
            ),
            TestInput(
                MessageDetailEvent.ErrorMarkingUnread,
                shouldReduceMessageMetadataState = false,
                shouldReduceMessageBodyState = false,
                shouldReduceBottomBarState = false,
                shouldReduceExitEffect = false,
                shouldReduceToErrorEffect = true,
                shouldReduceBottomSheetState = false
            ),
            TestInput(
                MessageDetailEvent.ErrorMovingToTrash,
                shouldReduceMessageMetadataState = false,
                shouldReduceMessageBodyState = false,
                shouldReduceBottomBarState = false,
                shouldReduceExitEffect = false,
                shouldReduceToErrorEffect = true,
                shouldReduceBottomSheetState = false
            ),
            TestInput(
                MessageDetailEvent.MessageBottomSheetEvent(
                    BottomSheetEvent.Data(MailLabelUiModelTestData.spamAndCustomFolder)
                ),
                shouldReduceMessageMetadataState = false,
                shouldReduceMessageBodyState = false,
                shouldReduceBottomBarState = false,
                shouldReduceExitEffect = false,
                shouldReduceToErrorEffect = false,
                shouldReduceBottomSheetState = true
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
        val shouldReduceMessageBodyState: Boolean,
        val shouldReduceBottomBarState: Boolean,
        val shouldReduceExitEffect: Boolean,
        val shouldReduceBottomSheetState: Boolean,
        val shouldReduceToErrorEffect: Boolean,
        val exitMessage: TextUiModel? = null
    )
}
