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

package ch.protonmail.android.mailcommon.data.file

import android.content.Intent
import android.text.SpannedString
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class MailToIntentExtensionsTest {

    @AfterTest
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `adds the address and subject sent as extras to an empty mailto`() {
        // Given
        val intent = mailToIntent(
            dataString = "mailto:",
            arrays = mapOf(Intent.EXTRA_EMAIL to arrayOf("support@example.com")),
            texts = mapOf(Intent.EXTRA_SUBJECT to "Thank you")
        )

        // When
        val mailTo = intent.getMailToUri()

        // Then
        assertEquals("mailto:?to=support%40example.com&subject=Thank%20you", mailTo)
    }

    @Test
    fun `returns the mailto unchanged when the intent has no extras`() {
        // Given
        val intent = mailToIntent(dataString = "mailto:support@example.com?subject=Hi")

        // When
        val mailTo = intent.getMailToUri()

        // Then
        assertEquals("mailto:support@example.com?subject=Hi", mailTo)
    }

    @Test
    fun `returns a mailto with a malformed escape unchanged when the intent has no extras`() {
        // Given
        val intent = mailToIntent(dataString = "mailto:a@b.com?subject=100%")

        // When
        val mailTo = intent.getMailToUri()

        // Then
        assertEquals("mailto:a@b.com?subject=100%", mailTo)
    }

    @Test
    fun `adds the extras to a mailto with a malformed escape`() {
        // Given
        val intent = mailToIntent(
            dataString = "mailto:a@b.com?body=%zz",
            arrays = mapOf(Intent.EXTRA_CC to arrayOf("c@x.com"))
        )

        // When
        val mailTo = intent.getMailToUri()

        // Then
        assertEquals("mailto:a@b.com?body=%zz&cc=c%40x.com", mailTo)
    }

    @Test
    fun `adds an address that is only part of an address already in the mailto`() {
        // Given
        val intent = mailToIntent(
            dataString = "mailto:ba@x.com",
            arrays = mapOf(Intent.EXTRA_EMAIL to arrayOf("a@x.com"))
        )

        // When
        val mailTo = intent.getMailToUri()

        // Then
        assertEquals("mailto:ba@x.com?to=a%40x.com", mailTo)
    }

    @Test
    fun `adds an address that only appears in the subject of the mailto`() {
        // Given
        val intent = mailToIntent(
            dataString = "mailto:?subject=Ping%20support@x.com",
            arrays = mapOf(Intent.EXTRA_EMAIL to arrayOf("support@x.com"))
        )

        // When
        val mailTo = intent.getMailToUri()

        // Then
        assertEquals("mailto:?subject=Ping%20support@x.com&to=support%40x.com", mailTo)
    }

    @Test
    fun `keeps the values already in the mailto instead of adding them again`() {
        // Given
        val intent = mailToIntent(
            dataString = "mailto:Support@example.com?subject=Hello",
            arrays = mapOf(
                Intent.EXTRA_EMAIL to arrayOf("support@example.com"),
                Intent.EXTRA_CC to arrayOf("cc@example.com")
            ),
            texts = mapOf(Intent.EXTRA_SUBJECT to "Thank you")
        )

        // When
        val mailTo = intent.getMailToUri()

        // Then
        assertEquals("mailto:Support@example.com?subject=Hello&cc=cc%40example.com", mailTo)
    }

    @Test
    fun `does not add an address already in a to param of the mailto`() {
        // Given
        val intent = mailToIntent(
            dataString = "mailto:?to=a%40x.com;b@x.com",
            arrays = mapOf(Intent.EXTRA_EMAIL to arrayOf("A@x.com", "b@x.com"))
        )

        // When
        val mailTo = intent.getMailToUri()

        // Then
        assertEquals("mailto:?to=a%40x.com;b@x.com", mailTo)
    }

    @Test
    fun `keeps the subject of the mailto whatever the case of its name`() {
        // Given
        val intent = mailToIntent(
            dataString = "mailto:a@b.com?SUBJECT=Hello",
            texts = mapOf(Intent.EXTRA_SUBJECT to "Thank you")
        )

        // When
        val mailTo = intent.getMailToUri()

        // Then
        assertEquals("mailto:a@b.com?SUBJECT=Hello", mailTo)
    }

    @Test
    fun `adds the subject when the mailto only has an encoded subject inside its body`() {
        // Given
        val intent = mailToIntent(
            dataString = "mailto:a@b.com?body=x%26subject%3Dy",
            texts = mapOf(Intent.EXTRA_SUBJECT to "Thank you")
        )

        // When
        val mailTo = intent.getMailToUri()

        // Then
        assertEquals("mailto:a@b.com?body=x%26subject%3Dy&subject=Thank%20you", mailTo)
    }

    @Test
    fun `adds every recipient once across to, cc and bcc`() {
        // Given
        val intent = mailToIntent(
            dataString = "mailto:",
            arrays = mapOf(
                Intent.EXTRA_EMAIL to arrayOf("a@x.com", "A@x.com", " b@x.com "),
                Intent.EXTRA_CC to arrayOf("b@x.com", "c@x.com"),
                Intent.EXTRA_BCC to arrayOf("d@x.com", "")
            )
        )

        // When
        val mailTo = intent.getMailToUri()

        // Then
        assertEquals("mailto:?to=a%40x.com&to=b%40x.com&cc=c%40x.com&bcc=d%40x.com", mailTo)
    }

    @Test
    fun `adds the extras after the query a mailto already has`() {
        // Given
        val intent = mailToIntent(
            dataString = "mailto:a@b.com?subject=Hi",
            arrays = mapOf(Intent.EXTRA_EMAIL to arrayOf("c@x.com"))
        )

        // When
        val mailTo = intent.getMailToUri()

        // Then
        assertEquals("mailto:a@b.com?subject=Hi&to=c%40x.com", mailTo)
    }

    @Test
    fun `adds no empty param to a mailto ending with a question mark`() {
        // Given
        val intent = mailToIntent(
            dataString = "mailto:a@b.com?",
            texts = mapOf(Intent.EXTRA_SUBJECT to "Hi")
        )

        // When
        val mailTo = intent.getMailToUri()

        // Then
        assertEquals("mailto:a@b.com?subject=Hi", mailTo)
    }

    @Test
    fun `adds the extras before the fragment of the mailto`() {
        // Given
        val intent = mailToIntent(
            dataString = "mailto:a@b.com#frag",
            texts = mapOf(Intent.EXTRA_SUBJECT to "Hi")
        )

        // When
        val mailTo = intent.getMailToUri()

        // Then
        assertEquals("mailto:a@b.com?subject=Hi#frag", mailTo)
    }

    @Test
    fun `percent-encodes reserved characters, line breaks and non-ASCII text in the body`() {
        // Given
        val intent = mailToIntent(
            dataString = "mailto:",
            texts = mapOf(Intent.EXTRA_TEXT to "a&b=c?d#e+f g\nçé")
        )

        // When
        val mailTo = intent.getMailToUri()

        // Then
        assertEquals("mailto:?body=a%26b%3Dc%3Fd%23e%2Bf%20g%0A%C3%A7%C3%A9", mailTo)
    }

    @Test
    fun `reads an address sent as a single String`() {
        // Given
        val intent = mailToIntent(
            dataString = "mailto:",
            strings = mapOf(Intent.EXTRA_EMAIL to "a@x.com, b@x.com")
        )

        // When
        val mailTo = intent.getMailToUri()

        // Then
        assertEquals("mailto:?to=a%40x.com&to=b%40x.com", mailTo)
    }

    @Test
    fun `reads a body sent as styled text`() {
        // Given
        val body = mockk<SpannedString> { every { this@mockk.toString() } returns "Hello" }
        val intent = mailToIntent(dataString = "mailto:", texts = mapOf(Intent.EXTRA_TEXT to body))

        // When
        val mailTo = intent.getMailToUri()

        // Then
        assertEquals("mailto:?body=Hello", mailTo)
    }

    @Test
    fun `adds the subject and body when the mailto has them empty`() {
        // Given
        val intent = mailToIntent(
            dataString = "mailto:a@b.com?subject=&body=",
            texts = mapOf(Intent.EXTRA_SUBJECT to "Hi", Intent.EXTRA_TEXT to "Hello")
        )

        // When
        val mailTo = intent.getMailToUri()

        // Then
        assertEquals("mailto:a@b.com?subject=&body=&subject=Hi&body=Hello", mailTo)
    }

    @Test
    fun `reads addresses sent as a single String separated by semicolons`() {
        // Given
        val intent = mailToIntent(
            dataString = "mailto:",
            strings = mapOf(Intent.EXTRA_EMAIL to "a@x.com;b@x.com")
        )

        // When
        val mailTo = intent.getMailToUri()

        // Then
        assertEquals("mailto:?to=a%40x.com&to=b%40x.com", mailTo)
    }

    private fun mailToIntent(
        dataString: String,
        arrays: Map<String, Array<String>> = emptyMap(),
        strings: Map<String, String> = emptyMap(),
        texts: Map<String, CharSequence> = emptyMap()
    ): Intent = mockk {
        every { this@mockk.dataString } returns dataString
        every { getStringArrayExtra(any()) } answers { arrays[firstArg()] }
        every { getStringExtra(any()) } answers { strings[firstArg()] }
        every { getCharSequenceExtra(any()) } answers { texts[firstArg()] }
    }
}
