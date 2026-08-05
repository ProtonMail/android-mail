/*
 * Copyright (c) 2026 Proton Technologies AG
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

package ch.protonmail.android.mailcontentsearch.presentation.ui

import androidx.activity.compose.BackHandler
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import ch.protonmail.android.design.compose.component.ProtonModalBottomSheetLayout
import ch.protonmail.android.mailcommon.presentation.ConsumableLaunchedEffect
import ch.protonmail.android.mailcommon.presentation.model.BottomSheetContentState
import ch.protonmail.android.mailcommon.presentation.model.BottomSheetState
import ch.protonmail.android.mailcommon.presentation.model.BottomSheetVisibilityEffect
import ch.protonmail.android.maillabel.presentation.bottomsheet.LabelAsBottomSheet
import ch.protonmail.android.maillabel.presentation.bottomsheet.LabelAsBottomSheetEntryPoint
import ch.protonmail.android.maillabel.presentation.bottomsheet.LabelAsBottomSheetScreen
import ch.protonmail.android.maillabel.presentation.bottomsheet.moveto.MoveToBottomSheet
import ch.protonmail.android.maillabel.presentation.bottomsheet.moveto.MoveToBottomSheetEntryPoint
import ch.protonmail.android.maillabel.presentation.bottomsheet.moveto.MoveToBottomSheetScreen
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.LabelAsBottomSheetState
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.MailboxMoreActionsBottomSheetState
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.MoveToBottomSheetState
import ch.protonmail.android.mailmessage.presentation.ui.bottomsheet.MailboxMoreActionBottomSheetContent
import ch.protonmail.android.mailmessage.presentation.ui.bottomsheet.MoreActionBottomSheetContent
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ContentSearchBottomSheetHost(
    bottomSheetState: BottomSheetState?,
    inSelectionMode: Boolean,
    actions: ContentSearchActions,
    content: @Composable () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showBottomSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    BackHandler(enabled = inSelectionMode && !sheetState.isVisible) { actions.onExitSelectionMode() }
    BackHandler(enabled = sheetState.isVisible) { actions.onDismissBottomSheet() }

    bottomSheetState?.let {
        ConsumableLaunchedEffect(effect = it.bottomSheetVisibilityEffect) { effect ->
            when (effect) {
                BottomSheetVisibilityEffect.Show -> showBottomSheet = true
                BottomSheetVisibilityEffect.Hide -> scope.launch { sheetState.hide() }
                    .invokeOnCompletion { if (!sheetState.isVisible) showBottomSheet = false }
            }
        }
    }

    ProtonModalBottomSheetLayout(
        showBottomSheet = showBottomSheet,
        onDismissed = { showBottomSheet = false },
        dismissOnBack = true,
        sheetState = sheetState,
        sheetContent = {
            ContentSearchBottomSheetContent(contentState = bottomSheetState?.contentState, actions = actions)
        },
        content = content
    )
}

@Composable
private fun ContentSearchBottomSheetContent(contentState: BottomSheetContentState?, actions: ContentSearchActions) {
    when (contentState) {
        is MoveToBottomSheetState.Requested -> MoveToBottomSheetScreen(
            providedData = MoveToBottomSheet.InitialData(
                userId = contentState.userId,
                currentLocationLabelId = contentState.currentLabel,
                items = contentState.itemIds,
                entryPoint = contentState.entryPoint
            ),
            actions = MoveToBottomSheet.Actions(
                onCreateNewFolderClick = actions.onDismissBottomSheet,
                onError = { _ -> actions.onDismissBottomSheet() },
                onMessage = { _ -> actions.onDismissBottomSheet() },
                onDismiss = actions.onDismissBottomSheet,
                onMoveToComplete = { label, _, entryPoint ->
                    actions.onMoveToCompleted(label, entryPoint.selectedItemCount())
                }
            )
        )

        is LabelAsBottomSheetState.Requested -> LabelAsBottomSheetScreen(
            providedData = LabelAsBottomSheet.InitialData(
                userId = contentState.userId,
                currentLocationLabelId = contentState.currentLocationLabelId,
                items = contentState.itemIds,
                entryPoint = contentState.entryPoint
            ),
            actions = LabelAsBottomSheet.Actions(
                onCreateNewLabelClick = actions.onDismissBottomSheet,
                onError = { _ -> actions.onDismissBottomSheet() },
                onDismiss = actions.onDismissBottomSheet,
                onLabelAsComplete = { archived, entryPoint ->
                    actions.onLabelAsCompleted(archived, entryPoint.selectedItemCount())
                }
            )
        )

        is MailboxMoreActionsBottomSheetState -> MailboxMoreActionBottomSheetContent(
            state = contentState,
            actionCallbacks = actions.toMoreActionCallbacks()
        )

        else -> Unit
    }
}

private fun MoveToBottomSheetEntryPoint.selectedItemCount(): Int =
    (this as? MoveToBottomSheetEntryPoint.Mailbox)?.itemCount ?: 1

private fun LabelAsBottomSheetEntryPoint.selectedItemCount(): Int =
    (this as? LabelAsBottomSheetEntryPoint.Mailbox)?.itemCount ?: 1

private fun ContentSearchActions.toMoreActionCallbacks(): MoreActionBottomSheetContent.Actions {
    fun dismissing(action: () -> Unit): () -> Unit = {
        action()
        onDismissBottomSheet()
    }

    return MoreActionBottomSheetContent.Actions(
        onStar = dismissing(onStarSelection),
        onUnStar = dismissing(onUnStarSelection),
        onArchive = dismissing(onArchive),
        onSpam = dismissing(onSpam),
        onLabel = onRequestLabelAs,
        onMarkRead = dismissing(onMarkRead),
        onMarkUnread = dismissing(onMarkUnread),
        onTrash = dismissing(onTrash),
        onDelete = dismissing(onDelete),
        onMoveTo = onRequestMoveTo,
        onInbox = dismissing(onMoveToInbox),
        onCustomizeToolbar = onDismissBottomSheet,
        onSnooze = onDismissBottomSheet
    )
}
