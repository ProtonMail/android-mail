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

package ch.protonmail.android.mailcommon.presentation.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import ch.protonmail.android.mailcommon.domain.model.Action
import ch.protonmail.android.mailcommon.presentation.AdaptivePreviews
import ch.protonmail.android.mailcommon.presentation.R
import ch.protonmail.android.mailcommon.presentation.compose.MailDimens
import ch.protonmail.android.mailcommon.presentation.model.BottomBarState
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcommon.presentation.model.string
import ch.protonmail.android.mailcommon.presentation.previewdata.BottomActionBarPreviewProvider
import me.proton.core.compose.component.ProtonCenteredProgress
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.ProtonTypography
import me.proton.core.compose.theme.default

@Composable
fun BottomActionBar(
    state: BottomBarState,
    viewActionCallbacks: BottomActionBar.Actions,
    modifier: Modifier = Modifier
) {
    if (state is BottomBarState.Data.Hidden) return
    Column(
        modifier = modifier.background(ProtonTheme.colors.backgroundNorm)
    ) {
        Divider(color = ProtonTheme.colors.separatorNorm, thickness = MailDimens.SeparatorHeight)

        Row(
            modifier = Modifier
                .testTag(BottomActionBarTestTags.RootItem)
                .clickable(
                    enabled = false,
                    onClick = {
                        // this is needed otherwise the click event is passed down the view hierarchy
                    }
                )
                .fillMaxWidth()
                .padding(horizontal = 0.dp, vertical = ProtonDimens.DefaultSpacing),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            when (state) {
                is BottomBarState.Loading -> {
                    ProtonCenteredProgress(modifier = Modifier.size(MailDimens.ProgressDefaultSize))
                }

                is BottomBarState.Error -> Text(
                    text = stringResource(id = R.string.common_error_loading_actions),
                    style = ProtonTypography.Default.default
                )

                is BottomBarState.Data.Shown -> {
                    state.actions.forEachIndexed { index, uiModel ->
                        if (index.exceedsMaxActionsShowed()) {
                            return@forEachIndexed
                        }
                        BottomBarIcon(
                            modifier = Modifier.testTag("${BottomActionBarTestTags.Button}$index"),
                            iconId = uiModel.icon,
                            description = uiModel.contentDescription,
                            onClick = callbackForAction(uiModel.action, viewActionCallbacks)
                        )
                    }
                }

                else -> {
                    // no-op, Hidden state is handled at the beginning
                }
            }
        }
    }
}

@SuppressWarnings("ComplexMethod")
fun callbackForAction(action: Action, viewActionCallbacks: BottomActionBar.Actions) = when (action) {
    Action.MarkRead -> viewActionCallbacks.onMarkRead
    Action.MarkUnread -> viewActionCallbacks.onMarkUnread
    Action.Star -> viewActionCallbacks.onStar
    Action.Unstar -> viewActionCallbacks.onUnstar
    Action.Label -> viewActionCallbacks.onLabel
    Action.Move -> viewActionCallbacks.onMove
    Action.Trash -> viewActionCallbacks.onTrash
    Action.Delete -> viewActionCallbacks.onDelete
    Action.Archive -> viewActionCallbacks.onArchive
    Action.Spam -> viewActionCallbacks.onSpam
    Action.ViewInLightMode -> viewActionCallbacks.onViewInLightMode
    Action.ViewInDarkMode -> viewActionCallbacks.onViewInDarkMode
    Action.Print -> viewActionCallbacks.onPrint
    Action.ViewHeaders -> viewActionCallbacks.onViewHeaders
    Action.ViewHtml -> viewActionCallbacks.onViewHtml
    Action.ReportPhishing -> viewActionCallbacks.onReportPhishing
    Action.Remind -> viewActionCallbacks.onRemind
    Action.SavePdf -> viewActionCallbacks.onSavePdf
    Action.SenderEmails -> viewActionCallbacks.onSenderEmail
    Action.SaveAttachments -> viewActionCallbacks.onSaveAttachments
    Action.More -> viewActionCallbacks.onMore
    Action.Reply -> viewActionCallbacks.onReply
    Action.ReplyAll -> viewActionCallbacks.onReplyAll
    Action.Forward -> viewActionCallbacks.onForward
    Action.OpenCustomizeToolbar -> viewActionCallbacks.onCustomizeToolbar
}

@Composable
private fun Int.exceedsMaxActionsShowed() = this > BottomActionBar.MAX_ACTIONS_COUNT

@Composable
private fun BottomBarIcon(
    modifier: Modifier = Modifier,
    @DrawableRes iconId: Int,
    description: TextUiModel,
    onClick: () -> Unit
) {
    IconButton(
        modifier = modifier.size(ProtonDimens.DefaultIconSize),
        onClick = onClick
    ) {
        Icon(
            modifier = Modifier,
            painter = painterResource(id = iconId),
            contentDescription = description.string(),
            tint = ProtonTheme.colors.iconNorm
        )
    }
}

object BottomActionBar {

    internal const val MAX_ACTIONS_COUNT = 5

    data class Actions(
        val onMarkRead: () -> Unit,
        val onMarkUnread: () -> Unit,
        val onStar: () -> Unit,
        val onUnstar: () -> Unit,
        val onMove: () -> Unit,
        val onLabel: () -> Unit,
        val onTrash: () -> Unit,
        val onDelete: () -> Unit,
        val onArchive: () -> Unit,
        val onReply: () -> Unit,
        val onReplyAll: () -> Unit,
        val onForward: () -> Unit,
        val onSpam: () -> Unit,
        val onViewInLightMode: () -> Unit,
        val onViewInDarkMode: () -> Unit,
        val onPrint: () -> Unit,
        val onViewHeaders: () -> Unit,
        val onViewHtml: () -> Unit,
        val onReportPhishing: () -> Unit,
        val onRemind: () -> Unit,
        val onSavePdf: () -> Unit,
        val onSenderEmail: () -> Unit,
        val onSaveAttachments: () -> Unit,
        val onMore: () -> Unit,
        val onCustomizeToolbar: () -> Unit
    ) {

        companion object {

            val Empty = Actions(
                onMarkRead = {},
                onMarkUnread = {},
                onStar = {},
                onUnstar = {},
                onMove = {},
                onLabel = {},
                onTrash = {},
                onDelete = {},
                onArchive = {},
                onForward = {},
                onSpam = {},
                onReply = {},
                onReplyAll = {},
                onViewInLightMode = {},
                onViewInDarkMode = {},
                onPrint = {},
                onViewHeaders = {},
                onViewHtml = {},
                onReportPhishing = {},
                onRemind = {},
                onSavePdf = {},
                onSenderEmail = {},
                onSaveAttachments = {},
                onMore = {},
                onCustomizeToolbar = {}
            )
        }
    }

}

@Composable
@AdaptivePreviews
private fun BottomActionPreview(@PreviewParameter(BottomActionBarPreviewProvider::class) state: BottomBarState) {
    ProtonTheme {
        BottomActionBar(state = state, viewActionCallbacks = BottomActionBar.Actions.Empty)
    }
}

object BottomActionBarTestTags {

    const val RootItem = "BottomActionBarRootItem"
    const val Button = "BottomActionBarIcon"
}
