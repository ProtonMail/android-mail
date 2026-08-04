/*
 * Copyright (c) 2026 Proton Technologies AG
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

package ch.protonmail.android.mailcontentsearch.domain.usecase

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcontentsearch.domain.model.RecentFoundItem
import ch.protonmail.android.mailcontentsearch.domain.model.RecentFoundMailboxItem
import ch.protonmail.android.mailmailbox.domain.mapper.MessageMailboxItemMapper
import ch.protonmail.android.mailmailbox.domain.model.MailboxItem
import ch.protonmail.android.mailmessage.domain.model.Message
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import kotlin.test.Test
import kotlin.test.assertEquals

internal class GetRecentFoundMailboxItemsTest {

    private val userId = UserId("user-1")
    private val getRecentFoundItems = mockk<GetRecentFoundItems>()
    private val messageMailboxItemMapper = mockk<MessageMailboxItemMapper>()

    private val getRecentFoundMailboxItems = GetRecentFoundMailboxItems(
        getRecentFoundItems = getRecentFoundItems,
        messageMailboxItemMapper = messageMailboxItemMapper
    )

    @Test
    fun `maps each recent entry's resolved message to a mailbox row and keeps the recorded query`() = runTest {
        // Given
        val firstMessage = mockk<Message>()
        val secondMessage = mockk<Message>()
        val firstMailboxItem = mockk<MailboxItem>()
        val secondMailboxItem = mockk<MailboxItem>()
        coEvery { getRecentFoundItems(userId, Limit) } returns
            listOf(recent(firstMessage), recent(secondMessage)).right()
        every { messageMailboxItemMapper.toMailboxItem(firstMessage) } returns firstMailboxItem
        every { messageMailboxItemMapper.toMailboxItem(secondMessage) } returns secondMailboxItem

        // When
        val result = getRecentFoundMailboxItems(userId, Limit)

        // Then
        assertEquals(
            listOf(
                RecentFoundMailboxItem(firstMailboxItem, searchQuery = "invoice"),
                RecentFoundMailboxItem(secondMailboxItem, searchQuery = "invoice")
            ).right(),
            result
        )
    }

    @Test
    fun `surfaces a failure to read the recents themselves`() = runTest {
        // Given
        coEvery { getRecentFoundItems(userId, Limit) } returns DataError.Local.Unknown.left()

        // When
        val result = getRecentFoundMailboxItems(userId, Limit)

        // Then
        assertEquals(DataError.Local.Unknown.left(), result)
    }

    private fun recent(message: Message) = RecentFoundItem(
        message = message,
        searchQuery = "invoice",
        lastOpenedAt = 1_700_000_000L
    )

    private companion object {

        const val Limit = 5
    }
}
