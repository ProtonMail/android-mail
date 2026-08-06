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

package ch.protonmail.android.mailcontentsearch.data.repository

import app.cash.turbine.test
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailsession.data.wrapper.SyncServiceWrapper
import ch.protonmail.android.mailsession.data.usecase.ExecuteWithUserSession
import ch.protonmail.android.mailsession.domain.repository.UserSessionRepository
import ch.protonmail.android.mailsession.domain.wrapper.MailUserSessionWrapper
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import kotlin.test.Test
import kotlin.test.assertEquals

internal class ContentSearchSettingsRepositoryImplTest {

    private val userId = UserId("user-1")
    private val dispatcher = UnconfinedTestDispatcher()
    private val wrapper = mockk<MailUserSessionWrapper>()
    private val syncServiceWrapper = mockk<SyncServiceWrapper>()

    private val userSessionRepository = mockk<UserSessionRepository> {
        coEvery { getUserSession(userId) } returns wrapper
    }
    private val executeWithUserSession = ExecuteWithUserSession(userSessionRepository, dispatcher)

    private val repository = ContentSearchSettingsRepositoryImpl(
        executeWithUserSession = executeWithUserSession,
        syncService = syncServiceWrapper,
        ioDispatcher = dispatcher
    )

    @Test
    fun `isEnabled returns the value reported by the sync service`() = runTest {
        // Given
        coEvery { syncServiceWrapper.isEnabled(wrapper) } returns true.right()

        // When
        val result = repository.isEnabled(userId)

        // Then
        assertEquals(true.right(), result)
    }

    @Test
    fun `isEnabled propagates the sync service error`() = runTest {
        // Given
        coEvery { syncServiceWrapper.isEnabled(wrapper) } returns DataError.Local.Unknown.left()

        // When
        val result = repository.isEnabled(userId)

        // Then
        assertEquals(DataError.Local.Unknown.left(), result)
    }

    @Test
    fun `setEnabled forwards the value to the sync service`() = runTest {
        // Given
        coEvery { syncServiceWrapper.setEnabled(wrapper, true) } returns Unit.right()

        // When
        val result = repository.setEnabled(userId, true)

        // Then
        assertEquals(Unit.right(), result)
        coVerify { syncServiceWrapper.setEnabled(wrapper, true) }
    }

    @Test
    fun `observeIsEnabled emits the current value on start`() = runTest {
        // Given
        coEvery { syncServiceWrapper.isEnabled(wrapper) } returns true.right()

        // When + Then
        repository.observeIsEnabled(userId).test {
            assertEquals(true, awaitItem())
        }
    }
}
