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

import java.net.UnknownHostException
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.composer.data.remote.response.UploadAttachmentResponse
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.model.NetworkError
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailmessage.data.remote.resource.AttachmentResource
import ch.protonmail.android.mailmessage.domain.model.MessageId
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.crypto.common.pgp.EncryptedFile
import me.proton.core.network.data.ApiManagerFactory
import me.proton.core.network.data.ApiProvider
import me.proton.core.network.domain.session.SessionId
import me.proton.core.network.domain.session.SessionProvider
import me.proton.core.test.android.api.TestApiManager
import me.proton.core.util.kotlin.DefaultDispatcherProvider
import kotlin.test.Test
import kotlin.test.assertEquals

class AttachmentRemoteDataSourceImplTest {

    private val userId = UserIdSample.Primary
    private val sessionProvider = mockk<SessionProvider> {
        coEvery { getSessionId(userId) } returns SessionId("test- session-id")
    }

    private val attachmentApi = mockk<AttachmentApi>()
    private val apiManagerFactory = mockk<ApiManagerFactory> {
        every { create(any(), AttachmentApi::class) } returns TestApiManager(attachmentApi)
    }

    private val apiProvider = ApiProvider(
        apiManagerFactory = apiManagerFactory,
        sessionProvider = sessionProvider,
        dispatcherProvider = DefaultDispatcherProvider()
    )

    private val attachmentRemoteDataSource by lazy { AttachmentRemoteDataSourceImpl(apiProvider) }

    @Test
    fun `upload attachment returns response when successful`() = runTest {
        // Given
        val expectedResource = UploadAttachmentResponse(
            code = 1000,
            attachment = AttachmentResource(
                id = "test-id",
                name = "test-name",
                size = 123,
                mimeType = "application/pdf",
                signature = "test-signature",
                encSignature = "test-enc-signature",
                keyPackets = "test-key-packets",
                headers = emptyMap()
            )
        )
        expectUploadSuccessful(expectedResource)

        // When
        val actual = attachmentRemoteDataSource.uploadAttachment(
            userId = userId,
            uploadAttachmentModel = UploadAttachmentModel(
                messageId = MessageId("test-message-id"),
                fileName = "test-file-name",
                mimeType = "application/pdf",
                attachment = EncryptedFile("test-attachment"),
                keyPacket = ByteArray(0),
                signature = ByteArray(0)
            )
        )

        // Then
        assertEquals(expectedResource.right(), actual)
    }

    @Test
    fun `upload attachment returns error when failed`() = runTest {
        // Given
        expectUploadFailed()

        // When
        val actual = attachmentRemoteDataSource.uploadAttachment(
            userId = userId,
            uploadAttachmentModel = UploadAttachmentModel(
                messageId = MessageId("test-message-id"),
                fileName = "test-file-name",
                mimeType = "application/pdf",
                attachment = EncryptedFile("test-attachment"),
                keyPacket = ByteArray(0),
                signature = ByteArray(0)
            )
        )

        // Then
        assertEquals(DataError.Remote.Http(NetworkError.NoNetwork, "No error message found").left(), actual)
    }

    private fun expectUploadSuccessful(expectedResource: UploadAttachmentResponse) {
        coEvery {
            attachmentApi.uploadAttachment(
                filename = any(),
                messageID = any(),
                mimeType = any(),
                keyPackets = any(),
                dataPacket = any(),
                signature = any()
            )
        } returns expectedResource
    }

    private fun expectUploadFailed() {
        coEvery {
            attachmentApi.uploadAttachment(
                filename = any(),
                messageID = any(),
                mimeType = any(),
                keyPackets = any(),
                dataPacket = any(),
                signature = any()
            )
        } throws UnknownHostException()
    }

}
