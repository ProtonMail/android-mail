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

package ch.protonmail.android.mailspotlight.domain.usecase

import app.cash.turbine.test
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailsession.domain.repository.UserSessionRepository
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

internal class ObserveIsBusinessUserTest {

    private val userSessionRepository = mockk<UserSessionRepository>()

    private val observeIsBusinessUser = ObserveIsBusinessUser(userSessionRepository)

    @AfterTest
    fun teardown() {
        clearAllMocks()
    }

    @Test
    fun `given a business account, when invoked, then emits true`() = runTest {
        // Given
        every { userSessionRepository.observePrimaryUserId() } returns flowOf(UserIdSample)
        coEvery { userSessionRepository.isBusiness(UserIdSample) } returns true.right()

        // When
        observeIsBusinessUser().test {
            // Then
            assertEquals(true.right(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `given a consumer account, when invoked, then emits false`() = runTest {
        // Given
        every { userSessionRepository.observePrimaryUserId() } returns flowOf(UserIdSample)
        coEvery { userSessionRepository.isBusiness(UserIdSample) } returns false.right()

        // When
        observeIsBusinessUser().test {
            // Then
            assertEquals(false.right(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `given no primary user, when invoked, then emits NoUserSession error`() = runTest {
        // Given
        every { userSessionRepository.observePrimaryUserId() } returns flowOf(null)

        // When
        observeIsBusinessUser().test {
            // Then
            assertEquals(DataError.Local.NoUserSession.left(), awaitItem())
            awaitComplete()
        }
        coVerify(exactly = 0) { userSessionRepository.isBusiness(any()) }
    }

    @Test
    fun `given the session call fails, when invoked, then propagates the error`() = runTest {
        // Given
        every { userSessionRepository.observePrimaryUserId() } returns flowOf(UserIdSample)
        coEvery { userSessionRepository.isBusiness(UserIdSample) } returns DataError.Local.NoUserSession.left()

        // When
        observeIsBusinessUser().test {
            // Then
            assertEquals(DataError.Local.NoUserSession.left(), awaitItem())
            awaitComplete()
        }
    }

    private companion object {

        val UserIdSample = UserId("user-id")
    }
}
