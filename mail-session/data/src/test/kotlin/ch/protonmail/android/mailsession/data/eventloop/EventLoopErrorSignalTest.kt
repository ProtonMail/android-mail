/*
 * Copyright (c) 2025 Proton Technologies AG
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

package ch.protonmail.android.mailsession.data.eventloop

import app.cash.turbine.test
import ch.protonmail.android.mailsession.domain.eventloop.EventLoopErrorSignal
import ch.protonmail.android.test.utils.rule.MainDispatcherRule
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import uniffi.mail_uniffi.EventError
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

internal class EventLoopErrorSignalTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var signal: EventLoopErrorSignal

    @BeforeTest
    fun setup() {
        signal = EventLoopErrorSignal()
    }

    @Test
    fun `submit emits error to flow`() = runTest {
        // Given
        val error = mockk<EventError>()

        // When/Then
        signal.observeEventLoopErrors().test {
            signal.submit(error)
            assertEquals(error, awaitItem())
        }
    }

    @Test
    fun `observeEventLoopErrors replays last error`() = runTest {
        // Given
        val error = mockk<EventError>()
        signal.submit(error)

        // When/Then
        signal.observeEventLoopErrors().test {
            assertEquals(error, awaitItem())
        }
    }

    @Test
    fun `drops oldest error when buffer overflows`() = runTest {
        // Given
        val error1 = mockk<EventError>(name = "error1")
        val error2 = mockk<EventError>(name = "error2")
        val error3 = mockk<EventError>(name = "error3")

        // When
        signal.submit(error1)
        signal.submit(error2)
        signal.submit(error3)

        // Then
        signal.observeEventLoopErrors().test {
            assertEquals(error3, awaitItem())
        }
    }
}
