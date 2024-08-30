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

import android.app.Notification
import android.app.NotificationChannel
import android.content.Context
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
import android.os.Build
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import ch.protonmail.android.mailcommon.domain.util.requireNotBlank
import ch.protonmail.android.mailcommon.presentation.system.NotificationProvider
import ch.protonmail.android.mailmessage.data.R
import ch.protonmail.android.mailmessage.data.local.AttachmentLocalDataSource
import ch.protonmail.android.mailmessage.data.remote.AttachmentApi
import ch.protonmail.android.mailmessage.domain.model.AttachmentId
import ch.protonmail.android.mailmessage.domain.model.AttachmentWorkerStatus
import ch.protonmail.android.mailmessage.domain.model.MessageId
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import me.proton.core.domain.entity.UserId
import me.proton.core.network.data.ApiProvider
import me.proton.core.network.domain.ApiResult
import timber.log.Timber

@HiltWorker
class GetAttachmentWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted private val workerParameters: WorkerParameters,
    private val apiProvider: ApiProvider,
    private val attachmentLocalDataSource: AttachmentLocalDataSource,
    private val notificationProvider: NotificationProvider
) : CoroutineWorker(context, workerParameters) {

    private val userId = UserId(extractStringFromWorkerParams(RawUserIdKey, "User id"))
    private val messageId = MessageId(extractStringFromWorkerParams(RawMessageIdKey, "Message id"))
    private val attachmentId = AttachmentId(extractStringFromWorkerParams(RawAttachmentIdKey, "Attachment id"))

    @Suppress("TooGenericExceptionCaught", "ReturnCount")
    override suspend fun doWork(): Result {

        Timber.d("Start downloading attachment $attachmentId")
        setWorkerStatusToDb(AttachmentWorkerStatus.Running)

        setForegroundAsync(createForegroundInfo(attachmentId))

        Timber.d("Foreground information set")

        val result = try {
            apiProvider.get<AttachmentApi>(userId).invoke {
                getAttachment(attachmentId = attachmentId.id)
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to get attachment")
            setWorkerStatusToDb(AttachmentWorkerStatus.Failed.Generic)
            return Result.failure()
        }

        val encryptedByteArray = try {
            result.valueOrThrow.bytes()
        } catch (e: OutOfMemoryError) {
            Timber.w(e, "Not enough memory to process attachment from response")
            setWorkerStatusToDb(AttachmentWorkerStatus.Failed.OutOfMemory)
            return Result.failure()
        } catch (e: Exception) {
            Timber.w(e, "Failed to extract attachment from response")
            setWorkerStatusToDb(AttachmentWorkerStatus.Failed.Generic)
            return Result.failure()
        }

        return when (result) {
            is ApiResult.Success -> {
                Timber.d("Attachment $attachmentId downloaded successfully")
                attachmentLocalDataSource.upsertAttachment(
                    userId = userId,
                    messageId = messageId,
                    attachmentId = attachmentId,
                    encryptedAttachment = encryptedByteArray,
                    status = AttachmentWorkerStatus.Success
                )
                Result.success()
            }

            else -> {
                Timber.d("Failed to get attachment")
                setWorkerStatusToDb(AttachmentWorkerStatus.Failed.Generic)
                Result.failure()
            }
        }
    }

    private fun createForegroundInfo(attachmentId: AttachmentId): ForegroundInfo {
        val channel = notificationProvider.provideNotificationChannel(NotificationProvider.ATTACHMENT_CHANNEL_ID)

        // On API >= 34, foreground service type needs to be explicitly defined.
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ForegroundInfo(
                attachmentId.id.hashCode(),
                createNotification(channel),
                FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(
                attachmentId.id.hashCode(),
                createNotification(channel)
            )
        }
    }

    private fun createNotification(notificationChannel: NotificationChannel): Notification {
        return notificationProvider.provideNotification(
            context = context,
            channel = notificationChannel,
            title = R.string.attachment_download_notification_title
        )
    }

    private suspend fun setWorkerStatusToDb(status: AttachmentWorkerStatus) {
        attachmentLocalDataSource.updateAttachmentDownloadStatus(userId, messageId, attachmentId, status)
    }

    private fun extractStringFromWorkerParams(key: String, fieldName: String) =
        requireNotBlank(workerParameters.inputData.getString(key), fieldName = fieldName)


    companion object {

        internal const val RawUserIdKey = "getAttachmentWorkParamUserId"
        internal const val RawMessageIdKey = "getAttachmentWorkerParamMessageId"
        internal const val RawAttachmentIdKey = "getAttachmentWorkerParamAttachmentId"

        fun params(
            userId: UserId,
            messageId: MessageId,
            attachmentId: AttachmentId
        ) = mapOf(
            RawUserIdKey to userId.id,
            RawMessageIdKey to messageId.id,
            RawAttachmentIdKey to attachmentId.id
        )
    }
}
