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

package ch.protonmail.android.maildetail.presentation.ui.header

import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import ch.protonmail.android.maildetail.presentation.R
import ch.protonmail.android.maildetail.presentation.model.MessageDetailHeaderUiModel
import ch.protonmail.android.mailmessage.domain.model.MessageId

@Composable
fun MessageDetailHeaderActions(
    modifier: Modifier = Modifier,
    uiModel: MessageDetailHeaderUiModel,
    actions: MessageDetailHeader.Actions
) {
    Row(modifier = modifier) {
        if (uiModel.recipientsCount > 1) {
            ReplyAllActionButton(action = { actions.onReplyAll(MessageId(uiModel.messageIdUiModel.id)) })
        } else {
            ReplyActionButton(action = { actions.onReply(MessageId(uiModel.messageIdUiModel.id)) })
        }

        MoreActionButton(action = { actions.onMore(MessageId(uiModel.messageIdUiModel.id)) })
    }
}

@Composable
private fun ReplyActionButton(action: () -> Unit) {
    MessageDetailHeaderButton(
        modifier = Modifier.testTag(MessageDetailHeaderTestTags.ReplyButton),
        iconResource = R.drawable.ic_proton_reply,
        contentDescriptionResource = R.string.quick_reply_button_content_description,
        onClick = action
    )
}

@Composable
private fun ReplyAllActionButton(action: () -> Unit) {
    MessageDetailHeaderButton(
        modifier = Modifier.testTag(MessageDetailHeaderTestTags.ReplyAllButton),
        iconResource = R.drawable.ic_proton_reply_all,
        contentDescriptionResource = R.string.quick_reply_all_button_content_description,
        onClick = action
    )
}

@Composable
private fun MoreActionButton(action: () -> Unit) {
    MessageDetailHeaderButton(
        modifier = Modifier.testTag(MessageDetailHeaderTestTags.MoreButton),
        iconResource = R.drawable.ic_baseline_more_vert,
        contentDescriptionResource = R.string.more_button_content_description,
        onClick = action
    )
}

private val MessageDetailHeaderUiModel.recipientsCount
    get() = (toRecipients + ccRecipients + bccRecipients).toSet().size
