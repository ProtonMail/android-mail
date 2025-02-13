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

package ch.protonmail.android.mailcomposer.presentation.facade

import android.net.Uri
import ch.protonmail.android.mailcomposer.domain.model.SenderEmail
import ch.protonmail.android.mailcomposer.domain.usecase.DeleteAllAttachments
import ch.protonmail.android.mailcomposer.domain.usecase.DeleteAttachment
import ch.protonmail.android.mailcomposer.domain.usecase.ObserveMessageAttachments
import ch.protonmail.android.mailcomposer.domain.usecase.ReEncryptAttachments
import ch.protonmail.android.mailcomposer.domain.usecase.StoreAttachments
import ch.protonmail.android.mailcomposer.domain.usecase.StoreExternalAttachments
import ch.protonmail.android.mailmessage.domain.model.AttachmentId
import ch.protonmail.android.mailmessage.domain.model.AttachmentSyncState
import ch.protonmail.android.mailmessage.domain.model.MessageId
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class AttachmentsFacadeTest {

    private val observeMessageAttachments = mockk<ObserveMessageAttachments>(relaxed = true)
    private val storeAttachments = mockk<StoreAttachments>(relaxed = true)
    private val storeExternalAttachments = mockk<StoreExternalAttachments>(relaxed = true)
    private val deleteAttachment = mockk<DeleteAttachment>(relaxed = true)
    private val deleteAllAttachments = mockk<DeleteAllAttachments>(relaxed = true)
    private val reEncryptAttachments = mockk<ReEncryptAttachments>(relaxed = true)

    lateinit var attachmentsFacade: AttachmentsFacade

    @BeforeTest
    fun setup() {
        attachmentsFacade = AttachmentsFacade(
            observeMessageAttachments,
            storeAttachments,
            storeExternalAttachments,
            deleteAttachment,
            deleteAllAttachments,
            reEncryptAttachments
        )
    }

    @Test
    fun `should proxy observeMessageAttachments accordingly`() {
        // Given
        val userId = UserId("user-id")
        val messageId = MessageId("message-id")

        // When
        attachmentsFacade.observeMessageAttachments(userId, messageId)

        // Then
        verify(exactly = 1) { observeMessageAttachments(userId, messageId) }
    }

    @Test
    fun `should proxy storeAttachments accordingly`() = runTest {
        // Given
        val userId = UserId("user-id")
        val messageId = MessageId("message-id")
        val senderEmail = SenderEmail("sender@email.com")
        val uriList = mockk<List<Uri>>()

        // When
        attachmentsFacade.storeAttachments(userId, messageId, senderEmail, uriList)

        // Then
        coVerify(exactly = 1) { storeAttachments(userId, messageId, senderEmail, uriList) }
    }

    @Test
    fun `should proxy storeExternalAttachments accordingly`() = runTest {
        // Given
        val userId = UserId("user-id")
        val messageId = MessageId("message-id")
        val syncState = mockk<AttachmentSyncState>()

        // When
        attachmentsFacade.storeExternalAttachments(userId, messageId, syncState)

        // Then
        coVerify(exactly = 1) { storeExternalAttachments(userId, messageId, syncState) }
    }

    @Test
    fun `should proxy deleteAttachment accordingly`() = runTest {
        // Given
        val userId = UserId("user-id")
        val messageId = MessageId("message-id")
        val senderEmail = SenderEmail("sender@email.com")
        val attachmentId = AttachmentId("id")

        // When
        attachmentsFacade.deleteAttachment(userId, messageId, senderEmail, attachmentId)

        // Then
        coVerify(exactly = 1) { deleteAttachment(userId, senderEmail, messageId, attachmentId) }
    }

    @Test
    fun `should proxy deleteAllAttachments accordingly`() = runTest {
        // Given
        val userId = UserId("user-id")
        val messageId = MessageId("message-id")
        val senderEmail = SenderEmail("sender@email.com")

        // When
        attachmentsFacade.deleteAllAttachments(userId, senderEmail, messageId)

        // Then
        coVerify(exactly = 1) { deleteAllAttachments(userId, senderEmail, messageId) }
    }

    @Test
    fun `should proxy reEncryptAttachments accordingly`() = runTest {
        // Given
        val userId = UserId("user-id")
        val messageId = MessageId("message-id")
        val previousSenderEmail = SenderEmail("prev-sender@email.com")
        val newSenderEmail = SenderEmail("new-sender@email.com")

        // When
        attachmentsFacade.reEncryptAttachments(userId, messageId, previousSenderEmail, newSenderEmail)

        // Then
        coVerify(exactly = 1) { reEncryptAttachments(userId, messageId, previousSenderEmail, newSenderEmail) }
    }
}
