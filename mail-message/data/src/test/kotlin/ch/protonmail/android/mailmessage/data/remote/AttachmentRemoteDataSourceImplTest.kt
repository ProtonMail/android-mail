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

package ch.protonmail.android.mailmessage.data.remote

import java.net.UnknownHostException
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.data.worker.Enqueuer
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.model.NetworkError
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailmessage.data.remote.worker.GetAttachmentWorker
import ch.protonmail.android.mailmessage.domain.model.AttachmentId
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
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

class AttachmentRemoteDataSourceImplTest {

    private val userId = UserIdSample.Primary
    private val messageId = MessageIdSample.Invoice
    private val attachmentId = AttachmentId("attachmentId")
    private val responseBody = mockk<ResponseBody>()

    private val sessionProvider = mockk<SessionProvider> {
        coEvery { getSessionId(userId) } returns SessionId("testSessionId")
    }
    private val attachmentApi = mockk<AttachmentApi> {
        coEvery { getAttachment(attachmentId.id) } returns responseBody
    }
    private val apiManagerFactory = mockk<ApiManagerFactory> {
        every { create(any(), AttachmentApi::class) } returns TestApiManager(attachmentApi)
    }
    private val enqueuer: Enqueuer = mockk {
        every {
            this@mockk.enqueueUniqueWork<GetAttachmentWorker>(
                userId = userId,
                workerId = attachmentId.id,
                params = GetAttachmentWorker.params(userId, messageId, attachmentId),
                constraints = null
            )
        } returns mockk()
    }
    private val apiProvider = ApiProvider(apiManagerFactory, sessionProvider, DefaultDispatcherProvider())
    private val attachmentRemoteDataSource = AttachmentRemoteDataSourceImpl(apiProvider, enqueuer)

    @Test
    fun `should enqueues work to get attachment`() = runTest {
        // When
        attachmentRemoteDataSource.enqueueGetAttachmentWorker(userId, messageId, attachmentId)

        // Then
        coVerify {
            enqueuer.enqueueUniqueWork<GetAttachmentWorker>(
                userId = userId,
                workerId = attachmentId.id,
                params = GetAttachmentWorker.params(userId, messageId, attachmentId),
                constraints = null
            )
        }
    }

    @Test
    fun `should return api response mapped to either when embedded image api call was successful`() = runTest {
        // Given
        val attachmentByteArray = "attachment".toByteArray()
        every { responseBody.bytes() } returns attachmentByteArray
        val expected = attachmentByteArray.right()

        // When
        val actual = attachmentRemoteDataSource.getAttachment(userId, messageId, attachmentId)

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `should return api response mapped to either when embedded image api call has failed`() = runTest {
        // Given
        coEvery { attachmentApi.getAttachment(attachmentId.id) } throws UnknownHostException()
        val expected = DataError.Remote.Http(
            NetworkError.NoNetwork, "No error message found", isRetryable = true
        ).left()

        // When
        val actual = attachmentRemoteDataSource.getAttachment(userId, messageId, attachmentId)

        // Then
        assertEquals(expected, actual)
    }
}
