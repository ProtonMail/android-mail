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

package ch.protonmail.android.mailattachments.domain.usecase

import android.net.Uri
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailattachments.domain.model.AttachmentId
import ch.protonmail.android.mailattachments.domain.model.AttachmentOpenMode
import ch.protonmail.android.mailattachments.domain.model.DecryptedAttachment
import ch.protonmail.android.mailattachments.domain.model.OpenAttachmentIntentValues
import ch.protonmail.android.mailattachments.domain.repository.AttachmentRepository
import ch.protonmail.android.mailattachments.domain.sample.AttachmentMetadataSamples
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GetAttachmentIntentValuesTest {

    private val userId = UserIdSample.Primary
    private val attachmentId = AttachmentId("1")

    private val uri = mockk<Uri>()

    private val decryptedAttachment by lazy {
        DecryptedAttachment(
            metadata = AttachmentMetadataSamples.Pdf,
            fileUri = uri,
            fileName = "file.pdf"
        )
    }

    private val attachmentRepository = mockk<AttachmentRepository>()

    private val getAttachmentIntentValues = GetAttachmentIntentValues(attachmentRepository)

    @Test
    fun `should return intent values when attachment and metadata is locally available`() = runTest {
        // Given
        val id = AttachmentId("6")
        coEvery {
            attachmentRepository.getAttachment(
                userId = userId,
                attachmentId = id
            )
        } returns decryptedAttachment.right()

        // When
        val result = getAttachmentIntentValues(userId, AttachmentOpenMode.Open, id)

        // Then
        assertEquals(
            OpenAttachmentIntentValues(
                openMode = AttachmentOpenMode.Open,
                name = "ebook.pdf",
                mimeType = "application/pdf",
                uri = uri,
                userId = userId,
                attachmentId = id
            ).right(),
            result
        )
    }

    @Test
    fun `should return no data cached when attachment is not locally available`() = runTest {
        // Given
        coEvery {
            attachmentRepository.getAttachment(
                userId = userId,
                attachmentId = attachmentId
            )
        } returns DataError.Local.NoDataCached.left()

        // When
        val result = getAttachmentIntentValues(userId, AttachmentOpenMode.Open, attachmentId)

        // Then
        assertEquals(DataError.Local.NoDataCached.left(), result)
    }

    @Test
    fun `should return attachment repository error when getting attachment fails`() = runTest {
        // Given
        coEvery {
            attachmentRepository.getAttachment(
                userId = userId,
                attachmentId = attachmentId
            )
        } returns DataError.Local.CryptoError.left()

        // When
        val result = getAttachmentIntentValues(userId, AttachmentOpenMode.Open, attachmentId)

        // Then
        assertEquals(DataError.Local.CryptoError.left(), result)
    }
}
