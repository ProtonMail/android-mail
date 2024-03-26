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

import ch.protonmail.android.mailmessage.presentation.model.MessageBodyWithType
import ch.protonmail.android.mailmessage.presentation.model.MimeTypeUiModel
import org.junit.Assert.assertEquals
import org.junit.Test

internal class ConvertPlainTextToHtmlTest {

    private val convertPlainTextIntoHtml = ConvertPlainTextIntoHtml()

    @Test
    fun shouldKeepBodyUnchangedWhenMimeTypeIsNotPlainText() {
        // Given
        val messageBodyWithType = MessageBodyWithType("<div>Something something</div>", MimeTypeUiModel.Html)

        // When
        val actual = convertPlainTextIntoHtml(messageBodyWithType)

        // Then
        assertEquals(messageBodyWithType.messageBody, actual)
    }

    @Test
    fun shouldParseBodyIntoHtmlAndEscapeCharactersWhenMimeTypeIsPlainText() {
        // Given
        val plainTextMessage = """
            A message
            with body & new lines.
            
            With some other characters at the end <> /\
        """.trimIndent()

        val messageBodyWithType = MessageBodyWithType(plainTextMessage, MimeTypeUiModel.PlainText)
        val expected = """
            <body style="word-wrap: break-word;">
            <p dir="ltr" style="margin-top:0; margin-bottom:0;">A message</p>
            <p dir="ltr" style="margin-top:0; margin-bottom:0;">with body &amp; new lines.</p>
            <br>
            <p dir="ltr" style="margin-top:0; margin-bottom:0;">With some other characters at the end &lt;&gt; /\</p>
            </body>
        """.trimIndent()

        // When
        val actual = convertPlainTextIntoHtml(messageBodyWithType)

        // Then
        assertEquals(expected, actual)
    }
}
