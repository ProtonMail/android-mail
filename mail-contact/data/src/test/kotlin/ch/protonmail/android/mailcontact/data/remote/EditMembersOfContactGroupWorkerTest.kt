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
import ch.protonmail.android.mailcommon.domain.sample.LabelIdSample
import ch.protonmail.android.mailcontact.data.local.ContactGroupLocalDataSource
import ch.protonmail.android.mailcontact.data.remote.response.ContactEmailCodeResponse
import ch.protonmail.android.mailcontact.data.remote.response.ContactEmailIdResponse
import ch.protonmail.android.mailcontact.data.remote.response.LabelContactEmailsResponse
import ch.protonmail.android.mailcontact.data.remote.response.UnlabelContactEmailsResponse
import ch.protonmail.android.testdata.contact.ContactEmailSample
import ch.protonmail.android.testdata.user.UserIdTestData
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import me.proton.core.contact.domain.entity.ContactEmailId
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId
import me.proton.core.network.data.ApiProvider
import me.proton.core.network.domain.ApiResult
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@Suppress("MaxLineLength")
class EditMembersOfContactGroupWorkerTest {

    private val userId = UserIdTestData.userId
    private val labelId = LabelIdSample.LabelCoworkers
    private val contactEmailIdsToAdd = setOf(
        ContactEmailSample.contactEmail1.id
    )
    private val contactEmailIdsToRemove = setOf(
        ContactEmailSample.contactEmail2.id
    )

    private val workManager: WorkManager = mockk {
        coEvery { enqueue(any<OneTimeWorkRequest>()) } returns mockk()
    }
    private val parameters: WorkerParameters = mockk {
        every { taskExecutor } returns mockk(relaxed = true)
        every { inputData.getString(EditMembersOfContactGroupWorker.RawUserIdKey) } returns userId.id
        every { inputData.getString(EditMembersOfContactGroupWorker.RawLabelIdKey) } returns labelId.id
        every {
            inputData.getStringArray(EditMembersOfContactGroupWorker.RawLabelContactEmailIdsKey)
        } returns contactEmailIdsToAdd.map { it.id }
            .toTypedArray()
        every {
            inputData.getStringArray(EditMembersOfContactGroupWorker.RawUnlabelContactEmailIdsKey)
        } returns contactEmailIdsToRemove.map { it.id }
            .toTypedArray()
    }
    private val context: Context = mockk()

    private val contactGroupApi: ContactGroupApi = mockk()

    private val contactGroupLocalDataSourceMock: ContactGroupLocalDataSource = mockk()

    private val apiProvider: ApiProvider = mockk {
        coEvery { get<ContactGroupApi>(userId).invoke<LabelContactEmailsResponse>(block = any()) } coAnswers {
            val block = firstArg<suspend ContactGroupApi.() -> LabelContactEmailsResponse>()
            ApiResult.Success(block(contactGroupApi))
        }
    }
    private lateinit var worker: EditMembersOfContactGroupWorker

    @BeforeTest
    fun setUp() {
        worker = EditMembersOfContactGroupWorker(context, parameters, apiProvider, contactGroupLocalDataSourceMock)
    }

    @Test
    fun `worker is enqueued with given parameters`() {
        // When
        Enqueuer(workManager).enqueue<EditMembersOfContactGroupWorker>(
            userId,
            EditMembersOfContactGroupWorker.params(
                userId, labelId, contactEmailIdsToAdd, contactEmailIdsToRemove
            )
        )

        // Then
        val slot = slot<OneTimeWorkRequest>()
        verify { workManager.enqueue(capture(slot)) }
        val workSpec = slot.captured.workSpec
        val constraints = workSpec.constraints
        val inputData = workSpec.input
        val actualUserId = inputData.getString(EditMembersOfContactGroupWorker.RawUserIdKey)
        val actualLabelId = inputData.getString(EditMembersOfContactGroupWorker.RawLabelIdKey)
        val actualLabelContactEmailIds =
            inputData.getStringArray(EditMembersOfContactGroupWorker.RawLabelContactEmailIdsKey)
        val actualUnlabelContactEmailIds =
            inputData.getStringArray(EditMembersOfContactGroupWorker.RawUnlabelContactEmailIdsKey)
        assertEquals(userId.id, actualUserId)
        assertEquals(labelId.id, actualLabelId)
        assertEquals(contactEmailIdsToAdd, actualLabelContactEmailIds!!.map { ContactEmailId(it) }.toSet())
        assertEquals(contactEmailIdsToRemove, actualUnlabelContactEmailIds!!.map { ContactEmailId(it) }.toSet())
        assertEquals(NetworkType.CONNECTED, constraints.requiredNetworkType)
    }

    @Test
    fun `doWork should return failure when userId is null`() = runTest {
        every { parameters.inputData.getString(EditMembersOfContactGroupWorker.RawUserIdKey) } returns null

        val result = worker.doWork()

        assertEquals(Result.failure(), result)
    }

    @Test
    fun `doWork should return failure when labelId is null`() = runTest {
        every { parameters.inputData.getString(EditMembersOfContactGroupWorker.RawLabelIdKey) } returns null

        val result = worker.doWork()

        assertEquals(Result.failure(), result)
    }

