/*
 * Copyright (c) 2025 Proton Technologies AG
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

package ch.protonmail.android.mailattachments.domain.usecase

import android.net.Uri
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailattachments.domain.model.AttachmentId
import ch.protonmail.android.mailattachments.domain.model.DecryptedAttachment
import ch.protonmail.android.mailattachments.domain.repository.AttachmentRepository
import ch.protonmail.android.mailattachments.domain.sample.AttachmentMetadataSamples
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import kotlin.test.Test
import kotlin.test.assertEquals

internal class GetDecryptedAttachmentUriTest {

    private val userId = UserIdSample.Primary
    private val attachmentId = AttachmentId("1")

    private val uri = mockk<Uri>()

    private val attachmentRepository = mockk<AttachmentRepository>()

    private val getDecryptedAttachmentUri = GetDecryptedAttachmentUri(attachmentRepository)

    @Test
    fun `should return the uri of the decrypted attachment`() = runTest {
        // Given
        coEvery {
            attachmentRepository.getAttachment(userId, attachmentId)
        } returns decryptedAttachment(uri).right()

        // When
        val result = getDecryptedAttachmentUri(userId, attachmentId)

        // Then
        assertEquals(uri.right(), result)
    }

    @Test
    fun `should ask the repository every time so an evicted file is re-materialised`() = runTest {
        // Given
        val firstUri = mockk<Uri>()
        val secondUri = mockk<Uri>()
        coEvery {
            attachmentRepository.getAttachment(userId, attachmentId)
        } returnsMany listOf(decryptedAttachment(firstUri).right(), decryptedAttachment(secondUri).right())

        // When
        val firstResult = getDecryptedAttachmentUri(userId, attachmentId)
        val secondResult = getDecryptedAttachmentUri(userId, attachmentId)

        // Then
        assertEquals(firstUri.right(), firstResult)
        assertEquals(secondUri.right(), secondResult)
        coVerify(exactly = 2) { attachmentRepository.getAttachment(userId, attachmentId) }
    }

    @Test
    fun `should return an error when the attachment file can no longer be read`() = runTest {
        // Given
        coEvery {
            attachmentRepository.getAttachment(userId, attachmentId)
        } returns DataError.Local.FailedToReadFile.left()

        // When
        val result = getDecryptedAttachmentUri(userId, attachmentId)

        // Then
        assertEquals(DataError.Local.FailedToReadFile.left(), result)
    }

    @Test
    fun `should return an error when the attachment can not be fetched`() = runTest {
        // Given
        coEvery {
            attachmentRepository.getAttachment(userId, attachmentId)
        } returns DataError.Remote.NoNetwork.left()

        // When
        val result = getDecryptedAttachmentUri(userId, attachmentId)

        // Then
        assertEquals(DataError.Remote.NoNetwork.left(), result)
    }

    @Test
    fun `should resolve the attachment for the requested user`() = runTest {
        // Given
        val otherUserId = UserId("other-user")
        coEvery {
            attachmentRepository.getAttachment(otherUserId, attachmentId)
        } returns decryptedAttachment(uri).right()

        // When
        val result = getDecryptedAttachmentUri(otherUserId, attachmentId)

        // Then
        assertEquals(uri.right(), result)
        coVerify(exactly = 1) { attachmentRepository.getAttachment(otherUserId, attachmentId) }
    }

    private fun decryptedAttachment(fileUri: Uri) = DecryptedAttachment(
        metadata = AttachmentMetadataSamples.Pdf,
        fileUri = fileUri,
        fileName = "file.pdf"
    )
}
