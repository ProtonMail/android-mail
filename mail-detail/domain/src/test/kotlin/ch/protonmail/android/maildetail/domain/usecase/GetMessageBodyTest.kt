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
import ch.protonmail.android.mailmessage.domain.entity.MessageId
import ch.protonmail.android.mailmessage.domain.entity.MessageWithBody
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import ch.protonmail.android.testdata.message.MessageBodyTestData
import ch.protonmail.android.testdata.message.MessageTestData
import ch.protonmail.android.testdata.user.UserIdTestData
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GetMessageBodyTest {

    private val messageId = MessageId("messageId")

    private val messageRepository = mockk<MessageRepository> {
        coEvery { getMessageWithBody(UserIdTestData.userId, messageId) } returns
            MessageWithBody(
                MessageTestData.message,
                MessageBodyTestData.messageBody
            ).right()
    }

    private val getMessageBody = GetMessageBody(messageRepository)

    @Test
    fun `when repository method returns a message body then the use case returns the message body`() = runTest {
        // Given
        val expected = MessageBodyTestData.messageBody.right()

        // When
        val actual = getMessageBody(UserIdTestData.userId, messageId)

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `when repository method returns an error then the use case returns the error`() = runTest {
        // Given
        coEvery {
            messageRepository.getMessageWithBody(UserIdTestData.userId, messageId)
        } returns DataError.Local.NoDataCached.left()
        val expected = DataError.Local.NoDataCached.left()

        // When
        val actual = getMessageBody(UserIdTestData.userId, messageId)

        // Then
        assertEquals(expected, actual)
    }
}
