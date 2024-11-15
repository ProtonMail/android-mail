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

import ch.protonmail.android.maildetail.presentation.mapper.MessageDetailActionBarUiModelMapper
import ch.protonmail.android.maildetail.presentation.mapper.MessageDetailFooterUiModelMapper
import ch.protonmail.android.maildetail.presentation.mapper.MessageDetailHeaderUiModelMapper
import ch.protonmail.android.maildetail.presentation.model.MessageDetailEvent
import ch.protonmail.android.maildetail.presentation.model.MessageDetailOperation
import ch.protonmail.android.maildetail.presentation.model.MessageMetadataState
import ch.protonmail.android.mailmessage.domain.sample.MessageSample
import ch.protonmail.android.mailmessage.domain.sample.MessageWithLabelsSample
import ch.protonmail.android.mailsettings.domain.model.AutoDeleteSetting
import ch.protonmail.android.mailsettings.domain.model.FolderColorSettings
import ch.protonmail.android.testdata.maildetail.MessageDetailFooterUiModelTestData.messageDetailFooterUiModel
import ch.protonmail.android.testdata.maildetail.MessageDetailHeaderUiModelTestData.messageDetailHeaderUiModel
import ch.protonmail.android.testdata.message.MessageDetailActionBarUiModelTestData
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
class MessageStateReducerTest(
    private val testName: String,
    private val testInput: TestInput
) {

    private val messageDetailActionBarUiModelMapper = mockk<MessageDetailActionBarUiModelMapper> {
        every { toUiModel(MessageSample.Invoice) } returns messageUiModel
        every { toUiModel(MessageSample.UnreadInvoice) } returns updatedMessageUiModel
    }
    private val messageDetailHeaderUiModelMapper = mockk<MessageDetailHeaderUiModelMapper> {
        coEvery {
            toUiModel(MessageWithLabelsSample.Invoice, emptyList(), FolderColorSettings(), AutoDeleteSetting.Disabled)
        } returns messageDetailHeaderUiModel
        coEvery {
            toUiModel(
                MessageWithLabelsSample.UnreadInvoice,
                emptyList(),
                FolderColorSettings(),
                AutoDeleteSetting.Disabled
            )
        } returns messageDetailHeaderUiModel
    }
    private val messageDetailFooterUiModelMapper = mockk<MessageDetailFooterUiModelMapper> {
        coEvery {
            toUiModel(MessageWithLabelsSample.Invoice)
        } returns messageDetailFooterUiModel
        coEvery {
            toUiModel(MessageWithLabelsSample.UnreadInvoice)
        } returns messageDetailFooterUiModel
    }

    private val detailReducer = MessageDetailMetadataReducer(
        messageDetailActionBarUiModelMapper,
        messageDetailHeaderUiModelMapper,
        messageDetailFooterUiModelMapper
    )

    @Test
    fun `should produce the expected new state`() = runTest {
        with(testInput) {
            val actualState = detailReducer.newStateFrom(currentState, operation)

            assertEquals(expectedState, actualState, testName)
        }
    }

    companion object {

        private val messageUiModel = MessageDetailActionBarUiModelTestData.buildMessageDetailActionBarUiModel(
            "This email is about subjects"
        )
        private val updatedMessageUiModel = MessageDetailActionBarUiModelTestData.buildMessageDetailActionBarUiModel(
            "[Re] This email is about subjects"
        )

        private val transitionsFromLoadingState = listOf(
            TestInput(
                currentState = MessageMetadataState.Loading,
                operation = MessageDetailEvent.MessageWithLabelsEvent(
                    MessageWithLabelsSample.Invoice,
                    emptyList(),
                    FolderColorSettings(),
                    AutoDeleteSetting.Disabled
                ),
                expectedState = MessageMetadataState.Data(
                    messageUiModel,
                    messageDetailHeaderUiModel,
                    messageDetailFooterUiModel
                )
            )
        )

        private val transitionsFromDataState = listOf(
            TestInput(
                currentState = MessageMetadataState.Data(
                    messageUiModel,
                    messageDetailHeaderUiModel,
                    messageDetailFooterUiModel
                ),
                operation = MessageDetailEvent.MessageWithLabelsEvent(
                    MessageWithLabelsSample.UnreadInvoice,
                    emptyList(),
                    FolderColorSettings(),
                    AutoDeleteSetting.Disabled
                ),
                expectedState = MessageMetadataState.Data(
                    updatedMessageUiModel,
                    messageDetailHeaderUiModel,
                    messageDetailFooterUiModel
                )
            )
        )

        private val transitionsFromErrorState = listOf<TestInput>()

        @JvmStatic
        @Parameterized.Parameters
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
        val currentState: MessageMetadataState,
        val operation: MessageDetailOperation.AffectingMessage,
        val expectedState: MessageMetadataState
    )
}
