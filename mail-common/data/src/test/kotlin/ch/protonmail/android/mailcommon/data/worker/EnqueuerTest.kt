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

import androidx.work.ExistingWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import kotlin.test.Test
import kotlin.test.assertEquals

class EnqueuerTest {

    private val workManager = mockk<WorkManager>()

    private val enqueuer = Enqueuer(workManager)

    @Test
    fun `keep the existing enqueued work when trying to enqueue again some unique work`() = runTest {
        // Given
        val workId = "SyncDraftWork-test-message-id"
        val params = mapOf("messageId" to "test-message-id")
        givenEnqueueWorkSucceeds(workId, ExistingWorkPolicy.KEEP)

        // When
        enqueuer.enqueueUniqueWork<ListenableWorker>(workId, params)

        // Then
        val workPolicySlot = slot<ExistingWorkPolicy>()
        coVerify { workManager.enqueueUniqueWork(workId, capture(workPolicySlot), any<OneTimeWorkRequest>()) }
        assertEquals(ExistingWorkPolicy.KEEP, workPolicySlot.captured)
    }

    @Test
    fun `changes default existing work policy when explicitly requested`() = runTest {
        // Given
        val workId = "SyncDraftWork-test-message-id"
        val params = mapOf("messageId" to "test-message-id")
        val existingWorkPolicy = ExistingWorkPolicy.APPEND
        givenEnqueueWorkSucceeds(workId, existingWorkPolicy)

        // When
        enqueuer.enqueueUniqueWork<ListenableWorker>(workId, params, existingWorkPolicy = existingWorkPolicy)

        // Then
        val workPolicySlot = slot<ExistingWorkPolicy>()
        coVerify { workManager.enqueueUniqueWork(workId, capture(workPolicySlot), any<OneTimeWorkRequest>()) }
        assertEquals(existingWorkPolicy, workPolicySlot.captured)
    }

    @Test
    fun `cancel all work cancels work by tag for the given user Id`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        givenCancelWorkSucceeds(userId)

        // When
        enqueuer.cancelAllWork(userId)

        // Then
        coVerify { workManager.cancelAllWorkByTag(userId.id) }
    }

    private fun givenCancelWorkSucceeds(userId: UserId) {
        every { workManager.cancelAllWorkByTag(userId.id) } returns mockk()
    }

    private fun givenEnqueueWorkSucceeds(workId: String, existingWorkPolicy: ExistingWorkPolicy) {
        every {
            workManager.enqueueUniqueWork(workId, existingWorkPolicy, any<OneTimeWorkRequest>())
        } returns mockk()
    }

}
