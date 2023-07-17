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
import ch.protonmail.android.mailcommon.presentation.ui.MailDivider
import ch.protonmail.android.maildetail.domain.usecase.GetEmbeddedImageResult
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailMessageUiModel
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailMessageUiModel.Collapsed
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailMessageUiModel.Expanded
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailMessageUiModel.Expanding
import ch.protonmail.android.mailmessage.domain.entity.AttachmentId
import ch.protonmail.android.mailmessage.domain.entity.MessageId
import me.proton.core.compose.component.ProtonCenteredProgress
import me.proton.core.compose.theme.ProtonTheme

@Composable
@Suppress("LongParameterList")
fun ConversationDetailItem(
    uiModel: ConversationDetailMessageUiModel,
    actions: ConversationDetailItem.Actions,
    modifier: Modifier = Modifier,
    webViewHeight: Int?,
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
            is Collapsed -> {
                ConversationDetailCollapsedMessageHeader(
                    uiModel = uiModel,
                    modifier = Modifier.clickable {
                        actions.onExpand(uiModel.messageId)
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
                    webViewHeight = webViewHeight,
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
    onMessageBodyLoadFinished: (messageId: MessageId, height: Int) -> Unit,
    webViewHeight: Int?
) {
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
            showFeatureMissingSnackbar = actions.showFeatureMissingSnackbar
        )
        MailDivider()
        MessageBody(
            messageBodyUiModel = uiModel.messageBodyUiModel,
            onMessageBodyLoaded = onMessageBodyLoadFinished,
            webViewHeight = webViewHeight,
            messageId = uiModel.messageId,
            actions = MessageBody.Actions(
                onMessageBodyLinkClicked = { actions.onMessageBodyLinkClicked(it.toString()) },
                onShowAllAttachments = { actions.onShowAllAttachmentsForMessage(uiModel.messageId) },
                onAttachmentClicked = { actions.onAttachmentClicked(uiModel.messageId, it) },
                loadEmbeddedImage = actions.loadEmbeddedImage
            )
        )
    }
}

object ConversationDetailItem {
    data class Actions(
        val onCollapse: (MessageId) -> Unit,
        val onExpand: (MessageId) -> Unit,
        val onMessageBodyLinkClicked: (url: String) -> Unit,
        val onOpenMessageBodyLink: (url: String) -> Unit,
        val onShowAllAttachmentsForMessage: (MessageId) -> Unit,
        val onAttachmentClicked: (MessageId, AttachmentId) -> Unit,
        val showFeatureMissingSnackbar: () -> Unit,
        val loadEmbeddedImage: (messageId: MessageId?, contentId: String) -> GetEmbeddedImageResult?
    )
}

object ConversationDetailItemTestTags {

    const val CollapseAnchor = "ConversationDetailItemCollapseAnchor"
}
