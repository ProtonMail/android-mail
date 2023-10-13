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

package ch.protonmail.android.composer.data.remote

import androidx.work.WorkManager
import arrow.core.Either
import ch.protonmail.android.composer.data.remote.response.UploadAttachmentResponse
import ch.protonmail.android.mailcommon.data.mapper.toEither
import ch.protonmail.android.mailcommon.data.worker.Enqueuer
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailmessage.domain.model.AttachmentId
import me.proton.core.domain.entity.UserId
import me.proton.core.network.data.ApiProvider
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

class AttachmentRemoteDataSourceImpl @Inject constructor(
    private val enqueuer: Enqueuer,
    private val workManager: WorkManager,
    private val apiProvider: ApiProvider
) : AttachmentRemoteDataSource {

    override suspend fun uploadAttachment(
        userId: UserId,
        uploadAttachmentModel: UploadAttachmentModel
    ): Either<DataError.Remote, UploadAttachmentResponse> {
        val octetStream = "application/octet-stream".toMediaType()
        val keyPacket = MultipartBody.Part.createFormData(
            name = "KeyPackets",
            filename = "KeyPackets",
            uploadAttachmentModel.keyPacket.toRequestBody(octetStream)
        )
        val dataPacket = MultipartBody.Part.createFormData(
            name = "DataPacket",
            filename = uploadAttachmentModel.fileName,
            uploadAttachmentModel.attachment.asRequestBody(uploadAttachmentModel.mimeType.toMediaType())
        )
        val signature = MultipartBody.Part.createFormData(
            name = "Signature",
            filename = "Signature",
            uploadAttachmentModel.signature.toRequestBody(octetStream)
        )

        return apiProvider.get<AttachmentApi>(userId).invoke {
            uploadAttachment(
                filename = uploadAttachmentModel.fileName.toRequestBody(),
                messageID = uploadAttachmentModel.messageId.id.toRequestBody(),
                mimeType = uploadAttachmentModel.mimeType.toRequestBody(),
                keyPackets = keyPacket,
                dataPacket = dataPacket,
                signature = signature
            )
        }.toEither()
    }

    override fun deleteAttachmentFromDraft(userId: UserId, attachmentId: AttachmentId) {
        enqueuer.enqueueUniqueWork<DeleteAttachmentWorker>(
            userId = userId,
            workerId = attachmentId.id,
            params = DeleteAttachmentWorker.params(userId, attachmentId)
        )
    }

    override fun cancelAttachmentUpload(attachmentId: AttachmentId) {
        workManager.getWorkInfosForUniqueWork(attachmentId.id).get().forEach {
            workManager.cancelUniqueWork(attachmentId.id)
        }
    }
}
