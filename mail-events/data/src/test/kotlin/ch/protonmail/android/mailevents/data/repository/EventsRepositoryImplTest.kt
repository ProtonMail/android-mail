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

package ch.protonmail.android.mailevents.data.repository

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailevents.data.local.MailEventsDataSource
import ch.protonmail.android.mailevents.data.remote.EventsDataSource
import ch.protonmail.android.mailevents.data.remote.model.EventPayload
import ch.protonmail.android.mailevents.domain.model.AppEvent
import ch.protonmail.android.mailevents.domain.model.AppInfo
import ch.protonmail.android.mailevents.domain.model.DeviceInfo
import ch.protonmail.android.mailevents.domain.repository.AppInfoProvider
import ch.protonmail.android.mailevents.domain.repository.DeviceInfoProvider
import ch.protonmail.android.mailevents.domain.usecase.IsNewAppInstall
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

internal class EventsRepositoryImplTest {

    private val mailEventsDataSource = mockk<MailEventsDataSource>(relaxUnitFun = true)
    private val eventsDataSource = mockk<EventsDataSource>()
    private val appInfoProvider = mockk<AppInfoProvider>()
    private val deviceInfoProvider = mockk<DeviceInfoProvider>()
    private val isNewAppInstall = mockk<IsNewAppInstall>()

    private val repository = EventsRepositoryImpl(
        mailEventsDataSource = mailEventsDataSource,
        eventsDataSource = eventsDataSource,
        appInfoProvider = appInfoProvider,
        deviceInfoProvider = deviceInfoProvider,
        isNewAppInstall = isNewAppInstall
    )

    private val testAppInfo = AppInfo(
        packageName = "ch.protonmail.android",
        identifier = "ch.protonmail.android",
        version = "1.0.0"
    )

    private val testDeviceInfo = DeviceInfo(
        platform = DeviceInfo.PLATFORM_ANDROID,
        osVersion = "14",
        locale = "en_US",
        languageCode = "en",
        make = "Google",
        model = "Pixel"
    )

    @Test
    fun `should skip event when not eligible`() = runTest {
        // Given
        coEvery { isNewAppInstall() } returns false
        coEvery { mailEventsDataSource.hasInstallEventBeenSent() } returns false

        // When
        val result = repository.sendEvent(AppEvent.AppOpen(isNewSession = true))

        // Then
        assertTrue(result.isLeft())
        coVerify(exactly = 0) { eventsDataSource.sendEvent(any()) }
    }

    @Test
    fun `should send event when is new app install`() = runTest {
        // Given
        coEvery { isNewAppInstall() } returns true
        coEvery { mailEventsDataSource.getOrCreateAsid() } returns "test-asid".right()
        every { appInfoProvider.getAppInfo() } returns testAppInfo
        coEvery { deviceInfoProvider.getDeviceInfo() } returns testDeviceInfo
        coEvery { eventsDataSource.sendEvent(any()) } returns Unit.right()

        // When
        val result = repository.sendEvent(AppEvent.AppOpen(isNewSession = true))

        // Then
        assertTrue(result.isRight())
        coVerify(exactly = 1) { eventsDataSource.sendEvent(match { it is EventPayload.Open }) }
    }

    @Test
    fun `should send event when install event has been sent`() = runTest {
        // Given
        coEvery { isNewAppInstall() } returns false
        coEvery { mailEventsDataSource.hasInstallEventBeenSent() } returns true
        coEvery { mailEventsDataSource.getOrCreateAsid() } returns "test-asid".right()
        every { appInfoProvider.getAppInfo() } returns testAppInfo
        coEvery { deviceInfoProvider.getDeviceInfo() } returns testDeviceInfo
        coEvery { eventsDataSource.sendEvent(any()) } returns Unit.right()

        // When
        val result = repository.sendEvent(AppEvent.AppOpen(isNewSession = true))

        // Then
        assertTrue(result.isRight())
        coVerify(exactly = 1) { eventsDataSource.sendEvent(match { it is EventPayload.Open }) }
    }

    @Test
    fun `should return error when getOrCreateAsid fails`() = runTest {
        // Given
        coEvery { isNewAppInstall() } returns true
        coEvery { mailEventsDataSource.getOrCreateAsid() } returns DataError.Local.Unknown.left()

        // When
        val result = repository.sendEvent(AppEvent.AppOpen(isNewSession = true))

        // Then
        assertTrue(result.isLeft())
        coVerify(exactly = 0) { eventsDataSource.sendEvent(any()) }
    }

    @Test
    fun `should return error when sendEvent fails`() = runTest {
        // Given
        coEvery { isNewAppInstall() } returns true
        coEvery { mailEventsDataSource.getOrCreateAsid() } returns "test-asid".right()
        every { appInfoProvider.getAppInfo() } returns testAppInfo
        coEvery { deviceInfoProvider.getDeviceInfo() } returns testDeviceInfo
        coEvery { eventsDataSource.sendEvent(any()) } returns DataError.Remote.Unknown.left()

        // When
        val result = repository.sendEvent(AppEvent.AppOpen(isNewSession = true))

        // Then
        assertTrue(result.isLeft())
        coVerify(exactly = 1) { eventsDataSource.sendEvent(match { it is EventPayload.Open }) }
    }
}
