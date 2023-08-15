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

package ch.protonmail.android.mailmessage.domain.test

import app.cash.turbine.test
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import ch.protonmail.android.mailmessage.domain.usecase.ObserveMessage
import ch.protonmail.android.testdata.message.MessageTestData
import ch.protonmail.android.testdata.user.UserIdTestData.userId
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ObserveMessageTest {

    private val repository = mockk<MessageRepository> {
        every { this@mockk.observeCachedMessage(userId, any()) } returns flowOf(DataError.Local.NoDataCached.left())
    }

    private val observeMessage = ObserveMessage(repository)

    @Test
    fun `returns local data error when message does not exist in repository`() = runTest {
        // Given
        val messageId = MessageId(MessageTestData.RAW_MESSAGE_ID)
        val error = DataError.Local.NoDataCached
        every { repository.observeCachedMessage(userId, messageId) } returns flowOf(error.left())

        // When
        observeMessage(userId, messageId).test {
            // Then
            assertEquals(error.left(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `returns message when it exists in repository`() = runTest {
        // Given
        val messageId = MessageId(MessageTestData.RAW_MESSAGE_ID)
        val message = MessageTestData.message
        every { repository.observeCachedMessage(userId, messageId) } returns flowOf(message.right())

        // When
        observeMessage(userId, messageId).test {
            // Then
            assertEquals(message.right(), awaitItem())
            awaitComplete()
        }
    }
}
