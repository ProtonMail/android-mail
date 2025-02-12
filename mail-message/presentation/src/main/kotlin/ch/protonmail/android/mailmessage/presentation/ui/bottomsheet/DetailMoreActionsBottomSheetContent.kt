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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import ch.protonmail.android.mailcommon.domain.model.Action
import ch.protonmail.android.mailcommon.presentation.NO_CONTENT_DESCRIPTION
import ch.protonmail.android.mailcommon.presentation.compose.MailDimens
import ch.protonmail.android.mailcommon.presentation.model.ActionUiModel
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcommon.presentation.model.string
import ch.protonmail.android.mailcommon.presentation.ui.MailDivider
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.DetailMoreActionsBottomSheetState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import me.proton.core.compose.component.ProtonCenteredProgress
import me.proton.core.compose.component.ProtonRawListItem
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultStrongNorm
import me.proton.core.compose.theme.defaultWeak
import timber.log.Timber

@Composable
fun DetailMoreActionsBottomSheetContent(
    state: DetailMoreActionsBottomSheetState,
    actions: DetailMoreActionsBottomSheetContent.Actions
) {
    when (state) {
        is DetailMoreActionsBottomSheetState.Data -> DetailMoreActionsBottomSheetContent(
            isAffectingConversation = state.isAffectingConversation,
            uiModel = state.messageDataUiModel,
            actionsUiModel = state.replyActionsUiModel,
            actionCallbacks = actions
        )

        else -> ProtonCenteredProgress()
    }
}

