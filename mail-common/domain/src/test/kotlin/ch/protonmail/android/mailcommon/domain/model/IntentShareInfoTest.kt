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
import org.junit.Assert.assertFalse
import org.junit.Test

class IntentShareInfoTest {

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
        val intentShareInfo = IntentShareInfo.Empty.copy(
            attachmentUris = listOf(uri1.toString(), uri2.toString()),
            emailRecipientTo = toRecipients.toList(),
            emailRecipientCc = ccRecipients.toList(),
            emailRecipientBcc = bccRecipients.toList(),
            emailBody = body,
            emailSubject = subject
        )

        // When
        val encodedDecodedIntentShareInfo = intentShareInfo.encode().decode()

        // Then
        assertEquals(intentShareInfo, encodedDecodedIntentShareInfo)
    }

    @Test
    fun `encode should not add forward slashes when encoding very long content`() {
        // Given
        every { uri1.toString() } returns "content://test1"
        every { uri2.toString() } returns "content://test2"
        val toRecipients = arrayOf("toemail1@example.com", "toemail2@example.com")
        val ccRecipients = arrayOf("ccemail1@example.com", "ccemail2@example.com")
        val bccRecipients = arrayOf("bccemail1@example.com", "bccemail2@example.com")
        val subject = "Test Subject"
        val body = AVeryLongString
        val intentShareInfo = IntentShareInfo.Empty.copy(
            attachmentUris = listOf(uri1.toString(), uri2.toString()),
            emailRecipientTo = toRecipients.toList(),
            emailRecipientCc = ccRecipients.toList(),
            emailRecipientBcc = bccRecipients.toList(),
            emailBody = body,
            emailSubject = subject
        )

        // When
        val encodedFileShareInfo = intentShareInfo.encode()
        val encodedDecodedIntentShareInfo = encodedFileShareInfo.decode()

        // Then
        assertFalse(encodedFileShareInfo.emailBody!!.contains("/"))
        assertEquals(intentShareInfo, encodedDecodedIntentShareInfo)
    }

    private companion object {

        @Suppress("MaxLineLength")
        const val AVeryLongString =
            "Lorem ipsum の痛みは改善され、エリートの脂肪が蓄積され、一時的に痛みが発生し、労働力と痛みが大きくなります。 iaculis nunc sed augueのウルトリス。アンティのティンシダント・イド・アリケット・リスス・フェウギア。 Vitae tortor condimentum lacinia quis vel eros donec。ウルナのテルスにあるscelerisque purus semper eget duis。 Ut sem nulla pharetra diam sit amet.オルナーレのEnim lobortis scelerisque fermentum dui faucibus。ペレンテスクの生息地モルビ トリスティック セネクトゥスとネットスなど。テルスのリスス・ビベラ・アディピシングで。 nisl nisi scelerisqueのViverra accumsan。"
    }
}
