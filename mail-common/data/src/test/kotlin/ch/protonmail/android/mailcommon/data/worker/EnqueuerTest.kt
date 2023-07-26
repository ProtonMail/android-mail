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

package ch.protonmail.android.mailcommon.data.worker

import java.util.UUID
import androidx.work.Data
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.google.common.util.concurrent.ListenableFuture
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class EnqueuerTest {

    private val workManager = mockk<WorkManager>()
    private val listenableFuture: ListenableFuture<List<WorkInfo>> = mockk {
        every { isDone } returns true
    }

    private val enqueuer = Enqueuer(workManager)

    @Test
    fun `does not remove existing work when already started`() = runTest {
        // Given
        val workId = "SyncDraftWork-test-message-id"
        val workInfo = WorkInfo(UUID.randomUUID(), WorkInfo.State.RUNNING, Data.EMPTY, emptyList(), Data.EMPTY, 0)
        givenWorkManagerReturns(workId, workInfo)

        // When
        enqueuer.removeUnStartedExistingWork(workId)

        // Then
        verify(exactly = 0) { workManager.cancelUniqueWork(workId) }
    }

    @Test
    fun `removes existing work when enqueued but not started yet`() = runTest {
        // Given
        val workId = "SyncDraftWork-test-message-id"
        val workInfo = WorkInfo(UUID.randomUUID(), WorkInfo.State.ENQUEUED, Data.EMPTY, emptyList(), Data.EMPTY, 0)
        givenWorkManagerReturns(workId, workInfo)
        givenCancelUniqueWorkSucceeds(workId)

        // When
        enqueuer.removeUnStartedExistingWork(workId)

        // Then
        verify { workManager.cancelUniqueWork(workId) }
    }

    private fun givenCancelUniqueWorkSucceeds(workId: String) {
        every { workManager.cancelUniqueWork(workId) } returns mockk()
    }


    private fun givenWorkManagerReturns(workId: String, workInfo: WorkInfo) {
        every { workManager.getWorkInfosForUniqueWork(workId) } returns listenableFuture
        coEvery { listenableFuture.get() } returns listOf(workInfo)
    }
}
