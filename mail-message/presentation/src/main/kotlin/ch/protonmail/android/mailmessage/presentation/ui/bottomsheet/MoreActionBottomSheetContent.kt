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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import ch.protonmail.android.mailcommon.domain.model.Action
import ch.protonmail.android.mailcommon.presentation.AdaptivePreviews
import ch.protonmail.android.mailcommon.presentation.NO_CONTENT_DESCRIPTION
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.MoreActionsBottomSheetState
import ch.protonmail.android.mailmessage.presentation.previewdata.MoreActionBottomSheetPreviewDataProvider
import me.proton.core.compose.component.ProtonCenteredProgress
import me.proton.core.compose.component.ProtonRawListItem
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme3

@Composable
fun MoreActionBottomSheetContent(
    modifier: Modifier = Modifier,
    state: MoreActionsBottomSheetState,
    actionCallbacks: MoreActionBottomSheetContent.Actions
) {
    when (state) {
        is MoreActionsBottomSheetState.Data -> MoreActionBottomSheetContent(modifier, state, actionCallbacks)
        else -> ProtonCenteredProgress()
    }
}

@Composable
fun MoreActionBottomSheetContent(
    modifier: Modifier = Modifier,
    state: MoreActionsBottomSheetState.Data,
    actionCallbacks: MoreActionBottomSheetContent.Actions
) {
    LazyColumn(modifier = modifier.padding(vertical = ProtonDimens.DefaultSpacing)) {
        items(state.actionUiModels) { actionItem ->
            ProtonRawListItem(
                modifier = Modifier
                    .testTag(MoreActionsBottomSheetTestTags.ActionItem)
                    .clickable { callbackForAction(actionItem.action, actionCallbacks) }
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
                    text = stringResource(id = actionItem.description),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

object MoreActionBottomSheetContent {

    data class Actions(
        val onStar: () -> Unit,
        val onUnStar: () -> Unit,
        val onArchive: () -> Unit,
        val onSpam: () -> Unit
    ) {

        companion object {

            val Empty = Actions(
                onStar = {},
                onUnStar = {},
                onArchive = {},
                onSpam = {}
            )
        }
    }
}

@Composable
@AdaptivePreviews
private fun MoreActionBottomSheetContentPreview(
    @PreviewParameter(MoreActionBottomSheetPreviewDataProvider::class) state: MoreActionsBottomSheetState.Data
) {
    ProtonTheme3 {
        MoreActionBottomSheetContent(state = state, actionCallbacks = MoreActionBottomSheetContent.Actions.Empty)
    }
}

fun callbackForAction(action: Action, actionCallbacks: MoreActionBottomSheetContent.Actions): () -> Unit =
    when (action) {
        Action.Star -> actionCallbacks.onStar
        Action.Unstar -> actionCallbacks.onUnStar
        Action.Archive -> actionCallbacks.onArchive
        Action.Spam -> actionCallbacks.onSpam
        else -> {
            {}
        }
    }

object MoreActionsBottomSheetTestTags {

    const val ActionItem = "MoreActionsBottomSheetActionItem"
    const val LabelIcon = "MoreActionsBottomSheetLabelIcon"

}
