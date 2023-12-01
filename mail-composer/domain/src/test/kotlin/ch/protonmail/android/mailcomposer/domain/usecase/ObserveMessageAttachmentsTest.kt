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

package ch.protonmail.android.mailcomposer.domain.usecase

import app.cash.turbine.test
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailmessage.domain.repository.DraftStateRepository
import ch.protonmail.android.mailcomposer.domain.sample.DraftStateSample
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.mailmessage.domain.sample.MessageAttachmentSample
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ObserveMessageAttachmentsTest {

    private val userId = UserIdSample.Primary
    private val messageId = MessageIdSample.LocalDraft

    private val draftStateRepository = mockk<DraftStateRepository>()
    private val messageRepository = mockk<MessageRepository>()


    private val observeMessageAttachments = ObserveMessageAttachments(draftStateRepository, messageRepository)

    @Test
    fun `should observe draft state and message repo to observe message attachments`() = runTest {
        // Given
        every { draftStateRepository.observe(userId, messageId) } returns flowOf(DraftStateSample.NewDraftState.right())
        val expectedAttachment = listOf(MessageAttachmentSample.invoice)
        every { messageRepository.observeMessageAttachments(userId, messageId) } returns flowOf(
            expectedAttachment
        )

        // When
        val actual = observeMessageAttachments(userId, messageId)

        // Then
        assertEquals(expectedAttachment, actual.first())
        verify { messageRepository.observeMessageAttachments(userId, messageId) }
    }

    @Test
    fun `should update message attachment observation but not emitting when draft state changes`() = runTest {
        // Given
        val draftStateFlow = MutableStateFlow(DraftStateSample.NewDraftState.right())
        every { draftStateRepository.observe(userId, messageId) } returns draftStateFlow
        val expectedAttachment = listOf(MessageAttachmentSample.invoice)

        every { messageRepository.observeMessageAttachments(userId, messageId) } returns flowOf(
            expectedAttachment
        )
        every { messageRepository.observeMessageAttachments(userId, MessageIdSample.RemoteDraft) } returns flowOf(
            expectedAttachment
        )

        // When
        observeMessageAttachments(userId, messageId).test {
            // Then
            assertEquals(expectedAttachment, awaitItem())
            draftStateFlow.value = DraftStateSample.RemoteDraftState.right()
            expectNoEvents()

            coVerifyOrder {
                draftStateRepository.observe(userId, messageId)
                messageRepository.observeMessageAttachments(userId, messageId)
                messageRepository.observeMessageAttachments(userId, MessageIdSample.RemoteDraft)
            }
        }
    }

    @Test
    fun `should load attachments when messageId changes and attachment observer returns empty list`() = runTest {
        // Given
        val expectedAttachment = listOf(MessageAttachmentSample.invoice)
        val initialDraftStateFlow = MutableStateFlow(DraftStateSample.NewDraftState.right())
        val updatedDraftStateFlow = MutableStateFlow(DraftStateSample.RemoteDraftState.right())
        val attachmentFlow = MutableStateFlow(expectedAttachment)

        every {
            draftStateRepository.observe(userId, messageId)
        } returns initialDraftStateFlow andThen updatedDraftStateFlow

        every { messageRepository.observeMessageAttachments(userId, messageId) } returns attachmentFlow
        every { messageRepository.observeMessageAttachments(userId, MessageIdSample.RemoteDraft) } returns flowOf(
            expectedAttachment
        )

        // When
        observeMessageAttachments(userId, messageId).test {
            // Then
            assertEquals(expectedAttachment, awaitItem())
            attachmentFlow.value = emptyList()
            expectNoEvents()

            coVerifyOrder {
                draftStateRepository.observe(userId, messageId)
                messageRepository.observeMessageAttachments(userId, messageId)
                draftStateRepository.observe(userId, messageId)
                messageRepository.observeMessageAttachments(userId, MessageIdSample.RemoteDraft)
            }
        }
    }
}
