/*
 * Copyright (c) 2021 Proton Technologies AG
 * This file is part of Proton Technologies AG and ProtonMail.
 *
 * ProtonMail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonMail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonMail.  If not, see <https://www.gnu.org/licenses/>.
 */

package ch.protonmail.android.mailmessage.data

import ch.protonmail.android.mailmessage.data.repository.MessageRepositoryImpl
import ch.protonmail.android.mailmessage.domain.repository.MessageLocalDataSource
import ch.protonmail.android.mailmessage.domain.repository.MessageRemoteDataSource
import ch.protonmail.android.mailpagination.domain.entity.PageFilter
import ch.protonmail.android.mailpagination.domain.entity.PageKey
import io.mockk.Ordering
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import org.junit.Before
import org.junit.Test
import java.io.IOException
import kotlin.test.assertEquals

class MessageRepositoryImplTest {

    private val userId = UserId("1")

    private val remoteDataSource = mockk<MessageRemoteDataSource> {
        coEvery { this@mockk.getMessages(any(), any()) } returns listOf(
            getMessage(id = "1", time = 1000),
            getMessage(id = "2", time = 2000),
            getMessage(id = "3", time = 3000),
            getMessage(id = "4", time = 4000),
        )
    }
    private val localDataSource = mockk<MessageLocalDataSource> {
        coEvery { this@mockk.getMessages(any(), any()) } returns emptyList()
        coEvery { this@mockk.isLocalPageValid(any(), any(), any()) } returns false
        coEvery { this@mockk.upsertMessages(any(), any(), any()) } just Runs
    }

    private lateinit var messageRepository: MessageRepositoryImpl

    @Before
    fun setUp() {
        messageRepository = MessageRepositoryImpl(remoteDataSource, localDataSource)
    }

    @Test
    fun `return remote if local page is invalid`() = runTest {
        // Given
        val pageKey = PageKey()
        val localMessages = listOf(
            getMessage(id = "1", time = 1000),
        )
        val remoteMessages = listOf(
            getMessage(id = "1", time = 1000),
            getMessage(id = "2", time = 2000),
            getMessage(id = "3", time = 3000),
        )
        coEvery { localDataSource.getMessages(any(), any()) } returns localMessages
        coEvery { localDataSource.isLocalPageValid(any(), any(), any()) } returns false
        coEvery { localDataSource.getClippedPageKey(any(), any()) } returns pageKey
        coEvery { remoteDataSource.getMessages(any(), any()) } returns remoteMessages

        // When
        val result = messageRepository.getMessages(userId, pageKey)

        // Then
        assertEquals(3, result.size)
        coVerify(exactly = 1) { localDataSource.isLocalPageValid(userId, pageKey, localMessages) }
        coVerify(exactly = 1) { remoteDataSource.getMessages(userId, pageKey) }
        coVerify(exactly = 1) { localDataSource.upsertMessages(userId, pageKey, remoteMessages) }
    }

    @Test
    fun `return local if remote fail`() = runTest {
        // Given
        val pageKey = PageKey()
        val localMessages = listOf(
            getMessage(id = "1", time = 1000),
            getMessage(id = "2", time = 2000),
        )
        coEvery { localDataSource.getMessages(any(), any()) } returns localMessages
        coEvery { localDataSource.isLocalPageValid(any(), any(), any()) } returns false
        coEvery { localDataSource.getClippedPageKey(any(), any()) } returns pageKey
        coEvery { remoteDataSource.getMessages(any(), any()) } throws IOException()

        // When
        val result = messageRepository.getMessages(userId, pageKey)

        // Then
        assertEquals(2, result.size)
        coVerify(exactly = 1) { localDataSource.isLocalPageValid(userId, pageKey, localMessages) }
        coVerify(exactly = 1) { remoteDataSource.getMessages(userId, pageKey) }
    }

    @Test
    fun `return local if valid`() = runTest {
        // Given
        val pageKey = PageKey()
        val localMessages = listOf(
            getMessage(id = "1", time = 1000),
            getMessage(id = "2", time = 2000),
        )
        val remoteMessages = listOf(
            getMessage(id = "1", time = 1000),
            getMessage(id = "2", time = 2000),
            getMessage(id = "3", time = 3000),
        )
        coEvery { localDataSource.getMessages(any(), any()) } returns localMessages
        coEvery { localDataSource.isLocalPageValid(any(), any(), any()) } returns true
        coEvery { remoteDataSource.getMessages(any(), any()) } returns remoteMessages

        // When
        val messages = messageRepository.getMessages(userId, pageKey)

        // Then
        assertEquals(2, messages.size)
        coVerify(exactly = 1) { localDataSource.isLocalPageValid(userId, pageKey, localMessages) }
        coVerify(exactly = 0) { remoteDataSource.getMessages(any(), any()) }
    }

    @Test
    fun `clip pageKey before calling remote`() = runTest {
        // Given
        val pageKey = PageKey()
        val clippedPageKey = PageKey(filter = PageFilter(minTime = 0))
        coEvery { localDataSource.getMessages(any(), any()) } returns emptyList()
        coEvery { localDataSource.isLocalPageValid(any(), any(), any()) } returns false
        coEvery { localDataSource.getClippedPageKey(any(), any()) } returns clippedPageKey
        coEvery { remoteDataSource.getMessages(any(), any()) } returns emptyList()
        // When
        val messages = messageRepository.getMessages(userId, pageKey)

        // Then
        assertEquals(0, messages.size)
        coVerify(exactly = 1) { localDataSource.isLocalPageValid(any(), any(), any()) }
        coVerify(ordering = Ordering.ORDERED) {
            localDataSource.getClippedPageKey(userId, pageKey)
            remoteDataSource.getMessages(userId, clippedPageKey)
        }
    }
}
