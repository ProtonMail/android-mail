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

package ch.protonmail.android.navigation

import android.content.Intent
import app.cash.turbine.test
import ch.protonmail.android.navigation.share.ShareIntentObserver
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class ShareIntentObserverTest {

    private lateinit var shareIntentObserver: ShareIntentObserver

    @Before
    fun setUp() {
        shareIntentObserver = ShareIntentObserver()
    }

    @Test
    fun `should emit intent for action send`() = runTest {
        // Given
        val intent = mockk<Intent>(relaxed = true) {
            every { action } returns Intent.ACTION_SEND
        }

        // When
        shareIntentObserver.onNewIntent(intent)

        // Then
        shareIntentObserver().test {
            val actual = awaitItem()
            assert(actual == intent)
        }
    }

    @Test
    fun `should emit intent for action view`() = runTest {
        // Given
        val intent = mockk<Intent>(relaxed = true) {
            every { action } returns Intent.ACTION_VIEW
        }

        // When
        shareIntentObserver.onNewIntent(intent)

        // Then
        shareIntentObserver().test {
            val actual = awaitItem()
            assert(actual == intent)
        }
    }

    @Test
    fun `should emit intent for action sendto`() = runTest {
        // Given
        val intent = mockk<Intent>(relaxed = true) {
            every { action } returns Intent.ACTION_SENDTO
        }

        // When
        shareIntentObserver.onNewIntent(intent)

        // Then
        shareIntentObserver().test {
            val actual = awaitItem()
            assert(actual == intent)
        }
    }

    @Test
    fun `should emit intent for action send multiple`() = runTest {
        // Given
        val intent = mockk<Intent>(relaxed = true) {
            every { action } returns Intent.ACTION_SEND_MULTIPLE
        }

        // When
        shareIntentObserver.onNewIntent(intent)

        // Then
        shareIntentObserver().test {
            val actual = awaitItem()
            assert(actual == intent)
        }
    }

    @Test
    fun `should not emit intent for an unhandled action`() = runTest {
        // Given
        val intent = mockk<Intent>(relaxed = true) {
            every { action } returns Intent.ACTION_APP_ERROR
        }

        // When
        shareIntentObserver.onNewIntent(intent)

        // Then
        shareIntentObserver().test {
            expectNoEvents()
        }
    }
}
