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
import ch.protonmail.android.maillabel.domain.model.MailLabels
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.maillabel.domain.model.toMailLabelSystem
import ch.protonmail.android.maillabel.domain.usecase.ObserveExclusiveMailFolders
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.mailmessage.domain.sample.MessageSample
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

class MoveMessageTest {

    private val userId = UserIdSample.Primary
    private val messageId = MessageIdSample.AugWeatherForecast
    private val fromLabelId = SystemLabelId.Archive.labelId
    private val toLabelId = SystemLabelId.Spam.labelId

    private val messageRepository: MessageRepository = mockk {
        every {
            this@mockk.observeCachedMessage(
                userId,
                messageId
            )
        } returns flowOf(
            MessageSample.AugWeatherForecast.copy(
                labelIds = listOf(fromLabelId)
            ).right()
        )
    }
    private val observeExclusiveMailFolders: ObserveExclusiveMailFolders = mockk {
        every { this@mockk.invoke(userId) } returns flowOf(
            MailLabels(
                systemLabels = SystemLabelId.exclusiveList.map { it.toMailLabelSystem() },
                folders = emptyList(),
                labels = emptyList()
            )
        )
    }
    private val move = MoveMessage(messageRepository, observeExclusiveMailFolders)


    @Test
    fun `when observer messages returns to returns error then return error`() = runTest {
        // Given
        val error = DataError.Local.NoDataCached.left()
        coEvery {
            messageRepository.observeCachedMessage(userId, messageId)
        } returns flowOf(error)

        // When
        val actual = move(userId, messageId, toLabelId)

        // Then
        assertEquals(error, actual)
    }

    @Test
    fun `when move to returns error then return error`() = runTest {
        // Given
        val error = DataError.Local.NoDataCached.left()
        coEvery {
            messageRepository.moveTo(
                userId = userId,
                messageId = messageId,
                fromLabel = fromLabelId,
                toLabel = toLabelId
            )
        } returns error

        // When
        val actual = move(userId, messageId, toLabelId)

        // Then
        assertEquals(error, actual)
    }

    @Test
    fun `when move a message then repository is called with the given data`() = runTest {
        // Given
        val message = MessageSample.AugWeatherForecast.right()
        coEvery {
            messageRepository.moveTo(
                userId,
                messageId,
                fromLabelId,
                toLabelId
            )
        } returns message

        // When
        val actual = move(userId, messageId, toLabelId)

        // Then
        coVerify { messageRepository.moveTo(userId, messageId, fromLabelId, toLabelId) }
        assertEquals(message, actual)
    }
}
