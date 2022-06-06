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

package ch.protonmail.android.mailsettings.domain.usecase

import app.cash.turbine.test
import ch.protonmail.android.testdata.user.UserIdTestData.userId
import ch.protonmail.android.testdata.usersettings.UserSettingsTestData
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.ResponseSource
import me.proton.core.usersettings.domain.repository.UserSettingsRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class ObserveUserSettingsTest {

    private val userSettingsRepository: UserSettingsRepository = mockk {
        every { getUserSettingsFlow(userId) } returns flowOf(
            DataResult.Success(ResponseSource.Local, UserSettingsTestData.userSettings)
        )
    }
    private val observeUserSettings = ObserveUserSettings(userSettingsRepository)

    @Test
    fun `return correct user settings`() = runTest {
        // when
        observeUserSettings(userId).test {

            // then
            val expected = UserSettingsTestData.userSettings
            assertEquals(expected, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `return null on error`() = runTest {
        // given
        every { userSettingsRepository.getUserSettingsFlow(userId) } returns flowOf(
            DataResult.Error.Local(cause = null, message = "error")
        )

        // when
        observeUserSettings(userId).test {

            // then
            assertNull(awaitItem())
            awaitComplete()
        }
    }
}
