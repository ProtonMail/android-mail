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
import kotlin.test.Test
import kotlin.test.assertEquals

class SanitizeHtmlOfDecryptedMessageBodyTest {

    private val sanitizeHtmlOfDecryptedMessageBody = SanitizeHtmlOfDecryptedMessageBody()

    @Test
    fun `should not sanitize message body if it is plain text`() {
        // Given
        val messageBodyWithType = MessageBodyWithType(TestData.plainTextMessageBody, MimeTypeUiModel.PlainText)
        val expected = "Plain text message body"

        // When
        val actual = sanitizeHtmlOfDecryptedMessageBody(messageBodyWithType)

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `should remove blacklisted elements from message body`() {
        // Given
        val messageBodyWithType = MessageBodyWithType(
            TestData.messageBodyWithBlacklistedElements,
            MimeTypeUiModel.Html
        )
        val expected = """
            <html>
             <head>
             </head>
             <body>
              <p>HTML message body</p> <label for="name">First name:</label>
              <br>
              <br>
             </body>
            </html>
        """.trimIndent()

        // When
        val actual = sanitizeHtmlOfDecryptedMessageBody(messageBodyWithType)

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `should remove ping attributes from message body`() {
        // Given
        val messageBodyWithType = MessageBodyWithType(
            TestData.messageBodyWithPingAttributes,
            MimeTypeUiModel.Html
        )
        val expected = """
            <html>
             <head>
             </head>
             <body>
              <p>HTML message body</p><a href="URL">link</a>
             </body>
            </html>
        """.trimIndent()

        // When
        val actual = sanitizeHtmlOfDecryptedMessageBody(messageBodyWithType)

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `should remove link elements from message body`() {
        // Given
        val messageBodyWithType = MessageBodyWithType(
            TestData.messageBodyWithLinkElements,
            MimeTypeUiModel.Html
        )
        val expected = """
            <html>
             <head>
             </head>
             <body>
              <p>HTML message body</p>
             </body>
            </html>
        """.trimIndent()

        // When
        val actual = sanitizeHtmlOfDecryptedMessageBody(messageBodyWithType)

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `should remove event attributes from message body`() {
        // Given
        val messageBodyWithType = MessageBodyWithType(
            TestData.messageBodyWithEventAttributes,
            MimeTypeUiModel.Html
        )
        val expected = """
            <html>
             <head>
             </head>
             <body>
              <p>HTML message body</p><button>Click</button>
             </body>
            </html>
        """.trimIndent()

        // When
        val actual = sanitizeHtmlOfDecryptedMessageBody(messageBodyWithType)

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `should remove content editable from message body`() {
        // Given
        val messageBodyWithType = MessageBodyWithType(
            TestData.messageBodyWithContentEditableAttribute,
            MimeTypeUiModel.Html
        )
        val expected = """
            <html>
             <head>
             </head>
             <body>
              <p style="color:blue">HTML p one</p>
              <div>
               HTML div one
              </div>
              <p id="some_id">Another p</p>
             </body>
            </html>
        """.trimIndent()

        // When
        val actual = sanitizeHtmlOfDecryptedMessageBody(messageBodyWithType)

        // Then
        assertEquals(expected, actual)
    }

    object TestData {

        const val plainTextMessageBody = "Plain text message body"
        const val messageBodyWithBlacklistedElements = """
            <html>
             <head>
              <meta name="description" content="Message body">
             </head>
             <body>
              <p>HTML message body</p>
              <form action="/action_page.php" method="get">
               <label for="name">First name:</label>
               <input type="text" id="name" name="name"><br><br>
               <input type="submit" value="Submit">
              </form>
             </body>
            </html>
        """
        const val messageBodyWithPingAttributes = """
            <html>
             <head>
             </head>
             <body>
              <p>HTML message body</p>
              <a href="URL" ping="PING_URL">link</a>
             </body>
            </html>
        """
        const val messageBodyWithLinkElements = """
            <html>
             <head>
              <link rel="prefetch">
              <link rel="stylesheet">
              <link rel="preload">
              <link rel="alternate stylesheet">
             </head>
             <body>
              <p>HTML message body</p>
             </body>
            </html>
        """
        const val messageBodyWithEventAttributes = """
            <html>
             <head>
             </head>
             <body onLoad="loadFunction()" onError="errorFunction()">
              <p>HTML message body</p>
              <button onClick="clickFunction()">Click</button>
             </body>
            </html>
        """
        const val messageBodyWithContentEditableAttribute = """
            <html>
             <head>
             </head>
             <body>
              <p contenteditable=true style="color:blue">HTML p one</p>
              <div contenteditable=true>HTML div one</div>
              <p id="some_id">Another p</p>
             </body>
            </html>
        """
    }
}
