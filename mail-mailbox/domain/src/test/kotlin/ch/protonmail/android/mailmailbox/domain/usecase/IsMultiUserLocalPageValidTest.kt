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

package ch.protonmail.android.mailmailbox.domain.usecase

import ch.protonmail.android.mailmailbox.domain.model.MailboxItemType
import ch.protonmail.android.mailmailbox.domain.model.MailboxPageKey
import ch.protonmail.android.mailpagination.domain.model.PageKey
import io.mockk.coEvery
import io.mockk.coVerifySequence
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IsMultiUserLocalPageValidTest {

    private val userId1 = UserId("user1")
    private val userId2 = UserId("user2")

    private val isLocalPageValid: IsLocalPageValid = mockk()

    private val isMultiUserLocalPageValid = IsMultiUserLocalPageValid(isLocalPageValid)

    @Test
    fun `returns false when local page is not valid for any user`() = runTest {
        // given
        val type = MailboxItemType.Conversation
        val pageKey = PageKey()
        val mailboxPageKey = MailboxPageKey(listOf(userId1, userId2), pageKey)
        coEvery { isLocalPageValid(userId1, type, pageKey) } returns true
        coEvery { isLocalPageValid(userId2, type, pageKey) } returns false

        // when
        val result = isMultiUserLocalPageValid(type, mailboxPageKey)

        // then
        coVerifySequence {
            isLocalPageValid(userId1, type, pageKey)
            isLocalPageValid(userId2, type, pageKey)
        }
        assertFalse(result)
    }

    @Test
    fun `returns true when local page is valid for all users`() = runTest {
        // given
        val type = MailboxItemType.Message
        val pageKey = PageKey()
        val mailboxPageKey = MailboxPageKey(listOf(userId1, userId2), pageKey)
        coEvery { isLocalPageValid(userId1, type, pageKey) } returns true
        coEvery { isLocalPageValid(userId2, type, pageKey) } returns true

        // when
        val result = isMultiUserLocalPageValid(type, mailboxPageKey)

        // then
        coVerifySequence {
            isLocalPageValid(userId1, type, pageKey)
            isLocalPageValid(userId2, type, pageKey)
        }
        assertTrue(result)
    }

    @Test
    fun `returns true when no user ids are given`() = runTest {
        // given
        val type = MailboxItemType.Message
        val pageKey = PageKey()
        val mailboxPageKey = MailboxPageKey(emptyList(), pageKey)

        // when
        val result = isMultiUserLocalPageValid(type, mailboxPageKey)

        // then
        assertTrue(result)
    }
}
