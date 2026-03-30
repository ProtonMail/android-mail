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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.design.compose.theme.ProtonTypography
import ch.protonmail.android.design.compose.theme.bodyLargeNorm
import ch.protonmail.android.mailcommon.domain.model.Action
import ch.protonmail.android.mailcommon.presentation.AdaptivePreviews
import ch.protonmail.android.mailcommon.presentation.R
import ch.protonmail.android.mailcommon.presentation.compose.MailDimens
import ch.protonmail.android.mailcommon.presentation.model.BottomBarState
import ch.protonmail.android.mailcommon.presentation.model.BottomBarTarget
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcommon.presentation.model.string
import ch.protonmail.android.mailcommon.presentation.previewdata.BottomActionBarPreviewProvider
import timber.log.Timber

@Composable
fun BottomActionBar(
    state: BottomBarState,
    viewActionCallbacks: BottomActionBar.Actions,
    modifier: Modifier = Modifier
) {
    val isVisible = state !is BottomBarState.Data.Hidden

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
    ) {
        Column(
            modifier = modifier
                .shadow(
                    elevation = ProtonDimens.ShadowElevation.Lifted,
                    ambientColor = ProtonTheme.colors.shadowSoft,
                    spotColor = ProtonTheme.colors.shadowSoft
                )
                .background(ProtonTheme.colors.backgroundInvertedSecondary)
                .windowInsetsPadding(
                    WindowInsets.safeDrawing
                        .only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)
                )
        ) {
            HorizontalDivider(thickness = MailDimens.SeparatorHeight, color = ProtonTheme.colors.separatorNorm)

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
                    .padding(vertical = ProtonDimens.Spacing.Standard),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                when (state) {
                    is BottomBarState.Error -> Text(
                        modifier = Modifier.padding(vertical = ProtonDimens.Spacing.Standard),
                        text = stringResource(id = R.string.common_error_loading_actions),
                        style = ProtonTypography.Default.bodyLargeNorm
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
                                onClick = callbackForAction(uiModel.action, viewActionCallbacks, state.target)
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
}

@SuppressWarnings("ComplexMethod")
fun callbackForAction(
    action: Action,
    viewActionCallbacks: BottomActionBar.Actions,
    target: BottomBarTarget
): () -> Unit = when (action) {
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
    Action.ViewHeaders -> viewActionCallbacks.onViewHeaders
    Action.ViewHtml -> viewActionCallbacks.onViewHtml
    Action.Remind -> viewActionCallbacks.onRemind
    Action.SavePdf -> viewActionCallbacks.onSavePdf
    Action.SenderEmails -> viewActionCallbacks.onSenderEmail
    Action.SaveAttachments -> viewActionCallbacks.onSaveAttachments
    Action.More -> viewActionCallbacks.onMore
    Action.Inbox -> viewActionCallbacks.onMoveToInbox
    Action.CustomizeToolbar -> viewActionCallbacks.onCustomizeToolbar
    Action.Snooze -> viewActionCallbacks.onSnooze

    Action.ReportPhishing -> when (target) {
        is BottomBarTarget.Message -> { { viewActionCallbacks.onReportPhishing(target.id) } }
        else -> { { Timber.d("ReportPhishing not available for $target") } }
    }

    Action.Print -> when (target) {
        is BottomBarTarget.Message -> { { viewActionCallbacks.onPrint(target.id) } }
        else -> { { Timber.d("Print not available for $target") } }
    }

    Action.Reply -> when (target) {
        is BottomBarTarget.Message -> { { viewActionCallbacks.onReply(target.id) } }
        else -> { { Timber.d("Reply not available for $target") } }
    }

    Action.ReplyAll -> when (target) {
        is BottomBarTarget.Message -> { { viewActionCallbacks.onReplyAll(target.id) } }
        else -> { { Timber.d("Reply All not available for $target") } }
    }

    Action.Forward -> when (target) {
        is BottomBarTarget.Message -> { { viewActionCallbacks.onForward(target.id) } }
        else -> { { Timber.d("Forward not available for $target") } }
    }

    Action.Pin, Action.Unpin -> { { Timber.d("Action not handled for BottomActionBar - $action.") } }
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
        modifier = modifier,
        onClick = onClick
    ) {
        Icon(
            modifier = Modifier,
            painter = painterResource(id = iconId),
            contentDescription = description.string(),
            tint = ProtonTheme.colors.iconWeak
        )
    }
}

object BottomActionBar {

    const val MAX_ACTIONS_COUNT = 5

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
        val onSpam: () -> Unit,
        val onMoveToInbox: () -> Unit,
        val onViewInLightMode: () -> Unit,
        val onViewInDarkMode: () -> Unit,
        val onPrint: (String) -> Unit,
        val onReply: (String) -> Unit,
        val onReplyAll: (String) -> Unit,
        val onForward: (String) -> Unit,
        val onViewHeaders: () -> Unit,
        val onViewHtml: () -> Unit,
        val onReportPhishing: (String) -> Unit,
        val onRemind: () -> Unit,
        val onSavePdf: () -> Unit,
        val onSenderEmail: () -> Unit,
        val onSaveAttachments: () -> Unit,
        val onMore: () -> Unit,
        val onCustomizeToolbar: () -> Unit,
        val onSnooze: () -> Unit
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
                onSpam = {},
                onMoveToInbox = {},
                onViewInLightMode = {},
                onViewInDarkMode = {},
                onPrint = {},
                onReply = {},
                onReplyAll = {},
                onForward = {},
                onViewHeaders = {},
                onViewHtml = {},
                onReportPhishing = {},
                onRemind = {},
                onSavePdf = {},
                onSenderEmail = {},
                onSaveAttachments = {},
                onMore = {},
                onCustomizeToolbar = {},
                onSnooze = {}
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
