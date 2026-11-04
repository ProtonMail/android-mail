/*
 * Copyright (c) 2026 Proton Technologies AG
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

package ch.protonmail.android.mailbyoe.domain.usecase

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailbyoe.domain.model.GmailConnectionSettingsAccess
import ch.protonmail.android.mailbyoe.domain.repository.ByoeGuidanceRepository
import ch.protonmail.android.mailcommon.domain.model.DataError
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

internal class GetGmailConnectionSettingsAccessTest {

    private val byoeGuidanceRepository = mockk<ByoeGuidanceRepository>()
    private val getGmailConnectionSettingsAccess = GetGmailConnectionSettingsAccess(byoeGuidanceRepository)

    @AfterTest
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `given the SDK allows connecting, when getting the access, then it is full`() = runTest {
        // Given
        coEvery { byoeGuidanceRepository.getSettingsAccess(userId) } returns GmailConnectionSettingsAccess.Full.right()

        // When
        val result = getGmailConnectionSettingsAccess(userId)

        // Then
        assertEquals(GmailConnectionSettingsAccess.Full, result)
    }

    @Test
    fun `given the guidance fails, when getting the access, then it is hidden`() = runTest {
        // Given
        coEvery { byoeGuidanceRepository.getSettingsAccess(userId) } returns DataError.Local.NoUserSession.left()

        // When
        val result = getGmailConnectionSettingsAccess(userId)

        // Then
        assertEquals(GmailConnectionSettingsAccess.Hidden, result)
    }

    private companion object {

        val userId = UserId("user")
    }
}
