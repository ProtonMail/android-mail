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

package ch.protonmail.android.mailmessage.data.remote.worker

import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Test
import kotlin.test.assertEquals

internal class MarkMessageAsUnreadWorkerTest {

    private val userId = UserIdSample.Primary
    private val messageId = MessageIdSample.Invoice

    private val params: WorkerParameters = mockk {
        every { taskExecutor } returns mockk(relaxed = true)
        every { inputData.getString(MarkMessageAsUnreadWorker.RawUserIdKey) } returns userId.id
        every { inputData.getString(MarkMessageAsUnreadWorker.RawMessageIdKey) } returns messageId.id
    }
    private val workManager: WorkManager = mockk {
        coEvery { enqueue(ofType<OneTimeWorkRequest>()) } returns mockk()
    }

    private val worker = MarkMessageAsUnreadWorker(
        context = mockk(),
        workerParameters = params
    )

    @Test
    fun `worker is enqueued with correct constraints`() {
        // given
        val enqueuer = MarkMessageAsUnreadWorker.Enqueuer(workManager)
        val expectedNetworkType = NetworkType.CONNECTED

        // when
        enqueuer.enqueue(userId, messageId)

        // then
        val requestSlot = slot<OneTimeWorkRequest>()
        verify { workManager.enqueue(capture(requestSlot)) }
        assertEquals(expectedNetworkType, requestSlot.captured.workSpec.constraints.requiredNetworkType)
    }

    @Test
    fun `worker is enqueued with correct parameters`() {
        // given
        val enqueuer = MarkMessageAsUnreadWorker.Enqueuer(workManager)

        // when
        enqueuer.enqueue(userId, messageId)

        // then
        val requestSlot = slot<OneTimeWorkRequest>()
        verify { workManager.enqueue(capture(requestSlot)) }
        assertEquals(
            expected = userId.id,
            actual = requestSlot.captured.workSpec.input.getString(MarkMessageAsUnreadWorker.RawUserIdKey)
        )
        assertEquals(
            expected = messageId.id,
            actual = requestSlot.captured.workSpec.input.getString(MarkMessageAsUnreadWorker.RawMessageIdKey)
        )
    }
}
