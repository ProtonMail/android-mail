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

import ch.protonmail.android.composer.data.remote.response.UploadAttachmentResponse
import me.proton.core.network.data.protonApi.BaseRetrofitApi
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.http.DELETE
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface AttachmentApi : BaseRetrofitApi {

    @Suppress("LongParameterList")
    @Multipart
    @POST("mail/v4/attachments")
    suspend fun uploadAttachment(
        @Part("Filename") filename: RequestBody,
        @Part("MessageID") messageID: RequestBody,
        @Part("MIMEType") mimeType: RequestBody,
        @Part keyPackets: MultipartBody.Part,
        @Part dataPacket: MultipartBody.Part,
        @Part signature: MultipartBody.Part
    ): UploadAttachmentResponse

    @DELETE("mail/v4/attachments/{AttachmentID}")
    suspend fun deleteAttachment(@Path("AttachmentID") attachmentId: String): ResponseBody

}
