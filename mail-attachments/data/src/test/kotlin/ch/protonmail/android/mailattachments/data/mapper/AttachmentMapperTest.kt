/*
 * Copyright (c) 2026 Proton Technologies AG
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

package ch.protonmail.android.mailattachments.data.mapper

import ch.protonmail.android.mailattachments.domain.model.AttachmentError
import ch.protonmail.android.mailattachments.domain.model.RemoveAttachmentError
import ch.protonmail.android.mailcommon.domain.model.DataError
import org.junit.Test
import uniffi.mail_uniffi.DraftAttachmentError
import uniffi.mail_uniffi.DraftAttachmentRemoveError
import uniffi.mail_uniffi.DraftAttachmentRemoveErrorReason
import uniffi.mail_uniffi.ProtonError
import kotlin.test.assertEquals

internal class AttachmentMapperTest {

    @Test
    fun `maps draft attachment remove error to remove attachment error`() {
        // Given
        val error = DraftAttachmentError.Remove(
            DraftAttachmentRemoveError.Reason(DraftAttachmentRemoveErrorReason.AttachmentDoesNotExist)
        )
        val expected = AttachmentError.RemoveAttachment(RemoveAttachmentError.AttachmentDoesNotExist)

        // When
        val actual = error.toDraftAttachmentError()

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `maps remove error reason attachment does not exist`() {
        // Given
        val error = DraftAttachmentRemoveError.Reason(DraftAttachmentRemoveErrorReason.AttachmentDoesNotExist)

        // When
        val actual = error.toRemoveAttachmentError()

        // Then
        assertEquals(RemoveAttachmentError.AttachmentDoesNotExist, actual)
    }

    @Test
    fun `maps remove error reason bad request keeping the server message`() {
        // Given
        val message = "bad request details"
        val error = DraftAttachmentRemoveError.Reason(
            DraftAttachmentRemoveErrorReason.BadRequest(message)
        )

        // When
        val actual = error.toRemoveAttachmentError()

        // Then
        assertEquals(RemoveAttachmentError.BadRequest(message), actual)
    }

    @Test
    fun `maps remove other error to remove attachment other error`() {
        // Given
        val error = DraftAttachmentRemoveError.Other(ProtonError.Network)

        // When
        val actual = error.toRemoveAttachmentError()

        // Then
        assertEquals(RemoveAttachmentError.Other(DataError.Remote.NoNetwork), actual)
    }
}
