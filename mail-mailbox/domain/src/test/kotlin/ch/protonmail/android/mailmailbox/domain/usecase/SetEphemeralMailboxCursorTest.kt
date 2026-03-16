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

package ch.protonmail.android.mailmailbox.domain.usecase

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.ConversationCursorError
import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailcommon.domain.model.CursorId
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.repository.ConversationCursor
import ch.protonmail.android.mailcommon.domain.repository.EphemeralMailboxCursorRepository
import ch.protonmail.android.mailconversation.domain.repository.ConversationRepository
import ch.protonmail.android.maillabel.domain.model.LabelId
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import org.junit.Test

class SetEphemeralMailboxCursorTest {

    private val mockConversationRepository = mockk<ConversationRepository>()
    private val mockMessageRepository = mockk<MessageRepository>()
    private val repository = mockk<EphemeralMailboxCursorRepository>(relaxed = true)
    private val conversationCursor = mockk<ConversationCursor>()
    private val labelId = LabelId("0")
    private val userId = UserId("userId")

    @Test
    fun `when viewModeIsConversation getCursor from Conversation repository`() = runTest {
        coEvery {
            mockConversationRepository.getConversationCursor(
                any(),
                userId,
                labelId
            )
        } returns conversationCursor.right()

        SetEphemeralMailboxCursor(mockConversationRepository, mockMessageRepository, repository)
            .invoke(userId, true, CursorId(ConversationId("conversationID")), labelId)

        coVerify(exactly = 1) {
            mockConversationRepository.getConversationCursor(
                CursorId(ConversationId("conversationID")),
                userId, labelId
            )
        }
        coVerify(exactly = 0) { mockMessageRepository.getConversationCursor(any(), userId, labelId) }
        coVerify(exactly = 1) { repository.setEphemeralCursor(conversationCursor) }
    }

    @Test
    fun `when viewModeIsConversation false getCursor from MessageRepository repository`() = runTest {
        coEvery {
            mockMessageRepository.getConversationCursor(
                any(),
                userId,
                labelId
            )
        } returns conversationCursor.right()

        SetEphemeralMailboxCursor(mockConversationRepository, mockMessageRepository, repository)
            .invoke(userId, false, CursorId(ConversationId("conversationID")), labelId)

        coVerify(exactly = 1) {
            mockMessageRepository.getConversationCursor(
                CursorId(ConversationId("conversationID")),
                userId, labelId
            )
        }
        coVerify(exactly = 0) { mockConversationRepository.getConversationCursor(any(), userId, labelId) }
        coVerify(exactly = 1) { repository.setEphemeralCursor(conversationCursor) }
    }

    @Test
    fun `when error returned then do not setEphemeralCursor`() = runTest {
        coEvery {
            mockMessageRepository.getConversationCursor(
                any(),
                userId,
                labelId
            )
        } returns ConversationCursorError.Other(
            DataError.Local.Unknown
        ).left()

        SetEphemeralMailboxCursor(mockConversationRepository, mockMessageRepository, repository)
            .invoke(userId, false, CursorId(ConversationId("conversationID")), labelId)

        coVerify(exactly = 1) {
            mockMessageRepository.getConversationCursor(
                CursorId(ConversationId("conversationID")),
                userId, labelId
            )
        }
        coVerify(exactly = 0) { repository.setEphemeralCursor(conversationCursor) }
    }
}
