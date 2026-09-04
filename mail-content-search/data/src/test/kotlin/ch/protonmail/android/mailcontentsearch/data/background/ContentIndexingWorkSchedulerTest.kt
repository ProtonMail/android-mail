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

package ch.protonmail.android.mailcontentsearch.data.background

import androidx.work.ExistingWorkPolicy
import ch.protonmail.android.mailcommon.data.worker.Enqueuer
import ch.protonmail.android.mailcontentsearch.data.worker.ContentIndexingWorker
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

internal class ContentIndexingWorkSchedulerTest {

    private var isRunning = false
    private val enqueuer = mockk<Enqueuer>(relaxUnitFun = true) {
        coEvery { isWorkRunning(ContentIndexingWorkScheduler.WORKER_ID) } answers { isRunning }
    }
    private val scheduler = ContentIndexingWorkScheduler(enqueuer)

    @Test
    fun `enqueues a worker when none is running`() = runTest {
        // Given
        isRunning = false

        // When
        scheduler.ensureWorkerRunning()

        // Then
        verifyEnqueued(times = 1)
    }

    @Test
    fun `leaves a running worker alone`() = runTest {
        // Given
        isRunning = true

        // When
        scheduler.ensureWorkerRunning()

        // Then
        verifyEnqueued(times = 0)
    }

    @Test
    fun `enqueues against a worker that is still running but on its way out`() = runTest {
        // Given
        isRunning = true
        scheduler.markWorkerExiting()

        // When
        scheduler.ensureWorkerRunning()

        // Then
        verifyEnqueued(times = 1)
    }

    @Test
    fun `counts a fresh worker as looking after indexing again`() = runTest {
        // Given
        isRunning = true
        scheduler.markWorkerExiting()
        scheduler.markWorkerRunning()

        // When
        scheduler.ensureWorkerRunning()

        // Then
        verifyEnqueued(times = 0)
    }

    private fun verifyEnqueued(times: Int) = verify(exactly = times) {
        enqueuer.enqueueUniqueWork(
            workerId = ContentIndexingWorkScheduler.WORKER_ID,
            worker = ContentIndexingWorker::class.java,
            existingWorkPolicy = ExistingWorkPolicy.REPLACE,
            constraints = any(),
            params = any()
        )
    }
}
