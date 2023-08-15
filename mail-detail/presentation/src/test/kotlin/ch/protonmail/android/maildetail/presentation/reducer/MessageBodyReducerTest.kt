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

import ch.protonmail.android.maildetail.presentation.model.MessageBodyAttachmentsUiModel
import ch.protonmail.android.maildetail.presentation.model.MessageBodyState
import ch.protonmail.android.maildetail.presentation.model.MessageDetailEvent
import ch.protonmail.android.maildetail.presentation.model.MessageDetailOperation
import ch.protonmail.android.maildetail.presentation.model.MessageViewAction
import ch.protonmail.android.maildetail.presentation.sample.AttachmentUiModelSample
import ch.protonmail.android.mailmessage.domain.model.AttachmentId
import ch.protonmail.android.mailmessage.domain.model.AttachmentWorkerStatus
import ch.protonmail.android.testdata.message.MessageBodyUiModelTestData
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
class MessageBodyReducerTest(
    private val testName: String,
    private val testInput: TestInput
) {

    private val messageBodyReducer = MessageBodyReducer()

    @Test
    fun `should produce the expected new state`() = with(testInput) {
        // When
        val newState = messageBodyReducer.newStateFrom(currentState, operation)

        // Then
        assertEquals(expectedState, newState, testName)
    }

    companion object {

        private val actions = listOf(
            TestInput(
                MessageBodyState.Loading,
                MessageViewAction.Reload,
                MessageBodyState.Loading
            )
        )
        private val events = listOf(
            TestInput(
                MessageBodyState.Loading,
                MessageDetailEvent.MessageBodyEvent(
                    messageBody = MessageBodyUiModelTestData.plainTextMessageBodyUiModel
                ),
                MessageBodyState.Data(MessageBodyUiModelTestData.plainTextMessageBodyUiModel)
            ),
            TestInput(
                MessageBodyState.Loading,
                MessageDetailEvent.ErrorGettingMessageBody(isNetworkError = true),
                MessageBodyState.Error.Data(true)
            ),
            TestInput(
                MessageBodyState.Loading,
                MessageDetailEvent.ErrorGettingMessageBody(isNetworkError = false),
                MessageBodyState.Error.Data(false)
            ),
            TestInput(
                MessageBodyState.Loading,
                MessageDetailEvent.ErrorDecryptingMessageBody(
                    MessageBodyUiModelTestData.plainTextMessageBodyUiModel
                ),
                MessageBodyState.Error.Decryption(
                    MessageBodyUiModelTestData.plainTextMessageBodyUiModel
                )
            ),
            TestInput(
                MessageBodyState.Data(MessageBodyUiModelTestData.messageBodyWithAttachmentsUiModel),
                MessageDetailEvent.AttachmentStatusChanged(
                    attachmentId = AttachmentId("invoice"),
                    status = AttachmentWorkerStatus.Running
                ),
                MessageBodyState.Data(
                    MessageBodyUiModelTestData.buildMessageBodyUiModel(
                        attachments = MessageBodyAttachmentsUiModel(
                            limit = 3,
                            attachments = listOf(
                                AttachmentUiModelSample.invoiceStatusRunning,
                                AttachmentUiModelSample.document,
                                AttachmentUiModelSample.documentWithMultipleDots,
                                AttachmentUiModelSample.image
                            )
                        )
                    )
                )
            ),
            TestInput(
                MessageBodyState.Data(MessageBodyUiModelTestData.messageBodyWithAttachmentsUiModel),
                MessageDetailEvent.AttachmentStatusChanged(
                    attachmentId = AttachmentId("attachmentId"),
                    status = AttachmentWorkerStatus.Running
                ),
                MessageBodyState.Data(MessageBodyUiModelTestData.messageBodyWithAttachmentsUiModel)
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
        val currentState: MessageBodyState,
        val operation: MessageDetailOperation.AffectingMessageBody,
        val expectedState: MessageBodyState
    )
}
