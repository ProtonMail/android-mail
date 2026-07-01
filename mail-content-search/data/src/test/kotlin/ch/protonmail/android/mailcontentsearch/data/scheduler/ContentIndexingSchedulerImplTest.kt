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

package ch.protonmail.android.mailcontentsearch.data.scheduler

import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import ch.protonmail.android.mailcommon.domain.AppInBackgroundState
import ch.protonmail.android.mailcontentsearch.domain.model.ContentIndexingState
import ch.protonmail.android.mailcontentsearch.domain.model.EnqueueIndexingResult
import ch.protonmail.android.mailcontentsearch.data.worker.ContentIndexingWorker
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import kotlin.test.Test
import kotlin.test.assertEquals

internal class ContentIndexingSchedulerImplTest {

    private val userId = UserId("user-1")
    private val otherUserId = UserId("user-2")

    private val workInfos = MutableStateFlow<List<WorkInfo>>(emptyList())
    private val workManager = mockk<WorkManager>(relaxUnitFun = true) {
        every { getWorkInfosForUniqueWorkFlow(ContentIndexingWorker.UniqueName) } returns workInfos
        every { enqueueUniqueWork(any(), any<ExistingWorkPolicy>(), any<OneTimeWorkRequest>()) } returns mockk()
    }
    private val appInBackgroundState = mockk<AppInBackgroundState>()

    private val scheduler = ContentIndexingSchedulerImpl(workManager, appInBackgroundState)

    @Test
    fun `enqueueSweep replaces the unique sweep work and reports scheduled`() = runTest {
        // Given
        every { appInBackgroundState.isAppInBackground() } returns true

        // When
        val result = scheduler.enqueueSweep(allowMobileData = true)

        // Then
        assertEquals(EnqueueIndexingResult.Scheduled, result)
        val request = slot<OneTimeWorkRequest>()
        verify {
            workManager.enqueueUniqueWork(
                ContentIndexingWorker.UniqueName,
                ExistingWorkPolicy.REPLACE,
                capture(request)
            )
        }
        val input = request.captured.workSpec.input
        assertEquals(true, input.getBoolean(ContentIndexingWorker.KeyRunAsForeground, false))
        assertEquals(true, input.getBoolean(ContentIndexingWorker.KeyAllowMobileData, false))
    }

    @Test
    fun `enqueueSweep keeps an in-progress sweep when replaceExisting is false`() = runTest {
        // Given
        every { appInBackgroundState.isAppInBackground() } returns true

        // When
        scheduler.enqueueSweep(allowMobileData = true, replaceExisting = false)

        // Then
        verify {
            workManager.enqueueUniqueWork(
                ContentIndexingWorker.UniqueName,
                ExistingWorkPolicy.KEEP,
                any<OneTimeWorkRequest>()
            )
        }
    }

    @Test
    fun `enqueueSweep runs in background when the app is not in the background`() = runTest {
        // Given
        every { appInBackgroundState.isAppInBackground() } returns false

        // When
        scheduler.enqueueSweep(allowMobileData = false)

        // Then
        val request = slot<OneTimeWorkRequest>()
        verify { workManager.enqueueUniqueWork(any(), any<ExistingWorkPolicy>(), capture(request)) }
        val input = request.captured.workSpec.input
        assertEquals(false, input.getBoolean(ContentIndexingWorker.KeyRunAsForeground, true))
        assertEquals(false, input.getBoolean(ContentIndexingWorker.KeyAllowMobileData, true))
    }

    @Test
    fun `observeState reports running progress for the account the sweep is indexing`() = runTest {
        // Given
        workInfos.value = listOf(runningInfo(userId, progress = 0.42))

        // When
        val result = scheduler.observeState(userId).first()

        // Then
        assertEquals(ContentIndexingState.Running(0.42), result)
    }

    @Test
    fun `observeState reports idle for an account the sweep is not currently indexing`() = runTest {
        // Given
        workInfos.value = listOf(runningInfo(otherUserId, progress = 0.42))

        // When
        val result = scheduler.observeState(userId).first()

        // Then
        assertEquals(ContentIndexingState.Idle, result)
    }

    @Test
    fun `observeState reports idle when there is no sweep work`() = runTest {
        // Given
        workInfos.value = emptyList()

        // When
        val result = scheduler.observeState(userId).first()

        // Then
        assertEquals(ContentIndexingState.Idle, result)
    }

    @Test
    fun `observeState reports initializing while running before the first progress tick`() = runTest {
        // Given
        workInfos.value = listOf(
            infoFor(WorkInfo.State.RUNNING, workDataOf(ContentIndexingWorker.KeyCurrentUserId to userId.id))
        )

        // When
        val result = scheduler.observeState(userId).first()

        // Then
        assertEquals(ContentIndexingState.Initializing, result)
    }

    @Test
    fun `observeState maps terminal work states for the indexed account`() = runTest {
        // Given
        val currentUserProgress = workDataOf(ContentIndexingWorker.KeyCurrentUserId to userId.id)

        // When / Then
        workInfos.value = listOf(infoFor(WorkInfo.State.SUCCEEDED, currentUserProgress))
        assertEquals(ContentIndexingState.Completed, scheduler.observeState(userId).first())

        workInfos.value = listOf(infoFor(WorkInfo.State.CANCELLED, currentUserProgress))
        assertEquals(ContentIndexingState.Cancelled, scheduler.observeState(userId).first())

        workInfos.value = listOf(infoFor(WorkInfo.State.FAILED, currentUserProgress))
        assertEquals(ContentIndexingState.Failed, scheduler.observeState(userId).first())

        workInfos.value = listOf(infoFor(WorkInfo.State.ENQUEUED, currentUserProgress))
        assertEquals(ContentIndexingState.Initializing, scheduler.observeState(userId).first())
    }

    @Test
    fun `observeState reports initializing while the sweep is starting before it announces an account`() = runTest {
        // Given
        workInfos.value = listOf(infoFor(WorkInfo.State.ENQUEUED, workDataOf()))

        // When
        val result = scheduler.observeState(userId).first()

        // Then
        assertEquals(ContentIndexingState.Initializing, result)
    }

    @Test
    fun `observeState reports idle for a finished sweep that never announced an account`() = runTest {
        // Given
        workInfos.value = listOf(infoFor(WorkInfo.State.SUCCEEDED, workDataOf()))

        // When
        val result = scheduler.observeState(userId).first()

        // Then
        assertEquals(ContentIndexingState.Idle, result)
    }

    private fun runningInfo(userId: UserId, progress: Double) = infoFor(
        WorkInfo.State.RUNNING,
        workDataOf(
            ContentIndexingWorker.KeyCurrentUserId to userId.id,
            ContentIndexingWorker.KeyProgress to progress
        )
    )

    private fun infoFor(state: WorkInfo.State, progress: Data): WorkInfo = mockk {
        every { this@mockk.state } returns state
        every { this@mockk.progress } returns progress
    }
}
