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

package ch.protonmail.android.maildetail.presentation.sample

import java.util.UUID
import ch.protonmail.android.mailcommon.domain.sample.UserAddressSample
import ch.protonmail.android.mailcommon.presentation.model.AvatarUiModel
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.maildetail.presentation.R
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailMessageUiModel
import ch.protonmail.android.maildetail.presentation.model.MessageBannersUiModel
import ch.protonmail.android.maildetail.presentation.model.MessageDetailFooterUiModel
import ch.protonmail.android.maildetail.presentation.model.MessageIdUiModel
import ch.protonmail.android.maildetail.presentation.model.MessageLocationUiModel
import ch.protonmail.android.maildetail.presentation.model.ParticipantUiModel
import ch.protonmail.android.maillabel.presentation.model.LabelUiModel
import ch.protonmail.android.mailmessage.domain.model.Message
import ch.protonmail.android.mailmessage.domain.model.MessageWithLabels
import ch.protonmail.android.mailmessage.domain.sample.MessageWithLabelsSample
import ch.protonmail.android.mailmessage.presentation.model.AttachmentGroupUiModel
import ch.protonmail.android.mailmessage.presentation.model.MessageBodyExpandCollapseMode
import ch.protonmail.android.mailmessage.presentation.model.MessageBodyUiModel
import ch.protonmail.android.mailmessage.presentation.sample.AttachmentUiModelSample
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

object ConversationDetailMessageUiModelSample {

    val AugWeatherForecast = buildCollapsed(
        messageWithLabels = MessageWithLabelsSample.AugWeatherForecast
    )

    val AugWeatherForecastExpanded = buildExpanded(
        messageWithLabels = MessageWithLabelsSample.AugWeatherForecast
    )

    val AugWeatherForecastExpanding = buildExpanding(AugWeatherForecast)

    val EmptyDraft = buildCollapsed(
        messageWithLabels = MessageWithLabelsSample.EmptyDraft,
        avatar = AvatarUiModel.DraftIcon
    )

    val ExpiringInvitation = buildCollapsed(
        messageWithLabels = MessageWithLabelsSample.ExpiringInvitation,
        expiration = TextUiModel("12h")
    )

    val InvoiceForwarded = buildCollapsed(
        messageWithLabels = MessageWithLabelsSample.Invoice,
        forwardedIcon = ConversationDetailMessageUiModel.ForwardedIcon.Forwarded
    )

    val InvoiceReplied = buildCollapsed(
        messageWithLabels = MessageWithLabelsSample.Invoice,
        repliedIcon = ConversationDetailMessageUiModel.RepliedIcon.Replied
    )

    val InvoiceRepliedAll = buildCollapsed(
        messageWithLabels = MessageWithLabelsSample.Invoice,
        repliedIcon = ConversationDetailMessageUiModel.RepliedIcon.RepliedAll
    )

    val LotteryScam = buildCollapsed(
        messageWithLabels = MessageWithLabelsSample.LotteryScam
    )

    val SepWeatherForecast = buildCollapsed(
        messageWithLabels = MessageWithLabelsSample.SepWeatherForecast
    )

    val StarredInvoice = buildCollapsed(
        messageWithLabels = MessageWithLabelsSample.Invoice,
        message = MessageWithLabelsSample.Invoice.message,
        isStarred = true
    )

    val UnreadInvoice = buildCollapsed(
        messageWithLabels = MessageWithLabelsSample.UnreadInvoice
    )

    val InvoiceWithTwoLabels = buildCollapsed(
        messageWithLabels = MessageWithLabelsSample.InvoiceWithTwoLabels
    )

    val InvoiceWithLabel = buildCollapsed(
        messageWithLabels = MessageWithLabelsSample.InvoiceWithLabel
    )

    val InvoiceWithLabelExpanded = buildExpanded(
        messageWithLabels = MessageWithLabelsSample.InvoiceWithLabel
    )

    val InvoiceWithLabelExpanding = buildExpanding(
        collapsed = InvoiceWithLabel
    )

    val InvoiceWithLabelExpandingUnread = buildExpanding(
        collapsed = InvoiceWithLabel.copy(isUnread = true)
    )

    val InvoiceWithoutLabels = buildCollapsed(
        messageWithLabels = MessageWithLabelsSample.InvoiceWithoutLabels
    )

    val InvoiceWithoutLabelsCustomFolderExpanded = buildExpanded(
        messageWithLabels = MessageWithLabelsSample.InvoiceWithoutLabels,
        locationUiModel = MessageLocationUiModelSample.CustomFolder
    )

    val AnotherInvoiceWithoutLabels = buildCollapsed(
        messageWithLabels = MessageWithLabelsSample.AnotherInvoiceWithoutLabels
    )

    val MessageWithRemoteContentBlocked = buildExpanded(
        messageWithLabels = MessageWithLabelsSample.LotteryScam,
        messageBodyUiModel = MessageDetailBodyUiModelSample.withBlockedRemoteContent
    )

    val MessageWithRemoteContentLoaded = buildExpanded(
        messageWithLabels = MessageWithLabelsSample.LotteryScam,
        messageBodyUiModel = MessageDetailBodyUiModelSample.withAllowedRemoteContent
    )

    val MessageWithEmbeddedImagesBlocked = buildExpanded(
        messageWithLabels = MessageWithLabelsSample.AugWeatherForecast,
        messageBodyUiModel = MessageDetailBodyUiModelSample.withBlockedEmbeddedImages
    )

    val MessageWithEmbeddedImagesLoaded = buildExpanded(
        messageWithLabels = MessageWithLabelsSample.AugWeatherForecast,
        messageBodyUiModel = MessageDetailBodyUiModelSample.withAllowedEmbeddedImages
    )

