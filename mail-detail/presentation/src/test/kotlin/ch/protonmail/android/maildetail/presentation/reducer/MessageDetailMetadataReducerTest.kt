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
import ch.protonmail.android.maildetail.presentation.model.MessageViewAction
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
class MessageDetailMetadataReducerTest(
    private val testInput: TestInput
) {

    private val messageDetailActionBarUiModelMapper = mockk<MessageDetailActionBarUiModelMapper> {
        every { toUiModel(MessageSample.Invoice) } returns notStarredActionBarUiModel
    }
    private val messageDetailHeaderUiModelMapper = mockk<MessageDetailHeaderUiModelMapper> {
        coEvery {
            toUiModel(MessageWithLabelsSample.Invoice, emptyList(), FolderColorSettings(), AutoDeleteSetting.Disabled)
        } returns messageDetailHeaderUiModel
    }
    private val messageDetailFooterUiModelMapper = mockk<MessageDetailFooterUiModelMapper> {
        coEvery {
            toUiModel(MessageWithLabelsSample.Invoice)
        } returns messageDetailFooterUiModel
    }

    private val detailReducer = MessageDetailMetadataReducer(
        messageDetailActionBarUiModelMapper,
        messageDetailHeaderUiModelMapper,
        messageDetailFooterUiModelMapper
    )

    @Test
    fun `should produce the expected new state`() = runTest {
        val actualState = detailReducer.newStateFrom(testInput.currentState, testInput.operation)

        assertEquals(testInput.expectedState, actualState)
    }

    companion object {

        private val notStarredActionBarUiModel =
            MessageDetailActionBarUiModelTestData.buildMessageDetailActionBarUiModel(
                subject = "This email is about subjects",
                isStarred = false
            )
        private val starredActionBarUiModel = notStarredActionBarUiModel.copy(isStarred = true)

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
                    notStarredActionBarUiModel,
                    messageDetailHeaderUiModel,
                    messageDetailFooterUiModel
                )
            ).toArray(),
            TestInput(
                currentState = MessageMetadataState.Loading,
                operation = MessageViewAction.Star,
                expectedState = MessageMetadataState.Loading
            ).toArray(),
            TestInput(
                currentState = MessageMetadataState.Loading,
                operation = MessageDetailEvent.ErrorAddingStar,
                expectedState = MessageMetadataState.Loading
            ).toArray(),
            TestInput(
                currentState = MessageMetadataState.Loading,
                operation = MessageDetailEvent.ErrorRemovingStar,
                expectedState = MessageMetadataState.Loading
            ).toArray()
        )

        private val transitionsFromDataState = listOf(
            TestInput(
                currentState = MessageMetadataState.Data(
                    notStarredActionBarUiModel,
                    messageDetailHeaderUiModel,
                    messageDetailFooterUiModel
                ),
                operation = MessageViewAction.Star,
                expectedState = MessageMetadataState.Data(
                    starredActionBarUiModel,
                    messageDetailHeaderUiModel,
                    messageDetailFooterUiModel
                )
            ).toArray(),
            TestInput(
                currentState = MessageMetadataState.Data(
                    starredActionBarUiModel,
                    messageDetailHeaderUiModel,
                    messageDetailFooterUiModel
                ),
                operation = MessageDetailEvent.ErrorAddingStar,
                expectedState = MessageMetadataState.Data(
                    notStarredActionBarUiModel,
                    messageDetailHeaderUiModel,
                    messageDetailFooterUiModel
                )
            ).toArray(),
            TestInput(
                currentState = MessageMetadataState.Data(
                    notStarredActionBarUiModel,
                    messageDetailHeaderUiModel,
                    messageDetailFooterUiModel
                ),
                operation = MessageDetailEvent.ErrorRemovingStar,
                expectedState = MessageMetadataState.Data(
                    starredActionBarUiModel,
                    messageDetailHeaderUiModel,
                    messageDetailFooterUiModel
                )
            ).toArray()
        )

        @JvmStatic
        @Parameterized.Parameters
        fun data(): Collection<Array<TestInput>> = transitionsFromLoadingState + transitionsFromDataState
    }

    class TestInput(
        val currentState: MessageMetadataState,
        val operation: MessageDetailOperation.AffectingMessage,
        val expectedState: MessageMetadataState
    ) {

        fun toArray() = arrayOf(this)
    }
}
