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

package ch.protonmail.android.mailmessage.data.wrapper

import ch.protonmail.android.mailcommon.domain.model.CursorResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import uniffi.mail_uniffi.Id
import uniffi.mail_uniffi.MailMessageCursor
import uniffi.mail_uniffi.MailMessageCursorFetchNextResult
import uniffi.mail_uniffi.Message
import uniffi.mail_uniffi.NextMailCursorMessage
import kotlin.test.Test
import kotlin.test.assertEquals

internal class MailMessageCursorWrapperTest {

    private val mailMessageCursor = mockk<MailMessageCursor>()
    private val sut = MailMessageCursorWrapper(mailMessageCursor)

    @Test
    fun `test when None then get End`() = runTest {
        coEvery { mailMessageCursor.peekNext() } returns NextMailCursorMessage.None

        val mockMessage = mockk<Message>()
        every { mockMessage.id } returns Id(1.toULong())
        coEvery { mailMessageCursor.fetchNext() } returns MailMessageCursorFetchNextResult.Ok(mockMessage)

        val result = sut.nextPage()
        assertEquals(result, CursorResult.End)
        coVerify(exactly = 0) { mailMessageCursor.fetchNext() }
    }

    @Test
    fun `test when Maybe then fetch async`() = runTest {
        coEvery { mailMessageCursor.peekNext() } returns NextMailCursorMessage.Maybe

        val mockMessage = mockk<Message>()
        every { mockMessage.id } returns Id(1.toULong())
        every { mockMessage.conversationId } returns Id(2.toULong())
        coEvery { mailMessageCursor.fetchNext() } returns MailMessageCursorFetchNextResult.Ok(mockMessage)

        val result = sut.nextPage()
        assertEquals((result as CursorResult.Cursor).conversationId.id, "2")
        coVerify { mailMessageCursor.fetchNext() }
    }

    @Test
    fun `test when Some then do not fetch async`() = runTest {
        val mockMessage = mockk<Message>()
        every { mockMessage.id } returns Id(1.toULong())
        every { mockMessage.conversationId } returns Id(2.toULong())
        coEvery { mailMessageCursor.peekNext() } returns NextMailCursorMessage.Some(mockMessage)

        val result = sut.nextPage()
        assertEquals((result as CursorResult.Cursor).conversationId.id, "2")
        coVerify(exactly = 0) { mailMessageCursor.fetchNext() }
    }
}
