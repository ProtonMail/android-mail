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
package ch.protonmail.android.mailmailbox.presentation.mailbox

import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CategorySkeletonLatchTest {

    private val latch = CategorySkeletonLatch()

    @Test
    fun `should not arm on the first update when a load count already exists`() {
        // Given a composable recreated (e.g. config change) while the ViewModel survived
        // When
        val latched = latch.update(firstPageLoadingStartCount = 7, isRefreshLoading = false, isInError = false)

        // Then
        assertFalse(latched)
    }

    @Test
    fun `should not arm while the load count stays the same`() {
        // Given
        latch.update(firstPageLoadingStartCount = 1, isRefreshLoading = false, isInError = false)

        // When
        val latched = latch.update(firstPageLoadingStartCount = 1, isRefreshLoading = false, isInError = false)

        // Then
        assertFalse(latched)
    }

    @Test
    fun `should arm when the load count is bumped and bridge the gap before the refresh starts`() {
        // Given
        latch.update(firstPageLoadingStartCount = 0, isRefreshLoading = false, isInError = false)

        // When the scroller starts loading but Paging has not begun refreshing yet
        val latched = latch.update(firstPageLoadingStartCount = 1, isRefreshLoading = false, isInError = false)

        // Then
        assertTrue(latched)
    }

    @Test
    fun `should stay latched until the refresh has been seen loading and has settled`() {
        // Given
        latch.update(firstPageLoadingStartCount = 0, isRefreshLoading = false, isInError = false)
        latch.update(firstPageLoadingStartCount = 1, isRefreshLoading = false, isInError = false)

        // When the refresh runs
        assertTrue(latch.update(firstPageLoadingStartCount = 1, isRefreshLoading = true, isInError = false))

        // Then it clears only once that refresh settles
        assertFalse(latch.update(firstPageLoadingStartCount = 1, isRefreshLoading = false, isInError = false))
    }

    @Test
    fun `should clear as soon as the page is in error`() {
        // Given an armed latch
        latch.update(firstPageLoadingStartCount = 0, isRefreshLoading = false, isInError = false)
        assertTrue(latch.update(firstPageLoadingStartCount = 1, isRefreshLoading = false, isInError = false))

        // When
        val latched = latch.update(firstPageLoadingStartCount = 1, isRefreshLoading = false, isInError = true)

        // Then
        assertFalse(latched)
    }

    @Test
    fun `should re-arm when a further load starts`() {
        // Given a completed load cycle
        latch.update(firstPageLoadingStartCount = 0, isRefreshLoading = false, isInError = false)
        latch.update(firstPageLoadingStartCount = 1, isRefreshLoading = true, isInError = false)
        assertFalse(latch.update(firstPageLoadingStartCount = 1, isRefreshLoading = false, isInError = false))

        // When
        val latched = latch.update(firstPageLoadingStartCount = 2, isRefreshLoading = false, isInError = false)

        // Then
        assertTrue(latched)
    }

    @Test
    fun `should arm even when several loads are conflated into a single count jump`() {
        // Given
        latch.update(firstPageLoadingStartCount = 0, isRefreshLoading = false, isInError = false)

        // When the conflated StateFlow only delivers the latest count
        val latched = latch.update(firstPageLoadingStartCount = 3, isRefreshLoading = false, isInError = false)

        // Then
        assertTrue(latched)
    }
}
