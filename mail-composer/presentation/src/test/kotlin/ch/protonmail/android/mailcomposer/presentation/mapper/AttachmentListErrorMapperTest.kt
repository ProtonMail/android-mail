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

package ch.protonmail.android.mailcomposer.presentation.mapper

import ch.protonmail.android.mailattachments.domain.model.AddAttachmentError
import ch.protonmail.android.mailattachments.domain.model.AttachmentError
import ch.protonmail.android.mailattachments.domain.model.AttachmentMetadataWithState
import ch.protonmail.android.mailattachments.domain.model.AttachmentState
import io.mockk.every
import io.mockk.mockk
import junit.framework.TestCase.assertNull
import kotlin.test.Test
import kotlin.test.assertEquals

internal class AttachmentListErrorMapperTest {

    private fun createAttachmentWithError(reason: AttachmentError): AttachmentMetadataWithState {
        val item = mockk<AttachmentMetadataWithState>()
        every { item.attachmentState } returns AttachmentState.Error(reason)
        return item
    }

    @Test
    fun `returns null when there are no errors`() {
        // Given
        val attachment = mockk<AttachmentMetadataWithState>()
        every { attachment.attachmentState } returns AttachmentState.Uploaded

        // When
        val result = AttachmentListErrorMapper.toAttachmentAddErrorWithList(listOf(attachment))

        // Then
        assertNull(result)
    }

    @Test
    fun `returns TooManyAttachments error when present`() {
        // Given
        val attachment = createAttachmentWithError(AttachmentError.AddAttachment(AddAttachmentError.TooManyAttachments))

        // When
        val result = AttachmentListErrorMapper.toAttachmentAddErrorWithList(listOf(attachment))

        // Then
        assertEquals(AddAttachmentError.TooManyAttachments, result?.error)
        assertEquals(listOf(attachment), result?.failedAttachments)
    }

    @Test
    fun `returns AttachmentTooLarge when no higher-priority error exists`() {
        // Given
        val attachment = createAttachmentWithError(AttachmentError.AddAttachment(AddAttachmentError.AttachmentTooLarge))

        // When
        val result = AttachmentListErrorMapper.toAttachmentAddErrorWithList(listOf(attachment))

        // Then
        assertEquals(AddAttachmentError.AttachmentTooLarge, result?.error)
        assertEquals(listOf(attachment), result?.failedAttachments)
    }

    @Test
    fun `returns InvalidDraftMessage when no higher-priority error exists`() {
        // Given
        val attachment =
            createAttachmentWithError(AttachmentError.AddAttachment(AddAttachmentError.InvalidDraftMessage))

        // When
        val result = AttachmentListErrorMapper.toAttachmentAddErrorWithList(listOf(attachment))

        // Then
        assertEquals(AddAttachmentError.InvalidDraftMessage, result?.error)
        assertEquals(listOf(attachment), result?.failedAttachments)
    }

    @Test
    fun `returns EncryptionError when no higher-priority error exists`() {
        // Given
        val attachment = createAttachmentWithError(AttachmentError.AddAttachment(AddAttachmentError.EncryptionError))

        // When
        val result = AttachmentListErrorMapper.toAttachmentAddErrorWithList(listOf(attachment))

        // Then
        assertEquals(AddAttachmentError.EncryptionError, result?.error)
        assertEquals(listOf(attachment), result?.failedAttachments)
    }

    @Test
    fun `returns StorageQuotaExceeded error when present`() {
        // Given
        val attachment =
            createAttachmentWithError(AttachmentError.AddAttachment(AddAttachmentError.StorageQuotaExceeded))

        // When
        val result = AttachmentListErrorMapper.toAttachmentAddErrorWithList(listOf(attachment))

        // Then
        assertEquals(AddAttachmentError.StorageQuotaExceeded, result?.error)
        assertEquals(listOf(attachment), result?.failedAttachments)
    }

    @Test
    fun `returns highest priority error when multiple errors exist`() {
        // Given
        val storageExceeded =
            createAttachmentWithError(AttachmentError.AddAttachment(AddAttachmentError.StorageQuotaExceeded))
        val encryptionError =
            createAttachmentWithError(AttachmentError.AddAttachment(AddAttachmentError.EncryptionError))

        // When
        val result = AttachmentListErrorMapper.toAttachmentAddErrorWithList(listOf(storageExceeded, encryptionError))

        // Then
        assertEquals(AddAttachmentError.StorageQuotaExceeded, result?.error)
        assertEquals(listOf(storageExceeded), result?.failedAttachments)
    }
}

