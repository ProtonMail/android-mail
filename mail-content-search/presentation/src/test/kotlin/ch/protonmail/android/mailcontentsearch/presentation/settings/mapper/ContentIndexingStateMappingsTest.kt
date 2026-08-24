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

package ch.protonmail.android.mailcontentsearch.presentation.settings.mapper

import ch.protonmail.android.mailcontentsearch.domain.model.ContentIndexingState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class ContentIndexingStateMappingsTest {

    @Test
    fun `an account mid-backfill reports its percentage and counts as active`() {
        // Given
        val state = ContentIndexingState.Running(percentage = 42.5)

        // Then
        assertEquals(42.5, state.toPercentage())
        assertTrue(state.isActive())
    }

    @Test
    fun `a fully indexed account shows no progress at all`() {
        // Given - Rust stays in Running once the backfill has caught up, so 100% would otherwise
        // render as "Preparing 100.00%" forever.
        val state = ContentIndexingState.Running(percentage = 100.0)

        // Then
        assertNull(state.toPercentage())
        assertFalse(state.isActive())
    }

    @Test
    fun `a percentage that overshoots is still treated as finished`() {
        // Given - the percentage is derived from counters Rust revises as it goes.
        val state = ContentIndexingState.Running(percentage = 100.4)

        // Then
        assertNull(state.toPercentage())
        assertFalse(state.isActive())
    }

    @Test
    fun `initializing is active but has no percentage to show yet`() {
        assertNull(ContentIndexingState.Initializing.toPercentage())
        assertTrue(ContentIndexingState.Initializing.isActive())
    }

    @Test
    fun `waiting for an unmetered connection is a pause, not an end`() {
        // Given - Rust parked the queue and will resume it on its own, so the account still has a
        // backfill to run and the screen says so rather than showing a stalled percentage.
        val state = ContentIndexingState.WaitingForUnmeteredConnection

        // Then
        assertTrue(state.isWaitingForUnmeteredConnection())
        assertTrue(state.isActive())
        assertNull(state.toPercentage())
    }

    @Test
    fun `no other state is mistaken for waiting on Wi-Fi`() {
        listOf(
            ContentIndexingState.Idle,
            ContentIndexingState.Initializing,
            ContentIndexingState.Running(percentage = 42.5),
            ContentIndexingState.Completed,
            ContentIndexingState.Cancelled,
            ContentIndexingState.Failed
        ).forEach { state ->
            assertFalse(state.isWaitingForUnmeteredConnection(), "$state should not be waiting on Wi-Fi")
        }
    }

    @Test
    fun `a failed account is reported as failed and nothing else`() {
        // Given
        val state = ContentIndexingState.Failed

        // Then
        assertTrue(state.isFailed())
        assertFalse(state.isActive())
        assertFalse(state.isWaitingForUnmeteredConnection())
        assertNull(state.toPercentage())
    }

    @Test
    fun `no other state is mistaken for a failure`() {
        listOf(
            ContentIndexingState.Idle,
            ContentIndexingState.Initializing,
            ContentIndexingState.WaitingForUnmeteredConnection,
            ContentIndexingState.Running(percentage = 42.5),
            ContentIndexingState.Completed,
            ContentIndexingState.Cancelled
        ).forEach { state ->
            assertFalse(state.isFailed(), "$state should not be failed")
        }
    }

    @Test
    fun `settled states are neither active nor carry a percentage`() {
        listOf(
            ContentIndexingState.Idle,
            ContentIndexingState.Completed,
            ContentIndexingState.Cancelled,
            ContentIndexingState.Failed
        ).forEach { state ->
            assertNull(state.toPercentage(), "$state should not report a percentage")
            assertFalse(state.isActive(), "$state should not be active")
        }
    }
}
