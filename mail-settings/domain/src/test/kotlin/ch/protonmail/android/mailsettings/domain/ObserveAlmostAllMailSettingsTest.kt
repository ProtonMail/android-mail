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
import ch.protonmail.android.mailsettings.domain.usecase.ObserveAlmostAllMailSettings
import ch.protonmail.android.testdata.mailsettings.MailSettingsTestData.buildMailSettings
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.ResponseSource
import me.proton.core.domain.entity.UserId
import me.proton.core.domain.type.IntEnum
import me.proton.core.mailsettings.domain.entity.AlmostAllMail
import me.proton.core.mailsettings.domain.entity.MailSettings
import me.proton.core.mailsettings.domain.repository.MailSettingsRepository
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class ObserveAlmostAllMailSettingsTest {

    private val userId = UserId("1")

    private val mutableMailSettings = MutableSharedFlow<DataResult<MailSettings>>(replay = 1)
    private val mailSettingsRepository = mockk<MailSettingsRepository> {
        coEvery { getMailSettingsFlow(any()) } returns mutableMailSettings
    }

    private lateinit var usecase: ObserveAlmostAllMailSettings

    @Before
    fun setUp() {
        usecase = ObserveAlmostAllMailSettings(mailSettingsRepository)
    }

    @Test
    fun `return correct value on success`() = runTest {
        // Given
        mutableMailSettings.emit(
            DataResult.Success(
                source = ResponseSource.Local,
                value = buildMailSettings(
                    almostAllMail = IntEnum(1, AlmostAllMail.Enabled)
                )
            )
        )

        // When
        usecase.invoke(userId).test {
            // Then
            val item = awaitItem()
            assertEquals(expected = true, actual = true)
        }
    }

    @Test
    fun `return default value on error`() = runTest {
        // Given
        mutableMailSettings.emit(DataResult.Error.Local(message = "Error", cause = null))

        // When
        usecase.invoke(userId).test {
            // Then
            val item = awaitItem()
            assertEquals(expected = false, actual = false)
        }
    }
}
