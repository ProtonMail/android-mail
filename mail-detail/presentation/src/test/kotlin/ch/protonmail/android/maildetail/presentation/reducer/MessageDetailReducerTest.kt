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
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.BottomSheetState
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.LabelAsBottomSheetState
import ch.protonmail.android.maildetail.presentation.model.MessageBodyState
import ch.protonmail.android.maildetail.presentation.model.MessageDetailActionBarUiModel
import ch.protonmail.android.maildetail.presentation.model.MessageDetailEvent
import ch.protonmail.android.maildetail.presentation.model.MessageDetailOperation
import ch.protonmail.android.maildetail.presentation.model.MessageDetailState
import ch.protonmail.android.maildetail.presentation.model.MessageMetadataState
import ch.protonmail.android.maildetail.presentation.model.MessageViewAction
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.MoveToBottomSheetState
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.mailmessage.domain.model.AttachmentId
import ch.protonmail.android.mailmessage.domain.model.AttachmentWorkerStatus
import ch.protonmail.android.mailmessage.presentation.reducer.BottomSheetReducer
import ch.protonmail.android.testdata.action.ActionUiModelTestData
import ch.protonmail.android.testdata.maildetail.MessageDetailHeaderUiModelTestData
import ch.protonmail.android.testdata.maillabel.MailLabelUiModelTestData
import ch.protonmail.android.testdata.message.MessageBodyUiModelTestData
import ch.protonmail.android.testdata.message.MessageDetailActionBarUiModelTestData
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.collections.immutable.toImmutableList
import me.proton.core.label.domain.entity.LabelId
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
        every { newStateFrom(any(), any()) } returns reducedState.messageBodyState
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
                    currentState.messageBodyState,
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

        if (shouldReduceOpenMessageBodyLinkEffect) {
            assertNotNull(nextState.openMessageBodyLinkEffect.consume(), testName)
        } else {
            assertEquals(currentState.exitScreenEffect, nextState.exitScreenEffect, testName)
        }

        if (exitMessage != null) {
            assertEquals(exitMessage, nextState.exitScreenWithMessageEffect.consume())
        }

        // Reducer should not change the requestLinkConfirmation flag, just copy it
        assertEquals(currentState.requestLinkConfirmation, nextState.requestLinkConfirmation)
    }

    companion object {

        private val detailHeaderUiModel = MessageDetailHeaderUiModelTestData.messageDetailHeaderUiModel
        private val messageBodyUiModel = MessageBodyUiModelTestData.plainTextMessageBodyUiModel
        private val actionBarUiModel = MessageDetailActionBarUiModelTestData.uiModel
        private val currentState = MessageDetailState.Loading
        private val reducedState = MessageDetailState(
            messageMetadataState = MessageMetadataState.Data(actionBarUiModel, detailHeaderUiModel),
            messageBodyState = MessageBodyState.Data(messageBodyUiModel),
            bottomBarState = BottomBarState.Data.Shown(listOf(ActionUiModelTestData.markUnread).toImmutableList()),
            bottomSheetState = BottomSheetState(
                MoveToBottomSheetState.Data(
                    MailLabelUiModelTestData.spamAndCustomFolder,
                    null
                )
            ),
            exitScreenEffect = Effect.empty(),
            exitScreenWithMessageEffect = Effect.empty(),
            error = Effect.empty(),
            openMessageBodyLinkEffect = Effect.empty(),
            openAttachmentEffect = Effect.empty(),
            showReplyActionsFeatureFlag = false,
            requestLinkConfirmation = false
        )

        private val actions = listOf(
            TestInput(
                MessageViewAction.Star,
                shouldReduceMessageMetadataState = true,
                shouldReduceMessageBodyState = false,
                shouldReduceBottomBarState = false,
                shouldReduceExitEffect = false,
                shouldReduceToErrorEffect = false,
                shouldReduceBottomSheetState = false,
                shouldReduceOpenMessageBodyLinkEffect = false
            ),
            TestInput(
                MessageViewAction.UnStar,
                shouldReduceMessageMetadataState = true,
                shouldReduceMessageBodyState = false,
                shouldReduceBottomBarState = false,
                shouldReduceExitEffect = false,
                shouldReduceToErrorEffect = false,
                shouldReduceBottomSheetState = false,
                shouldReduceOpenMessageBodyLinkEffect = false
            ),
            TestInput(
                MessageViewAction.MarkUnread,
                shouldReduceMessageMetadataState = false,
                shouldReduceMessageBodyState = false,
                shouldReduceBottomBarState = false,
                shouldReduceExitEffect = true,
                shouldReduceToErrorEffect = false,
                shouldReduceBottomSheetState = false,
                shouldReduceOpenMessageBodyLinkEffect = false
            ),
            TestInput(
                MessageViewAction.Trash,
                shouldReduceMessageMetadataState = false,
                shouldReduceMessageBodyState = false,
                shouldReduceBottomBarState = false,
                shouldReduceExitEffect = false,
                exitMessage = TextUiModel(string.message_moved_to_trash),
                shouldReduceToErrorEffect = false,
                shouldReduceBottomSheetState = false,
                shouldReduceOpenMessageBodyLinkEffect = false
            ),
            TestInput(
                MessageViewAction.RequestMoveToBottomSheet,
                shouldReduceMessageMetadataState = false,
                shouldReduceMessageBodyState = false,
                shouldReduceBottomBarState = false,
                shouldReduceExitEffect = false,
                shouldReduceToErrorEffect = false,
                shouldReduceBottomSheetState = true,
                shouldReduceOpenMessageBodyLinkEffect = false
            ),
            TestInput(
                MessageViewAction.DismissBottomSheet,
                shouldReduceMessageMetadataState = false,
                shouldReduceMessageBodyState = false,
                shouldReduceBottomBarState = false,
                shouldReduceExitEffect = false,
                shouldReduceToErrorEffect = false,
                shouldReduceBottomSheetState = true,
                shouldReduceOpenMessageBodyLinkEffect = false
            ),
            TestInput(
                MessageViewAction.MoveToDestinationSelected(MailLabelId.System.Spam),
                shouldReduceMessageMetadataState = false,
                shouldReduceMessageBodyState = false,
                shouldReduceBottomBarState = false,
                shouldReduceExitEffect = false,
                shouldReduceToErrorEffect = false,
                shouldReduceBottomSheetState = true,
                shouldReduceOpenMessageBodyLinkEffect = false
            ),
            TestInput(
                MessageViewAction.MoveToDestinationConfirmed("testLabel"),
                shouldReduceMessageMetadataState = false,
                shouldReduceMessageBodyState = false,
                shouldReduceBottomBarState = false,
                shouldReduceExitEffect = false,
                exitMessage = TextUiModel(string.message_moved_to_selected_destination, "testLabel"),
                shouldReduceToErrorEffect = false,
                shouldReduceBottomSheetState = false,
                shouldReduceOpenMessageBodyLinkEffect = false
            ),
            TestInput(
                MessageViewAction.LabelAsToggleAction(LabelId("customLabel")),
                shouldReduceMessageMetadataState = false,
                shouldReduceMessageBodyState = false,
                shouldReduceBottomBarState = false,
                shouldReduceExitEffect = false,
                shouldReduceToErrorEffect = false,
                shouldReduceBottomSheetState = true,
                shouldReduceOpenMessageBodyLinkEffect = false
            ),
            TestInput(
                MessageViewAction.LabelAsConfirmed(false),
                shouldReduceMessageMetadataState = false,
                shouldReduceMessageBodyState = false,
                shouldReduceBottomBarState = false,
                shouldReduceExitEffect = false,
                shouldReduceToErrorEffect = false,
                shouldReduceBottomSheetState = true,
                shouldReduceOpenMessageBodyLinkEffect = false
            ),
            TestInput(
                MessageViewAction.LabelAsConfirmed(true),
                shouldReduceMessageMetadataState = false,
                shouldReduceMessageBodyState = false,
                shouldReduceBottomBarState = false,
                shouldReduceExitEffect = false,
                exitMessage = TextUiModel(string.message_moved_to_archive),
                shouldReduceToErrorEffect = false,
                shouldReduceBottomSheetState = true,
                shouldReduceOpenMessageBodyLinkEffect = false
            ),
            TestInput(
                MessageViewAction.MessageBodyLinkClicked(mockk()),
                shouldReduceMessageMetadataState = false,
                shouldReduceMessageBodyState = false,
                shouldReduceBottomBarState = false,
                shouldReduceExitEffect = false,
                shouldReduceToErrorEffect = false,
                shouldReduceBottomSheetState = false,
                shouldReduceOpenMessageBodyLinkEffect = true
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
                shouldReduceBottomSheetState = false,
                shouldReduceOpenMessageBodyLinkEffect = false
            ),
            TestInput(
                MessageDetailEvent.MessageBodyEvent(
                    MessageBodyUiModelTestData.plainTextMessageBodyUiModel
                ),
                shouldReduceMessageMetadataState = false,
                shouldReduceMessageBodyState = true,
                shouldReduceBottomBarState = false,
                shouldReduceExitEffect = false,
                shouldReduceToErrorEffect = false,
                shouldReduceBottomSheetState = false,
                shouldReduceOpenMessageBodyLinkEffect = false
            ),
            TestInput(
                MessageDetailEvent.NoCachedMetadata,
                shouldReduceMessageMetadataState = true,
                shouldReduceMessageBodyState = false,
                shouldReduceBottomBarState = false,
                shouldReduceExitEffect = false,
                shouldReduceToErrorEffect = false,
                shouldReduceBottomSheetState = false,
                shouldReduceOpenMessageBodyLinkEffect = false
            ),
            TestInput(
                MessageDetailEvent.MessageBottomBarEvent(BottomBarEvent.ErrorLoadingActions),
                shouldReduceMessageMetadataState = false,
                shouldReduceMessageBodyState = false,
                shouldReduceBottomBarState = true,
                shouldReduceExitEffect = false,
                shouldReduceToErrorEffect = false,
                shouldReduceBottomSheetState = false,
                shouldReduceOpenMessageBodyLinkEffect = false
            ),
            TestInput(
                MessageDetailEvent.ErrorAddingStar,
                shouldReduceMessageMetadataState = true,
                shouldReduceMessageBodyState = false,
                shouldReduceBottomBarState = false,
                shouldReduceExitEffect = false,
                shouldReduceToErrorEffect = true,
                shouldReduceBottomSheetState = false,
                shouldReduceOpenMessageBodyLinkEffect = false
            ),
            TestInput(
                MessageDetailEvent.ErrorRemovingStar,
                shouldReduceMessageMetadataState = true,
                shouldReduceMessageBodyState = false,
                shouldReduceBottomBarState = false,
                shouldReduceExitEffect = false,
                shouldReduceToErrorEffect = true,
                shouldReduceBottomSheetState = false,
                shouldReduceOpenMessageBodyLinkEffect = false
            ),
            TestInput(
                MessageDetailEvent.ErrorMarkingUnread,
                shouldReduceMessageMetadataState = false,
                shouldReduceMessageBodyState = false,
                shouldReduceBottomBarState = false,
                shouldReduceExitEffect = false,
                shouldReduceToErrorEffect = true,
                shouldReduceBottomSheetState = false,
                shouldReduceOpenMessageBodyLinkEffect = false
            ),
            TestInput(
                MessageDetailEvent.ErrorMovingToTrash,
                shouldReduceMessageMetadataState = false,
                shouldReduceMessageBodyState = false,
                shouldReduceBottomBarState = false,
                shouldReduceExitEffect = false,
                shouldReduceToErrorEffect = true,
                shouldReduceBottomSheetState = false,
                shouldReduceOpenMessageBodyLinkEffect = false
            ),
            TestInput(
                MessageDetailEvent.MessageBottomSheetEvent(
                    MoveToBottomSheetState.MoveToBottomSheetEvent.ActionData(
                        MailLabelUiModelTestData.spamAndCustomFolder
                    )
                ),
                shouldReduceMessageMetadataState = false,
                shouldReduceMessageBodyState = false,
                shouldReduceBottomBarState = false,
                shouldReduceExitEffect = false,
                shouldReduceToErrorEffect = false,
                shouldReduceBottomSheetState = true,
                shouldReduceOpenMessageBodyLinkEffect = false
            ),
            TestInput(
                MessageDetailEvent.MessageBottomSheetEvent(
                    LabelAsBottomSheetState.LabelAsBottomSheetEvent.ActionData(
                        customLabelList = MailLabelUiModelTestData.customLabelList,
                        selectedLabels = listOf<LabelId>().toImmutableList()
                    )
                ),
                shouldReduceMessageMetadataState = false,
                shouldReduceMessageBodyState = false,
                shouldReduceBottomBarState = false,
                shouldReduceExitEffect = false,
                shouldReduceToErrorEffect = false,
                shouldReduceBottomSheetState = true,
                shouldReduceOpenMessageBodyLinkEffect = false
            ),
            TestInput(
                MessageDetailEvent.AttachmentStatusChanged(
                    attachmentId = AttachmentId("attachmentId"),
                    status = AttachmentWorkerStatus.Running
                ),
                shouldReduceMessageMetadataState = false,
                shouldReduceMessageBodyState = true,
                shouldReduceBottomBarState = false,
                shouldReduceExitEffect = false,
                shouldReduceToErrorEffect = false,
                shouldReduceBottomSheetState = false,
                shouldReduceOpenMessageBodyLinkEffect = false
            ),
            TestInput(
                MessageDetailEvent.ErrorGettingAttachment,
                shouldReduceMessageMetadataState = false,
                shouldReduceMessageBodyState = false,
                shouldReduceBottomBarState = false,
                shouldReduceExitEffect = false,
                shouldReduceToErrorEffect = true,
                shouldReduceBottomSheetState = false,
                shouldReduceOpenMessageBodyLinkEffect = false
            ),
            TestInput(
                MessageDetailEvent.ErrorGettingAttachmentNotEnoughSpace,
                shouldReduceMessageMetadataState = false,
                shouldReduceMessageBodyState = false,
                shouldReduceBottomBarState = false,
                shouldReduceExitEffect = false,
                shouldReduceToErrorEffect = true,
                shouldReduceBottomSheetState = false,
                shouldReduceOpenMessageBodyLinkEffect = false
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
        val shouldReduceOpenMessageBodyLinkEffect: Boolean,
        val exitMessage: TextUiModel? = null
    )
}
