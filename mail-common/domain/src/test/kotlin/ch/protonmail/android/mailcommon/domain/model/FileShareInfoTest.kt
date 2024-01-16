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

package ch.protonmail.android.mailcommon.domain.model

import android.net.Uri
import io.mockk.every
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import org.junit.Test

class FileShareInfoTest {

    private val uri1 = mockk<Uri>()
    private val uri2 = mockk<Uri>()

    @Test
    fun `encode and decode should result in the same data content`() {
        // Given
        every { uri1.toString() } returns "content://test1"
        every { uri2.toString() } returns "content://test2"
        val toRecipients = arrayOf("toemail1@example.com", "toemail2@example.com")
        val ccRecipients = arrayOf("ccemail1@example.com", "ccemail2@example.com")
        val bccRecipients = arrayOf("bccemail1@example.com", "bccemail2@example.com")
        val subject = "Test Subject"
        val body = "Test Body"
        val fileShareInfo = FileShareInfo.Empty.copy(
            attachmentUris = listOf(uri1.toString(), uri2.toString()),
            emailRecipientTo = toRecipients.toList(),
            emailRecipientCc = ccRecipients.toList(),
            emailRecipientBcc = bccRecipients.toList(),
            emailBody = body,
            emailSubject = subject
        )

        // When
        val encodedDecodedFileShareInfo = fileShareInfo.encode().decode()

        // Then
        assertEquals(fileShareInfo, encodedDecodedFileShareInfo)
    }
}
