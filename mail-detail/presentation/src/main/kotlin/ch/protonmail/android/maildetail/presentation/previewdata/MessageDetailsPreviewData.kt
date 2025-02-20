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

package ch.protonmail.android.maildetail.presentation.previewdata

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import ch.protonmail.android.mailcommon.domain.model.Action
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.ActionUiModel
import ch.protonmail.android.mailcommon.presentation.model.BottomBarState
import ch.protonmail.android.mailcommon.presentation.model.contentDescription
import ch.protonmail.android.mailcommon.presentation.model.description
import ch.protonmail.android.mailcommon.presentation.model.iconDrawable
import ch.protonmail.android.mailcommon.presentation.ui.delete.DeleteDialogState
import ch.protonmail.android.mailcommon.presentation.ui.spotlight.SpotlightTooltipState
import ch.protonmail.android.maildetail.presentation.model.MessageBannersState
import ch.protonmail.android.maildetail.presentation.model.MessageBannersUiModel
import ch.protonmail.android.maildetail.presentation.model.MessageBodyState
import ch.protonmail.android.maildetail.presentation.model.MessageDetailState
import ch.protonmail.android.maildetail.presentation.model.MessageMetadataState
import ch.protonmail.android.maildetail.presentation.model.ReportPhishingDialogState
import ch.protonmail.android.maillabel.presentation.MailLabelUiModel
import ch.protonmail.android.maillabel.presentation.sample.LabelUiModelSample
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.presentation.model.MessageBodyExpandCollapseMode
import ch.protonmail.android.mailmessage.presentation.model.MessageBodyUiModel
import ch.protonmail.android.mailmessage.presentation.model.MimeTypeUiModel
import ch.protonmail.android.mailmessage.presentation.model.ViewModePreference
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.BottomSheetState
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.MoveToBottomSheetEntryPoint
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.MoveToBottomSheetState
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

object MessageDetailsPreviewData {

    val Message = MessageDetailState(
        messageMetadataState = MessageMetadataState.Data(
            messageDetailActionBar = MessageDetailActionBarUiModelPreviewData.FirstWeekOfAugWeatherForecast,
            messageDetailHeader = MessageDetailHeaderPreviewData.WithoutLabels,
            messageDetailFooter = MessageDetailFooterPreviewData.ReplyAll
        ),
        messageBannersState = MessageBannersState.Data(
            messageBannersUiModel = MessageBannersUiModel(
                shouldShowPhishingBanner = false,
                expirationBannerText = null,
                autoDeleteBannerText = null
            )
        ),
        messageBodyState = MessageBodyState.Data(
            messageBodyUiModel = MessageBodyUiModel(
                messageId = MessageId("This is a messageId"),
                messageBody = "This is a message body with quote.",
                messageBodyWithoutQuote = "This is a message body without quote.",
                mimeType = MimeTypeUiModel.PlainText,
                shouldShowEmbeddedImages = false,
                shouldShowRemoteContent = false,
                shouldShowEmbeddedImagesBanner = false,
                shouldShowRemoteContentBanner = false,
                shouldShowExpandCollapseButton = false,
                shouldShowOpenInProtonCalendar = false,
                attachments = null,
                userAddress = null,
                viewModePreference = ViewModePreference.LightMode,
                printEffect = Effect.empty(),
                shouldRestrictWebViewHeight = false,
                replyEffect = Effect.empty(),
                replyAllEffect = Effect.empty(),
                forwardEffect = Effect.empty()
            ),
            expandCollapseMode = MessageBodyExpandCollapseMode.Collapsed
        ),
        bottomBarState = BottomBarState.Data.Shown(
            listOf(
                ActionUiModel(
                    Action.Archive,
                    Action.Archive.iconDrawable(),
                    Action.Archive.description(),
                    Action.Archive.contentDescription()
                )
            ).toImmutableList()
        ),
        bottomSheetState = BottomSheetState(
            MoveToBottomSheetState.Data(
                moveToDestinations = emptyList<MailLabelUiModel>().toImmutableList(),
                selected = null,
                entryPoint = MoveToBottomSheetEntryPoint.Conversation
            )
        ),
        exitScreenEffect = Effect.empty(),
        exitScreenWithMessageEffect = Effect.empty(),
        error = Effect.empty(),
        openMessageBodyLinkEffect = Effect.empty(),
        openAttachmentEffect = Effect.empty(),
        openProtonCalendarIntent = Effect.empty(),
        requestLinkConfirmation = false,
        requestPhishingLinkConfirmation = false,
        deleteDialogState = DeleteDialogState.Hidden,
        reportPhishingDialogState = ReportPhishingDialogState.Hidden,
        spotlightTooltip = SpotlightTooltipState.Hidden
    )

    val MessageWithLabels = Message.copy(
        messageMetadataState = MessageMetadataState.Data(
            messageDetailActionBar = MessageDetailActionBarUiModelPreviewData.FirstWeekOfAugWeatherForecast,
            messageDetailHeader = MessageDetailHeaderPreviewData.WithoutLabels.copy(
                labels = persistentListOf(
                    LabelUiModelSample.Document,
                    LabelUiModelSample.News
                )
            ),
            messageDetailFooter = MessageDetailFooterPreviewData.ReplyAll
        )
    )

    val Loading = MessageDetailState(
        messageMetadataState = MessageMetadataState.Loading,
        messageBannersState = MessageBannersState.Loading,
        messageBodyState = MessageBodyState.Loading,
        bottomBarState = BottomBarState.Loading,
        bottomSheetState = BottomSheetState(MoveToBottomSheetState.Loading),
        exitScreenEffect = Effect.empty(),
        exitScreenWithMessageEffect = Effect.empty(),
        error = Effect.empty(),
        openMessageBodyLinkEffect = Effect.empty(),
        openAttachmentEffect = Effect.empty(),
        openProtonCalendarIntent = Effect.empty(),
        requestLinkConfirmation = false,
        requestPhishingLinkConfirmation = false,
        deleteDialogState = DeleteDialogState.Hidden,
        reportPhishingDialogState = ReportPhishingDialogState.Hidden,
        spotlightTooltip = SpotlightTooltipState.Hidden
    )
}

class MessageDetailsPreviewProvider : PreviewParameterProvider<MessageDetailState> {

    override val values = sequenceOf(
        MessageDetailsPreviewData.Message,
        MessageDetailsPreviewData.MessageWithLabels,
        MessageDetailsPreviewData.Loading
    )
}
