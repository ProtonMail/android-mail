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

package ch.protonmail.android.mailmessage.data.repository

import app.cash.turbine.test
import ch.protonmail.android.mailmessage.data.remote.resource.UnreadMessageCountResource
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailmessage.data.local.UnreadMessagesCountLocalDataSource
import ch.protonmail.android.mailmessage.data.local.entity.UnreadMessagesCountEntity
import ch.protonmail.android.mailmessage.data.remote.UnreadMessagesCountRemoteDataSource
import ch.protonmail.android.mailmessage.domain.model.UnreadCounter
import io.mockk.Called
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.label.domain.entity.LabelId
import org.junit.Test
import kotlin.test.assertEquals

class UnreadMessagesCountRepositoryImplTest {

    private val localDataSource = mockk<UnreadMessagesCountLocalDataSource>()
    private val remoteDataSource = mockk<UnreadMessagesCountRemoteDataSource>()

    private val repository = UnreadMessageCountRepositoryImpl(localDataSource, remoteDataSource)

    @Test
    fun `refresh message counters from remote when not existing locally`() = runTest {
        // Given
        val expectedMessages = listOf(inboxUnreadMessageCounter)
        coEvery { remoteDataSource.getMessageCounters(userId) } returns listOf(inboxUnreadCounterResource)
        coEvery { localDataSource.observeMessageCounters(userId) } returns flowOf(emptyList())
        coEvery { localDataSource.saveMessageCounters(expectedMessages) } just Runs


        // When
        repository.observeUnreadCounters(userId).test {
            // Then
            awaitItem()
            coVerify { localDataSource.saveMessageCounters(expectedMessages) }
            awaitComplete()
        }

    }

    @Test
    fun `returns local unread counters when available`() = runTest {
        // Given
        val expectedMessages = listOf(inboxUnreadMessageCounter)
        coEvery { localDataSource.observeMessageCounters(userId) } returns flowOf(expectedMessages)

        // When
        repository.observeUnreadCounters(userId).test {
            // Then
            val expected = expectedMessages.map { UnreadCounter(it.labelId, it.unreadCount) }
            assertEquals(expected, awaitItem())
            verify { remoteDataSource wasNot Called }
            awaitComplete()
        }

    }

    companion object TestData {
        private val userId = UserIdSample.Primary

        val inboxUnreadMessageCounter = UnreadMessagesCountEntity(userId, LabelId("0"), 10, 1)

        val inboxUnreadCounterResource = UnreadMessageCountResource("0", 10, 1)
    }
}