    @Test
    fun `doWork should return failure when both labelContactEmailIdsKey and unlabelContactEmailIdsKey are null`() =
        runTest {
            every { parameters.inputData.getStringArray(EditMembersOfContactGroupWorker.RawLabelContactEmailIdsKey) } returns null
            every { parameters.inputData.getStringArray(EditMembersOfContactGroupWorker.RawUnlabelContactEmailIdsKey) } returns null

            val result = worker.doWork()

            assertEquals(Result.failure(), result)
        }

    @Test
    fun `doWork should return success when labelContactEmails and unlabelContactEmails return success`() = runTest {
        // Given
        val expectedLabelContactEmailsResponse = LabelContactEmailsResponse(
            1001,
            listOf(
                ContactEmailIdResponse(
                    contactEmailIdsToAdd.first().id,
                    ContactEmailCodeResponse(1000)
                )
            )
        )
        coEvery { contactGroupApi.labelContactEmails(any()) } returns expectedLabelContactEmailsResponse

        val expectedUnlabelContactEmailsResponse = UnlabelContactEmailsResponse(
            1001,
            listOf(
                ContactEmailIdResponse(
                    contactEmailIdsToRemove.first().id,
                    ContactEmailCodeResponse(1000)
                )
            )
        )
        coEvery { contactGroupApi.unlabelContactEmails(any()) } returns expectedUnlabelContactEmailsResponse

        // When
        val result = worker.doWork()

        // Then
        assertEquals(Result.success(), result)
    }

    @Test
    fun `doWork should return success when labelContactEmails succeeds but one of the results failed, and rollback the failed results`() =
        runTest {
            // Given
            val expectedLabelContactEmailsResponse = LabelContactEmailsResponse(
                1001,
                listOf(
                    ContactEmailIdResponse(
                        ContactEmailSample.contactEmail1.id.id,
                        ContactEmailCodeResponse(1000)
                    ),
                    ContactEmailIdResponse(
                        ContactEmailSample.contactEmail2.id.id,
                        ContactEmailCodeResponse(666) // fail
                    )
                )
            )
            val expectedRolledBackContactEmailId = ContactEmailSample.contactEmail2.id

            coEvery { contactGroupApi.labelContactEmails(any()) } returns expectedLabelContactEmailsResponse

            expectRemoveContactEmailIdsFromContactGroup(
                userId,
                labelId,
                setOf(expectedRolledBackContactEmailId)
            )

            every {
                parameters.inputData.getStringArray(EditMembersOfContactGroupWorker.RawUnlabelContactEmailIdsKey)
            } returns emptyArray()

            // When
            val result = worker.doWork()

            // Then
            assertEquals(Result.success(), result)
            coVerify(exactly = 1) {
                contactGroupLocalDataSourceMock.removeContactEmailIdsFromContactGroup(
                    userId,
                    labelId,
                    setOf(expectedRolledBackContactEmailId)
                )
            }
        }

    @Test
    fun `doWork should return success when unlabelContactEmails succeeds but one of the results failed, and rollback the failed results`() =
        runTest {
            // Given
            every {
                parameters.inputData.getStringArray(EditMembersOfContactGroupWorker.RawLabelContactEmailIdsKey)
            } returns emptyArray()

            val expectedUnlabelContactEmailsResponse = UnlabelContactEmailsResponse(
                1001,
                listOf(
                    ContactEmailIdResponse(
                        ContactEmailSample.contactEmail1.id.id,
                        ContactEmailCodeResponse(1000)
                    ),
                    ContactEmailIdResponse(
                        ContactEmailSample.contactEmail2.id.id,
                        ContactEmailCodeResponse(666) // fail
                    )
                )
            )
            val expectedRolledBackContactEmailId = ContactEmailSample.contactEmail2.id

            coEvery { contactGroupApi.unlabelContactEmails(any()) } returns expectedUnlabelContactEmailsResponse

            expectAddContactEmailIdsToContactGroup(
                userId,
                labelId,
                setOf(expectedRolledBackContactEmailId)
            )

            // When
            val result = worker.doWork()

            // Then
            assertEquals(Result.success(), result)
            coVerify(exactly = 1) {
                contactGroupLocalDataSourceMock.addContactEmailIdsToContactGroup(
                    userId,
                    labelId,
                    setOf(expectedRolledBackContactEmailId)
                )
            }
        }

    private fun expectRemoveContactEmailIdsFromContactGroup(
        userId: UserId,
        labelId: LabelId,
        contactEmailIds: Set<ContactEmailId>
    ) {
        coEvery {
            contactGroupLocalDataSourceMock.removeContactEmailIdsFromContactGroup(userId, labelId, contactEmailIds)
        } just Runs
    }

    private fun expectAddContactEmailIdsToContactGroup(
        userId: UserId,
        labelId: LabelId,
        contactEmailIds: Set<ContactEmailId>
    ) {
        coEvery {
            contactGroupLocalDataSourceMock.addContactEmailIdsToContactGroup(userId, labelId, contactEmailIds)
        } just Runs
    }

}
