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

package ch.protonmail.android.mailsettings.domain

import app.cash.turbine.test
import ch.protonmail.android.mailsettings.domain.testdata.MailSettingsTestData
import ch.protonmail.android.mailsettings.domain.testdata.UserIdTestData
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.DataResult.Error
import me.proton.core.domain.arch.DataResult.Success
import me.proton.core.domain.arch.ResponseSource.Local
import me.proton.core.domain.entity.UserId
import me.proton.core.mailsettings.domain.entity.MailSettings
import me.proton.core.mailsettings.domain.repository.MailSettingsRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.function.ThrowingRunnable
import java.io.IOException

class ObserveMailSettingsTest {
    private val userIdFlow by lazy { MutableSharedFlow<UserId?>() }
    private val accountManager = mockk<AccountManager> {
        every { this@mockk.getPrimaryUserId() } returns userIdFlow
    }

    private val mailSettingsFlow = MutableSharedFlow<DataResult<MailSettings>>()
    private val mailSettingsRepository = mockk<MailSettingsRepository> {
        every { this@mockk.getMailSettingsFlow(UserIdTestData.userId) } returns mailSettingsFlow
    }

    private lateinit var observeMailSettings: ObserveMailSettings

    @Before
    fun setUp() {
        observeMailSettings = ObserveMailSettings(
            accountManager,
            mailSettingsRepository
        )
    }

    @Test
    fun `returns mail settings when repository returns valid mail settings`() = runTest {
        observeMailSettings.invoke().test {
            // Given
            primaryUserIdIs(UserIdTestData.userId)

            // When
            mailSettingsFlow.emit(Success(Local, MailSettingsTestData.mailSettings))

            // Then
            val actual = awaitItem()
            assertEquals(MailSettingsTestData.mailSettings, actual)
        }
    }

    @Test
    fun `returns null when repository returns an error`() = runTest {
        observeMailSettings.invoke().test {
            // Given
            primaryUserIdIs(UserIdTestData.userId)

            // When
            mailSettingsFlow.emit(Error.Local("Test-IOException", IOException("Test")))

            // Then
            val actual = awaitItem()
            assertNull(actual)
        }
    }

    @Test
    fun `returns nothing when there is no valid userId`() = runTest {
        observeMailSettings.invoke().test {
            // Given
            primaryUserIdIs(null)

            // Then
            val thrownError = assertThrows(
                AssertionError::class.java,
                ThrowingRunnable { expectMostRecentItem() }
            )
            assertEquals("No item was found", thrownError.message)
        }
    }


    private suspend fun primaryUserIdIs(userId: UserId?) {
        userIdFlow.emit(userId)
    }

}
