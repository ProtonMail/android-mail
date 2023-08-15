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

package ch.protonmail.android.maildetail.domain.usecase

import app.cash.turbine.test
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailmessage.domain.model.AttachmentId
import ch.protonmail.android.mailmessage.domain.model.AttachmentWorkerStatus
import ch.protonmail.android.mailmessage.domain.model.MessageAttachmentMetadata
import ch.protonmail.android.mailmessage.domain.repository.AttachmentRepository
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import io.mockk.coEvery
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ObserveMessageAttachmentStatusTest {

    private val userId = UserIdSample.Primary
    private val messageId = MessageIdSample.Invoice
    private val attachmentId = AttachmentId("attachmentId")

    private val attachmentRepository = mockk<AttachmentRepository>()

    private val observeMessageAttachmentStatus = ObserveMessageAttachmentStatus(attachmentRepository)

    @Test
    fun `should emit attachment metadata when not null`() = runTest {
        // Given
        val mutableStateFlow = MutableStateFlow<MessageAttachmentMetadata?>(null)
        coEvery {
            attachmentRepository.observeAttachmentMetadata(userId, messageId, attachmentId)
        } returns mutableStateFlow

        val expectedMetadata = MessageAttachmentMetadata(
            userId = userId,
            messageId = messageId,
            attachmentId = attachmentId,
            uri = null,
            status = AttachmentWorkerStatus.Running
        )

        // When
        observeMessageAttachmentStatus(userId, messageId, attachmentId).test {
            // Then
            mutableStateFlow.emit(null)
            mutableStateFlow.emit(expectedMetadata)
            assertEquals(expectedMetadata, awaitItem())
            val resumingEvents = cancelAndConsumeRemainingEvents()
            assertEquals(0, resumingEvents.size)
        }
    }
}
