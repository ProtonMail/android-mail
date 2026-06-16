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

package ch.protonmail.android.mailcontentsearch.data.worker

import androidx.work.WorkInfo
import kotlin.test.Test
import kotlin.test.assertEquals

internal class ContentIndexingWorkerCancellationTest {

    @Test
    fun `should restart in background mode when FGS times out`() {
        assertEquals(
            CancellationAction.RestartInBackgroundMode,
            ContentIndexingWorker.decideCancellationAction(
                stopReason = WorkInfo.STOP_REASON_FOREGROUND_SERVICE_TIMEOUT,
                isSelfRestarting = false
            )
        )
    }

    @Test
    fun `should restart in background mode when FGS times out even during a self restart`() {
        // FGS timeout wins over self-restart so we don't lose the indexing session
        // because the worker happens to be mid-swap when the 6h deadline fires.
        assertEquals(
            CancellationAction.RestartInBackgroundMode,
            ContentIndexingWorker.decideCancellationAction(
                stopReason = WorkInfo.STOP_REASON_FOREGROUND_SERVICE_TIMEOUT,
                isSelfRestarting = true
            )
        )
    }

    @Test
    fun `should preserve indexer session when cancellation is part of a mode swap`() {
        assertEquals(
            CancellationAction.PreserveIndexerSession,
            ContentIndexingWorker.decideCancellationAction(
                stopReason = WorkInfo.STOP_REASON_CANCELLED_BY_APP,
                isSelfRestarting = true
            )
        )
    }

    @Test
    fun `should release indexer session when app cancels without a pending self restart`() {
        assertEquals(
            CancellationAction.ReleaseIndexerSession,
            ContentIndexingWorker.decideCancellationAction(
                stopReason = WorkInfo.STOP_REASON_CANCELLED_BY_APP,
                isSelfRestarting = false
            )
        )
    }

    @Test
    fun `should restart in background mode when system stops with a plain timeout`() {
        assertEquals(
            CancellationAction.RestartInBackgroundMode,
            ContentIndexingWorker.decideCancellationAction(
                stopReason = WorkInfo.STOP_REASON_TIMEOUT,
                isSelfRestarting = false
            )
        )
    }

    @Test
    fun `should preserve indexer session when system stops due to device state`() {
        assertEquals(
            CancellationAction.PreserveIndexerSession,
            ContentIndexingWorker.decideCancellationAction(
                stopReason = WorkInfo.STOP_REASON_DEVICE_STATE,
                isSelfRestarting = false
            )
        )
    }

    @Test
    fun `should preserve indexer session when system stops due to quota`() {
        assertEquals(
            CancellationAction.PreserveIndexerSession,
            ContentIndexingWorker.decideCancellationAction(
                stopReason = WorkInfo.STOP_REASON_QUOTA,
                isSelfRestarting = false
            )
        )
    }

    @Test
    fun `should preserve indexer session when system stops due to app standby`() {
        assertEquals(
            CancellationAction.PreserveIndexerSession,
            ContentIndexingWorker.decideCancellationAction(
                stopReason = WorkInfo.STOP_REASON_APP_STANDBY,
                isSelfRestarting = false
            )
        )
    }

    @Test
    fun `should preserve indexer session when stop reason is unknown`() {
        assertEquals(
            CancellationAction.PreserveIndexerSession,
            ContentIndexingWorker.decideCancellationAction(
                stopReason = WorkInfo.STOP_REASON_UNKNOWN,
                isSelfRestarting = false
            )
        )
    }
}
