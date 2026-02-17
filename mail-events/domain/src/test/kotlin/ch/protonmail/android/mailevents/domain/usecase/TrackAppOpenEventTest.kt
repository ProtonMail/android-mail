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

internal class TrackAppOpenEventTest {

    private val eventsRepository = mockk<EventsRepository>()

    private val trackAppOpenEvent = TrackAppOpenEvent(eventsRepository)

    @Test
    fun `should send AppOpen event with new session flag set to true`() = runTest {
        // Given
        coEvery { eventsRepository.sendEvent(any()) } returns Unit.right()

        // When
        val result = trackAppOpenEvent(isNewSession = true)

        // Then
        assertTrue(result.isRight())
        coVerify {
            eventsRepository.sendEvent(AppEvent.AppOpen(isNewSession = true))
        }
    }

    @Test
    fun `should send AppOpen event with new session flag set to false`() = runTest {
        // Given
        coEvery { eventsRepository.sendEvent(any()) } returns Unit.right()

        // When
        val result = trackAppOpenEvent(isNewSession = false)

        // Then
        assertTrue(result.isRight())
        coVerify {
            eventsRepository.sendEvent(AppEvent.AppOpen(isNewSession = false))
        }
    }

    @Test
    fun `should return error when repository fails to send event`() = runTest {
        // Given
        val error = DataError.Remote.Unknown
        coEvery { eventsRepository.sendEvent(any()) } returns error.left()

        // When
        val result = trackAppOpenEvent(isNewSession = true)

        // Then
        assertTrue(result.isLeft())
        coVerify {
            eventsRepository.sendEvent(AppEvent.AppOpen(isNewSession = true))
        }
    }
}
