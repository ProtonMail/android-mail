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

package ch.protonmail.android.mailsettings.data.local

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailsession.data.wrapper.MailSessionWrapper
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import uniffi.mail_uniffi.AppAppearance
import uniffi.mail_uniffi.AppProtection
import uniffi.mail_uniffi.AutoLock
import uniffi.mail_uniffi.MailSession
import uniffi.mail_uniffi.MailSessionGetAppSettingsResult
import uniffi.mail_uniffi.SessionReason
import uniffi.mail_uniffi.UserSessionError

class RustAppSettingsDataSourceTest {

    private val mockAppSettings = uniffi.mail_uniffi.AppSettings(
        AppAppearance.LIGHT_MODE,
        AppProtection.PIN,
        AutoLock.Always,
        useCombineContacts = true,
        useAlternativeRouting = true
    )

    val mailSession = mockk<MailSession> {
        coEvery { this@mockk.getAppSettings() } returns MailSessionGetAppSettingsResult.Ok(mockAppSettings)
    }

    val mailSessionWrapper = mockk<MailSessionWrapper> {
        coEvery { this@mockk.getRustMailSession() } returns mailSession
    }


    private val sut = RustAppSettingsDataSource()

    @Test
    fun `when getAppSettings then returns AppSettings right`() = runTest {

        val result = sut.getAppSettings(mailSessionWrapper)
        assertEquals(mockAppSettings.right(), result)
    }

    @Test
    fun `when getAppSettings and error then returns mapped error`() = runTest {
        val expectedError = DataError.Local.NotFound
        coEvery { mailSession.getAppSettings() } returns
            MailSessionGetAppSettingsResult.Error(
                UserSessionError.Reason(
                    SessionReason.UnknownLabel
                )
            )
        val result = sut.getAppSettings(mailSessionWrapper)
        assertEquals(expectedError.left(), result)
    }
}
