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

package ch.protonmail.android.mailmessage.presentation.usecase

import ch.protonmail.android.mailmessage.domain.usecase.ConvertPlainTextIntoHtml
import org.junit.Assert.assertEquals
import org.junit.Test

internal class ConvertPlainTextToHtmlTest {

    private val convertPlainTextIntoHtml = ConvertPlainTextIntoHtml()

    @Test
    fun shouldParseStringIntoHtmlAndEscapeCharacters() {
        // Given
        val plainTextMessage = """
            A message
            with body & new lines.
            
            With some other characters at the end <> /\
        """.trimIndent()

        val expected = """
            <p dir="ltr">A message<br>
            with body &amp; new lines.</p>
            <p dir="ltr">With some other characters at the end &lt;&gt; /\</p>

        """.trimIndent()

        // When
        val actual = convertPlainTextIntoHtml(plainTextMessage)

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun shouldParseStringIntoHtmlAndNotTransformLinksByDefault() {
        // Given
        val plainTextMessage = """
            A message
            with body & new lines.
            
            But also
            with a couple of links.
            
            noreply@proton.me
            https://www.proton.me
            
            With some other characters at the end <> /\
        """.trimIndent()

        val expected = """
            <p dir="ltr">A message<br>
            with body &amp; new lines.</p>
            <p dir="ltr">But also<br>
            with a couple of links.</p>
            <p dir="ltr">noreply@proton.me<br>
            https://www.proton.me</p>
            <p dir="ltr">With some other characters at the end &lt;&gt; /\</p>

        """.trimIndent()

        // When
        val actual = convertPlainTextIntoHtml(plainTextMessage)

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun shouldParseStringIntoHtmlAndTransformLinksWhenExplicitlyRequested() {
        // Given
        val plainTextMessage = """
            A message
            with body & new lines.
            
            But also
            with a couple of links.
            
            noreply@proton.me
            https://www.proton.me
            
            With some other characters at the end <> /\
        """.trimIndent()

        val expected = """
            <p dir="ltr">A message<br>
            with body &amp; new lines.</p>
            <p dir="ltr">But also<br>
            with a couple of links.</p>
            <p dir="ltr"><a href="mailto:noreply@proton.me">noreply@proton.me</a><br>
            <a href="https://www.proton.me">https://www.proton.me</a></p>
            <p dir="ltr">With some other characters at the end &lt;&gt; /\</p>

        """.trimIndent()

        // When
        val actual = convertPlainTextIntoHtml(plainTextMessage, autoTransformLinks = true)

        // Then
        assertEquals(expected, actual)
    }
}
