/*
 * Copyright (c) 2025 Proton Technologies AG
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

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.ConversationCursorError
import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailcommon.domain.model.CursorId
import ch.protonmail.android.mailcommon.domain.repository.ConversationCursor
import ch.protonmail.android.mailconversation.domain.repository.ConversationRepository
import ch.protonmail.android.maillabel.domain.model.LabelId
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import org.junit.Assert.assertEquals
import kotlin.test.Test

internal class GetConversationCursorTest {

    private val conversationRepository = mockk<ConversationRepository>()
    private val messageRepository = mockk<MessageRepository>()

    private val userId = UserId("userID")
    private val conversationId = ConversationId("conversationId")
    private val messageId = "messageId"
    private val labelId = LabelId("1")

    @Test
    fun `gets cursor from conversation repository when viewModeIsConversation is true`() = runTest {
        // Given
        val expected = mockk<ConversationCursor>()
        coEvery {
            conversationRepository.getConversationCursor(
                userId = userId,
                anchorItemId = CursorId(conversationId, messageId),
                labelId = labelId,
                categoryLabelId = null
            )
        } returns expected.right()

        // When
        val result = GetConversationCursor(
            conversationRepository = conversationRepository,
            messageRepository = messageRepository
        ).invoke(
            userId = userId,
            conversationId = conversationId,
            messageId = messageId,
            locationViewModeIsConversation = true,
            labelId = labelId,
            categoryLabelId = null
        )

        // Then
        assertEquals(expected.right(), result)
        coVerify(exactly = 1) {
            conversationRepository.getConversationCursor(
                userId = userId,
                anchorItemId = CursorId(conversationId, messageId),
                labelId = labelId,
                categoryLabelId = null
            )
        }
        coVerify(exactly = 0) {
            messageRepository.getConversationCursor(any(), any(), any(), null)
        }
    }

    @Test
    fun `gets cursor from message repository when viewModeIsConversation is false`() = runTest {
        // Given
        val expected = mockk<ConversationCursor>()
        coEvery {
            messageRepository.getConversationCursor(
                userId = userId,
                anchorItemId = CursorId(conversationId, messageId),
                labelId = labelId,
                categoryLabelId = null
            )
        } returns expected.right()

        // When
        val result = GetConversationCursor(
            conversationRepository = conversationRepository,
            messageRepository = messageRepository
        ).invoke(
            userId = userId,
            conversationId = conversationId,
            messageId = messageId,
            locationViewModeIsConversation = false,
            labelId = labelId,
            categoryLabelId = null
        )

        // Then
        assertEquals(expected.right(), result)
        coVerify(exactly = 1) {
            messageRepository.getConversationCursor(
                userId = userId,
                anchorItemId = CursorId(conversationId, messageId),
                labelId = labelId,
                categoryLabelId = null
            )
        }
        coVerify(exactly = 0) {
            conversationRepository.getConversationCursor(any(), any(), any(), null)
        }
    }

    @Test
    fun `returns error from conversation repository`() = runTest {
        // Given
        val expected = ConversationCursorError.InvalidState
        coEvery {
            conversationRepository.getConversationCursor(
                userId = userId,
                anchorItemId = CursorId(conversationId, messageId),
                labelId = labelId,
                categoryLabelId = null
            )
        } returns expected.left()

        // When
        val result = GetConversationCursor(
            conversationRepository = conversationRepository,
            messageRepository = messageRepository
        ).invoke(
            userId = userId,
            conversationId = conversationId,
            messageId = messageId,
            locationViewModeIsConversation = true,
            labelId = labelId,
            categoryLabelId = null
        )

        // Then
        assertEquals(expected.left(), result)
        coVerify(exactly = 1) {
            conversationRepository.getConversationCursor(
                userId = userId,
                anchorItemId = CursorId(conversationId, messageId),
                labelId = labelId,
                categoryLabelId = null
            )
        }
    }

    @Test
    fun `returns error from message repository`() = runTest {
        // Given
        val expected = ConversationCursorError.InvalidState
        coEvery {
            messageRepository.getConversationCursor(
                userId = userId,
                anchorItemId = CursorId(conversationId, messageId),
                labelId = labelId,
                categoryLabelId = null
            )
        } returns expected.left()

        // When
        val result = GetConversationCursor(
            conversationRepository = conversationRepository,
            messageRepository = messageRepository
        ).invoke(
            userId = userId,
            conversationId = conversationId,
            messageId = messageId,
            locationViewModeIsConversation = false,
            labelId = labelId,
            categoryLabelId = null
        )

        // Then
        assertEquals(expected.left(), result)
        coVerify(exactly = 1) {
            messageRepository.getConversationCursor(
                userId = userId,
                anchorItemId = CursorId(conversationId, messageId),
                labelId = labelId,
                categoryLabelId = null
            )
        }
    }
}
