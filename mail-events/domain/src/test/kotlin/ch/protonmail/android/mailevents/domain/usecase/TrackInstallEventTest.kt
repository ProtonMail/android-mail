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

package ch.protonmail.android.mailevents.domain.usecase

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailevents.domain.model.AppEvent
import ch.protonmail.android.mailevents.domain.model.InstallReferrer
import ch.protonmail.android.mailevents.domain.repository.AppInstallRepository
import ch.protonmail.android.mailevents.domain.repository.EventsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

internal class TrackInstallEventTest {

    private val eventsRepository = mockk<EventsRepository>(relaxUnitFun = true)
    private val appInstallRepository = mockk<AppInstallRepository>(relaxUnitFun = true)

    private val trackInstallEvent = TrackInstallEvent(eventsRepository, appInstallRepository)

    @Test
    fun `should skip when install event has already been sent`() = runTest {
        // Given
        coEvery { eventsRepository.hasInstallEventBeenSent() } returns true

        // When
        val result = trackInstallEvent()

        // Then
        assertTrue(result.isRight())
        coVerify(exactly = 0) { eventsRepository.sendEvent(any()) }
    }

    @Test
    fun `should send Install event with referrer when not yet sent`() = runTest {
        // Given
        val referrerUrl = "utm_source=google&utm_campaign=test"
        val installReferrer = InstallReferrer(
            referrerUrl = referrerUrl,
            referrerClickTimestampMs = 1000L,
            installBeginTimestampMs = 2000L,
            isGooglePlayInstant = false
        )
        coEvery { eventsRepository.hasInstallEventBeenSent() } returns false
        coEvery { appInstallRepository.getInstallReferrer() } returns installReferrer.right()
        coEvery { eventsRepository.sendEvent(any()) } returns Unit.right()

        // When
        val result = trackInstallEvent()

        // Then
        assertTrue(result.isRight())
        coVerify {
            eventsRepository.sendEvent(
                AppEvent.Install(
                    isReinstall = false,
                    installReceipt = null,
                    installRef = referrerUrl
                )
            )
        }
    }

    @Test
    fun `should send Install event without referrer when referrer is not available`() = runTest {
        // Given
        coEvery { eventsRepository.hasInstallEventBeenSent() } returns false
        coEvery { appInstallRepository.getInstallReferrer() } returns DataError.Remote.Unknown.left()
        coEvery { eventsRepository.sendEvent(any()) } returns Unit.right()

        // When
        val result = trackInstallEvent()

        // Then
        assertTrue(result.isRight())
        coVerify {
            eventsRepository.sendEvent(
                AppEvent.Install(
                    isReinstall = false,
                    installReceipt = null,
                    installRef = null
                )
            )
        }
    }

    @Test
    fun `should send Install event with isReinstall flag set to true`() = runTest {
        // Given
        coEvery { eventsRepository.hasInstallEventBeenSent() } returns false
        coEvery { appInstallRepository.getInstallReferrer() } returns DataError.Remote.Unknown.left()
        coEvery { eventsRepository.sendEvent(any()) } returns Unit.right()

        // When
        val result = trackInstallEvent(isReinstall = true)

        // Then
        assertTrue(result.isRight())
        coVerify {
            eventsRepository.sendEvent(
                AppEvent.Install(
                    isReinstall = true,
                    installReceipt = null,
                    installRef = null
                )
            )
        }
    }

    @Test
    fun `should mark install event sent on success`() = runTest {
        // Given
        coEvery { eventsRepository.hasInstallEventBeenSent() } returns false
        coEvery { appInstallRepository.getInstallReferrer() } returns DataError.Remote.Unknown.left()
        coEvery { eventsRepository.sendEvent(any()) } returns Unit.right()

        // When
        trackInstallEvent()

        // Then
        coVerify { eventsRepository.markInstallEventSent() }
    }

    @Test
    fun `should not mark install event sent on failure`() = runTest {
        // Given
        coEvery { eventsRepository.hasInstallEventBeenSent() } returns false
        coEvery { appInstallRepository.getInstallReferrer() } returns DataError.Remote.Unknown.left()
        coEvery { eventsRepository.sendEvent(any()) } returns DataError.Remote.Unknown.left()

        // When
        trackInstallEvent()

        // Then
        coVerify(exactly = 0) { eventsRepository.markInstallEventSent() }
    }

    @Test
    fun `should return error when repository fails to send event`() = runTest {
        // Given
        val error = DataError.Remote.Unknown
        coEvery { eventsRepository.hasInstallEventBeenSent() } returns false
        coEvery { appInstallRepository.getInstallReferrer() } returns DataError.Remote.Unknown.left()
        coEvery { eventsRepository.sendEvent(any()) } returns error.left()

        // When
        val result = trackInstallEvent()

        // Then
        assertTrue(result.isLeft())
    }
}
