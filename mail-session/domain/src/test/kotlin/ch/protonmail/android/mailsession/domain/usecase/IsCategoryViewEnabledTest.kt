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

package ch.protonmail.android.mailsession.domain.usecase

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailsession.domain.repository.UserSessionRepository
import ch.protonmail.android.testdata.user.UserIdTestData
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

class IsCategoryViewEnabledTest {

    private val userId = UserIdTestData.userId
    private val userSessionRepository = mockk<UserSessionRepository>()

    private val isCategoryViewEnabled = IsCategoryViewEnabled(userSessionRepository)

    @Test
    fun `should return the value resolved by the session`() = runTest {
        // Given
        coEvery { userSessionRepository.isCategoryViewEnabled(userId) } returns true.right()

        // Then
        assertEquals(true, isCategoryViewEnabled(userId))
    }

    @Test
    fun `should return false when the session resolution fails`() = runTest {
        // Given
        coEvery {
            userSessionRepository.isCategoryViewEnabled(userId)
        } returns DataError.Local.NoUserSession.left()

        // Then
        assertEquals(false, isCategoryViewEnabled(userId))
    }
}
