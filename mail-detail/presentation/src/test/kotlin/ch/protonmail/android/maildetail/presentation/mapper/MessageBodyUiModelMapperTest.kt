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

import ch.protonmail.android.mailattachments.domain.sample.AttachmentMetadataSamples
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcrashrecord.domain.usecase.HasMessageBodyWebViewCrashed
import ch.protonmail.android.mailfeatureflags.domain.model.FeatureFlag
import ch.protonmail.android.mailmessage.domain.model.DecryptedMessageBody
import ch.protonmail.android.mailmessage.domain.model.GetMessageBodyError
import ch.protonmail.android.mailmessage.domain.model.MessageBanner
import ch.protonmail.android.mailmessage.domain.model.MimeType
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.mailmessage.presentation.mapper.AttachmentGroupUiModelMapper
import ch.protonmail.android.mailmessage.presentation.model.MessageBodyContent
import ch.protonmail.android.mailmessage.presentation.model.MessageBodyUiModel
import ch.protonmail.android.mailmessage.presentation.model.MimeTypeUiModel
import ch.protonmail.android.mailmessage.presentation.model.ViewModePreference
import ch.protonmail.android.mailmessage.presentation.model.attachment.AttachmentGroupUiModel
import ch.protonmail.android.mailmessage.presentation.sample.AttachmentMetadataUiModelSamples
import ch.protonmail.android.testdata.message.MessageBodyTestData
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class MessageBodyUiModelMapperTest {

    private val decryptedMessageBody = "Decrypted message body."

    private val attachmentGroupUiModelMapper = mockk<AttachmentGroupUiModelMapper> {
        every { this@mockk.toUiModel(listOf(AttachmentMetadataSamples.Invoice), any()) } returns AttachmentGroupUiModel(
            attachments = listOf(AttachmentMetadataUiModelSamples.Invoice)
        )

        every {
            this@mockk.toUiModel(
                listOf(AttachmentMetadataSamples.Document), any()
            )
        } returns AttachmentGroupUiModel(
            attachments = listOf(AttachmentMetadataUiModelSamples.Document)
        )

        every {
            this@mockk.toUiModel(listOf(AttachmentMetadataSamples.DocumentWithMultipleDots), any())
        } returns AttachmentGroupUiModel(
            attachments = listOf(AttachmentMetadataUiModelSamples.DocumentWithMultipleDots)
        )

        every {
            this@mockk.toUiModel(
                listOf(AttachmentMetadataSamples.Calendar), any()
            )
        } returns AttachmentGroupUiModel(
            attachments = listOf(AttachmentMetadataUiModelSamples.Calendar)
        )

        every {
            this@mockk.toUiModel(
                listOf(
                    AttachmentMetadataSamples.Invoice,
                    AttachmentMetadataSamples.Document,
                    AttachmentMetadataSamples.DocumentWithMultipleDots
                ),
                any()
            )
        } returns AttachmentGroupUiModel(
            attachments = listOf(
                AttachmentMetadataUiModelSamples.Invoice,
                AttachmentMetadataUiModelSamples.Document,
                AttachmentMetadataUiModelSamples.DocumentWithMultipleDots
            )
        )
    }

    private val hasMessageBodyWebViewCrashed = mockk<HasMessageBodyWebViewCrashed> {
        coEvery { this@mockk() } returns true
    }

    private val bodyContentUiModelMapper = mockk<MessageBodyContentUiModelMapper> {
        coEvery {
            toUiContent(
                body = any(),
                messageId = any(),
                shouldRestrictHeight = any()
            )
        } answers {
            MessageBodyContent.Text(firstArg())
        }
    }

    private val messageBodyUiModelMapper = MessageBodyUiModelMapper(
        attachmentGroupUiModelMapper = attachmentGroupUiModelMapper,
        hasMessageBodyWebViewCrashed = hasMessageBodyWebViewCrashed,
        bodyContentUiModelMapper = bodyContentUiModelMapper,
        restrictMessageWebViewHeightEnabled = mockk<FeatureFlag<Boolean>> {
            coEvery { get() } returns true
        }
    )

    @Test
    fun `plain text message body is correctly mapped to a message body ui model`() = runTest {
        // Given
        val messageId = MessageIdSample.build()
        val messageBody = DecryptedMessageBody(
            messageId = messageId,
            value = decryptedMessageBody,
            mimeType = MimeType.PlainText,
            isUnread = false,
            hasQuotedText = false,
            hasCalendarInvite = false,
            banners = emptyList()
        )
        val expected = MessageBodyUiModel(
            messageId = messageId,
            messageBody = MessageBodyContent.Text(decryptedMessageBody),
            mimeType = MimeTypeUiModel.PlainText,
            shouldShowEmbeddedImagesBanner = false,
            shouldShowRemoteContentBanner = false,
            shouldShowImagesFailedToLoadBanner = false,
            shouldShowExpandCollapseButton = false,
            attachments = null,
            viewModePreference = ViewModePreference.ThemeDefault,
            reloadMessageEffect = Effect.empty(),
            shouldRestrictWebViewHeight = true
        )

        // When
        val actual = messageBodyUiModelMapper.toUiModel(messageBody, null)

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `plain text message body is correctly mapped to a message body ui model with attachments`() = runTest {
        // Given
        val messageId = MessageIdSample.build()
        val attachments = listOf(
            AttachmentMetadataSamples.Invoice,
            AttachmentMetadataSamples.Document,
            AttachmentMetadataSamples.DocumentWithMultipleDots
        )
        val messageBody = DecryptedMessageBody(
            messageId = messageId,
            value = decryptedMessageBody,
            mimeType = MimeType.PlainText,
            hasQuotedText = false,
            hasCalendarInvite = false,
            isUnread = false,
            banners = emptyList(),
            attachments = attachments
        )


        val expected = MessageBodyUiModel(
            messageId = messageId,
            messageBody = MessageBodyContent.Text(decryptedMessageBody),
            mimeType = MimeTypeUiModel.PlainText,
            shouldShowEmbeddedImagesBanner = false,
            shouldShowRemoteContentBanner = false,
            shouldShowImagesFailedToLoadBanner = false,
            shouldShowExpandCollapseButton = false,
            attachments = AttachmentGroupUiModel(
                attachments = listOf(
                    AttachmentMetadataUiModelSamples.Invoice,
                    AttachmentMetadataUiModelSamples.Document,
                    AttachmentMetadataUiModelSamples.DocumentWithMultipleDots
                )
            ),
            viewModePreference = ViewModePreference.ThemeDefault,
            reloadMessageEffect = Effect.empty(),
            shouldRestrictWebViewHeight = true
        )

        // When
        val actual = messageBodyUiModelMapper.toUiModel(messageBody, null)

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `plain text message body is correctly mapped to a message body ui model with calendar invite`() = runTest {
        // Given
        val messageId = MessageIdSample.build()
        val attachments = listOf(
            AttachmentMetadataSamples.Calendar
        )
        val messageBody = DecryptedMessageBody(
            messageId = messageId,
            value = decryptedMessageBody,
            mimeType = MimeType.PlainText,
            hasQuotedText = false,
            hasCalendarInvite = false,
            isUnread = false,
            banners = emptyList(),
            attachments = attachments
        )

        val expected = MessageBodyUiModel(
            messageId = messageId,
            messageBody = MessageBodyContent.Text(decryptedMessageBody),
            mimeType = MimeTypeUiModel.PlainText,
            shouldShowEmbeddedImagesBanner = false,
            shouldShowRemoteContentBanner = false,
            shouldShowImagesFailedToLoadBanner = false,
            shouldShowExpandCollapseButton = false,
            attachments = AttachmentGroupUiModel(
                attachments = listOf(
                    AttachmentMetadataUiModelSamples.Calendar
                )
            ),
            viewModePreference = ViewModePreference.ThemeDefault,
            reloadMessageEffect = Effect.empty(),
            shouldRestrictWebViewHeight = true
        )

        // When
        val actual = messageBodyUiModelMapper.toUiModel(messageBody, null)

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `plain text message body is correctly mapped to a message body ui model with broken PDF attachment`() =
        runTest {
            // Given
            val messageId = MessageIdSample.build()
            val attachments = listOf(
                AttachmentMetadataSamples.InvoiceWithBinaryContentType
            )
            val messageBody = DecryptedMessageBody(
                messageId = messageId,
                value = decryptedMessageBody,
                mimeType = MimeType.PlainText,
                hasQuotedText = false,
                hasCalendarInvite = false,
                isUnread = false,
                banners = emptyList(),
                attachments = attachments
            )
            val expected = MessageBodyUiModel(
                messageId = messageId,
                messageBody = MessageBodyContent.Text(decryptedMessageBody),
                mimeType = MimeTypeUiModel.PlainText,
                shouldShowEmbeddedImagesBanner = false,
                shouldShowRemoteContentBanner = false,
                shouldShowImagesFailedToLoadBanner = false,
                shouldShowExpandCollapseButton = false,
                attachments = AttachmentGroupUiModel(
                    attachments = listOf(
                        AttachmentMetadataUiModelSamples.InvoiceWithBinaryContentType
                    )
                ),
                viewModePreference = ViewModePreference.ThemeDefault,
                reloadMessageEffect = Effect.empty(),
                shouldRestrictWebViewHeight = true
            )

            every {
                attachmentGroupUiModelMapper.toUiModel(
                    attachments = listOf(
                        AttachmentMetadataSamples.InvoiceWithBinaryContentType
                    ),
                    null
                )
            } returns AttachmentGroupUiModel(
                attachments = listOf(AttachmentMetadataUiModelSamples.InvoiceWithBinaryContentType)
            )

            // When
            val actual = messageBodyUiModelMapper.toUiModel(messageBody, null)

            // Then
            assertEquals(expected, actual)
        }

    @Test
    fun `HTML message body is correctly mapped to a message body ui model`() = runTest {
        // Given
        val messageId = MessageIdSample.build()
        val messageBody = DecryptedMessageBody(
            messageId = messageId,
            value = decryptedMessageBody,
            mimeType = MimeType.Html,
            isUnread = false,
            hasQuotedText = false,
            hasCalendarInvite = false,
            banners = emptyList()
        )
        val expected = MessageBodyUiModel(
            messageId = messageId,
            messageBody = MessageBodyContent.Text(decryptedMessageBody),
            mimeType = MimeTypeUiModel.Html,
            shouldShowEmbeddedImagesBanner = false,
            shouldShowRemoteContentBanner = false,
            shouldShowImagesFailedToLoadBanner = false,
            shouldShowExpandCollapseButton = false,
            attachments = null,
            viewModePreference = ViewModePreference.ThemeDefault,
            reloadMessageEffect = Effect.empty(),
            shouldRestrictWebViewHeight = true
        )

        // When
        val actual = messageBodyUiModelMapper.toUiModel(messageBody, null)

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `message body is mapped to a ui model that allows showing remote content when setting value is true`() =
        runTest {
            // Given
            val messageId = MessageIdSample.build()
            val messageBody = DecryptedMessageBody(
                messageId = messageId,
                value = decryptedMessageBody,
                mimeType = MimeType.Html,
                isUnread = false,
                hasQuotedText = false,
                hasCalendarInvite = false,
                banners = emptyList()
            )
            val expected = MessageBodyUiModel(
                messageId = messageId,
                messageBody = MessageBodyContent.Text(decryptedMessageBody),
                mimeType = MimeTypeUiModel.Html,
                shouldShowEmbeddedImagesBanner = false,
                shouldShowRemoteContentBanner = false,
                shouldShowImagesFailedToLoadBanner = false,
                shouldShowExpandCollapseButton = false,
                attachments = null,
                viewModePreference = ViewModePreference.ThemeDefault,
                reloadMessageEffect = Effect.empty(),
                shouldRestrictWebViewHeight = true
            )

            // When
            val actual = messageBodyUiModelMapper.toUiModel(messageBody, null)

            // Then
            assertEquals(expected, actual)
        }

    @Test
    fun `message body is mapped to a ui model that doesn't allow showing remote content when setting value is false`() =
        runTest {
            // Given
            val messageId = MessageIdSample.build()
            val messageBody = DecryptedMessageBody(
                messageId = messageId,
                value = decryptedMessageBody,
                mimeType = MimeType.Html,
                isUnread = false,
                hasQuotedText = false,
                hasCalendarInvite = false,
                banners = listOf(MessageBanner.RemoteContent)
            )
            val expected = MessageBodyUiModel(
                messageId = messageId,
                messageBody = MessageBodyContent.Text(decryptedMessageBody),
                mimeType = MimeTypeUiModel.Html,
                shouldShowEmbeddedImagesBanner = false,
                shouldShowRemoteContentBanner = true,
                shouldShowImagesFailedToLoadBanner = false,
                shouldShowExpandCollapseButton = false,
                attachments = null,
                viewModePreference = ViewModePreference.ThemeDefault,
                reloadMessageEffect = Effect.empty(),
                shouldRestrictWebViewHeight = true
            )

            // When
            val actual = messageBodyUiModelMapper.toUiModel(messageBody, null)

            // Then
            assertEquals(expected, actual)
        }

    @Test
    fun `message body is mapped to a ui model that allows showing embedded images when setting value is true`() =
        runTest {
            // Given
            val messageId = MessageIdSample.build()
            val messageBody = DecryptedMessageBody(
                messageId = messageId,
                value = decryptedMessageBody,
                mimeType = MimeType.Html,
                isUnread = false,
                hasQuotedText = false,
                hasCalendarInvite = false,
                banners = emptyList()
            )
            val expected = MessageBodyUiModel(
                messageId = messageId,
                messageBody = MessageBodyContent.Text(decryptedMessageBody),
                mimeType = MimeTypeUiModel.Html,
                shouldShowEmbeddedImagesBanner = false,
                shouldShowRemoteContentBanner = false,
                shouldShowImagesFailedToLoadBanner = false,
                shouldShowExpandCollapseButton = false,
                attachments = null,
                viewModePreference = ViewModePreference.ThemeDefault,
                reloadMessageEffect = Effect.empty(),
                shouldRestrictWebViewHeight = true
            )

            // When
            val actual = messageBodyUiModelMapper.toUiModel(messageBody, null)

            // Then
            assertEquals(expected, actual)
        }

    @Test
    fun `message body is mapped to a ui model that doesn't allow showing embedded image when setting value is false`() =
        runTest {
            // Given
            val messageId = MessageIdSample.build()
            val messageBody = DecryptedMessageBody(
                messageId = messageId,
                value = decryptedMessageBody,
                mimeType = MimeType.Html,
                isUnread = false,
                hasQuotedText = false,
                hasCalendarInvite = false,
                banners = listOf(MessageBanner.EmbeddedImages)
            )
            val expected = MessageBodyUiModel(
                messageId = messageId,
                messageBody = MessageBodyContent.Text(decryptedMessageBody),
                mimeType = MimeTypeUiModel.Html,
                shouldShowEmbeddedImagesBanner = true,
                shouldShowRemoteContentBanner = false,
                shouldShowImagesFailedToLoadBanner = false,
                shouldShowExpandCollapseButton = false,
                attachments = null,
                viewModePreference = ViewModePreference.ThemeDefault,
                reloadMessageEffect = Effect.empty(),
                shouldRestrictWebViewHeight = true
            )

            // When
            val actual = messageBodyUiModelMapper.toUiModel(messageBody, null)

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
            messageBody = MessageBodyContent.Text(messageBody),
            mimeType = MimeTypeUiModel.PlainText,
            shouldShowEmbeddedImagesBanner = false,
            shouldShowRemoteContentBanner = false,
            shouldShowImagesFailedToLoadBanner = false,
            shouldShowExpandCollapseButton = false,
            attachments = null,
            viewModePreference = ViewModePreference.ThemeDefault,
            reloadMessageEffect = Effect.empty(),
            shouldRestrictWebViewHeight = false
        )

        // When
        val actual = messageBodyUiModelMapper.toUiModel(GetMessageBodyError.Decryption(messageId, messageBody))

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `user changeable fields should be mapped from the existing message body Ui model`() = runTest {
        // Given
        val messageId = MessageIdSample.build()
        val messageBody = DecryptedMessageBody(
            messageId = messageId,
            value = decryptedMessageBody,
            mimeType = MimeType.Html,
            isUnread = false,
            hasQuotedText = false,
            hasCalendarInvite = false,
            banners = listOf(MessageBanner.EmbeddedImages)
        )
        val existingState = MessageBodyUiModel(
            messageId = messageId,
            messageBody = MessageBodyContent.Text(decryptedMessageBody),
            mimeType = MimeTypeUiModel.Html,
            shouldShowEmbeddedImagesBanner = false,
            shouldShowRemoteContentBanner = true,
            shouldShowImagesFailedToLoadBanner = false,
            shouldShowExpandCollapseButton = false,
            attachments = null,
            viewModePreference = ViewModePreference.ThemeDefault,
            reloadMessageEffect = Effect.empty(),
            shouldRestrictWebViewHeight = true
        )

        // When
        val actual = messageBodyUiModelMapper.toUiModel(messageBody, null, existingState)

        // Then
        assertEquals(existingState.viewModePreference, actual.viewModePreference)
    }
}
