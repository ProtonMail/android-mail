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

package ch.protonmail.android.mailconversation.data.wrapper

import ch.protonmail.android.mailcommon.domain.model.CursorResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test
import uniffi.mail_uniffi.Conversation
import uniffi.mail_uniffi.Id
import uniffi.mail_uniffi.MailConversationCursor
import uniffi.mail_uniffi.MailConversationCursorFetchNextResult
import uniffi.mail_uniffi.NextMailCursorConversation

internal class ConversationCursorWrapperTest {

    private val conversationCursor = mockk<MailConversationCursor>()
    private val sut = ConversationCursorWrapper(conversationCursor)

    @Test
    fun `test when None then get End`() = runTest {
        coEvery { conversationCursor.peekNext() } returns NextMailCursorConversation.None

        val mockMessage = mockk<Conversation>()
        every { mockMessage.id } returns Id(1.toULong())
        coEvery { conversationCursor.fetchNext() } returns MailConversationCursorFetchNextResult.Ok(mockMessage)

        val result = sut.nextPage()
        Assert.assertEquals(result, CursorResult.End)
        coVerify(exactly = 0) { conversationCursor.fetchNext() }
    }

    @Test
    fun `test when Maybe then fetch async`() = runTest {
        coEvery { conversationCursor.peekNext() } returns NextMailCursorConversation.Maybe

        val mockMessage = mockk<Conversation>()
        every { mockMessage.id } returns Id(1.toULong())
        coEvery { conversationCursor.fetchNext() } returns MailConversationCursorFetchNextResult.Ok(mockMessage)

        val result = sut.nextPage()
        Assert.assertEquals((result as CursorResult.Cursor).conversationId.id, "1")
        coVerify { conversationCursor.fetchNext() }
    }

    @Test
    fun `test when Some then do not fetch async`() = runTest {
        val mockMessage = mockk<Conversation>()
        every { mockMessage.id } returns Id(1.toULong())
        coEvery { conversationCursor.peekNext() } returns NextMailCursorConversation.Some(mockMessage)

        val result = sut.nextPage()
        Assert.assertEquals((result as CursorResult.Cursor).conversationId.id, "1")
        coVerify(exactly = 0) { conversationCursor.fetchNext() }
    }
}
