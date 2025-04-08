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

package ch.protonmail.android.mailmessage.presentation.ui.bottomsheet

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import ch.protonmail.android.mailcommon.domain.model.Action
import ch.protonmail.android.mailcommon.presentation.AdaptivePreviews
import ch.protonmail.android.mailcommon.presentation.NO_CONTENT_DESCRIPTION
import ch.protonmail.android.mailcommon.presentation.model.string
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.MailboxMoreActionsBottomSheetState
import ch.protonmail.android.mailmessage.presentation.previewdata.MailboxMoreActionBottomSheetPreviewDataProvider
import me.proton.core.compose.component.ProtonCenteredProgress
import me.proton.core.compose.component.ProtonRawListItem
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme3

@Composable
fun MailboxMoreActionBottomSheetContent(
    modifier: Modifier = Modifier,
    state: MailboxMoreActionsBottomSheetState,
    actionCallbacks: MoreActionBottomSheetContent.Actions
) {
    when (state) {
        is MailboxMoreActionsBottomSheetState.Data ->
            MailboxMoreActionBottomSheetContent(modifier, state, actionCallbacks)

        else -> ProtonCenteredProgress()
    }
}

@Composable
fun MailboxMoreActionBottomSheetContent(
    modifier: Modifier = Modifier,
    state: MailboxMoreActionsBottomSheetState.Data,
    actionCallbacks: MoreActionBottomSheetContent.Actions
) {
    LazyColumn(modifier = modifier.padding(vertical = ProtonDimens.DefaultSpacing)) {
        items(state.actionUiModels) { actionItem ->
            ProtonRawListItem(
                modifier = Modifier
                    .testTag(MoreActionsBottomSheetTestTags.ActionItem)
                    .clickable { callbackForAction(actionItem.action, actionCallbacks).invoke() }
                    .padding(vertical = ProtonDimens.DefaultSpacing)
            ) {
                Icon(
                    modifier = Modifier
                        .testTag(MoreActionsBottomSheetTestTags.LabelIcon)
                        .padding(horizontal = ProtonDimens.DefaultSpacing),
                    painter = painterResource(id = actionItem.icon),
                    contentDescription = NO_CONTENT_DESCRIPTION
                )
                Text(
                    modifier = Modifier
                        .testTag(LabelAsBottomSheetTestTags.LabelNameText)
                        .weight(1f),
                    text = actionItem.description.string(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun callbackForAction(action: Action, actionCallbacks: MoreActionBottomSheetContent.Actions): () -> Unit =
    when (action) {
        Action.Star -> actionCallbacks.onStar
        Action.Unstar -> actionCallbacks.onUnStar
        Action.Archive -> actionCallbacks.onArchive
        Action.Spam -> actionCallbacks.onSpam
        Action.MarkRead -> actionCallbacks.onRead
        Action.MarkUnread -> actionCallbacks.onUnRead
        Action.Label -> actionCallbacks.onLabel
        Action.Move -> actionCallbacks.onMove
        Action.Trash -> actionCallbacks.onTrash
        Action.Delete -> actionCallbacks.onDelete
        Action.OpenCustomizeToolbar -> actionCallbacks.onOpenCustomizeToolbar
        Action.Reply,
        Action.ReplyAll,
        Action.Forward,
        Action.ViewInLightMode,
        Action.ViewInDarkMode,
        Action.Print,
        Action.ViewHeaders,
        Action.ViewHtml,
        Action.ReportPhishing,
        Action.Remind,
        Action.SavePdf,
        Action.SenderEmails,
        Action.SaveAttachments,
        Action.More -> {
            {}
        }
    }

object MoreActionBottomSheetContent {

    data class Actions(
        val onStar: () -> Unit,
        val onUnStar: () -> Unit,
        val onArchive: () -> Unit,
        val onSpam: () -> Unit,
        val onRead: () -> Unit,
        val onUnRead: () -> Unit,
        val onTrash: () -> Unit,
        val onDelete: () -> Unit,
        val onMove: () -> Unit,
        val onLabel: () -> Unit,
        val onOpenCustomizeToolbar: () -> Unit
    ) {

        companion object {

            val Empty = Actions(
                onStar = {},
                onUnStar = {},
                onArchive = {},
                onSpam = {},
                onMove = {},
                onRead = {},
                onUnRead = {},
                onLabel = {},
                onTrash = {},
                onDelete = {},
                onOpenCustomizeToolbar = {}
            )
        }
    }
}

@Composable
@AdaptivePreviews
private fun MoreActionBottomSheetContentPreview(
    @PreviewParameter(MailboxMoreActionBottomSheetPreviewDataProvider::class)
    state: MailboxMoreActionsBottomSheetState.Data
) {
    ProtonTheme3 {
        MailboxMoreActionBottomSheetContent(state = state, actionCallbacks = MoreActionBottomSheetContent.Actions.Empty)
    }
}

object MoreActionsBottomSheetTestTags {

    const val ActionItem = "MoreActionsBottomSheetActionItem"
    const val LabelIcon = "MoreActionsBottomSheetLabelIcon"

}
