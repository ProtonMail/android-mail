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

import java.io.IOException
import app.cash.turbine.test
import ch.protonmail.android.mailsettings.domain.usecase.ObserveMailSettings
import ch.protonmail.android.testdata.mailsettings.MailSettingsTestData
import ch.protonmail.android.testdata.user.UserIdTestData.userId
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.DataResult.Error
import me.proton.core.domain.arch.DataResult.Success
import me.proton.core.domain.arch.ResponseSource.Local
import me.proton.core.mailsettings.domain.entity.MailSettings
import me.proton.core.mailsettings.domain.repository.MailSettingsRepository
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ObserveMailSettingsTest {

    private val mailSettingsFlow = MutableSharedFlow<DataResult<MailSettings>>()
    private val mailSettingsRepository = mockk<MailSettingsRepository> {
        every { this@mockk.getMailSettingsFlow(userId) } returns mailSettingsFlow
    }

    private lateinit var observeMailSettings: ObserveMailSettings

    @BeforeTest
    fun setUp() {
        observeMailSettings = ObserveMailSettings(
            mailSettingsRepository
        )
    }

    @Test
    fun `returns mail settings when repository returns valid mail settings`() = runTest {
        // Given
        observeMailSettings(userId).test {

            // When
            mailSettingsFlow.emit(Success(Local, MailSettingsTestData.mailSettings))

            // Then
            val actual = awaitItem()
            assertEquals(MailSettingsTestData.mailSettings, actual)
        }
    }

    @Test
    fun `returns null when repository returns an error`() = runTest {
        // Given
        observeMailSettings(userId).test {

            // When
            mailSettingsFlow.emit(Error.Local("Test-IOException", IOException("Test")))

            // Then
            val actual = awaitItem()
            assertNull(actual)
        }
    }
}
