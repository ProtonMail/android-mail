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
import ch.protonmail.android.mailmessage.presentation.model.MessageBodyWithType
import ch.protonmail.android.mailmessage.presentation.model.MimeTypeUiModel
import io.mockk.called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifySequence
import org.junit.Test

internal class TransformDecryptedMessageBodyTest {

    private val injectCssIntoDecryptedMessageBody = mockk<InjectCssIntoDecryptedMessageBody>()
    private val convertPlainTextIntoHtml = mockk<ConvertPlainTextIntoHtml>()
    private val transformDecryptedMessageBody = TransformDecryptedMessageBody(
        injectCssIntoDecryptedMessageBody,
        convertPlainTextIntoHtml
    )

    @Test
    fun `should only perform css injection when mime type is html`() {
        // Given
        val messageBodyWithType = MessageBodyWithType("message body", MimeTypeUiModel.Html)
        every { injectCssIntoDecryptedMessageBody(messageBodyWithType) } returns ""
        every { convertPlainTextIntoHtml(messageBodyWithType.messageBody, autoTransformLinks = true) } returns ""

        // When
        transformDecryptedMessageBody(messageBodyWithType)

        // Then
        verify(exactly = 1) { injectCssIntoDecryptedMessageBody(messageBodyWithType) }
        verify { convertPlainTextIntoHtml wasNot called }
    }

    @Test
    fun `should only transform line breaks when mime type is plain text`() {
        // Given
        val messageBodyWithType = MessageBodyWithType("message body", MimeTypeUiModel.PlainText)
        val convertedMessageBodyWithType = MessageBodyWithType("converted message body", MimeTypeUiModel.Html)
        every {
            convertPlainTextIntoHtml(messageBodyWithType.messageBody, autoTransformLinks = true)
        } returns "converted message body"
        every { injectCssIntoDecryptedMessageBody(convertedMessageBodyWithType) } returns ""

        // When
        transformDecryptedMessageBody(messageBodyWithType)

        // Then
        verifySequence {
            convertPlainTextIntoHtml(messageBodyWithType.messageBody, autoTransformLinks = true)
            injectCssIntoDecryptedMessageBody(convertedMessageBodyWithType)
        }
    }
}
