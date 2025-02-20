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

package ch.protonmail.android.maildetail.presentation.ui

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import ch.protonmail.android.mailcommon.presentation.compose.MailDimens
import ch.protonmail.android.mailcommon.presentation.model.AvatarUiModel
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailMessageUiModel
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailMessageUiModel.Collapsed
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailMessageUiModel.Hidden
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailMessageUiModel.Expanded
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailMessageUiModel.Expanding
import ch.protonmail.android.maildetail.presentation.model.MessageIdUiModel
import ch.protonmail.android.maildetail.presentation.model.ParticipantUiModel
import ch.protonmail.android.maildetail.presentation.ui.MessageBody.DoOnDisplayedEffect
import ch.protonmail.android.maildetail.presentation.ui.footer.MessageDetailFooter
import ch.protonmail.android.maildetail.presentation.ui.header.MessageDetailHeader
import ch.protonmail.android.mailmessage.domain.model.AttachmentId
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.usecase.GetEmbeddedImageResult
import ch.protonmail.android.mailmessage.presentation.model.ViewModePreference
import me.proton.core.compose.component.ProtonCenteredProgress
import me.proton.core.compose.theme.ProtonTheme

@Composable
@Suppress("LongParameterList")
fun ConversationDetailItem(
    uiModel: ConversationDetailMessageUiModel,
    actions: ConversationDetailItem.Actions,
    modifier: Modifier = Modifier,
    onMessageBodyLoadFinished: (messageId: MessageId, height: Int) -> Unit
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = ProtonTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors(
            containerColor = ProtonTheme.colors.backgroundNorm
        )
    ) {
        when (uiModel) {
            is Hidden -> Unit
            is Collapsed -> {
                ConversationDetailCollapsedMessageHeader(
                    uiModel = uiModel,
                    modifier = Modifier.clickable {
                        when (uiModel.isDraft) {
                            true -> actions.onOpenComposer(uiModel.messageId)
                            else -> actions.onExpand(uiModel.messageId)
                        }
                    }
                )
            }

            is Expanding -> {
                ConversationDetailExpandingItem(uiModel = uiModel)
            }

            is Expanded -> {
                ConversationDetailExpandedItem(
                    uiModel = uiModel,
                    actions = actions,
                    onMessageBodyLoadFinished = onMessageBodyLoadFinished
                )
            }
        }
    }
}

@Composable
private fun ConversationDetailExpandingItem(uiModel: Expanding, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        ConversationDetailCollapsedMessageHeader(
            uiModel = uiModel.collapsed
        )
        ProtonCenteredProgress(modifier = Modifier.padding(MailDimens.ProgressDefaultSize))
    }
}

@Composable
private fun ConversationDetailExpandedItem(
    uiModel: Expanded,
    actions: ConversationDetailItem.Actions,
    modifier: Modifier = Modifier,
    onMessageBodyLoadFinished: (messageId: MessageId, height: Int) -> Unit
) {
    val headerActions = MessageDetailHeader.Actions.Empty.copy(
        onReply = actions.onReply,
        onReplyAll = actions.onReplyAll,
        onMore = actions.onMoreActionsClick,
        onAvatarClicked = actions.onAvatarClicked,
        onParticipantClicked = actions.onParticipantClicked,
        onShowFeatureMissingSnackbar = actions.showFeatureMissingSnackbar
    )

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .testTag(ConversationDetailItemTestTags.CollapseAnchor)
                .clickable { actions.onCollapse(uiModel.messageId) }
                .fillMaxWidth()
                .height(MailDimens.ConversationMessageCollapseBarHeight)
        )
        MessageDetailHeader(
            uiModel = uiModel.messageDetailHeaderUiModel,
            headerActions = headerActions
        )
        MessageBanners(messageBannersUiModel = uiModel.messageBannersUiModel)
        MessageBody(
            messageBodyUiModel = uiModel.messageBodyUiModel,
            expandCollapseMode = uiModel.expandCollapseMode,
            actions = MessageBody.Actions(
                onMessageBodyLinkClicked = { actions.onMessageBodyLinkClicked(uiModel.messageId, it) },
                onShowAllAttachments = { actions.onShowAllAttachmentsForMessage(uiModel.messageId) },
                onAttachmentClicked = { actions.onAttachmentClicked(uiModel.messageId, it) },
                onExpandCollapseButtonClicked = {
                    actions.onBodyExpandCollapseButtonClicked(uiModel.messageId)
                },
                loadEmbeddedImage = actions.loadEmbeddedImage,
                onReply = actions.onReply,
                onReplyAll = actions.onReplyAll,
                onForward = actions.onForward,
                onEffectConsumed = actions.onEffectConsumed,
                onLoadRemoteContent = { actions.onLoadRemoteContent(it) },
                onLoadEmbeddedImages = { actions.onLoadEmbeddedImages(it) },
                onLoadRemoteAndEmbeddedContent = { actions.onLoadRemoteAndEmbeddedContent(it) },
                onOpenInProtonCalendar = { actions.onOpenInProtonCalendar(it) },
                onPrint = { actions.onPrint(it) },
                onViewEntireMessageClicked = actions.onViewEntireMessageClicked
            ),
            onMessageBodyLoaded = onMessageBodyLoadFinished
        )
        MessageDetailFooter(
            uiModel = uiModel.messageDetailFooterUiModel,
            actions = MessageDetailFooter.Actions.fromConversationDetailItemActions(actions)
        )
    }
}

object ConversationDetailItem {
    data class Actions(
        val onCollapse: (MessageIdUiModel) -> Unit,
        val onExpand: (MessageIdUiModel) -> Unit,
        val onOpenComposer: (MessageIdUiModel) -> Unit,
        val onMessageBodyLinkClicked: (messageId: MessageIdUiModel, url: Uri) -> Unit,
        val onOpenMessageBodyLink: (url: Uri) -> Unit,
        val onShowAllAttachmentsForMessage: (MessageIdUiModel) -> Unit,
        val onAttachmentClicked: (MessageIdUiModel, AttachmentId) -> Unit,
        val showFeatureMissingSnackbar: () -> Unit,
        val loadEmbeddedImage: (messageId: MessageId?, contentId: String) -> GetEmbeddedImageResult?,
        val onReply: (MessageId) -> Unit,
        val onReplyAll: (MessageId) -> Unit,
        val onEffectConsumed: (MessageId, DoOnDisplayedEffect) -> Unit,
        val onForward: (MessageId) -> Unit,
        val onScrollRequestCompleted: () -> Unit,
        val onBodyExpandCollapseButtonClicked: (MessageIdUiModel) -> Unit,
        val onMoreActionsClick: (MessageId) -> Unit,
        val onLoadRemoteContent: (MessageId) -> Unit,
        val onLoadEmbeddedImages: (MessageId) -> Unit,
        val onLoadRemoteAndEmbeddedContent: (MessageId) -> Unit,
        val onOpenInProtonCalendar: (MessageId) -> Unit,
        val onPrint: (MessageId) -> Unit,
        val onAvatarClicked: (ParticipantUiModel, AvatarUiModel) -> Unit,
        val onParticipantClicked: (ParticipantUiModel, AvatarUiModel) -> Unit,
        val onViewEntireMessageClicked: (MessageId, Boolean, Boolean, ViewModePreference) -> Unit
    )
}

object ConversationDetailItemTestTags {

    const val CollapseAnchor = "ConversationDetailItemCollapseAnchor"
}
