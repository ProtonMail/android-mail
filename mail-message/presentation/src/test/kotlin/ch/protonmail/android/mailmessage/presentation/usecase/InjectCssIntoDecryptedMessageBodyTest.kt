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

import java.io.InputStream
import android.content.Context
import ch.protonmail.android.mailmessage.presentation.R
import ch.protonmail.android.mailmessage.presentation.model.MessageBodyWithType
import ch.protonmail.android.mailmessage.presentation.model.MimeTypeUiModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class InjectCssIntoDecryptedMessageBodyTest {

    private val cssInputStream = mockk<InputStream>(relaxUnitFun = true)
    private val context = mockk<Context> {
        every { resources } returns mockk {
            every { openRawResource(R.raw.css_reset_with_media_scheme_plus_custom_props) } returns cssInputStream
        }
    }

    private val injectCssIntoDecryptedMessageBody = InjectCssIntoDecryptedMessageBody(context)

    @BeforeTest
    fun setUp() {
        mockkStatic("kotlin.io.ByteStreamsKt")
    }

    @AfterTest
    fun breakDown() {
        unmockkAll()
    }

    @Test
    fun `should return the unchanged message body if the message is plain text`() {
        // When
        val messageBodyWithType = MessageBodyWithType(TestData.plainTextMessageBody, MimeTypeUiModel.PlainText)
        val actual = injectCssIntoDecryptedMessageBody(messageBodyWithType)

        // Then
        assertEquals(TestData.plainTextMessageBody, actual)
    }

    @Test
    fun `should inject light mode css in html message body when the app is in light mode`() {
        // Given
        every { cssInputStream.readBytes() } returns TestData.css.encodeToByteArray()
        val messageBodyWithType = MessageBodyWithType(TestData.htmlMessageBody.trimIndent(), MimeTypeUiModel.Html)
        val expected = """
            <html>
             <head>
              <meta name="viewport" content="width=device-width, user-scalable=yes">
              <style>${TestData.css}</style>
             </head>
             <body>
              <p>HTML message body</p>
             </body>
            </html>
        """.trimIndent()

        // When
        val actual = injectCssIntoDecryptedMessageBody(messageBodyWithType)

        // Then
        assertEquals(expected, actual)
    }

    object TestData {

        const val css = "css"
        const val htmlMessageBody = """
            <html>
             <head>
             </head>
             <body>
              <p>HTML message body</p>
             </body>
            </html>
        """
        const val plainTextMessageBody = "Plain text message body"
    }
}
