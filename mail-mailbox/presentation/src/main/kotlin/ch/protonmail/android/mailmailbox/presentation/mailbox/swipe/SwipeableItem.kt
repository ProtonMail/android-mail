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

package ch.protonmail.android.mailmailbox.presentation.mailbox.swipe

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxDefaults
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.unit.Density
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.mailcommon.presentation.compose.SwipeThreshold
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxItemUiModel
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.SwipeActionsUiModel
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import me.proton.core.mailsettings.domain.entity.SwipeAction
import kotlin.math.abs

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun SwipeableItem(
    modifier: Modifier = Modifier,
    swipeActionsUiModel: SwipeActionsUiModel?,
    swipeActionCallbacks: SwipeActions.Actions,
    swipingEnabled: Boolean = true,
    item: MailboxItemUiModel,
    content: @Composable () -> Unit
) = BoxWithConstraints(modifier) {
    val width = constraints.maxWidth.toFloat()
    val haptic = LocalHapticFeedback.current
    val threshold = SwipeThreshold.SWIPE_THRESHOLD_PERCENTAGE

    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { _ -> width * threshold },
        enableFling = false
    )

    var lifecycle: SwipeLifecycleState by remember { mutableStateOf(SwipeLifecycleState.Idle) }
    var prevDirection by remember { mutableStateOf(SwipeToDismissBoxValue.Settled) }

    fun dispatch(event: SwipeLifecycleEvent) {
        lifecycle = SwipeLifecycleReducer.reduce(lifecycle, event)
    }

    // Swipe disabled
    LaunchedEffect(swipingEnabled) {
        if (!swipingEnabled) {
            dispatch(SwipeLifecycleEvent.SwipingDisabled)
        }
    }

    // Direction change events
    LaunchedEffect(dismissState.dismissDirection, swipingEnabled) {
        if (!swipingEnabled) return@LaunchedEffect

        val newDirection = dismissState.dismissDirection

        val gestureStarted =
            prevDirection == SwipeToDismissBoxValue.Settled &&
                newDirection != SwipeToDismissBoxValue.Settled

        if (gestureStarted) {
            dispatch(SwipeLifecycleEvent.GestureStarted(newDirection))
        } else {
            dispatch(SwipeLifecycleEvent.DirectionChanged(newDirection))
        }

        prevDirection = newDirection
    }

    // Finger up detection
    val releaseDetectorModifier = Modifier.pointerInput(swipingEnabled) {
        if (!swipingEnabled) return@pointerInput

        awaitPointerEventScope {
            while (true) {
                awaitFirstDown(requireUnconsumed = false)

                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Final)
                    val anyPressed = event.changes.any { it.pressed }
                    if (!anyPressed) {
                        dispatch(SwipeLifecycleEvent.PointerReleased)
                        break
                    }
                }
            }
        }
    }

    // Swiping -> Threshold reached
    if (lifecycle is SwipeLifecycleState.Swiping && swipingEnabled) {
        LaunchedEffect(width) {

            val direction = lifecycle.direction
            if (lifecycle.direction == SwipeToDismissBoxValue.Settled) return@LaunchedEffect

            // dismissState.requireOffset() is the only available API to calculate the swipe fraction.
            // However, early reading of dismissState.requireOffset may cause exception. Therefore we call it only after
            // the swipe direction has been determined ( not settled). This means user started swiping.
            snapshotFlow { runCatching { dismissState.requireOffset() }.getOrNull() }
                .filterNotNull()
                .first { offset ->
                    val fraction = (abs(offset) / width).coerceIn(0f, 1f)
                    fraction >= threshold
                }

            dispatch(SwipeLifecycleEvent.ThresholdReached(direction))
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }


    // Armed -> Swiped back under threshold -> Threshold revoked
    if (lifecycle is SwipeLifecycleState.Armed) {
        LaunchedEffect(width) {

            val direction = lifecycle.direction
            if (lifecycle.direction == SwipeToDismissBoxValue.Settled) return@LaunchedEffect

            snapshotFlow { runCatching { dismissState.requireOffset() }.getOrNull() }
                .filterNotNull()
                .first { offset ->
                    val fraction = (abs(offset) / width).coerceIn(0f, 1f)
                    fraction < threshold
                }

            dispatch(SwipeLifecycleEvent.ThresholdRevoked(direction))
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    // Execute on finger release
    LaunchedEffect(lifecycle, swipingEnabled, swipeActionsUiModel) {
        if (!swipingEnabled) return@LaunchedEffect

        val ready = lifecycle as? SwipeLifecycleState.ReadyToExecute ?: return@LaunchedEffect
        val uiModel = swipeActionsUiModel ?: return@LaunchedEffect

        when (ready.direction) {
            SwipeToDismissBoxValue.StartToEnd ->
                callbackForSwipeAction(uiModel.start.swipeAction, swipeActionCallbacks)()

            SwipeToDismissBoxValue.EndToStart ->
                callbackForSwipeAction(uiModel.end.swipeAction, swipeActionCallbacks)()

            SwipeToDismissBoxValue.Settled -> Unit
        }

        dispatch(SwipeLifecycleEvent.ActionExecuted)
    }

    val enableDismissFromStartToEnd = swipeActionsUiModel?.start?.let {
        it.swipeAction != SwipeAction.None && it.isEnabled
    } ?: false

    val enableDismissFromEndToStart = swipeActionsUiModel?.end?.let {
        it.swipeAction != SwipeAction.None && it.isEnabled
    } ?: false

    SwipeToDismissBox(
        modifier = modifier.then(releaseDetectorModifier),
        state = dismissState,
        gesturesEnabled = swipingEnabled,
        enableDismissFromStartToEnd = enableDismissFromStartToEnd,
        enableDismissFromEndToStart = enableDismissFromEndToStart,
        backgroundContent = {
            swipeActionsUiModel ?: return@SwipeToDismissBox

            val properties = when (dismissState.dismissDirection) {
                SwipeToDismissBoxValue.StartToEnd -> SwipeActionProperties(
                    swipeActionsUiModel.start.getColor(),
                    Alignment.CenterStart,
                    swipeActionsUiModel.start.iconProvider.resolve(item),
                    swipeActionsUiModel.start.descriptionRes
                )

                SwipeToDismissBoxValue.EndToStart -> SwipeActionProperties(
                    swipeActionsUiModel.end.getColor(),
                    Alignment.CenterEnd,
                    swipeActionsUiModel.end.iconProvider.resolve(item),
                    swipeActionsUiModel.end.descriptionRes
                )

                SwipeToDismissBoxValue.Settled -> return@SwipeToDismissBox
            }

            val scale by animateFloatAsState(
                targetValue = lifecycle.scaleTarget,
                animationSpec = tween(durationMillis = 500),
                label = "swipe_scale"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(properties.color)
                    .padding(horizontal = ProtonDimens.Spacing.Medium),
                contentAlignment = properties.alignment
            ) {
                Icon(
                    modifier = Modifier
                        .align(properties.alignment)
                        .scale(scale),
                    painter = painterResource(id = properties.icon),
                    contentDescription = stringResource(id = properties.description),
                    tint = ProtonTheme.colors.iconInverted
                )
            }
        }
    ) {
        content()
    }
}

// We're using the deprecated [SwipeToDismissBoxState.Saver] and [SwipeToDismissBoxState] constructors on purpose here.
// There is no proper way to control the velocity threshold that is needed to prevent accidental swipes.
// See https://issuetracker.google.com/issues/252334353
@Composable
@Suppress("Deprecation")
private fun rememberSwipeToDismissBoxState(
    initialValue: SwipeToDismissBoxValue = SwipeToDismissBoxValue.Settled,
    confirmValueChange: (SwipeToDismissBoxValue) -> Boolean = { false },
    positionalThreshold: (totalDistance: Float) -> Float = SwipeToDismissBoxDefaults.positionalThreshold,
    enableFling: Boolean = false
): SwipeToDismissBoxState {
    val density = if (enableFling) LocalDensity.current else Density(Float.POSITIVE_INFINITY)
    return rememberSaveable(
        saver = SwipeToDismissBoxState.Saver(
            confirmValueChange = confirmValueChange, density = density, positionalThreshold = positionalThreshold
        )
    ) {
        SwipeToDismissBoxState(initialValue, density, confirmValueChange, positionalThreshold)
    }
}

private data class SwipeActionProperties(
    val color: Color,
    val alignment: Alignment,
    val icon: Int,
    val description: Int
)

private fun callbackForSwipeAction(action: SwipeAction, swipeActionCallbacks: SwipeActions.Actions) = when (action) {
    SwipeAction.None -> swipeActionCallbacks.onNone
    SwipeAction.Trash -> swipeActionCallbacks.onTrash
    SwipeAction.Spam -> swipeActionCallbacks.onSpam
    SwipeAction.Star -> swipeActionCallbacks.onStar
    SwipeAction.Archive -> swipeActionCallbacks.onArchive
    SwipeAction.MarkRead -> swipeActionCallbacks.onMarkRead
    SwipeAction.MoveTo -> swipeActionCallbacks.onMoveTo
    SwipeAction.LabelAs -> swipeActionCallbacks.onLabelAs
}

fun getAccessibilityActionsForTalkback(
    swipingEnabled: Boolean,
    swipeActionsUiModel: SwipeActionsUiModel,
    swipeActions: SwipeActions.Actions,
    context: Context
) = mutableListOf<CustomAccessibilityAction>().apply {
    if (swipingEnabled) {
        if (swipeActionsUiModel.start.isEnabled) {
            add(
                CustomAccessibilityAction(
                    label = context.getString(swipeActionsUiModel.start.descriptionRes)
                ) {
                    callbackForSwipeAction(swipeActionsUiModel.start.swipeAction, swipeActions)()
                    true
                }
            )
        }
        if (swipeActionsUiModel.end.isEnabled) {
            add(
                CustomAccessibilityAction(
                    label = context.getString(swipeActionsUiModel.end.descriptionRes)
                ) {
                    callbackForSwipeAction(swipeActionsUiModel.end.swipeAction, swipeActions)()
                    true
                }
            )
        }
    }
}

object SwipeActions {
    data class Actions(
        val onNone: () -> Unit = {},
        val onTrash: () -> Unit,
        val onSpam: () -> Unit,
        val onStar: () -> Unit,
        val onArchive: () -> Unit,
        val onMarkRead: () -> Unit,
        val onMoveTo: () -> Unit,
        val onLabelAs: () -> Unit
    )
}

private const val DELAY_ANIMATION_VALUE = 200L
