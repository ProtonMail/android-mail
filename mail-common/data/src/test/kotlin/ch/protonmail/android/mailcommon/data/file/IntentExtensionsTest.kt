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

package ch.protonmail.android.mailcommon.data.file

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import ch.protonmail.android.mailcommon.domain.model.IntentShareInfo
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class IntentExtensionsTest {

    private val uri1 = mockk<Uri> {
        every { scheme } returns "content"
    }
    private val uri2 = mockk<Uri> {
        every { scheme } returns "content"
    }
    private val mailToUri = mockk<Uri> {
        every { scheme } returns "mailto"
    }

    @Before
    fun setUp() {
        mockkStatic(Uri::class)
    }

    @After
    fun tearDown() {
        unmockkStatic(Uri::class)
    }

    @Test
    fun `should return empty share info when intent has unhandled action`() {
        // Given
        val intent = mockIntent(action = "unhandled_action")

        // When
        val fileShareInfo = intent.getShareInfo()

        // Then
        assertEquals(IntentShareInfo.Empty, fileShareInfo)
    }

    @Test
    fun `should return empty share info when no Uri in intent data for action view`() {
        // Given
        val intent = mockIntent(action = Intent.ACTION_VIEW, data = null)

        // When
        val fileShareInfo = intent.getShareInfo()

        // Then
        assertEquals(IntentShareInfo.Empty, fileShareInfo)
    }

    @Test
    fun `should return file uri when uri defined intent data for action view`() {
        // Given
        every { uri1.toString() } returns "content://test1"
        val intent = mockIntent(action = Intent.ACTION_VIEW, data = uri1)
        val expected = IntentShareInfo.Empty.copy(attachmentUris = listOf(uri1.toString()))

        // When
        val fileShareInfo = intent.getShareInfo()

        // Then
        assertEquals(expected, fileShareInfo)
    }

    @Test
    fun `should return share info with recipient when uri has mailto scheme in intent data for action view`() {
        // Given
        val recipientEmail = "user1@example.com"
        every { Uri.decode(any()) } returns recipientEmail
        every { Uri.parse(any()) } returns mailToUri
        every { mailToUri.toString() } returns "mailto:$recipientEmail"
        val intent = mockIntent(action = Intent.ACTION_VIEW, data = mailToUri)
        val expected = IntentShareInfo.Empty.copy(emailRecipientTo = listOf(recipientEmail))

        // When
        val fileShareInfo = intent.getShareInfo()

        // Then
        assertEquals(expected, fileShareInfo)
    }

    @Test
    fun `should return share info with multiple recipients when uri has mailto scheme in intent for action view`() {
        // Given
        val recipientEmail1 = "user1@example.com"
        val recipientEmail2 = "user2@example.com"
        val recipientEmail3 = "user3@example.com"
        every { Uri.decode(any()) } returns "$recipientEmail1,$recipientEmail2,$recipientEmail3"
        every { Uri.parse(any()) } returns mailToUri
        every { mailToUri.toString() } returns "mailto:$recipientEmail1,$recipientEmail2,$recipientEmail3"
        val intent = mockIntent(action = Intent.ACTION_VIEW, data = mailToUri)
        val expected = IntentShareInfo.Empty.copy(
            emailRecipientTo = listOf(
                recipientEmail1,
                recipientEmail2,
                recipientEmail3
            )
        )

        // When
        val fileShareInfo = intent.getShareInfo()

        // Then
        assertEquals(expected, fileShareInfo)
    }

    @Test
    fun `should return empty share info when no Uri in intent data for action sendto`() {
        // Given
        val intent = mockIntent(action = Intent.ACTION_SENDTO, data = null)

        // When
        val fileShareInfo = intent.getShareInfo()

        // Then
        assertEquals(IntentShareInfo.Empty, fileShareInfo)
    }

    @Test
    fun `should return file uri when uri defined intent data for action sendto`() {
        // Given
        every { uri1.toString() } returns "content://test1"
        val intent = mockIntent(action = Intent.ACTION_SENDTO, data = uri1)
        val expected = IntentShareInfo.Empty.copy(attachmentUris = listOf(uri1.toString()))

        // When
        val fileShareInfo = intent.getShareInfo()

        // Then
        assertEquals(expected, fileShareInfo)
    }

    @Test
    fun `should get single uri from intent extra stream given action is send`() {
        // Given
        every { uri1.toString() } returns "content://test1"
        val intent = mockIntent(action = Intent.ACTION_SEND, extraStreamUri = uri1)
        val expected = IntentShareInfo.Empty.copy(attachmentUris = listOf(uri1.toString()))

        // when
        val fileShareInfo = intent.getShareInfo()

        // then
        assertEquals(expected, fileShareInfo)
    }

    @Test
    fun `should return empty share info when intent extra stream is null given action is send`() {
        // Given
        val intent = mockIntent(action = Intent.ACTION_SEND, extraStreamUri = null)

        // when
        val fileShareInfo = intent.getShareInfo()

        // then
        assertEquals(IntentShareInfo.Empty, fileShareInfo)
    }

    @Test
    fun `should return empty share info when extra stream is null and action is send`() {
        // Given
        val intent = mockIntent(action = Intent.ACTION_SEND, extraStreamUri = null)

        // when
        val fileShareInfo = intent.getShareInfo()

        // then
        assertEquals(IntentShareInfo.Empty, fileShareInfo)
    }

    @Test
    fun `should return uri when intent has clipData with a uri and action is send single`() {
        // Given
        every { uri1.toString() } returns "content://test1"
        val clipData = mockk<ClipData> {
            every { itemCount } returns 1
            every { getItemAt(0) } returns mockk { every { uri } returns uri1 }
        }
        val intent = mockIntent(action = Intent.ACTION_SEND, clipData = clipData)
        val expected = IntentShareInfo.Empty.copy(attachmentUris = listOf(uri1.toString()))

        // When
        val fileShareInfo = intent.getShareInfo()

        // Then
        assertEquals(expected, fileShareInfo)
    }

    @Test
    fun `should return uri when intent has no clipData but extra stream contains uri and action is send single`() {
        // Given
        every { uri1.toString() } returns "content://test1"
        val intent = mockIntent(action = Intent.ACTION_SEND, clipData = null, extraStreamUri = uri1)
        val expected = IntentShareInfo.Empty.copy(attachmentUris = listOf(uri1.toString()))

        // When
        val fileShareInfo = intent.getShareInfo()

        // Then
        assertEquals(expected, fileShareInfo)
    }

    @Test
    fun `should return multiple uris when intent has clipData with list of uris and action is send multiple`() {
        // Given
        every { uri1.toString() } returns "content://test1"
        every { uri2.toString() } returns "content://test2"
        val clipData = mockk<ClipData> {
            every { itemCount } returns 2
            every { getItemAt(0) } returns mockk { every { uri } returns uri1 }
            every { getItemAt(1) } returns mockk { every { uri } returns uri2 }
        }
        val intent = mockIntent(action = Intent.ACTION_SEND_MULTIPLE, clipData = clipData)
        val expected = IntentShareInfo.Empty.copy(attachmentUris = listOf(uri1.toString(), uri2.toString()))

        // When
        val fileShareInfo = intent.getShareInfo()

        // Then
        assertEquals(expected, fileShareInfo)
    }

    @Test
    fun `should return share info when intent has no clipdata but extra stream has uris and action is send multiple`() {
        // Given
        every { uri1.toString() } returns "content://test1"
        every { uri2.toString() } returns "content://test2"
        val intent = mockIntent(
            action = Intent.ACTION_SEND_MULTIPLE,
            extraStreamUriList = arrayListOf(uri1, uri2)
        )
        val expected = IntentShareInfo.Empty.copy(attachmentUris = listOf(uri1.toString(), uri2.toString()))

        // When
        val fileShareInfo = intent.getShareInfo()

        // Then
        assertEquals(expected, fileShareInfo)
    }

    @Test
    fun `should return share info with all email data when intent contains all information`() {
        // Given
        every { uri1.toString() } returns "content://test1"
        every { uri2.toString() } returns "content://test2"
        val toRecipients = arrayOf("toemail1@example.com", "toemail2@example.com")
        val ccRecipients = arrayOf("ccemail1@example.com", "ccemail2@example.com")
        val bccRecipients = arrayOf("bccemail1@example.com", "bccemail2@example.com")
        val subject = "Test Subject"
        val body = "Test Body"

        val intent = mockIntent(
            action = Intent.ACTION_SEND_MULTIPLE,
            extraStreamUriList = arrayListOf(uri1, uri2),
            extraRecipientTo = toRecipients,
            extraRecipientCc = ccRecipients,
            extraRecipientBcc = bccRecipients,
            extraSubject = subject,
            extraText = body
        )

        val expected = IntentShareInfo.Empty.copy(
            attachmentUris = listOf(uri1.toString(), uri2.toString()),
            emailRecipientTo = toRecipients.toList(),
            emailRecipientCc = ccRecipients.toList(),
            emailRecipientBcc = bccRecipients.toList(),
            emailBody = body,
            emailSubject = subject
        )

        // When
        val fileShareInfo = intent.getShareInfo()

        // Then
        assertEquals(expected, fileShareInfo)
    }

    @Test
    fun `should ignore null uri when parsing attachments`() {
        // Given
        val clipData = mockk<ClipData> {
            every { itemCount } returns 1
            every { getItemAt(0).uri } returns null
        }
        val intent = mockIntent(Intent.ACTION_SEND, clipData = clipData)

        // When
        val shareInfo = intent.getShareInfo()

        // Then
        assertEquals(IntentShareInfo.Empty, shareInfo)
    }

    private fun mockIntent(
        action: String = "",
        data: Uri? = null,
        clipData: ClipData? = null,
        extraStreamUri: Uri? = null,
        extraStreamUriList: ArrayList<Uri>? = null,
        extraSubject: String? = null,
        extraRecipientTo: Array<String>? = null,
        extraRecipientCc: Array<String>? = null,
        extraRecipientBcc: Array<String>? = null,
        extraText: String? = null
    ): Intent {
        return mockk {
            every { this@mockk.action } returns action
            every { this@mockk.data } returns data
            every { this@mockk.clipData } returns clipData
            every { this@mockk.getParcelableExtra<Uri>(Intent.EXTRA_STREAM) } returns extraStreamUri
            every { this@mockk.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM) } returns extraStreamUriList
            every { this@mockk.getStringExtra(Intent.EXTRA_SUBJECT) } returns extraSubject
            every { this@mockk.getStringArrayExtra(Intent.EXTRA_EMAIL) } returns extraRecipientTo
            every { this@mockk.getStringArrayExtra(Intent.EXTRA_CC) } returns extraRecipientCc
            every { this@mockk.getStringArrayExtra(Intent.EXTRA_BCC) } returns extraRecipientBcc
            every { this@mockk.getStringExtra(Intent.EXTRA_TEXT) } returns extraText
        }
    }
}
