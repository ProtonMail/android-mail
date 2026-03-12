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

import ch.protonmail.android.mailevents.data.local.MailEventsDataSource
import ch.protonmail.android.mailevents.data.referrer.InstallReferrerDataSource
import ch.protonmail.android.mailevents.data.remote.EventsDataSource
import ch.protonmail.android.mailevents.domain.model.AppEvent
import ch.protonmail.android.mailevents.domain.repository.AppInfoProvider
import ch.protonmail.android.mailevents.domain.repository.DeviceInfoProvider
import ch.protonmail.android.mailevents.domain.usecase.IsNewAppInstall
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

internal class EventsRepositoryImplTest {

    private val mailEventsDataSource = mockk<MailEventsDataSource>(relaxUnitFun = true)
    private val installReferrerDataSource = mockk<InstallReferrerDataSource>()
    private val eventsDataSource = mockk<EventsDataSource>()
    private val appInfoProvider = mockk<AppInfoProvider>()
    private val deviceInfoProvider = mockk<DeviceInfoProvider>()
    private val isNewAppInstall = mockk<IsNewAppInstall>()

    private val repository = EventsRepositoryImpl(
        mailEventsDataSource = mailEventsDataSource,
        installReferrerDataSource = installReferrerDataSource,
        eventsDataSource = eventsDataSource,
        appInfoProvider = appInfoProvider,
        deviceInfoProvider = deviceInfoProvider,
        isNewAppInstall = isNewAppInstall
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
}
