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

package ch.protonmail.android.mailpagination.data.repository

import app.cash.turbine.test
import ch.protonmail.android.mailpagination.domain.model.PageInvalidationEvent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class InMemoryPageInvalidationRepositoryImplTest {

    private val repository = InMemoryPageInvalidationRepositoryImpl()

    @Test
    fun `submit should emit event to observer`() = runTest {
        // Given
        val testEvent = PageInvalidationEvent.ConversationsInvalidated()

        // When
        repository.observePageInvalidationEvents().test {
            repository.submit(testEvent)

            // Then
            assertEquals(testEvent, awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `late subscription should still receive events emitted before observing`() = runTest {
        // Given
        val event1 = PageInvalidationEvent.ConversationsInvalidated()
        val event2 = PageInvalidationEvent.ConversationsInvalidated()

        // When: emit BEFORE subscribing
        repository.submit(event1)
        repository.submit(event2)

        // Then: subscriber should still receive both in order (channel buffered)
        repository.observePageInvalidationEvents().test {
            assertEquals(event1, awaitItem())
            assertEquals(event2, awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `early subscription should receive events emitted after observing`() = runTest {
        // Given
        val events = listOf(
            PageInvalidationEvent.ConversationsInvalidated(),
            PageInvalidationEvent.ConversationsInvalidated(),
            PageInvalidationEvent.ConversationsInvalidated()
        )

        repository.observePageInvalidationEvents().test {
            // Ensure no events are emitted initially
            expectNoEvents()

            // When
            events.forEach { repository.submit(it) }

            // Then
            events.forEach { expected ->
                assertEquals(expected, awaitItem())
            }

            cancelAndIgnoreRemainingEvents()
        }
    }
}
