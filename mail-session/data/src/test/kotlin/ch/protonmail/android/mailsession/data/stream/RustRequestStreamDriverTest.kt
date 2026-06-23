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

package ch.protonmail.android.mailsession.data.stream

import ch.protonmail.android.mailsession.data.stream.RustRequestStreamDriver.Poll
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class RustRequestStreamDriverTest {

    @Test
    fun `answers each request in order until the stream closes`() = runTest {
        // Given
        val first = FakeRequest()
        val second = FakeRequest()
        val driver = FakeDriver(listOf(Poll.Request(first), Poll.Request(second), Poll.Closed("done")))

        // When
        driver.loop(this)

        // Then
        assertEquals(listOf(first, second), driver.answered)
    }

    @Test
    fun `disposes every request after answering it`() = runTest {
        // Given
        val first = FakeRequest()
        val second = FakeRequest()
        val driver = FakeDriver(listOf(Poll.Request(first), Poll.Request(second), Poll.Closed("done")))

        // When
        driver.loop(this)

        // Then
        assertTrue(first.closed)
        assertTrue(second.closed)
    }

    @Test
    fun `stops without answering when the stream is already closed`() = runTest {
        // Given
        val driver = FakeDriver(listOf(Poll.Closed("closed")))

        // When
        driver.loop(this)

        // Then
        assertEquals(emptyList(), driver.answered)
    }

    @Test
    fun `concurrent driver keeps consuming requests without waiting for slow answers`() = runTest {
        // Given
        val gate = CompletableDeferred<Unit>()
        val first = FakeRequest()
        val second = FakeRequest()
        val driver = FakeDriver(
            polls = listOf(Poll.Request(first), Poll.Request(second), Poll.Closed("done")),
            concurrent = true,
            onAnswer = { gate.await() }
        )

        // When
        driver.loop(this)

        // Then - the loop has drained the stream while both (gated) answers are still pending
        assertEquals(emptyList(), driver.answered)

        // When the answers are allowed to complete
        gate.complete(Unit)
        advanceUntilIdle()

        // Then both are answered and disposed
        assertEquals(listOf(first, second), driver.answered)
        assertTrue(first.closed && second.closed)
    }

    @Test
    fun `keeps servicing the stream when an answer throws`() = runTest {
        // Given
        val failing = FakeRequest()
        val recovered = FakeRequest()
        val driver = FakeDriver(
            polls = listOf(Poll.Request(failing), Poll.Request(recovered), Poll.Closed("done")),
            onAnswer = { request -> if (request === failing) error("boom") }
        )

        // When
        driver.loop(this)

        // Then
        assertEquals(listOf(recovered), driver.answered)
        assertTrue(failing.closed && recovered.closed)
    }

    @Test
    fun `concurrent driver survives an answer that throws`() = runTest {
        // Given
        val failing = FakeRequest()
        val recovered = FakeRequest()
        val driver = FakeDriver(
            polls = listOf(Poll.Request(failing), Poll.Request(recovered), Poll.Closed("done")),
            concurrent = true,
            onAnswer = { request -> if (request === failing) error("boom") }
        )

        // When
        driver.loop(this)
        advanceUntilIdle()

        // Then
        assertEquals(listOf(recovered), driver.answered)
        assertTrue(failing.closed && recovered.closed)
    }

    private class FakeRequest : AutoCloseable {

        var closed = false
            private set

        override fun close() {
            closed = true
        }
    }

    private class FakeDriver(
        private val polls: List<Poll<FakeRequest>>,
        concurrent: Boolean = false,
        private val onAnswer: suspend (FakeRequest) -> Unit = {}
    ) : RustRequestStreamDriver<FakeRequest>(name = "fake", concurrent = concurrent) {

        val answered = mutableListOf<FakeRequest>()
        private var index = 0

        override suspend fun awaitRequest(): Poll<FakeRequest> = polls[index++]

        override suspend fun answer(request: FakeRequest) {
            onAnswer(request)
            answered += request
        }
    }
}
