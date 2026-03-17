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

package ch.protonmail.android.deeplinks

import app.cash.turbine.test
import ch.protonmail.android.navigation.deeplinks.NotificationsDeepLinkData
import ch.protonmail.android.navigation.deeplinks.NotificationsDeepLinkHandler
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class NotificationsDeepLinkHandlerTest {

    private lateinit var handler: NotificationsDeepLinkHandler

    @BeforeTest
    fun setup() {
        handler = NotificationsDeepLinkHandler()
    }

    @Test
    fun `initial state has no pending data`() {
        // When
        val hasPending = handler.hasPending()

        // Then
        assertFalse(hasPending)
    }

    @Test
    fun `hasPending returns true when data is set`() {
        // Given
        val testData = mockk<NotificationsDeepLinkData>()

        // When
        handler.setPending(testData)

        // Then
        assertTrue(handler.hasPending())
    }

    @Test
    fun `hasPending returns false after consume is called`() {
        // Given
        val testData = mockk<NotificationsDeepLinkData>()
        handler.setPending(testData)

        // When
        handler.consume()

        // Then
        assertFalse(handler.hasPending())
    }

    @Test
    fun `pending flow does not emit when locked`() = runTest {
        // Given
        val testData = mockk<NotificationsDeepLinkData>()
        handler.setLocked()

        // When
        handler.setPending(testData)

        // Then
        handler.pending.test {
            expectNoEvents()
        }
    }

    @Test
    fun `pending flow emits data when unlocked and data is set`() = runTest {
        // Given
        val testData = mockk<NotificationsDeepLinkData>()
        handler.setUnlocked()

        // When
        handler.setPending(testData)

        // Then
        handler.pending.test {
            assertEquals(testData, awaitItem())
        }
    }

    @Test
    fun `pending flow does not emit when unlocked with no data`() = runTest {
        // Given
        handler.setUnlocked()

        // Then
        handler.pending.test {
            expectNoEvents()
        }
    }

    @Test
    fun `pending flow emits data when unlocking with existing pending data`() = runTest {
        // Given
        val testData = mockk<NotificationsDeepLinkData>()
        handler.setLocked()
        handler.setPending(testData)

        // When
        handler.setUnlocked()

        // Then
        handler.pending.test {
            assertEquals(testData, awaitItem())
        }
    }

    @Test
    fun `setPending replaces existing data when unlocked`() = runTest {
        // Given
        val firstData = mockk<NotificationsDeepLinkData>(name = "first")
        val secondData = mockk<NotificationsDeepLinkData>(name = "second")
        handler.setUnlocked()

        handler.pending.test {
            // When
            handler.setPending(firstData)
            assertEquals(firstData, awaitItem())

            handler.setPending(secondData)
            assertEquals(secondData, awaitItem())
        }
    }

    @Test
    fun `consume clears data that was set while locked`() {
        // Given
        val testData = mockk<NotificationsDeepLinkData>()
        handler.setLocked()
        handler.setPending(testData)

        // When
        handler.consume()

        // Then
        assertFalse(handler.hasPending())
    }

    @Test
    fun `setLocked does not clear pending data flag`() {
        // Given
        val testData = mockk<NotificationsDeepLinkData>()
        handler.setPending(testData)

        // When
        handler.setLocked()

        // Then
        assertTrue(handler.hasPending())
    }

    @Test
    fun `setUnlocked does not clear pending data flag`() {
        // Given
        val testData = mockk<NotificationsDeepLinkData>()
        handler.setPending(testData)

        // When
        handler.setUnlocked()

        // Then
        assertTrue(handler.hasPending())
    }

    @Test
    fun `pending flow stops emitting when locked after being unlocked`() = runTest {
        // Given
        val testData = mockk<NotificationsDeepLinkData>()
        handler.setUnlocked()

        handler.pending.test {
            handler.setPending(testData)
            assertEquals(testData, awaitItem())

            // When
            handler.setLocked()

            // Then - no more emissions after locking
            expectNoEvents()
        }
    }

    @Test
    fun `setPending while locked after prior unlock does not emit until unlocked`() = runTest {
        // Given - simulate a previous unlock/lock cycle
        val firstData = mockk<NotificationsDeepLinkData>(name = "first")
        val secondData = mockk<NotificationsDeepLinkData>(name = "second")
        handler.setUnlocked()

        handler.pending.test {
            handler.setPending(firstData)
            assertEquals(firstData, awaitItem())

            // Lock again (simulates auto-lock timer firing)
            handler.setLocked()

            // When - new deep link arrives while locked
            handler.setPending(secondData)

            // Then - should NOT emit while locked
            expectNoEvents()

            // When - unlock
            handler.setUnlocked()

            // Then - now it should emit
            assertEquals(secondData, awaitItem())
        }
    }
}
