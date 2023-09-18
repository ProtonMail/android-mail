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

import ch.protonmail.android.mailmessage.domain.model.DecryptedMessageBody
import ch.protonmail.android.maildetail.domain.usecase.DoesMessageBodyHaveEmbeddedImages
import ch.protonmail.android.maildetail.domain.usecase.DoesMessageBodyHaveRemoteContent
import ch.protonmail.android.maildetail.domain.usecase.ShouldShowEmbeddedImages
import ch.protonmail.android.maildetail.domain.usecase.ShouldShowRemoteContent
import ch.protonmail.android.mailmessage.presentation.model.MessageBodyUiModel
import ch.protonmail.android.mailmessage.presentation.model.MimeTypeUiModel
import ch.protonmail.android.mailmessage.presentation.sample.AttachmentUiModelSample
import ch.protonmail.android.maildetail.presentation.usecase.InjectCssIntoDecryptedMessageBody
import ch.protonmail.android.maildetail.presentation.usecase.SanitizeHtmlOfDecryptedMessageBody
import ch.protonmail.android.mailmessage.domain.model.GetDecryptedMessageBodyError
import ch.protonmail.android.mailmessage.domain.model.MimeType
import ch.protonmail.android.mailmessage.presentation.mapper.AttachmentUiModelMapper
import ch.protonmail.android.mailmessage.presentation.model.AttachmentGroupUiModel
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.testdata.message.MessageAttachmentTestData
import ch.protonmail.android.testdata.message.MessageBodyTestData
import ch.protonmail.android.testdata.user.UserIdTestData
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class MessageBodyUiModelMapperTest {

    private val decryptedMessageBody = "Decrypted message body."
    private val sanitizedDecryptedMessageBody = "Sanitized decrypted message body."
    private val sanitizedDecryptedMessageBodyWithCss = "Sanitized decrypted message body with CSS."

    private val attachmentUiModelMapper = mockk<AttachmentUiModelMapper> {
        every { this@mockk.toUiModel(MessageAttachmentTestData.invoice) } returns AttachmentUiModelSample.invoice
        every { this@mockk.toUiModel(MessageAttachmentTestData.document) } returns AttachmentUiModelSample.document
        every {
            this@mockk.toUiModel(MessageAttachmentTestData.documentWithMultipleDots)
        } returns AttachmentUiModelSample.documentWithMultipleDots
    }
    private val doesMessageBodyHaveEmbeddedImages = mockk<DoesMessageBodyHaveEmbeddedImages> {
        every { this@mockk.invoke(any()) } returns false
    }
    private val doesMessageBodyHaveRemoteContent = mockk<DoesMessageBodyHaveRemoteContent> {
        every { this@mockk.invoke(any()) } returns false
    }
    private val injectCssIntoDecryptedMessageBody = mockk<InjectCssIntoDecryptedMessageBody> {
        every { this@mockk.invoke(decryptedMessageBody, any()) } returns decryptedMessageBody
    }
    private val sanitizeHtmlOfDecryptedMessageBody = mockk<SanitizeHtmlOfDecryptedMessageBody> {
        every { this@mockk.invoke(decryptedMessageBody, any()) } returns decryptedMessageBody
    }
    private val shouldShowEmbeddedImages = mockk<ShouldShowEmbeddedImages> {
        coEvery { this@mockk.invoke(UserIdTestData.userId) } returns false
    }
    private val shouldShowRemoteContent = mockk<ShouldShowRemoteContent> {
        coEvery { this@mockk.invoke(UserIdTestData.userId) } returns false
    }
    private val messageBodyUiModelMapper = MessageBodyUiModelMapper(
        attachmentUiModelMapper = attachmentUiModelMapper,
        doesMessageBodyHaveEmbeddedImages = doesMessageBodyHaveEmbeddedImages,
        doesMessageBodyHaveRemoteContent = doesMessageBodyHaveRemoteContent,
        injectCssIntoDecryptedMessageBody = injectCssIntoDecryptedMessageBody,
        sanitizeHtmlOfDecryptedMessageBody = sanitizeHtmlOfDecryptedMessageBody,
        shouldShowEmbeddedImages = shouldShowEmbeddedImages,
        shouldShowRemoteContent = shouldShowRemoteContent
    )

    @Test
    fun `plain text message body is correctly mapped to a message body ui model`() = runTest {
        // Given
        val messageId = MessageIdSample.build()
        val messageBody = DecryptedMessageBody(messageId, decryptedMessageBody, MimeType.PlainText)
        val expected = MessageBodyUiModel(
            messageId = messageId,
            messageBody = decryptedMessageBody,
            mimeType = MimeTypeUiModel.PlainText,
            shouldShowEmbeddedImages = false,
            shouldShowRemoteContent = false,
            shouldShowEmbeddedImagesBanner = false,
            shouldShowRemoteContentBanner = false,
            attachments = null
        )

        // When
        val actual = messageBodyUiModelMapper.toUiModel(UserIdTestData.userId, messageBody)

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `plain text message body is correctly mapped to a message body ui model with attachments`() = runTest {
        // Given
        val messageId = MessageIdSample.build()
        val messageBody = DecryptedMessageBody(
            messageId,
            decryptedMessageBody,
            MimeType.PlainText,
            listOf(
                MessageAttachmentTestData.invoice,
                MessageAttachmentTestData.document,
                MessageAttachmentTestData.documentWithMultipleDots
            )
        )
        val expected = MessageBodyUiModel(
            messageId = messageId,
            messageBody = decryptedMessageBody,
            mimeType = MimeTypeUiModel.PlainText,
            shouldShowEmbeddedImages = false,
            shouldShowRemoteContent = false,
            shouldShowEmbeddedImagesBanner = false,
            shouldShowRemoteContentBanner = false,
            attachments = AttachmentGroupUiModel(
                attachments = listOf(
                    AttachmentUiModelSample.invoice,
                    AttachmentUiModelSample.document,
                    AttachmentUiModelSample.documentWithMultipleDots
                )
            )
        )

        // When
        val actual = messageBodyUiModelMapper.toUiModel(UserIdTestData.userId, messageBody)

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `HTML message body is correctly mapped to a message body ui model`() = runTest {
        // Given
        every {
            sanitizeHtmlOfDecryptedMessageBody(decryptedMessageBody, MimeTypeUiModel.Html)
        } returns sanitizedDecryptedMessageBody
        every {
            injectCssIntoDecryptedMessageBody(sanitizedDecryptedMessageBody, MimeTypeUiModel.Html)
        } returns sanitizedDecryptedMessageBodyWithCss
        val messageId = MessageIdSample.build()
        val messageBody = DecryptedMessageBody(messageId, decryptedMessageBody, MimeType.Html)
        val expected = MessageBodyUiModel(
            messageId = messageId,
            messageBody = sanitizedDecryptedMessageBodyWithCss,
            mimeType = MimeTypeUiModel.Html,
            shouldShowEmbeddedImages = false,
            shouldShowRemoteContent = false,
            shouldShowEmbeddedImagesBanner = false,
            shouldShowRemoteContentBanner = false,
            attachments = null
        )

        // When
        val actual = messageBodyUiModelMapper.toUiModel(UserIdTestData.userId, messageBody)

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `message body is mapped to a ui model that allows showing remote content when setting value is true`() =
        runTest {
            // Given
            val messageId = MessageIdSample.build()
            val messageBody = DecryptedMessageBody(messageId, decryptedMessageBody, MimeType.Html)
            val expected = MessageBodyUiModel(
                messageId = messageId,
                messageBody = decryptedMessageBody,
                mimeType = MimeTypeUiModel.Html,
                shouldShowEmbeddedImages = false,
                shouldShowRemoteContent = true,
                shouldShowEmbeddedImagesBanner = false,
                shouldShowRemoteContentBanner = false,
                attachments = null
            )
            every { doesMessageBodyHaveRemoteContent(messageBody) } returns true
            coEvery { shouldShowRemoteContent(UserIdTestData.userId) } returns true

            // When
            val actual = messageBodyUiModelMapper.toUiModel(UserIdTestData.userId, messageBody)

            // Then
            assertEquals(expected, actual)
        }

    @Test
    fun `message body is mapped to a ui model that doesn't allow showing remote content when setting value is false`() =
        runTest {
            // Given
            val messageId = MessageIdSample.build()
            val messageBody = DecryptedMessageBody(messageId, decryptedMessageBody, MimeType.Html)
            val expected = MessageBodyUiModel(
                messageId = messageId,
                messageBody = decryptedMessageBody,
                mimeType = MimeTypeUiModel.Html,
                shouldShowEmbeddedImages = false,
                shouldShowRemoteContent = false,
                shouldShowEmbeddedImagesBanner = false,
                shouldShowRemoteContentBanner = true,
                attachments = null
            )
            every { doesMessageBodyHaveRemoteContent(messageBody) } returns true
            coEvery { shouldShowRemoteContent(UserIdTestData.userId) } returns false

            // When
            val actual = messageBodyUiModelMapper.toUiModel(UserIdTestData.userId, messageBody)

            // Then
            assertEquals(expected, actual)
        }

    @Test
    fun `message body is mapped to a ui model that allows showing embedded images when setting value is true`() =
        runTest {
            // Given
            val messageId = MessageIdSample.build()
            val messageBody = DecryptedMessageBody(messageId, decryptedMessageBody, MimeType.Html)
            val expected = MessageBodyUiModel(
                messageId = messageId,
                messageBody = decryptedMessageBody,
                mimeType = MimeTypeUiModel.Html,
                shouldShowEmbeddedImages = true,
                shouldShowRemoteContent = false,
                shouldShowEmbeddedImagesBanner = false,
                shouldShowRemoteContentBanner = false,
                attachments = null
            )
            every { doesMessageBodyHaveEmbeddedImages(messageBody) } returns true
            coEvery { shouldShowEmbeddedImages(UserIdTestData.userId) } returns true

            // When
            val actual = messageBodyUiModelMapper.toUiModel(UserIdTestData.userId, messageBody)

            // Then
            assertEquals(expected, actual)
        }

    @Test
    fun `message body is mapped to a ui model that doesn't allow showing embedded image when setting value is false`() =
        runTest {
            // Given
            val messageId = MessageIdSample.build()
            val messageBody = DecryptedMessageBody(messageId, decryptedMessageBody, MimeType.Html)
            val expected = MessageBodyUiModel(
                messageId = messageId,
                messageBody = decryptedMessageBody,
                mimeType = MimeTypeUiModel.Html,
                shouldShowEmbeddedImages = false,
                shouldShowRemoteContent = false,
                shouldShowEmbeddedImagesBanner = true,
                shouldShowRemoteContentBanner = false,
                attachments = null
            )
            every { doesMessageBodyHaveEmbeddedImages(messageBody) } returns true
            coEvery { shouldShowEmbeddedImages(UserIdTestData.userId) } returns false

            // When
            val actual = messageBodyUiModelMapper.toUiModel(UserIdTestData.userId, messageBody)

            // Then
            assertEquals(expected, actual)
        }

    @Test
    fun `string is correctly mapped to a message body ui model`() {
        // Given
        val messageId = MessageIdSample.build()
        val messageBody = MessageBodyTestData.RAW_ENCRYPTED_MESSAGE_BODY
        val expected = MessageBodyUiModel(
            messageId = messageId,
            messageBody = messageBody,
            mimeType = MimeTypeUiModel.PlainText,
            shouldShowEmbeddedImages = false,
            shouldShowRemoteContent = false,
            shouldShowEmbeddedImagesBanner = false,
            shouldShowRemoteContentBanner = false,
            attachments = null
        )

        // When
        val actual = messageBodyUiModelMapper.toUiModel(GetDecryptedMessageBodyError.Decryption(messageId, messageBody))

        // Then
        assertEquals(expected, actual)
    }
}
