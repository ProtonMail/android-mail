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

package ch.protonmail.android.mailnotifications.data.repository

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailsession.data.repository.MailSessionRepository
import ch.protonmail.android.mailsession.data.wrapper.MailSessionWrapper
import ch.protonmail.android.test.utils.rule.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import uniffi.mail_uniffi.DeviceEnvironment
import uniffi.mail_uniffi.MailBackgroundExecScope
import uniffi.mail_uniffi.MailSession
import uniffi.mail_uniffi.MailSessionRegisterDeviceTaskResult
import uniffi.mail_uniffi.RegisterDeviceTaskHandle
import uniffi.mail_uniffi.RegisteredDevice
import uniffi.mail_uniffi.VoidActionResult
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.assertEquals

internal class DeviceRegistrationRepositoryImplTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private val mailSessionRepository = mockk<MailSessionRepository>()

    private val backgroundExecScope = mockk<MailBackgroundExecScope> {
        justRun { finsihed() }
    }

    private lateinit var repository: DeviceRegistrationRepositoryImpl

    @BeforeTest
    fun setup() {
        repository = DeviceRegistrationRepositoryImpl(mailSessionRepository, dispatcherRule.testDispatcher)
    }

    @AfterTest
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `should proxy the device registration successfully when invoked`() = runTest {
        // Given
        val testToken = "test_token"
        val expectedRegisteredDevice = RegisteredDevice(
            deviceToken = testToken,
            environment = DeviceEnvironment.GOOGLE,
            pingNotificationStatus = null,
            pushNotificationStatus = null
        )

        val mailSessionRegisterDeviceTaskResultOk = mockk<MailSessionRegisterDeviceTaskResult.Ok>()
        val mailSession = mockk<MailSession>()
        val registerDeviceTask = mockk<RegisterDeviceTaskHandle>()

        coEvery { mailSessionRepository.getMailSession() } returns MailSessionWrapper(mailSession)
        every { mailSession.newBackgroundExecutionScope() } returns backgroundExecScope
        every { mailSession.registerDeviceTask() } returns mailSessionRegisterDeviceTaskResultOk
        every { mailSessionRegisterDeviceTaskResultOk.v1 } returns registerDeviceTask
        every { registerDeviceTask.updateDevice(any()) } returns VoidActionResult.Ok

        // When
        val result = repository.registerDeviceToken(testToken)

        // Then
        assertEquals(Unit.right(), result)
        coVerify { mailSession.registerDeviceTask() }
        verify { registerDeviceTask.updateDevice(expectedRegisteredDevice) }
        verify { backgroundExecScope.finsihed() }
    }

    @Test
    fun `should return an error and not call update when register device task errors`() = runTest {
        // Given
        val testToken = "test_token"

        val mailSessionRegisterDeviceTaskResultError = mockk<MailSessionRegisterDeviceTaskResult.Error>()
        val mailSession = mockk<MailSession>()
        val registerDeviceTask = mockk<RegisterDeviceTaskHandle>()

        coEvery { mailSessionRepository.getMailSession() } returns MailSessionWrapper(mailSession)
        every { mailSession.newBackgroundExecutionScope() } returns backgroundExecScope
        every { mailSession.registerDeviceTask() } returns mailSessionRegisterDeviceTaskResultError
        every { mailSessionRegisterDeviceTaskResultError.v1 } returns mockk()

        // When
        val result = repository.registerDeviceToken(testToken)

        // Then
        assertEquals(DataError.Remote.Unknown.left(), result)
        coVerify { mailSession.registerDeviceTask() }
        verify(exactly = 0) { registerDeviceTask.updateDevice(any()) }
        verify { backgroundExecScope.finsihed() }
    }

    @Test
    fun `should return an error when updating the device fails`() = runTest {
        // Given
        val testToken = "test_token"

        val mailSessionRegisterDeviceTaskResultOk = mockk<MailSessionRegisterDeviceTaskResult.Ok>()
        val mailSession = mockk<MailSession>()
        val registerDeviceTask = mockk<RegisterDeviceTaskHandle>()

        coEvery { mailSessionRepository.getMailSession() } returns MailSessionWrapper(mailSession)
        every { mailSession.newBackgroundExecutionScope() } returns backgroundExecScope
        every { mailSession.registerDeviceTask() } returns mailSessionRegisterDeviceTaskResultOk
        every { mailSessionRegisterDeviceTaskResultOk.v1 } returns registerDeviceTask
        every { registerDeviceTask.updateDevice(any()) } returns VoidActionResult.Error(mockk())

        // When
        val result = repository.registerDeviceToken(testToken)

        // Then
        assertEquals(DataError.Remote.Unknown.left(), result)
        coVerify { mailSession.registerDeviceTask() }
        verify { backgroundExecScope.finsihed() }
    }
}
