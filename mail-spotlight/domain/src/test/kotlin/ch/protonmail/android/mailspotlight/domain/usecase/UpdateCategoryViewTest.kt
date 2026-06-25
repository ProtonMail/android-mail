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

internal class UpdateCategoryViewTest {

    private val userSessionRepository = mockk<UserSessionRepository>()

    private val updateCategoryView = UpdateCategoryView(userSessionRepository)

    @AfterTest
    fun teardown() {
        clearAllMocks()
    }

    @Test
    fun `updates the category view as enabled`() = runTest {
        // Given
        every { userSessionRepository.observePrimaryUserId() } returns flowOf(UserIdSample)
        coEvery { userSessionRepository.updateCategoryView(UserIdSample, true) } returns Unit.right()

        // When
        val result = updateCategoryView(enabled = true)

        // Then
        assertEquals(Unit.right(), result)
        coVerify(exactly = 1) { userSessionRepository.updateCategoryView(UserIdSample, true) }
    }

    @Test
    fun `updates the category view as disabled`() = runTest {
        // Given
        every { userSessionRepository.observePrimaryUserId() } returns flowOf(UserIdSample)
        coEvery { userSessionRepository.updateCategoryView(UserIdSample, false) } returns Unit.right()

        // When
        val result = updateCategoryView(enabled = false)

        // Then
        assertEquals(Unit.right(), result)
        coVerify(exactly = 1) { userSessionRepository.updateCategoryView(UserIdSample, false) }
    }

    @Test
    fun `returns NoUserSession error when no primary user`() = runTest {
        // Given
        every { userSessionRepository.observePrimaryUserId() } returns flowOf(null)

        // When
        val result = updateCategoryView(enabled = true)

        // Then
        assertEquals(DataError.Local.NoUserSession.left(), result)
        coVerify(exactly = 0) { userSessionRepository.updateCategoryView(any(), any()) }
    }

    @Test
    fun `propagates the error when the session call fails`() = runTest {
        // Given
        every { userSessionRepository.observePrimaryUserId() } returns flowOf(UserIdSample)
        coEvery {
            userSessionRepository.updateCategoryView(UserIdSample, true)
        } returns DataError.Local.NoUserSession.left()

        // When
        val result = updateCategoryView(enabled = true)

        // Then
        assertEquals(DataError.Local.NoUserSession.left(), result)
    }

    private companion object {

        val UserIdSample = UserId("user-id")
    }
}
