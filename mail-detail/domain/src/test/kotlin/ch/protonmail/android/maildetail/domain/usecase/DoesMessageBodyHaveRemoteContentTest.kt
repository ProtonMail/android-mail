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

package ch.protonmail.android.maildetail.domain.usecase

import ch.protonmail.android.mailcommon.domain.sample.UserAddressSample
import ch.protonmail.android.mailmessage.domain.model.DecryptedMessageBody
import ch.protonmail.android.mailmessage.domain.model.MimeType
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DoesMessageBodyHaveRemoteContentTest {

    private val doesMessageBodyHaveRemoteContent = DoesMessageBodyHaveRemoteContent()

    @Test
    fun `should return false when the message body is plain text`() {
        // Given
        val messageId = MessageIdSample.build()
        val messageBody = DecryptedMessageBody(
            messageId,
            TestData.plainTextMessageBody,
            MimeType.PlainText,
            emptyList(),
            UserAddressSample.PrimaryAddress
        )

        // When
        val actual = doesMessageBodyHaveRemoteContent(messageBody)

        // Then
        assertFalse(actual)
    }

    @Test
    fun `should return true when the message body contains img element with src attribute with remote url`() {
        // Given
        val messageId = MessageIdSample.build()
        val messageBody = DecryptedMessageBody(
            messageId,
            TestData.messageBodyWithRemoteUrlInImgElement,
            MimeType.Html,
            emptyList(),
            UserAddressSample.PrimaryAddress
        )

        // When
        val actual = doesMessageBodyHaveRemoteContent(messageBody)

        // Then
        assertTrue(actual)
    }

    @Test
    fun `should return true when the message body contains style element with remote url`() {
        // Given
        val messageId = MessageIdSample.build()
        val messageBody = DecryptedMessageBody(
            messageId,
            TestData.messageBodyWithRemoteUrlInStyleElement,
            MimeType.Html,
            emptyList(),
            UserAddressSample.PrimaryAddress
        )

        // When
        val actual = doesMessageBodyHaveRemoteContent(messageBody)

        // Then
        assertTrue(actual)
    }

    @Test
    fun `should return true when the message body contains an element with a style attribute with remote url`() {
        // Given
        val messageId = MessageIdSample.build()
        val messageBody = DecryptedMessageBody(
            messageId,
            TestData.messageBodyWithRemoteUrlInStyleAttribute,
            MimeType.Html,
            emptyList(),
            UserAddressSample.PrimaryAddress
        )

        // When
        val actual = doesMessageBodyHaveRemoteContent(messageBody)

        // Then
        assertTrue(actual)
    }

    @Test
    fun `should return false if no remote content was detected`() {
        // Given
        val messageId = MessageIdSample.build()
        val messageBody = DecryptedMessageBody(
            messageId,
            TestData.messageBodyWithoutRemoteContent,
            MimeType.Html,
            emptyList(),
            UserAddressSample.PrimaryAddress
        )

        // When
        val actual = doesMessageBodyHaveRemoteContent(messageBody)

        // Then
        assertFalse(actual)
    }

    object TestData {

        const val plainTextMessageBody = "Plain text message body."

        const val messageBodyWithRemoteUrlInImgElement = """
            <html>
             <head>
             </head>
             <body>
              <img src="https://proton.me/static/5739c69ac1a01c887e1d8b3154f15b5a/proton-logo.svg">
             </body>
            </html>
        """

        const val messageBodyWithRemoteUrlInStyleElement = """
            <html>
             <head>
             </head>
             <body>
              <style>
               p {
                 background-image: url('https://proton.me/static/5739c69ac1a01c887e1d8b3154f15b5a/proton-logo.svg');
               }
              </style>
             </body>
            </html>
        """

        @Suppress("MaxLineLength")
        const val messageBodyWithRemoteUrlInStyleAttribute = """
            <html>
             <head>
             </head>
             <body>
              <p style="background-image: url('https://proton.me/static/5739c69ac1a01c887e1d8b3154f15b5a/proton-logo.svg');">
             </body>
            </html>
        """

        const val messageBodyWithoutRemoteContent = """
            <html>
             <head>
             </head>
             <body>
              <p>
               Message body without remote content
              </p>
             </body>
            </html>
        """
    }
}
