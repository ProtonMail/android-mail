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

package ch.protonmail.android.testdata.message

import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.mailmessage.presentation.model.AttachmentGroupUiModel
import ch.protonmail.android.mailmessage.presentation.model.MessageBodyUiModel
import ch.protonmail.android.mailmessage.presentation.model.MimeTypeUiModel
import ch.protonmail.android.mailmessage.presentation.model.ViewModePreference
import ch.protonmail.android.mailmessage.presentation.sample.AttachmentUiModelSample
import me.proton.core.user.domain.entity.UserAddress

object MessageBodyUiModelTestData {

    val plainTextMessageBodyUiModel = buildMessageBodyUiModel()

    val messageBodyWithAttachmentsUiModel = buildMessageBodyUiModel(
        attachments = AttachmentGroupUiModel(
            limit = 3,
            attachments = listOf(
                AttachmentUiModelSample.invoice,
                AttachmentUiModelSample.document,
                AttachmentUiModelSample.documentWithMultipleDots,
                AttachmentUiModelSample.image
            )
        )
    )

    val messageBodyWithIcsAttachmentsUiModel = buildMessageBodyUiModel(
        attachments = AttachmentGroupUiModel(
            limit = 3,
            attachments = listOf(
                AttachmentUiModelSample.calendar
            )
        )
    )

    val htmlMessageBodyUiModel = buildMessageBodyUiModel(
        messageBody = """
            <div>
                <p>Dear Test,</p>
                <p>This is an HTML message body.</p>
                <p>Kind regards,<br>
                Developer</p>
            </div>
        """.trimIndent(),
        mimeType = MimeTypeUiModel.Html
    )

    val messageBodyWithRemoteContentBlocked = buildMessageBodyUiModel(
        messageBody = """
            <div>
                <p>Dear Test,</p>
                <p>This is an HTML message body.</p>
                <img src="http://remote-insecure-image" />
                <p>Kind regards,<br>
                Developer</p>
            </div>
        """.trimIndent(),
        mimeType = MimeTypeUiModel.Html,
        shouldShowRemoteContent = false,
        shouldShowRemoteContentBanner = true
    )

    val messageBodyWithRemoteContentLoaded = messageBodyWithRemoteContentBlocked.copy(
        shouldShowRemoteContent = true,
        shouldShowRemoteContentBanner = false
    )

    val messageBodyWithEmbeddedImagesBlocked = buildMessageBodyUiModel(
        messageBody = "MessageWithEmbeddedImages",
        shouldShowEmbeddedImages = false,
        shouldShowEmbeddedImagesBanner = true
    )

    val messageBodyWithEmbeddedImagesLoaded = messageBodyWithEmbeddedImagesBlocked.copy(
        shouldShowEmbeddedImages = true,
        shouldShowEmbeddedImagesBanner = false
    )

    val bodyWithRemoteAndEmbeddedContentBlocked = buildMessageBodyUiModel(
        messageBody = "MessageWithRemoteAndEmbeddedImages",
        shouldShowRemoteContent = false,
        shouldShowEmbeddedImages = false,
        shouldShowRemoteContentBanner = true,
        shouldShowEmbeddedImagesBanner = true
    )

    val bodyWithRemoteAndEmbeddedContentLoaded = bodyWithRemoteAndEmbeddedContentBlocked.copy(
        shouldShowEmbeddedImages = true,
        shouldShowRemoteContent = true,
        shouldShowRemoteContentBanner = false,
        shouldShowEmbeddedImagesBanner = false
    )


    fun buildMessageBodyUiModel(
        messageId: MessageId = MessageIdSample.build(),
        messageBody: String = MessageBodyTestData.messageBody.body,
        mimeType: MimeTypeUiModel = MimeTypeUiModel.PlainText,
        shouldShowEmbeddedImages: Boolean = false,
        shouldShowRemoteContent: Boolean = false,
        shouldShowEmbeddedImagesBanner: Boolean = false,
        shouldShowRemoteContentBanner: Boolean = false,
        shouldShowOpenInProtonCalendar: Boolean = false,
        attachments: AttachmentGroupUiModel? = null,
        userAddress: UserAddress? = null,
        viewModePreference: ViewModePreference = ViewModePreference.ThemeDefault
    ): MessageBodyUiModel {
        return MessageBodyUiModel(
            messageBody = messageBody,
            messageBodyWithoutQuote = messageBody,
            messageId = messageId,
            mimeType = mimeType,
            shouldShowEmbeddedImages = shouldShowEmbeddedImages,
            shouldShowRemoteContent = shouldShowRemoteContent,
            shouldShowEmbeddedImagesBanner = shouldShowEmbeddedImagesBanner,
            shouldShowRemoteContentBanner = shouldShowRemoteContentBanner,
            shouldShowOpenInProtonCalendar = shouldShowOpenInProtonCalendar,
            attachments = attachments,
            shouldShowExpandCollapseButton = false,
            userAddress = userAddress,
            viewModePreference = viewModePreference,
            printEffect = Effect.empty(),
            shouldRestrictWebViewHeight = false,
            replyEffect = Effect.empty(),
            replyAllEffect = Effect.empty(),
            forwardEffect = Effect.empty()
        )
    }
}
