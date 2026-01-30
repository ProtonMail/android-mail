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

package ch.protonmail.android.navigation

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import ch.protonmail.android.mailcommon.data.file.IntentExtraKeys
import ch.protonmail.android.mailcommon.domain.model.IntentShareInfo
import ch.protonmail.android.navigation.mapper.IntentMapper
import ch.protonmail.android.navigation.model.HomeNavigationEvent
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class IntentMapperTest {

    private val mapper = IntentMapper()

    private val sampleShareInfoExternal = IntentShareInfo(
        attachmentUris = listOf("content://media/1"),
        emailSubject = "Subject",
        emailRecipientTo = listOf("to@example.com"),
        emailRecipientCc = emptyList(),
        emailRecipientBcc = emptyList(),
        emailBody = "Body",
        encoded = false,
        isExternal = true
    )
    private val sampleShareInfoInternal = sampleShareInfoExternal.copy(isExternal = false)

    @Test
    fun `map launcher intent with category launcher`() {
        // Given
        val intent = mockIntent(
            action = Intent.ACTION_MAIN,
            data = null,
            externalBoolean = false,
            categories = setOf(Intent.CATEGORY_LAUNCHER)
        )

        // When
        val result = mapper.map(intent)

        // Then
        assertTrue(result is HomeNavigationEvent.LauncherIntentReceived)
    }

    @Test
    fun `map launcher intent with category default`() {
        // Given
        val intent = mockIntent(
            action = Intent.ACTION_MAIN,
            data = null,
            externalBoolean = false,
            categories = setOf(Intent.CATEGORY_DEFAULT)
        )

        // When
        val result = mapper.map(intent)

        // Then
        assertTrue(result is HomeNavigationEvent.LauncherIntentReceived)
    }

    @Test
    fun `map external share intent`() {
        // Given
        val streamUri = mockk<Uri> {
            every { scheme } returns "content"
        }
        every { streamUri.toString() } returns "content://media/1"

        val intent = mockIntent(
            action = Intent.ACTION_SEND,
            data = null,
            externalBoolean = true,
            extraSubject = sampleShareInfoExternal.emailSubject,
            extraRecipientTo = sampleShareInfoExternal.emailRecipientTo.toTypedArray(),
            extraText = sampleShareInfoExternal.emailBody,
            extraStreamUri = streamUri
        )

        // When
        val result = mapper.map(intent)

        // Then
        assertTrue(result is HomeNavigationEvent.ExternalShareIntentReceived)
        assertEquals(sampleShareInfoExternal, result.shareInfo)
    }

    @Test
    fun `map internal share intent`() {
        // Given
        val streamUri = mockk<Uri> {
            every { scheme } returns "content"
        }
        every { streamUri.toString() } returns "content://media/1"

        val intent = mockIntent(
            action = Intent.ACTION_SEND,
            data = null,
            externalBoolean = false,
            extraSubject = sampleShareInfoExternal.emailSubject,
            extraRecipientTo = sampleShareInfoExternal.emailRecipientTo.toTypedArray(),
            extraText = sampleShareInfoExternal.emailBody,
            extraStreamUri = streamUri
        )

        // When
        val result = mapper.map(intent)

        // Then
        assertTrue(result is HomeNavigationEvent.InternalShareIntentReceived)
        assertEquals(sampleShareInfoInternal, result.shareInfo)
    }

    @Test
    fun `map mailto intent`() {
        // Given
        val intent = mockIntent(
            action = Intent.ACTION_SEND,
            scheme = "mailto"
        )

        // When
        val result = mapper.map(intent)

        // Then
        assertIs<HomeNavigationEvent.MailToIntentReceived>(result)
    }

    @Test
    fun `map share intent with no valid data`() {
        // Given
        val intent = mockIntent(
            action = Intent.ACTION_SEND,
            data = null,
            externalBoolean = false
        )

        // When
        val result = mapper.map(intent)

        // Then
        assertTrue(result is HomeNavigationEvent.InvalidShareIntentReceived)
    }

    @Test
    fun `map unknown intents`() {
        // Given
        val intent = mockIntent(
            action = "some.unknown.ACTION",
            data = null,
            externalBoolean = false
        )

        // When
        val result = mapper.map(intent)

        // Then
        assertTrue(result is HomeNavigationEvent.UnknownIntentReceived)
    }


    private fun mockIntent(
        action: String = "",
        data: Uri? = null,
        scheme: String = "",
        categories: Set<String> = emptySet(),
        clipData: ClipData? = null,
        extraStreamUri: Uri? = null,
        extraStreamUriList: ArrayList<Uri>? = null,
        extraSubject: String? = null,
        extraRecipientTo: Array<String>? = null,
        extraRecipientCc: Array<String>? = null,
        extraRecipientBcc: Array<String>? = null,
        extraText: String? = null,
        externalBoolean: Boolean = false
    ): Intent {
        return mockk {
            every { this@mockk.action } returns action
            every { this@mockk.data } returns data
            every { this@mockk.scheme } returns scheme
            every { this@mockk.clipData } returns clipData
            every { this@mockk.categories } returns categories
            every { this@mockk.getParcelableExtra<Uri>(Intent.EXTRA_STREAM) } returns extraStreamUri
            every { this@mockk.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM) } returns extraStreamUriList
            every { this@mockk.getStringExtra(Intent.EXTRA_SUBJECT) } returns extraSubject
            every { this@mockk.getStringArrayExtra(Intent.EXTRA_EMAIL) } returns extraRecipientTo
            every { this@mockk.getStringArrayExtra(Intent.EXTRA_CC) } returns extraRecipientCc
            every { this@mockk.getStringArrayExtra(Intent.EXTRA_BCC) } returns extraRecipientBcc
            every { this@mockk.getStringExtra(Intent.EXTRA_TEXT) } returns extraText
            every { this@mockk.getBooleanExtra(IntentExtraKeys.EXTRA_EXTERNAL_SHARE, false) } returns externalBoolean
        }
    }
}
