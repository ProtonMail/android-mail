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

package ch.protonmail.android.mailsettings.domain

import app.cash.turbine.test
import ch.protonmail.android.mailsettings.domain.usecase.ObservePrimaryUserSettings
import ch.protonmail.android.testdata.user.UserIdTestData
import ch.protonmail.android.testdata.usersettings.UserSettingsTestData
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.ResponseSource.Remote
import me.proton.core.domain.entity.UserId
import me.proton.core.usersettings.domain.repository.UserSettingsRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class ObservePrimaryUserSettingsTest {

    private val userIdFlow = MutableSharedFlow<UserId?>()
    private val accountManager = mockk<AccountManager> {
        every { this@mockk.getPrimaryUserId() } returns userIdFlow
    }

    private val userSettingsRepository = mockk<UserSettingsRepository>()

    private lateinit var observeUserSettings: ObservePrimaryUserSettings

    @Before
    fun setUp() {
        observeUserSettings = ObservePrimaryUserSettings(
            accountManager,
            userSettingsRepository
        )
    }

    @Test
    fun `returns user settings when repository succeeds`() = runTest {
        observeUserSettings.invoke().test {
            // Given
            every { userSettingsRepository.getUserSettingsFlow(UserIdTestData.userId) } returns flowOf(
                DataResult.Success(Remote, UserSettingsTestData.userSettings)
            )

            // When
            primaryUserIdIs(UserIdTestData.userId)

            // Then
            val actual = awaitItem()
            assertEquals(UserSettingsTestData.userSettings, actual)
        }
    }

    @Test
    fun `returns null user settings when repository fails`() = runTest {
        observeUserSettings.invoke().test {
            // Given
            every { userSettingsRepository.getUserSettingsFlow(UserIdTestData.userId) } returns flowOf(
                DataResult.Error.Remote("Error", null)
            )

            // When
            primaryUserIdIs(UserIdTestData.userId)

            // Then
            assertNull(awaitItem())
        }
    }

    @Test
    fun `returns null when there is no valid userId`() = runTest {
        observeUserSettings.invoke().test {
            // Given
            primaryUserIdIs(null)

            // Then
            assertNull(awaitItem())
        }
    }

    private suspend fun primaryUserIdIs(userId: UserId?) {
        userIdFlow.emit(userId)
    }
}
