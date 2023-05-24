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

import ch.protonmail.android.mailcommon.data.worker.Enqueuer
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailmessage.data.remote.worker.GetAttachmentWorker
import ch.protonmail.android.mailmessage.domain.entity.AttachmentId
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AttachmentRemoteDataSourceImplTest {

    private val userId = UserIdSample.Primary
    private val messageId = MessageIdSample.Invoice
    private val attachmentId = AttachmentId("attachmentId")


    private val enqueuer: Enqueuer = mockk {
        every {
            this@mockk.enqueueUniqueWork<GetAttachmentWorker>(
                workerId = attachmentId.id,
                params = GetAttachmentWorker.params(userId, messageId, attachmentId)
            )
        } returns mockk()
    }
    private val attachmentRemoteDataSource = AttachmentRemoteDataSourceImpl(enqueuer)

    @Test
    fun `should enqueues work to get attachment`() = runTest {
        // When
        attachmentRemoteDataSource.getAttachment(userId, messageId, attachmentId)

        // Then
        coVerify {
            enqueuer.enqueueUniqueWork<GetAttachmentWorker>(
                workerId = attachmentId.id,
                params = GetAttachmentWorker.params(userId, messageId, attachmentId)
            )
        }
    }
}
