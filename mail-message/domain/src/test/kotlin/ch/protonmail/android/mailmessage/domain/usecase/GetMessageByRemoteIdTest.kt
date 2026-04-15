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

package ch.protonmail.android.mailmessage.domain.usecase

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import ch.protonmail.android.mailmessage.domain.sample.MessageSample
import ch.protonmail.android.testdata.message.rust.RemoteMessageIdSample
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

class GetMessageByRemoteIdTest {

    private val repository = mockk<MessageRepository>()

    private val getMessage = GetMessageByRemoteId(repository)

    @Test
    fun `returns message when repository succeeds`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val message = MessageSample.SepWeatherForecast
        val remoteId = RemoteMessageIdSample.SepWeatherForecast
        coEvery { repository.getMessageByRemoteId(userId, remoteId) } returns message.right()

        // When
        val actual = getMessage(userId, remoteId)

        // Then
        assertEquals(message.right(), actual)
    }

    @Test
    fun `retries up to three times when repository fails`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val error = DataError.Local.NoDataCached
        val remoteId = RemoteMessageIdSample.SepWeatherForecast
        coEvery { repository.getMessageByRemoteId(userId, remoteId) } returns error.left()

        // When
        val actual = getMessage(userId, remoteId)

        // Then
        assertEquals(error.left(), actual)
        coVerify(exactly = 3) { repository.getMessageByRemoteId(userId, remoteId) }
    }

}
