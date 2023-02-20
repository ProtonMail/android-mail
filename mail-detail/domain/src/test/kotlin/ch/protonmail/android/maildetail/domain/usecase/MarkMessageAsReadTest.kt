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

package ch.protonmail.android.maildetail.domain.usecase

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.mailmessage.domain.sample.MessageSample
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

internal class MarkMessageAsReadTest {

    private val messageRepository: MessageRepository = mockk()
    private val markRead = MarkMessageAsRead(messageRepository)

    @Test
    fun `when repository fails then error is returned`() = runTest {
        // given
        val error = DataError.Local.NoDataCached.left()
        coEvery { messageRepository.markRead(any(), any()) } returns error

        // when
        val result = markRead(UserIdSample.Primary, MessageIdSample.Invoice)

        // then
        assertEquals(error, result)
    }

    @Test
    fun `when repository succeed then message is returned`() = runTest {
        // given
        val message = MessageSample.Invoice.right()
        coEvery { messageRepository.markRead(any(), any()) } returns message

        // when
        val result = markRead(UserIdSample.Primary, MessageIdSample.Invoice)

        // then
        assertEquals(message, result)
    }
}
