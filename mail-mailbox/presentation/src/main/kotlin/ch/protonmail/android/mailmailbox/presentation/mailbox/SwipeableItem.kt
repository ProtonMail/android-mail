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

package ch.protonmail.android.mailmailbox.presentation.mailbox

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.DismissDirection
import androidx.compose.material.DismissState
import androidx.compose.material.DismissValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FractionalThreshold
import androidx.compose.material.Icon
import androidx.compose.material.ResistanceConfig
import androidx.compose.material.SwipeableDefaults
import androidx.compose.material.ThresholdConfig
import androidx.compose.material.rememberDismissState
import androidx.compose.material.swipeable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.SwipeActionsUiModel
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.mailsettings.domain.entity.SwipeAction
import timber.log.Timber
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterialApi::class)
@Suppress("ComplexMethod")
@Composable
fun SwipeableItem(
    modifier: Modifier = Modifier,
    swipeActionsUiModel: SwipeActionsUiModel?,
    swipeActionCallbacks: SwipeActions.Actions,
    swipingEnabled: Boolean = true,
    content: @Composable () -> Unit
) = BoxWithConstraints(modifier) {
    val width = constraints.maxWidth.toFloat()
    val threshold = 0.3f

    var willDismissDirection: DismissDirection? by remember { mutableStateOf(null) }
    val dismissState = rememberDismissState(
        confirmStateChange = { dismissValue ->
            swipeActionsUiModel?.let {
                if (willDismissDirection == DismissDirection.StartToEnd &&
                    dismissValue == DismissValue.DismissedToEnd
                ) {
                    callbackForSwipeAction(it.start.swipeAction, swipeActionCallbacks)()
                } else if (willDismissDirection == DismissDirection.EndToStart &&
                    dismissValue == DismissValue.DismissedToStart
                ) {
                    callbackForSwipeAction(it.end.swipeAction, swipeActionCallbacks)()
                }
            }
            false
        }
    )

    LaunchedEffect(key1 = Unit) {
        snapshotFlow { dismissState.offset.value }
            .collect {
                willDismissDirection = when {
                    it > width * threshold -> DismissDirection.StartToEnd
                    it < -width * threshold -> DismissDirection.EndToStart
                    else -> null
                }
            }
    }

    val haptic = LocalHapticFeedback.current
    LaunchedEffect(key1 = willDismissDirection) {
        Timber.d("Haptic effect")
        if (willDismissDirection != null) {
            Timber.d("Haptic effect: triggered")
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    SwipeableItemContainer(
        state = dismissState,
        dismissThresholds = { FractionalThreshold(threshold) },
        directions = if (swipingEnabled && swipeActionsUiModel != null) setOf(
            DismissDirection.StartToEnd,
            DismissDirection.EndToStart
        ) else emptySet(),
        background = {
            val direction = dismissState.dismissDirection ?: return@SwipeableItemContainer
            if (swipeActionsUiModel == null) return@SwipeableItemContainer

            val color = when (direction) {
                DismissDirection.StartToEnd -> swipeActionsUiModel.start.getColor()
                DismissDirection.EndToStart -> swipeActionsUiModel.end.getColor()
            }

            val alignment = when (direction) {
                DismissDirection.StartToEnd -> Alignment.CenterStart
                DismissDirection.EndToStart -> Alignment.CenterEnd
            }

            val icon = when (direction) {
                DismissDirection.StartToEnd -> swipeActionsUiModel.start.icon
                DismissDirection.EndToStart -> swipeActionsUiModel.end.icon
            }
            val description = when (direction) {
                DismissDirection.StartToEnd -> swipeActionsUiModel.start.descriptionRes
                DismissDirection.EndToStart -> swipeActionsUiModel.end.descriptionRes
            }

            val scale by animateFloatAsState(
                targetValue = when (dismissState.targetValue) {
                    DismissValue.Default -> 0.75f
                    DismissValue.DismissedToEnd, DismissValue.DismissedToStart -> 1f
                },
                label = "swipe_scale"
            )

            Box(
                Modifier
                    .fillMaxSize()
                    .background(color)
                    .padding(horizontal = ProtonDimens.MediumSpacing),
                contentAlignment = alignment
            ) {
                Icon(
                    modifier = Modifier
                        .align(alignment)
                        .scale(scale),
                    painter = painterResource(id = icon),
                    contentDescription = stringResource(id = description),
                    tint = ProtonTheme.colors.iconInverted
                )
            }
        }
    ) {
        content()
    }
}

@Composable
@ExperimentalMaterialApi
fun SwipeableItemContainer(
    state: DismissState,
    modifier: Modifier = Modifier,
    directions: Set<DismissDirection> = setOf(DismissDirection.EndToStart, DismissDirection.StartToEnd),
    dismissThresholds: (DismissDirection) -> ThresholdConfig,
    background: @Composable RowScope.() -> Unit,
    dismissContent: @Composable RowScope.() -> Unit
) = BoxWithConstraints(modifier) {
    val width = constraints.maxWidth.toFloat()
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    val anchors = mutableMapOf(0f to DismissValue.Default)
    if (DismissDirection.StartToEnd in directions) anchors += width to DismissValue.DismissedToEnd
    if (DismissDirection.EndToStart in directions) anchors += -width to DismissValue.DismissedToStart

    val thresholds = { from: DismissValue, to: DismissValue ->
        // reused from the composable in the material library
        dismissThresholds(getDismissDirection(from, to)!!)
    }

    val minFactor = if (DismissDirection.EndToStart in directions) SwipeableDefaults.StandardResistanceFactor else 0f
    val maxFactor = if (DismissDirection.StartToEnd in directions) SwipeableDefaults.StandardResistanceFactor else 0f

    Box(
        Modifier.swipeable(
            state = state,
            anchors = anchors,
            thresholds = thresholds,
            orientation = Orientation.Horizontal,
            enabled = state.currentValue == DismissValue.Default,
            reverseDirection = isRtl,
            resistance = ResistanceConfig(
                basis = width,
                factorAtMin = minFactor,
                factorAtMax = maxFactor
            )
        )
    ) {
        Row(
            content = background,
            modifier = Modifier.matchParentSize()
        )
        Row(
            content = dismissContent,
            modifier = Modifier.offset { IntOffset(state.offset.value.roundToInt(), 0) }
        )
    }
}

fun callbackForSwipeAction(action: SwipeAction, swipeActionCallbacks: SwipeActions.Actions) = when (action) {
    SwipeAction.Trash -> swipeActionCallbacks.onTrash
    SwipeAction.Spam -> swipeActionCallbacks.onSpam
    SwipeAction.Star -> swipeActionCallbacks.onStar
    SwipeAction.Archive -> swipeActionCallbacks.onArchive
    SwipeAction.MarkRead -> swipeActionCallbacks.onMarkRead
}

object SwipeActions {
    data class Actions(
        val onTrash: () -> Unit,
        val onSpam: () -> Unit,
        val onStar: () -> Unit,
        val onArchive: () -> Unit,
        val onMarkRead: () -> Unit
    )
}

@Suppress("ComplexMethod")
private fun getDismissDirection(from: DismissValue, to: DismissValue): DismissDirection? {
    return when {
        // settled at the default state
        from == to && from == DismissValue.Default -> null
        // has been dismissed to the end
        from == to && from == DismissValue.DismissedToEnd -> DismissDirection.StartToEnd
        // has been dismissed to the start
        from == to && from == DismissValue.DismissedToStart -> DismissDirection.EndToStart
        // is currently being dismissed to the end
        from == DismissValue.Default && to == DismissValue.DismissedToEnd -> DismissDirection.StartToEnd
        // is currently being dismissed to the start
        from == DismissValue.Default && to == DismissValue.DismissedToStart -> DismissDirection.EndToStart
        // has been dismissed to the end but is now animated back to default
        from == DismissValue.DismissedToEnd && to == DismissValue.Default -> DismissDirection.StartToEnd
        // has been dismissed to the start but is now animated back to default
        from == DismissValue.DismissedToStart && to == DismissValue.Default -> DismissDirection.EndToStart
        else -> null
    }
}
