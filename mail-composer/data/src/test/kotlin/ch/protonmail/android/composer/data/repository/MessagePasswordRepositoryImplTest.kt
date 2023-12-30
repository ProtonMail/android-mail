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

package ch.protonmail.android.composer.data.repository

import app.cash.turbine.test
import arrow.core.right
import ch.protonmail.android.composer.data.local.MessagePasswordLocalDataSource
import ch.protonmail.android.mailcomposer.domain.model.MessagePassword
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.testdata.user.UserIdTestData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class MessagePasswordRepositoryImplTest {

    val userId = UserIdTestData.userId
    val messageId = MessageIdSample.NewDraftWithSubjectAndBody

    private val messagePasswordLocalDataSource = mockk<MessagePasswordLocalDataSource>()

    private val messagePasswordRepository = MessagePasswordRepositoryImpl(messagePasswordLocalDataSource)

    @Test
    fun `should call method from local data source when saving message password`() = runTest {
        // Given
        val password = "password"
        val hint = "hint"
        val messagePassword = MessagePassword(userId, messageId, password, hint)
        coEvery { messagePasswordLocalDataSource.save(messagePassword) } returns Unit.right()

        // When
        val actual = messagePasswordRepository.saveMessagePassword(messagePassword)

        // Then
        coVerify {
            messagePasswordLocalDataSource.save(messagePassword)
        }
        assertEquals(Unit.right(), actual)
    }

    @Test
    fun `should call method from local data source when updating message password`() = runTest {
        // Given
        val password = "password"
        val hint = "hint"
        coEvery { messagePasswordLocalDataSource.update(userId, messageId, password, hint) } returns Unit.right()

        // When
        val actual = messagePasswordRepository.updateMessagePassword(userId, messageId, password, hint)

        // Then
        coVerify {
            messagePasswordLocalDataSource.update(userId, messageId, password, hint)
        }
        assertEquals(Unit.right(), actual)
    }

    @Test
    fun `should call method from local data source when observing message password`() = runTest {
        // Given
        val password = "password"
        val hint = "hint"
        val messagePassword = MessagePassword(userId, messageId, password, hint)
        coEvery { messagePasswordLocalDataSource.observe(userId, messageId) } returns flowOf(messagePassword)

        // When
        messagePasswordRepository.observeMessagePassword(userId, messageId).test {
            // Then
            coVerify {
                messagePasswordLocalDataSource.observe(userId, messageId)
            }
            assertEquals(messagePassword, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `should call delete method from local data source when deleting message password`() = runTest {
        // Given
        coEvery { messagePasswordLocalDataSource.delete(userId, messageId) } just runs

        // When
        messagePasswordRepository.deleteMessagePassword(userId, messageId)

        // Then
        coVerify { messagePasswordLocalDataSource.delete(userId, messageId) }
    }
}
