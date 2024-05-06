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

package ch.protonmail.android.maildetail.presentation.ui.footer

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import ch.protonmail.android.mailcommon.presentation.compose.MailDimens
import ch.protonmail.android.maildetail.presentation.R
import ch.protonmail.android.maildetail.presentation.model.MessageDetailFooterUiModel
import ch.protonmail.android.maildetail.presentation.previewdata.MessageDetailFooterPreview
import ch.protonmail.android.maildetail.presentation.previewdata.MessageDetailFooterPreviewProvider
import ch.protonmail.android.maildetail.presentation.ui.ConversationDetailItem
import ch.protonmail.android.maildetail.presentation.ui.MessageBodyTestTags
import ch.protonmail.android.maildetail.presentation.ui.MessageDetailContent
import ch.protonmail.android.mailmessage.domain.model.MessageId
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.captionNorm

@Composable
fun MessageDetailFooter(
    modifier: Modifier = Modifier,
    uiModel: MessageDetailFooterUiModel,
    actions: MessageDetailFooter.Actions
) {
    Row(
        modifier = modifier
            .testTag(MessageBodyTestTags.MessageActionsRootItem)
            .fillMaxWidth()
            .padding(ProtonDimens.SmallSpacing),
        horizontalArrangement = Arrangement.spacedBy(ProtonDimens.SmallSpacing)
    ) {
        MessageActionButton(
            modifier = Modifier
                .testTag(MessageBodyTestTags.MessageReplyButton)
                .weight(1f, false),
            onClick = { actions.onReply(MessageId(uiModel.messageId.id)) },
            iconResource = R.drawable.ic_proton_reply,
            textResource = R.string.action_reply
        )

        if (uiModel.shouldShowReplyAll) {
            MessageActionButton(
                modifier = Modifier
                    .testTag(MessageBodyTestTags.MessageReplyAllButton)
                    .weight(1f, false),
                onClick = { actions.onReplyAll(MessageId(uiModel.messageId.id)) },
                iconResource = R.drawable.ic_proton_reply_all,
                textResource = R.string.action_reply_all
            )
        }

        MessageActionButton(
            modifier = Modifier
                .testTag(MessageBodyTestTags.MessageForwardButton)
                .weight(1f, false),
            onClick = { actions.onForward(MessageId(uiModel.messageId.id)) },
            iconResource = R.drawable.ic_proton_forward,
            textResource = R.string.action_forward
        )
    }
}

@Composable
private fun MessageActionButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    @DrawableRes iconResource: Int,
    @StringRes textResource: Int
) {
    Button(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(MailDimens.ActionButtonShapeRadius),
        border = BorderStroke(MailDimens.DefaultBorder, ProtonTheme.colors.separatorNorm),
        colors = ButtonDefaults.buttonColors(backgroundColor = ProtonTheme.colors.backgroundNorm),
        elevation = ButtonDefaults.elevation(defaultElevation = 0.dp),
        onClick = { onClick() }
    ) {
        Icon(
            modifier = Modifier.padding(end = ProtonDimens.ExtraSmallSpacing),
            painter = painterResource(id = iconResource),
            tint = ProtonTheme.colors.iconNorm,
            contentDescription = null
        )
        Text(
            text = stringResource(textResource),
            style = ProtonTheme.typography.captionNorm
        )
    }
}

object MessageDetailFooter {
    data class Actions(
        val onReply: (MessageId) -> Unit,
        val onReplyAll: (MessageId) -> Unit,
        val onForward: (MessageId) -> Unit
    ) {

        companion object {

            val Empty = Actions(
                onReply = {},
                onReplyAll = {},
                onForward = {}
            )

            fun fromMessageDetailContentActions(actions: MessageDetailContent.Actions) = Actions(
                onReply = actions.onReply,
                onReplyAll = actions.onReplyAll,
                onForward = actions.onForward
            )

            fun fromConversationDetailItemActions(actions: ConversationDetailItem.Actions) = Actions(
                onReply = actions.onReply,
                onReplyAll = actions.onReplyAll,
                onForward = actions.onForward
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
internal fun MessageDetailFooterPreview(
    @PreviewParameter(MessageDetailFooterPreviewProvider::class) preview: MessageDetailFooterPreview
) {
    ProtonTheme {
        MessageDetailFooter(
            uiModel = preview.uiModel,
            actions = MessageDetailFooter.Actions.Empty
        )
    }
}

