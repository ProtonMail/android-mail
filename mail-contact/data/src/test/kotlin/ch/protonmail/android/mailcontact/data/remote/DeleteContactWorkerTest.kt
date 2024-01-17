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

package ch.protonmail.android.mailcontact.data.remote

import android.content.Context
import androidx.work.ListenableWorker.Result
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import ch.protonmail.android.mailcommon.data.worker.Enqueuer
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import me.proton.core.contact.domain.entity.ContactId
import me.proton.core.contact.domain.repository.ContactRemoteDataSource
import me.proton.core.domain.entity.UserId
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class DeleteContactWorkerTest {

    private val userId = UserId("userId")
    private val contactId = ContactId("contactId")

    private val workManager: WorkManager = mockk {
        coEvery { enqueue(any<OneTimeWorkRequest>()) } returns mockk()
    }
    private val parameters: WorkerParameters = mockk {
        every { taskExecutor } returns mockk(relaxed = true)
        every { inputData.getString(DeleteContactWorker.RawUserIdKey) } returns userId.id
        every { inputData.getString(DeleteContactWorker.RawContactIdKey) } returns contactId.id
    }
    private val context: Context = mockk()
    private val contactRemoteDataSource: ContactRemoteDataSource = mockk()
    private lateinit var worker: DeleteContactWorker

    @BeforeTest
    fun setUp() {
        worker = DeleteContactWorker(context, parameters, contactRemoteDataSource)
    }

    @Test
    fun `worker is enqueued with given parameters`() {
        // When
        Enqueuer(workManager).enqueue<DeleteContactWorker>(userId, DeleteContactWorker.params(userId.id, contactId.id))

        // Then
        val slot = slot<OneTimeWorkRequest>()
        verify { workManager.enqueue(capture(slot)) }
        val workSpec = slot.captured.workSpec
        val constraints = workSpec.constraints
        val inputData = workSpec.input
        val actualUserId = inputData.getString(DeleteContactWorker.RawUserIdKey)
        val actualContactId = inputData.getString(DeleteContactWorker.RawContactIdKey)
        assertEquals(userId.id, actualUserId)
        assertEquals(contactId.id, actualContactId)
        assertEquals(NetworkType.CONNECTED, constraints.requiredNetworkType)
    }

    @Test
    fun `doWork should return failure when userId is null`() = runTest {
        every { parameters.inputData.getString(DeleteContactWorker.RawUserIdKey) } returns null

        val result = worker.doWork()

        assertEquals(Result.failure(), result)
    }

    @Test
    fun `doWork should return failure when contactId is null`() = runTest {
        every { parameters.inputData.getString(DeleteContactWorker.RawContactIdKey) } returns null

        val result = worker.doWork()

        assertEquals(Result.failure(), result)
    }

    @Test
    fun `doWork should return failure when deleteContacts returns failure`() = runTest {
        coEvery { contactRemoteDataSource.deleteContacts(userId, listOf(contactId)) } throws Exception()

        val result = worker.doWork()

        assertEquals(Result.failure(), result)
    }

    @Test
    fun `doWork should return success when deleteContacts returns success`() = runTest {
        coEvery { contactRemoteDataSource.deleteContacts(userId, listOf(contactId)) } returns Unit

        val result = worker.doWork()

        assertEquals(Result.success(), result)
    }
}