    val WithRemoteAndEmbeddedContentBlocked = buildExpanded(
        messageWithLabels = MessageWithLabelsSample.InvoiceWithTwoLabels,
        messageBodyUiModel = MessageDetailBodyUiModelSample.withBlockedContent
    )

    val WithRemoteAndEmbeddedContentLoaded = buildExpanded(
        messageWithLabels = MessageWithLabelsSample.InvoiceWithTwoLabels,
        messageBodyUiModel = MessageDetailBodyUiModelSample.withAllowedContent
    )

    fun invoiceExpandedWithAttachments(limit: Int) = buildExpanded(
        messageWithLabels = MessageWithLabelsSample.InvoiceWithLabel,
        messageBodyUiModel = MessageDetailBodyUiModelSample.build(
            messageBody = "Invoice",
            attachments = AttachmentGroupUiModel(
                limit = limit,
                attachments = listOf(
                    AttachmentUiModelSample.document,
                    AttachmentUiModelSample.documentWithReallyLongFileName,
                    AttachmentUiModelSample.invoice,
                    AttachmentUiModelSample.image
                )
            )
        )
    )

    fun calendarInviteExpandedWithAttachments(limit: Int) = buildExpanded(
        messageWithLabels = MessageWithLabelsSample.CalendarWithoutLabels,
        messageBodyUiModel = MessageDetailBodyUiModelSample.build(
            messageBody = "Calendar Invite",
            attachments = AttachmentGroupUiModel(
                limit = limit,
                attachments = listOf(
                    AttachmentUiModelSample.calendar
                )
            )
        )
    )

    @Suppress("LongParameterList")
    private fun buildCollapsed(
        messageWithLabels: MessageWithLabels = MessageWithLabelsSample.build(),
        message: Message = messageWithLabels.message,
        avatar: AvatarUiModel = AvatarUiModel.ParticipantInitial(message.sender.name.substring(0, 1)),
        expiration: TextUiModel? = null,
        forwardedIcon: ConversationDetailMessageUiModel.ForwardedIcon =
            ConversationDetailMessageUiModel.ForwardedIcon.None,
        repliedIcon: ConversationDetailMessageUiModel.RepliedIcon = ConversationDetailMessageUiModel.RepliedIcon.None,
        isStarred: Boolean = false
    ): ConversationDetailMessageUiModel.Collapsed = ConversationDetailMessageUiModel.Collapsed(
        avatar = avatar,
        expiration = expiration,
        forwardedIcon = forwardedIcon,
        hasAttachments = message.numAttachments > message.attachmentCount.calendar,
        isStarred = isStarred,
        isUnread = message.unread,
        locationIcon = MessageLocationUiModelSample.AllMail,
        repliedIcon = repliedIcon,
        sender = ParticipantUiModel(message.sender.name, message.sender.address, R.drawable.ic_proton_lock, false),
        shortTime = TextUiModel("10:00"),
        labels = emptyList<LabelUiModel>().toImmutableList(),
        messageId = MessageIdUiModel(message.messageId.id),
        isDraft = message.isDraft()
    )

    private fun buildExpanded(
        messageWithLabels: MessageWithLabels = MessageWithLabelsSample.build(),
        message: Message = messageWithLabels.message,
        avatar: AvatarUiModel = AvatarUiModel.ParticipantInitial(message.sender.name.substring(0, 1)),
        isStarred: Boolean = false,
        messageBodyUiModel: MessageBodyUiModel = MessageDetailBodyUiModelSample.build(UUID.randomUUID().toString()),
        locationUiModel: MessageLocationUiModel = MessageLocationUiModelSample.AllMail
    ): ConversationDetailMessageUiModel.Expanded = ConversationDetailMessageUiModel.Expanded(
        isUnread = message.unread,
        messageId = MessageIdUiModel(message.messageId.id),
        messageDetailHeaderUiModel = MessageDetailHeaderUiModelSample.build(
            avatar = avatar,
            sender = ParticipantUiModel(
                participantName = message.sender.name,
                participantAddress = message.sender.address,
                participantPadlock = 0,
                shouldShowOfficialBadge = false
            ),
            isStarred = isStarred,
            location = locationUiModel,
            time = TextUiModel("10:00"),
            extendedTime = TextUiModel("10:00"),
            allRecipients = TextUiModel("Recipients"),
            toRecipients = emptyList<ParticipantUiModel>().toImmutableList(),
            ccRecipients = emptyList<ParticipantUiModel>().toImmutableList(),
            bccRecipients = emptyList<ParticipantUiModel>().toImmutableList(),
            labels = persistentListOf()
        ),
        messageDetailFooterUiModel = MessageDetailFooterUiModel(
            messageId = MessageIdUiModel(message.messageId.id),
            shouldShowReplyAll = false
        ),
        messageBannersUiModel = MessageBannersUiModel(
            shouldShowPhishingBanner = true,
            expirationBannerText = null,
            autoDeleteBannerText = null
        ),
        messageBodyUiModel = messageBodyUiModel,
        requestPhishingLinkConfirmation = false,
        expandCollapseMode = MessageBodyExpandCollapseMode.Collapsed,
        userAddress = UserAddressSample.PrimaryAddress
    )

    private fun buildExpanding(
        collapsed: ConversationDetailMessageUiModel.Collapsed
    ): ConversationDetailMessageUiModel.Expanding = ConversationDetailMessageUiModel.Expanding(
        messageId = collapsed.messageId,
        collapsed = collapsed
    )
}
