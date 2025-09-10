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

package ch.protonmail.android.maildetail.presentation.usecase

import arrow.core.NonEmptyList
import arrow.core.left
import arrow.core.nonEmptyListOf
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailconversation.domain.entity.ConversationError
import ch.protonmail.android.maildetail.domain.usecase.ObserveConversationMessages
import ch.protonmail.android.maildetail.presentation.usecase.TestData.conversationId
import ch.protonmail.android.maildetail.presentation.usecase.TestData.localLabelId
import ch.protonmail.android.maildetail.presentation.usecase.TestData.messageId
import ch.protonmail.android.maildetail.presentation.usecase.TestData.userId
import ch.protonmail.android.maillabel.domain.model.ExclusiveLocation
import ch.protonmail.android.maillabel.domain.model.LabelId
import ch.protonmail.android.mailmessage.domain.model.ConversationMessages
import ch.protonmail.android.mailmessage.domain.model.Message
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.testdata.message.MessageTestData
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(Enclosed::class)
internal class GetMessagesInSameExclusiveLocationTest {

    class Tests {

        private val observeConversationMessages = mockk<ObserveConversationMessages>()
        private val getMessagesInSameExclusiveLocation = GetMessagesInSameExclusiveLocation(observeConversationMessages)

        @AfterTest
        fun teardown() {
            unmockkAll()
        }

        @Test
        fun `should return error when messages can not be fetched`() = runTest {
            // Given
            coEvery {
                observeConversationMessages.invoke(userId, conversationId, localLabelId)
            } returns flowOf(ConversationError.UnknownLabel.left())

            // When
            val result = getMessagesInSameExclusiveLocation(userId, conversationId, messageId, localLabelId)

            // Then
            assertEquals(DataError.Local.NoDataCached.left(), result)
        }
    }

    @RunWith(Parameterized::class)
    class ParameterizedTests(
        private val testInput: TestInput
    ) {

        private val observeConversationMessages = mockk<ObserveConversationMessages>()
        private val getMessagesInSameExclusiveLocation = GetMessagesInSameExclusiveLocation(observeConversationMessages)

        @AfterTest
        fun teardown() {
            unmockkAll()
        }

        @Test
        fun `should return the correct count of messages in the location`() = runTest {
            // Given
            coEvery {
                observeConversationMessages.invoke(userId, conversationId, localLabelId)
            } returns flowOf(ConversationMessages(testInput.messagesList, testInput.messageId).right())

            // When
            val result = getMessagesInSameExclusiveLocation(userId, conversationId, testInput.messageId, localLabelId)
                .getOrNull()

            // Then
            assertEquals(testInput.expectedCount, result?.size)
        }

        companion object {
            data class TestInput(
                val testName: String,
                val messagesList: NonEmptyList<Message>,
                val messageId: MessageId,
                val expectedCount: Int
            )

            @JvmStatic
            @Parameterized.Parameters(name = "{0}")
            fun data() = listOf(
                TestInput(
                    testName = "should return 1 message when there's only 1 message in the conversation",
                    messagesList = nonEmptyListOf(
                        MessageTestData.message.copy(
                            messageId = messageId,
                            exclusiveLocation = ExclusiveLocation.Folder(name = "name", localLabelId, "#FFFFFF")
                        )
                    ),
                    messageId = messageId,
                    expectedCount = 1
                ),
                TestInput(
                    testName = "should return more than 1 messages when multiple match the exclusive location",
                    messagesList = nonEmptyListOf(
                        MessageTestData.message.copy(
                            messageId = messageId,
                            exclusiveLocation = ExclusiveLocation.Folder(name = "name", localLabelId, "#FFFFFF")
                        ),
                        MessageTestData.message.copy(
                            messageId = MessageId("message-id-2"),
                            exclusiveLocation = ExclusiveLocation.Folder(name = "name", localLabelId, "#FFFFFF")
                        )
                    ),
                    messageId = messageId,
                    expectedCount = 2
                )
            )
        }
    }
}

private object TestData {

    val userId = UserId("user-id")
    val conversationId = ConversationId("conversation-id")
    val messageId = MessageId("message-id")
    val localLabelId = LabelId("label-id")
}
