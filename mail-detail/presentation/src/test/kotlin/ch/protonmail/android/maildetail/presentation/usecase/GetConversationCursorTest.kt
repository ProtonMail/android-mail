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

import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailcommon.domain.model.CursorId
import ch.protonmail.android.mailcommon.domain.model.CursorResult
import ch.protonmail.android.mailcommon.domain.model.EphemeralMailboxCursor
import ch.protonmail.android.mailcommon.domain.repository.ConversationCursor
import ch.protonmail.android.mailcommon.domain.repository.EphemeralMailboxCursorRepository
import ch.protonmail.android.mailmailbox.domain.usecase.SetEphemeralMailboxCursor
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import org.junit.Assert.assertEquals
import kotlin.test.Test

internal class GetConversationCursorTest {

    private val setEphemeralMailboxCursor = mockk<SetEphemeralMailboxCursor>(relaxed = true)
    private val mockEphemeralMailboxCursorRepository = mockk<EphemeralMailboxCursorRepository>()
    private val userId = UserId("userID")
    private val conversationId = ConversationId("conversationId")
    private val messageId = "messageId"

    @Test
    fun `returns cursor when returned by flow and conversation matches`() = runTest {
        // Given
        val mockCursor = mockk<ConversationCursor> {
            every { current } returns CursorResult.Cursor(conversationId, null)
        }
        val expected = EphemeralMailboxCursor.Data(mockCursor)
        val mockFlow = flowOf(expected)
        every { mockEphemeralMailboxCursorRepository.observeCursor() } returns mockFlow

        // When
        val result = GetConversationCursor(mockEphemeralMailboxCursorRepository, setEphemeralMailboxCursor)
            .invoke(userId, false, conversationId, messageId, false)
            .first()

        // Then
        assertEquals(expected, result)
        coVerify(exactly = 0) { setEphemeralMailboxCursor.invoke(any(), any(), any()) }
    }

    @Test
    fun `given cursorflow returns state of null then setEphemeralMailboxCursor`() = runTest {
        // Given
        val mockCursor = mockk<ConversationCursor> {
            every { current } returns CursorResult.Cursor(conversationId, null)
        }
        val expected = EphemeralMailboxCursor.Data(mockCursor)
        val mockFlow = flowOf(null, expected)
        every { mockEphemeralMailboxCursorRepository.observeCursor() } returns mockFlow

        // When
        val result = GetConversationCursor(mockEphemeralMailboxCursorRepository, setEphemeralMailboxCursor)
            .invoke(userId, false, conversationId, messageId, false)
            .toList()

        // Then
        coVerify { setEphemeralMailboxCursor.invoke(userId, false, CursorId(conversationId, messageId)) }
        assertEquals(EphemeralMailboxCursor.Initialising, result.first())
        assertEquals(expected, result[1])
    }

    @Test
    fun `given cursorflow returns state of not Initialised then setEphemeralMailboxCursor`() = runTest {
        // Given
        val mockCursor = mockk<ConversationCursor> {
            every { current } returns CursorResult.Cursor(conversationId, null)
        }
        val expected = EphemeralMailboxCursor.Data(mockCursor)
        val mockFlow = flowOf(EphemeralMailboxCursor.NotInitalised, expected)
        every { mockEphemeralMailboxCursorRepository.observeCursor() } returns mockFlow

        // When
        val result = GetConversationCursor(mockEphemeralMailboxCursorRepository, setEphemeralMailboxCursor)
            .invoke(userId, false, conversationId, messageId, false)
            .toList()

        // Then
        coVerify { setEphemeralMailboxCursor.invoke(userId, false, CursorId(conversationId, messageId)) }
        assertEquals(EphemeralMailboxCursor.Initialising, result.first())
        assertEquals(expected, result[1])
    }

    @Test
    fun `given cursor is for different conversation then reinitialize cursor`() = runTest {
        // Given
        val oldConversationId = ConversationId("oldConversationId")
        val newConversationId = ConversationId("newConversationId")

        val oldCursor = mockk<ConversationCursor> {
            every { current } returns CursorResult.Cursor(oldConversationId, null)
        }
        val newCursor = mockk<ConversationCursor> {
            every { current } returns CursorResult.Cursor(newConversationId, null)
        }

        val oldData = EphemeralMailboxCursor.Data(oldCursor)
        val newData = EphemeralMailboxCursor.Data(newCursor)
        val mockFlow = flowOf(oldData, newData)
        every { mockEphemeralMailboxCursorRepository.observeCursor() } returns mockFlow

        // When
        val result = GetConversationCursor(mockEphemeralMailboxCursorRepository, setEphemeralMailboxCursor)
            .invoke(userId, false, newConversationId, messageId, false)
            .toList()

        // Then
        coVerify { setEphemeralMailboxCursor.invoke(userId, false, CursorId(newConversationId, messageId)) }
        assertEquals(EphemeralMailboxCursor.Initialising, result.first())
        assertEquals(newData, result[1])
    }

