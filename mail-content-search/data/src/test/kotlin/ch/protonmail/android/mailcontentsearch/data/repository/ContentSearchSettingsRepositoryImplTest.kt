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
import ch.protonmail.android.mailcontentsearch.domain.model.ContentIndexingState
import ch.protonmail.android.mailsession.data.usecase.ExecuteWithUserSession
import ch.protonmail.android.mailsession.domain.repository.UserSessionRepository
import ch.protonmail.android.mailsession.domain.wrapper.MailUserSessionWrapper
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import uniffi.mail_uniffi.ContentSearchIndexingStatus
import kotlin.test.Test
import kotlin.test.assertEquals

internal class ContentSearchSettingsRepositoryImplTest {

    private val userId = UserId("user-1")
    private val dispatcher = UnconfinedTestDispatcher()
    private val wrapper = mockk<MailUserSessionWrapper>()

    private val userSessionRepository = mockk<UserSessionRepository> {
        coEvery { getUserSession(userId) } returns wrapper
    }
    private val executeWithUserSession = ExecuteWithUserSession(userSessionRepository, dispatcher)

    private val repository = ContentSearchSettingsRepositoryImpl(
        executeWithUserSession = executeWithUserSession,
        ioDispatcher = dispatcher
    )

    @Test
    fun `isEnabled returns the value reported by the session`() = runTest {
        // Given
        coEvery { wrapper.contentSearchIsEnabled() } returns true.right()

        // When
        val result = repository.isEnabled(userId)

        // Then
        assertEquals(true.right(), result)
    }

    @Test
    fun `isEnabled propagates the session error`() = runTest {
        // Given
        coEvery { wrapper.contentSearchIsEnabled() } returns DataError.Local.Unknown.left()

        // When
        val result = repository.isEnabled(userId)

        // Then
        assertEquals(DataError.Local.Unknown.left(), result)
    }

    @Test
    fun `setEnabled forwards the value to the session`() = runTest {
        // Given
        coEvery { wrapper.contentSearchSetEnabled(true) } returns mockk()

        // When
        val result = repository.setEnabled(userId, true)

        // Then
        assertEquals(Unit.right(), result)
        coVerify { wrapper.contentSearchSetEnabled(true) }
    }

    @Test
    fun `clearLocalData delegates to the session`() = runTest {
        // Given
        coEvery { wrapper.contentSearchClearLocalData() } returns mockk()

        // When
        val result = repository.clearLocalData(userId)

        // Then
        assertEquals(Unit.right(), result)
        coVerify { wrapper.contentSearchClearLocalData() }
    }

    @Test
    fun `getIndexingStatus maps the session status to the domain state`() = runTest {
        // Given
        coEvery { wrapper.contentSearchGetIndexingStatus() } returns ContentSearchIndexingStatus.COMPLETED.right()
        coEvery { wrapper.contentSearchGetIndexingProgress() } returns DataError.Local.Unknown.left()

        // When
        val result = repository.getIndexingStatus(userId)

        // Then
        assertEquals(ContentIndexingState.Completed, result)
    }

    @Test
    fun `getIndexingStatus falls back to Idle when the session has no status`() = runTest {
        // Given
        coEvery { wrapper.contentSearchGetIndexingStatus() } returns DataError.Local.Unknown.left()

        // When
        val result = repository.getIndexingStatus(userId)

        // Then
        assertEquals(ContentIndexingState.Idle, result)
    }

    @Test
    fun `observeIsEnabled emits the current value on start`() = runTest {
        // Given
        coEvery { wrapper.contentSearchIsEnabled() } returns true.right()

        // When + Then
        repository.observeIsEnabled(userId).test {
            assertEquals(true, awaitItem())
        }
    }
}
