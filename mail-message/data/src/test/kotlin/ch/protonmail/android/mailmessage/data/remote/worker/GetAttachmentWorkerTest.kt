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

import java.net.UnknownHostException
import java.util.UUID
import android.app.Notification
import android.app.NotificationChannel
import android.content.Context
import androidx.work.ListenableWorker.Result
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import ch.protonmail.android.mailcommon.data.worker.Enqueuer
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailcommon.presentation.system.NotificationProvider
import ch.protonmail.android.mailmessage.data.R
import ch.protonmail.android.mailmessage.data.local.AttachmentLocalDataSource
import ch.protonmail.android.mailmessage.data.remote.AttachmentApi
import ch.protonmail.android.mailmessage.domain.model.AttachmentId
import ch.protonmail.android.mailmessage.domain.model.AttachmentWorkerStatus
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import io.mockk.Called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import me.proton.core.network.data.ApiManagerFactory
import me.proton.core.network.data.ApiProvider
import me.proton.core.network.domain.session.SessionId
import me.proton.core.network.domain.session.SessionProvider
import me.proton.core.test.android.api.TestApiManager
import me.proton.core.util.kotlin.DefaultDispatcherProvider
import okhttp3.ResponseBody
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GetAttachmentWorkerTest {

    private val userId = UserIdSample.Primary
    private val messageId = MessageIdSample.Invoice
    private val attachmentId = AttachmentId("attachmentId")
    private val attachmentContent = "This is a content"
    private val responseBody = mockk<ResponseBody>()
    private val workParameterId = UUID.randomUUID()
    private val mockedChannel = mockk<NotificationChannel>()
    private val mockedNotification = mockk<Notification>()

    private val workManager: WorkManager = mockk {
        coEvery { enqueue(any<OneTimeWorkRequest>()) } returns mockk()
    }

    private val parameters: WorkerParameters = mockk {
        every { taskExecutor } returns mockk(relaxed = true)
        every { foregroundUpdater } returns mockk(relaxed = true)
        every { id } returns workParameterId
        every { inputData.getString(GetAttachmentWorker.RawUserIdKey) } returns userId.id
        every { inputData.getString(GetAttachmentWorker.RawMessageIdKey) } returns messageId.id
        every { inputData.getString(GetAttachmentWorker.RawAttachmentIdKey) } returns attachmentId.id
    }
    private val context: Context = mockk {
        every { getString(any()) } returns "test"
    }

    private val notificationProvider = mockk<NotificationProvider>(relaxUnitFun = true) {
        every {
            provideNotificationChannel(
                channelId = NotificationProvider.ATTACHMENT_CHANNEL_ID
            )
        } returns mockedChannel
        every {
            provideNotification(
                context = context,
                channel = mockedChannel,
                title = R.string.attachment_download_notification_title
            )
        } returns mockedNotification
    }

    private val sessionProvider = mockk<SessionProvider> {
        coEvery { getSessionId(userId) } returns SessionId("testSessionId")
    }
    private val attachmentApi = mockk<AttachmentApi> {
        coEvery { getAttachment(attachmentId.id) } returns responseBody
    }
    private val apiManagerFactory = mockk<ApiManagerFactory> {
        every { create(any(), AttachmentApi::class) } returns TestApiManager(attachmentApi)
    }
    private val attachmentLocalDataSource = mockk<AttachmentLocalDataSource>(relaxUnitFun = true) {
        coEvery { upsertAttachment(userId, messageId, attachmentId, any(), any()) } returns mockk(relaxed = true)
    }

    private val apiProvider = ApiProvider(apiManagerFactory, sessionProvider, DefaultDispatcherProvider())

    @Test
    fun `worker is enqueued with given parameters`() {
        // When
        Enqueuer(workManager).enqueue<GetAttachmentWorker>(
            userId,
            GetAttachmentWorker.params(userId, messageId, attachmentId)
        )

        // Then
        val requestSlot = slot<OneTimeWorkRequest>()
        verify { workManager.enqueue(capture(requestSlot)) }
        val workSpec = requestSlot.captured.workSpec
        val inputData = workSpec.input
        val actualUserId = inputData.getString(GetAttachmentWorker.RawUserIdKey)
        val actualMessageId = inputData.getString(GetAttachmentWorker.RawMessageIdKey)
        val actualAttachmentId = inputData.getString(GetAttachmentWorker.RawAttachmentIdKey)
        assertEquals(userId.id, actualUserId)
        assertEquals(messageId.id, actualMessageId)
        assertEquals(attachmentId.id, actualAttachmentId)
    }

    @Test
    fun `when worker is started the api is called with the given parameters`() = runTest {
        // Given
        val getAttachmentWorker = createWorker()

        // When
        getAttachmentWorker.doWork()

        // Then
        coVerify { attachmentApi.getAttachment(attachmentId.id) }
    }

    @Test
    fun `worker fails when userid worker parameter is missing`() = runTest {
        // Given - Then
        every { parameters.inputData.getString(GetAttachmentWorker.RawUserIdKey) } returns null
        assertFailsWith<IllegalArgumentException> { createWorker() }
        coVerify { attachmentApi wasNot Called }
    }

    @Test
    fun `worker fails when messageId worker parameter is missing`() = runTest {
        // Given - Then
        every { parameters.inputData.getString(GetAttachmentWorker.RawMessageIdKey) } returns null
        assertFailsWith<IllegalArgumentException> { createWorker() }
        coVerify { attachmentApi wasNot Called }
    }

    @Test
    fun `worker fails when attachmentId worker parameter is missing`() = runTest {
        // Given - Then
        every { parameters.inputData.getString(GetAttachmentWorker.RawAttachmentIdKey) } returns null
        assertFailsWith<IllegalArgumentException> { createWorker() }
        coVerify { attachmentApi wasNot Called }
    }

    @Test
    fun `worker returns success and stores attachment when api call was successful`() = runTest {
        // Given
        coEvery { responseBody.bytes() } returns attachmentContent.toByteArray()
        val getAttachmentWorker = createWorker()

        // When
        val result = getAttachmentWorker.doWork()

        // Then
        assertEquals(Result.success(), result)
        coVerifyOrder {
            attachmentLocalDataSource.updateAttachmentDownloadStatus(
                userId = userId,
                messageId = messageId,
                attachmentId = attachmentId,
                status = AttachmentWorkerStatus.Running
            )
            notificationProvider.provideNotificationChannel(channelId = NotificationProvider.ATTACHMENT_CHANNEL_ID)
            notificationProvider.provideNotification(
                context = context,
                channel = mockedChannel,
                title = R.string.attachment_download_notification_title
            )
            attachmentLocalDataSource.upsertAttachment(
                userId = userId,
                messageId = messageId,
                attachmentId = attachmentId,
                encryptedAttachment = attachmentContent.toByteArray(),
                status = AttachmentWorkerStatus.Success
            )
        }
    }

    @Test
    fun `worker returns failure when api call was not successful`() = runTest {
        // Given
        coEvery { attachmentApi.getAttachment(attachmentId.id) } throws UnknownHostException()
        val getAttachmentWorker = createWorker()

        // When
        val result = getAttachmentWorker.doWork()

        // Then
        assertEquals(Result.failure(), result)
        coVerifyOrder {
            attachmentLocalDataSource.updateAttachmentDownloadStatus(
                userId,
                messageId,
                attachmentId,
                AttachmentWorkerStatus.Running
            )
            notificationProvider.provideNotificationChannel(channelId = NotificationProvider.ATTACHMENT_CHANNEL_ID)
            notificationProvider.provideNotification(
                context = context,
                channel = mockedChannel,
                title = R.string.attachment_download_notification_title
            )
            attachmentLocalDataSource.updateAttachmentDownloadStatus(
                userId,
                messageId,
                attachmentId,
                AttachmentWorkerStatus.Failed.Generic
            )
        }
    }

    @Test
    fun `worker returns failure when response can not be processed due to insufficient storage`() = runTest {
        // Given
        coEvery { responseBody.bytes() } throws OutOfMemoryError()
        val getAttachmentWorker = createWorker()

        // When
        val result = getAttachmentWorker.doWork()

        // Then
        assertEquals(Result.failure(), result)
        coVerifyOrder {
            attachmentLocalDataSource.updateAttachmentDownloadStatus(
                userId,
                messageId,
                attachmentId,
                AttachmentWorkerStatus.Running
            )
            notificationProvider.provideNotificationChannel(channelId = NotificationProvider.ATTACHMENT_CHANNEL_ID)
            notificationProvider.provideNotification(
                context = context,
                channel = mockedChannel,
                title = R.string.attachment_download_notification_title
            )
            attachmentLocalDataSource.updateAttachmentDownloadStatus(
                userId,
                messageId,
                attachmentId,
                AttachmentWorkerStatus.Failed.OutOfMemory
            )
        }
    }

    private fun createWorker() = GetAttachmentWorker(
        context,
        parameters,
        apiProvider,
        attachmentLocalDataSource,
        notificationProvider
    )
}
