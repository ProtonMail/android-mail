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

package ch.protonmail.android.maildetail.presentation.mapper

import ch.protonmail.android.maildetail.domain.model.DecryptedMessageBody
import ch.protonmail.android.maildetail.presentation.model.MessageBodyAttachmentsUiModel
import ch.protonmail.android.maildetail.presentation.model.MessageBodyUiModel
import ch.protonmail.android.maildetail.presentation.model.MimeTypeUiModel
import ch.protonmail.android.maildetail.presentation.sample.AttachmentUiModelSample
import ch.protonmail.android.maildetail.presentation.usecase.InjectCssIntoDecryptedMessageBody
import ch.protonmail.android.mailmessage.domain.entity.MimeType
import ch.protonmail.android.testdata.message.MessageAttachmentTestData
import ch.protonmail.android.testdata.message.MessageBodyTestData
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals

class MessageBodyUiModelMapperTest {

    private val decryptedMessageBody = "Decrypted message body."
    private val decryptedMessageBodyWithCss = "Decrypted message body with CSS."

    private val injectCssIntoDecryptedMessageBody = mockk<InjectCssIntoDecryptedMessageBody>()
    private val messageBodyUiModelMapper = MessageBodyUiModelMapper(injectCssIntoDecryptedMessageBody)

    @Test
    fun `plain text message body is correctly mapped to a message body ui model`() {
        // Given
        every {
            injectCssIntoDecryptedMessageBody(decryptedMessageBody, MimeTypeUiModel.PlainText)
        } returns decryptedMessageBody
        val messageBody = DecryptedMessageBody(decryptedMessageBody, MimeType.PlainText)
        val expected = MessageBodyUiModel(decryptedMessageBody, MimeTypeUiModel.PlainText, null)

        // When
        val actual = messageBodyUiModelMapper.toUiModel(messageBody)

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `plain text message body is correctly mapped to a message body ui model with attachments`() {
        // Given
        every {
            injectCssIntoDecryptedMessageBody(decryptedMessageBody, MimeTypeUiModel.PlainText)
        } returns decryptedMessageBody
        val messageBody = DecryptedMessageBody(
            decryptedMessageBody,
            MimeType.PlainText,
            listOf(
                MessageAttachmentTestData.invoice,
                MessageAttachmentTestData.document,
                MessageAttachmentTestData.documentWithMultipleDots
            )
        )
        val expected = MessageBodyUiModel(
            decryptedMessageBody,
            MimeTypeUiModel.PlainText,
            MessageBodyAttachmentsUiModel(
                attachments = listOf(
                    AttachmentUiModelSample.invoice,
                    AttachmentUiModelSample.document,
                    AttachmentUiModelSample.documentWithMultipleDots
                )
            )
        )

        // When
        val actual = messageBodyUiModelMapper.toUiModel(messageBody)

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `HTML message body is correctly mapped to a message body ui model`() {
        every {
            injectCssIntoDecryptedMessageBody(decryptedMessageBody, MimeTypeUiModel.Html)
        } returns decryptedMessageBodyWithCss
        val messageBody = DecryptedMessageBody(decryptedMessageBody, MimeType.Html)
        val expected = MessageBodyUiModel(decryptedMessageBodyWithCss, MimeTypeUiModel.Html, null)

        // When
        val actual = messageBodyUiModelMapper.toUiModel(messageBody)

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `string is correctly mapped to a message body ui model`() {
        // Given
        val messageBody = MessageBodyTestData.RAW_ENCRYPTED_MESSAGE_BODY
        val expected = MessageBodyUiModel(messageBody, MimeTypeUiModel.PlainText, null)

        // When
        val actual = messageBodyUiModelMapper.toUiModel(messageBody)

        // Then
        assertEquals(expected, actual)
    }
}
