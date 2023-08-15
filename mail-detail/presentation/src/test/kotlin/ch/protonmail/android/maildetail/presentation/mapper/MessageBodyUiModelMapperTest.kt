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
import ch.protonmail.android.maildetail.domain.usecase.ShouldShowEmbeddedImages
import ch.protonmail.android.maildetail.domain.usecase.ShouldShowRemoteContent
import ch.protonmail.android.maildetail.presentation.model.MessageBodyAttachmentsUiModel
import ch.protonmail.android.maildetail.presentation.model.MessageBodyUiModel
import ch.protonmail.android.maildetail.presentation.model.MimeTypeUiModel
import ch.protonmail.android.maildetail.presentation.sample.AttachmentUiModelSample
import ch.protonmail.android.maildetail.presentation.usecase.InjectCssIntoDecryptedMessageBody
import ch.protonmail.android.maildetail.presentation.usecase.SanitizeHtmlOfDecryptedMessageBody
import ch.protonmail.android.mailmessage.domain.model.MimeType
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

    private val doesMessageBodyHaveEmbeddedImages = mockk<DoesMessageBodyHaveEmbeddedImages> {
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
        doesMessageBodyHaveEmbeddedImages = doesMessageBodyHaveEmbeddedImages,
        injectCssIntoDecryptedMessageBody = injectCssIntoDecryptedMessageBody,
        sanitizeHtmlOfDecryptedMessageBody = sanitizeHtmlOfDecryptedMessageBody,
        shouldShowEmbeddedImages = shouldShowEmbeddedImages,
        shouldShowRemoteContent = shouldShowRemoteContent
    )

    @Test
    fun `plain text message body is correctly mapped to a message body ui model`() = runTest {
        // Given
        val messageBody = DecryptedMessageBody(decryptedMessageBody, MimeType.PlainText)
        val expected = MessageBodyUiModel(
            decryptedMessageBody,
            MimeTypeUiModel.PlainText,
            shouldShowEmbeddedImages = false,
            shouldShowRemoteContent = false,
            shouldShowEmbeddedImagesBanner = false,
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
            shouldShowEmbeddedImages = false,
            shouldShowRemoteContent = false,
            shouldShowEmbeddedImagesBanner = false,
            MessageBodyAttachmentsUiModel(
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
        val messageBody = DecryptedMessageBody(decryptedMessageBody, MimeType.Html)
        val expected = MessageBodyUiModel(
            sanitizedDecryptedMessageBodyWithCss,
            MimeTypeUiModel.Html,
            shouldShowEmbeddedImages = false,
            shouldShowRemoteContent = false,
            shouldShowEmbeddedImagesBanner = false,
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
            val messageBody = DecryptedMessageBody(decryptedMessageBody, MimeType.Html)
            val expected = MessageBodyUiModel(
                decryptedMessageBody,
                MimeTypeUiModel.Html,
                shouldShowEmbeddedImages = false,
                shouldShowRemoteContent = true,
                shouldShowEmbeddedImagesBanner = false,
                attachments = null
            )
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
            val messageBody = DecryptedMessageBody(decryptedMessageBody, MimeType.Html)
            val expected = MessageBodyUiModel(
                decryptedMessageBody,
                MimeTypeUiModel.Html,
                shouldShowEmbeddedImages = false,
                shouldShowRemoteContent = false,
                shouldShowEmbeddedImagesBanner = false,
                attachments = null
            )
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
            val messageBody = DecryptedMessageBody(decryptedMessageBody, MimeType.Html)
            val expected = MessageBodyUiModel(
                decryptedMessageBody,
                MimeTypeUiModel.Html,
                shouldShowEmbeddedImages = true,
                shouldShowRemoteContent = false,
                shouldShowEmbeddedImagesBanner = false,
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
            val messageBody = DecryptedMessageBody(decryptedMessageBody, MimeType.Html)
            val expected = MessageBodyUiModel(
                decryptedMessageBody,
                MimeTypeUiModel.Html,
                shouldShowEmbeddedImages = false,
                shouldShowRemoteContent = false,
                shouldShowEmbeddedImagesBanner = true,
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
        val messageBody = MessageBodyTestData.RAW_ENCRYPTED_MESSAGE_BODY
        val expected = MessageBodyUiModel(
            messageBody,
            MimeTypeUiModel.PlainText,
            shouldShowEmbeddedImages = false,
            shouldShowRemoteContent = false,
            shouldShowEmbeddedImagesBanner = false,
            attachments = null
        )

        // When
        val actual = messageBodyUiModelMapper.toUiModel(messageBody)

        // Then
        assertEquals(expected, actual)
    }
}