    @Test
    fun `given cursor current is End then does not reinitialize`() = runTest {
        // Given
        val mockCursor = mockk<ConversationCursor> {
            every { current } returns CursorResult.End
        }
        val expected = EphemeralMailboxCursor.Data(mockCursor)
        val mockFlow = flowOf(expected)
        every { mockEphemeralMailboxCursorRepository.observeCursor() } returns mockFlow

        // When
        val result = GetConversationCursor(mockEphemeralMailboxCursorRepository, setEphemeralMailboxCursor)
            .invoke(userId, false, conversationId, messageId, false)
            .first()

        // Then
        assertEquals(expected, result)
        coVerify(exactly = 0) { setEphemeralMailboxCursor.invoke(any(), any(), any()) }
    }

    @Test
    fun `given cursor current is Error then does not reinitialize`() = runTest {
        // Given
        val mockCursor = mockk<ConversationCursor> {
            every { current } returns CursorResult.Error(mockk())
        }
        val expected = EphemeralMailboxCursor.Data(mockCursor)
        val mockFlow = flowOf(expected)
        every { mockEphemeralMailboxCursorRepository.observeCursor() } returns mockFlow

        // When
        val result = GetConversationCursor(mockEphemeralMailboxCursorRepository, setEphemeralMailboxCursor)
            .invoke(userId, false, conversationId, messageId, false)
            .first()

        // Then
        assertEquals(expected, result)
        coVerify(exactly = 0) { setEphemeralMailboxCursor.invoke(any(), any(), any()) }
    }

    @Test
    fun `passes messageId to setEphemeralMailboxCursor when initializing`() = runTest {
        // Given
        val specificMessageId = "specificMessageId"
        val mockCursor = mockk<ConversationCursor> {
            every { current } returns CursorResult.Cursor(conversationId, null)
        }
        val expected = EphemeralMailboxCursor.Data(mockCursor)
        val mockFlow = flowOf(null, expected)
        every { mockEphemeralMailboxCursorRepository.observeCursor() } returns mockFlow

        // When
        GetConversationCursor(mockEphemeralMailboxCursorRepository, setEphemeralMailboxCursor)
            .invoke(userId, false, conversationId, specificMessageId, false)
            .toList()

        // Then
        coVerify {
            setEphemeralMailboxCursor.invoke(
                userId,
                false,
                CursorId(conversationId, specificMessageId)
            )
        }
    }

    @Test
    fun `passes null messageId to setEphemeralMailboxCursor when messageId is null`() = runTest {
        // Given
        val mockCursor = mockk<ConversationCursor> {
            every { current } returns CursorResult.Cursor(conversationId, null)
        }
        val expected = EphemeralMailboxCursor.Data(mockCursor)
        val mockFlow = flowOf(null, expected)
        every { mockEphemeralMailboxCursorRepository.observeCursor() } returns mockFlow

        // When
        GetConversationCursor(mockEphemeralMailboxCursorRepository, setEphemeralMailboxCursor)
            .invoke(userId, false, conversationId, messageId = null, false)
            .toList()

        // Then
        coVerify {
            setEphemeralMailboxCursor.invoke(
                userId,
                false,
                CursorId(conversationId, null)
            )
        }
    }

    @Test
    fun `passes viewModeIsConversationMode correctly when initializing`() = runTest {
        // Given
        val mockCursor = mockk<ConversationCursor> {
            every { current } returns CursorResult.Cursor(conversationId, null)
        }
        val expected = EphemeralMailboxCursor.Data(mockCursor)
        val mockFlow = flowOf(null, expected)
        every { mockEphemeralMailboxCursorRepository.observeCursor() } returns mockFlow

        // When
        GetConversationCursor(mockEphemeralMailboxCursorRepository, setEphemeralMailboxCursor)
            .invoke(userId, false, conversationId, messageId, locationViewModeIsConversation = true)
            .toList()

        // Then
        coVerify {
            setEphemeralMailboxCursor.invoke(
                userId,
                true,
                CursorId(conversationId, messageId)
            )
        }
    }
}
