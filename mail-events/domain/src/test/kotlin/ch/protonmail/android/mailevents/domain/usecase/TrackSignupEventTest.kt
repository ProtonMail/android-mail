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
import ch.protonmail.android.mailevents.domain.repository.EventsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

internal class TrackSignupEventTest {

    private val eventsRepository = mockk<EventsRepository>(relaxUnitFun = true)

    private val trackSignupEvent = TrackSignupEvent(eventsRepository)

    @Test
    fun `should skip when signup event has already been sent`() = runTest {
        // Given
        coEvery { eventsRepository.hasSignupEventBeenSent() } returns true

        // When
        val result = trackSignupEvent()

        // Then
        assertTrue(result.isRight())
        coVerify(exactly = 0) { eventsRepository.sendEvent(any()) }
    }

    @Test
    fun `should send AccountCreated event when not yet sent`() = runTest {
        // Given
        coEvery { eventsRepository.hasSignupEventBeenSent() } returns false
        coEvery { eventsRepository.sendEvent(any()) } returns Unit.right()

        // When
        val result = trackSignupEvent()

        // Then
        assertTrue(result.isRight())
        coVerify {
            eventsRepository.sendEvent(
                AppEvent.AccountCreated(
                    registrationMethod = null,
                    referralCode = null
                )
            )
        }
    }

    @Test
    fun `should mark signup event sent on success`() = runTest {
        // Given
        coEvery { eventsRepository.hasSignupEventBeenSent() } returns false
        coEvery { eventsRepository.sendEvent(any()) } returns Unit.right()

        // When
        trackSignupEvent()

        // Then
        coVerify { eventsRepository.markSignupEventSent() }
    }

    @Test
    fun `should not mark signup event sent on failure`() = runTest {
        // Given
        coEvery { eventsRepository.hasSignupEventBeenSent() } returns false
        coEvery { eventsRepository.sendEvent(any()) } returns DataError.Remote.Unknown.left()

        // When
        trackSignupEvent()

        // Then
        coVerify(exactly = 0) { eventsRepository.markSignupEventSent() }
    }
}