@Composable
fun DetailMoreActionsBottomSheetContent(
    isAffectingConversation: Boolean,
    uiModel: DetailMoreActionsBottomSheetState.MessageDataUiModel,
    actionsUiModel: ImmutableList<ActionUiModel>,
    actionCallbacks: DetailMoreActionsBottomSheetContent.Actions
) {

    Column {
        Text(
            modifier = Modifier
                .padding(top = ProtonDimens.DefaultSpacing)
                .padding(bottom = MailDimens.TinySpacing)
                .padding(horizontal = ProtonDimens.DefaultSpacing),
            text = uiModel.headerSubjectText.string(),
            style = ProtonTheme.typography.defaultStrongNorm,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            modifier = Modifier
                .padding(horizontal = ProtonDimens.DefaultSpacing)
                .padding(bottom = ProtonDimens.SmallSpacing),
            text = uiModel.headerDescriptionText.string(),
            style = ProtonTheme.typography.defaultWeak(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        MailDivider()

        LazyColumn {
            items(actionsUiModel) { actionItem ->
                if (shouldSkipActionItem(actionItem, isSystemInDarkTheme())) return@items

                ProtonRawListItem(
                    modifier = Modifier
                        .clickable {
                            val convCallback = if (isAffectingConversation) {
                                conversationCallbackForAction(
                                    actionItem.action, actionCallbacks
                                )
                            } else {
                                null
                            }
                            convCallback?.invoke() ?: callbackForAction(actionItem.action, actionCallbacks)
                                .invoke(MessageId(uiModel.messageId))
                        }
                        .padding(ProtonDimens.DefaultSpacing)
                ) {
                    Icon(
                        painter = painterResource(id = actionItem.icon),
                        contentDescription = NO_CONTENT_DESCRIPTION
                    )
                    Spacer(modifier = Modifier.size(ProtonDimens.DefaultSpacing))
                    Text(
                        modifier = Modifier.weight(1f),
                        text = actionItem.description.string(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

private fun callbackForAction(
    action: Action,
    actionCallbacks: DetailMoreActionsBottomSheetContent.Actions
): (MessageId) -> Unit = when (action) {
    Action.Reply -> actionCallbacks.onReply
    Action.ReplyAll -> actionCallbacks.onReplyAll
    Action.Forward -> actionCallbacks.onForward
    Action.MarkUnread -> actionCallbacks.onMarkUnread
    Action.Label -> actionCallbacks.onLabel
    Action.ViewInLightMode -> actionCallbacks.onViewInLightMode
    Action.ViewInDarkMode -> actionCallbacks.onViewInDarkMode
    Action.Trash -> actionCallbacks.onMoveToTrash
    Action.Archive -> actionCallbacks.onMoveToArchive
    Action.Spam -> actionCallbacks.onMoveToSpam
    Action.Move -> actionCallbacks.onMove
    Action.Print -> actionCallbacks.onPrint
    Action.ReportPhishing -> actionCallbacks.onReportPhishing

    else -> {
        { Timber.d("Action not handled $action.") }
    }
}

private fun conversationCallbackForAction(
    action: Action,
    actionCallbacks: DetailMoreActionsBottomSheetContent.Actions
): (() -> Unit)? = when (action) {
    Action.Reply -> null
    Action.ReplyAll -> null
    Action.Forward -> null
    Action.MarkUnread -> actionCallbacks.onMarkUnreadConversation
    Action.Label -> actionCallbacks.onLabelConversation
    Action.ViewInLightMode -> null
    Action.ViewInDarkMode -> null
    Action.Trash -> actionCallbacks.onMoveToTrashConversation
    Action.Archive -> actionCallbacks.onMoveToArchiveConversation
    Action.Spam -> actionCallbacks.onMoveToSpamConversation
    Action.Move -> actionCallbacks.onMoveConversation
    Action.Print -> null
    Action.ReportPhishing -> null

    else -> {
        { Timber.d("Action not handled $action.") }
    }
}

private fun shouldSkipActionItem(actionItem: ActionUiModel, isSystemInDarkTheme: Boolean): Boolean =
    !isSystemInDarkTheme && actionItem.action in arrayOf(Action.ViewInLightMode, Action.ViewInDarkMode)

object DetailMoreActionsBottomSheetContent {

    data class Actions(
        val onReply: (MessageId) -> Unit,
        val onReplyAll: (MessageId) -> Unit,
        val onForward: (MessageId) -> Unit,
        val onMarkUnread: (MessageId) -> Unit,
        val onMarkUnreadConversation: () -> Unit,
        val onLabel: (MessageId) -> Unit,
        val onLabelConversation: () -> Unit,
        val onViewInLightMode: (MessageId) -> Unit,
        val onViewInDarkMode: (MessageId) -> Unit,
        val onMoveToTrash: (MessageId) -> Unit,
        val onMoveToTrashConversation: () -> Unit,
        val onMoveToArchive: (MessageId) -> Unit,
        val onMoveToArchiveConversation: () -> Unit,
        val onMoveToSpam: (MessageId) -> Unit,
        val onMoveToSpamConversation: () -> Unit,
        val onMove: (MessageId) -> Unit,
        val onMoveConversation: () -> Unit,
        val onPrint: (MessageId) -> Unit,
        val onReportPhishing: (MessageId) -> Unit
    )
}

@Preview(showBackground = true)
@Composable
private fun BottomSheetContentPreview() {
    ProtonTheme {
        DetailMoreActionsBottomSheetContent(
            state = DetailMoreActionsBottomSheetState.Data(
                isAffectingConversation = false,
                messageDataUiModel = DetailMoreActionsBottomSheetState.MessageDataUiModel(
                    TextUiModel("Kudos on a Successful Completion of a Challenging Project!"),
                    TextUiModel("Message from Antony Hayes"),
                    "123"
                ),
                replyActionsUiModel = listOf(
                    ActionUiModel(Action.Reply),
                    ActionUiModel(Action.ReplyAll),
                    ActionUiModel(Action.Forward),
                    ActionUiModel(Action.ReportPhishing)
                ).toImmutableList()
            ),
            actions = DetailMoreActionsBottomSheetContent.Actions(
                onReply = {},
                onReplyAll = {},
                onForward = {},
                onMarkUnread = {},
                onLabel = {},
                onViewInLightMode = {},
                onViewInDarkMode = {},
                onMoveToTrash = {},
                onMoveToArchive = {},
                onMoveToSpam = {},
                onMove = {},
                onPrint = {},
                onReportPhishing = {},
                onMoveToSpamConversation = {},
                onMoveToArchiveConversation = {},
                onLabelConversation = {},
                onMoveConversation = {},
                onMoveToTrashConversation = {},
                onMarkUnreadConversation = {}
            )
        )
    }
}
