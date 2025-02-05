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

package ch.protonmail.android.mailsettings.presentation.settings.customizetoolbar.ui

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailsettings.presentation.R
import ch.protonmail.android.mailsettings.presentation.settings.customizetoolbar.ToolbarActionUiModel
import ch.protonmail.android.mailsettings.presentation.settings.customizetoolbar.model.CustomizeToolbarOperation
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme

internal const val DRAGGABLE_ACTIONS_START_IDX = 2

@Composable
internal fun ToolbarActions(
    items: List<ToolbarActionUiModel>,
    remainingItems: List<ToolbarActionUiModel>,
    disclaimer: TextUiModel,
    onAction: (CustomizeToolbarOperation) -> Unit,
    modifier: Modifier
) {
    var showResetToDefaultConfirmationDialog by remember { mutableStateOf(false) }

    if (showResetToDefaultConfirmationDialog) {
        ResetToDefaultConfirmationDialog(
            onConfirmClicked = {
                showResetToDefaultConfirmationDialog = false
                onAction(CustomizeToolbarOperation.ResetToDefaultConfirmed)
            },
            onCancelClicked = {
                showResetToDefaultConfirmationDialog = false
            }
        )
    }

    val listState = rememberLazyListState()
    val dragDropState = rememberDragDropState(
        listState,
        startIndex = DRAGGABLE_ACTIONS_START_IDX,
        count = items.size,
        onMove = { from, to ->
            onAction(CustomizeToolbarOperation.ActionMoved(fromIndex = from, toIndex = to))
        }
    )
    // Keep track of the top position of the column
    var lazyColumnPosY by remember { mutableFloatStateOf(0f) }
    LazyColumn(
        state = listState,
        modifier = modifier.onGloballyPositioned { coords ->
            lazyColumnPosY = coords.localToWindow(Offset.Zero).y
        }
    ) {
        item {
            ToolbarDisclaimer(
                text = disclaimer,
                modifier = Modifier.padding(top = ProtonDimens.DefaultSpacing)
                    .fillMaxWidth()
                    .padding(horizontal = ProtonDimens.DefaultSpacing)
            )
        }

        item {
            Text(
                text = stringResource(R.string.customize_toolbar_actions_section),
                color = ProtonTheme.colors.textNorm,
                style = ProtonTheme.typography.body1Regular,
                modifier = Modifier.padding(ProtonDimens.DefaultSpacing)
            )
        }

        itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
            DraggableItem(dragDropState, index + DRAGGABLE_ACTIONS_START_IDX) { _ ->
                var rowPosY by remember { mutableFloatStateOf(0f) }
                SelectedToolbarActionDisplay(
                    item, reorderButton = {
                        ActionDragHandle(
                            modifier = Modifier
                                .pointerInput(dragDropState) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = { localBoxOffset ->
                                            val boxTopInWindow = rowPosY + localBoxOffset.y
                                            val boxTopInLazyCol = boxTopInWindow - lazyColumnPosY
                                            dragDropState.onDragStart(Offset(0f, boxTopInLazyCol))
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            dragDropState.onDrag(dragAmount)
                                        },
                                        onDragEnd = { dragDropState.onDragInterrupted() },
                                        onDragCancel = { dragDropState.onDragInterrupted() }
                                    )
                                }
                        )
                    }, onRemoveClicked = {
                        onAction(CustomizeToolbarOperation.ActionRemoved(item.id))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { layoutCoordinates ->
                            rowPosY = layoutCoordinates.localToWindow(Offset.Zero).y
                        }
                )
            }
        }

        item {
            Text(
                text = stringResource(R.string.customize_toolbar_available_actions_section),
                color = ProtonTheme.colors.textNorm,
                style = ProtonTheme.typography.body1Regular,
                modifier = Modifier.padding(ProtonDimens.DefaultSpacing)
            )
        }

        itemsIndexed(remainingItems, key = { _, item -> item.id }) { index, item ->
            UnselectedToolbarActionDisplay(item, onAddClicked = {
                onAction(CustomizeToolbarOperation.ActionSelected(item.id))
            }, modifier = Modifier)
        }

        item {
            Box(modifier = Modifier.fillMaxWidth()) {
                ResetToDefaultButton(onClick = {
                    showResetToDefaultConfirmationDialog = true
                })
            }
        }
    }
}
