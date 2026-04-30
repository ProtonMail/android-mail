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

package ch.protonmail.android.mailmailbox.presentation.mailbox

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.mailcommon.presentation.compose.MailDimens
import ch.protonmail.android.mailcommon.presentation.model.BottomBarState
import ch.protonmail.android.mailcommon.presentation.ui.BottomActionBar
import ch.protonmail.android.mailcommon.presentation.ui.FloatingToolbarActionIcons
import ch.protonmail.android.mailcommon.presentation.ui.rememberWindowFocusState
import ch.protonmail.android.mailmailbox.presentation.R
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.UnreadFilterState

@Suppress("UseComposableActions")
@Composable
internal fun MailboxFabToolbarMorph(
    isInSelectionMode: Boolean,
    isInSearch: Boolean,
    showBottomUnreadFilter: Boolean,
    unreadFilterState: UnreadFilterState,
    bottomBarState: BottomBarState,
    bottomBarActions: BottomActionBar.Actions,
    onComposeClick: () -> Unit,
    onUnreadFilterEnabled: () -> Unit,
    onUnreadFilterDisabled: () -> Unit,
    isSnackbarVisible: Boolean = false,
    modifier: Modifier = Modifier
) {
    val lastShownState = remember { mutableStateOf<BottomBarState.Data.Shown?>(null) }
    if (bottomBarState is BottomBarState.Data.Shown) {
        lastShownState.value = bottomBarState
    }

    val snackbarOffset by animateDpAsState(
        targetValue = if (isSnackbarVisible) MailDimens.SnackbarFabOffset else 0.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "snackbarOffset"
    )

    val transition = updateTransition(targetState = isInSelectionMode, label = "fabToolbarMorph")

    val actionCount = (lastShownState.value?.actions?.size ?: 0)
        .coerceAtMost(BottomActionBar.MAX_ACTIONS_COUNT + 1)
    val expandedWidth = (actionCount * ICON_BUTTON_SIZE).dp + ToolbarHorizontalPadding * 2

    val containerWidth by transition.animateDp(
        transitionSpec = { spring(stiffness = Spring.StiffnessMediumLow) },
        label = "width"
    ) { inSelection -> if (inSelection) expandedWidth else FabSize }

    val fabAlpha by transition.animateFloat(
        transitionSpec = { tween(if (targetState) 100 else 200) },
        label = "fabAlpha"
    ) { inSelection -> if (inSelection) 0f else 1f }

    val toolbarAlpha by transition.animateFloat(
        transitionSpec = { tween(if (targetState) 200 else 100, delayMillis = if (targetState) 80 else 0) },
        label = "toolbarAlpha"
    ) { inSelection -> if (inSelection) 1f else 0f }

    val horizontalBias by transition.animateFloat(
        transitionSpec = { spring(stiffness = Spring.StiffnessMediumLow) },
        label = "horizontalBias"
    ) { inSelection -> if (inSelection) 0f else 1f }

    val hasWindowFocus by rememberWindowFocusState()

    Box(
        modifier = modifier
            .padding(bottom = snackbarOffset)
            .fillMaxWidth()
    ) {
        val wantsUnreadFilter = showBottomUnreadFilter && !isInSelectionMode && !isInSearch
        var showUnreadFilter by remember { mutableStateOf(false) }
        LaunchedEffect(wantsUnreadFilter) {
            showUnreadFilter = wantsUnreadFilter
        }

        val unreadSpring = spring<Float>(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        )
        val unreadAlpha by animateFloatAsState(
            targetValue = if (showUnreadFilter) 1f else 0f,
            animationSpec = unreadSpring,
            label = "unreadAlpha"
        )
        val unreadScale by animateFloatAsState(
            targetValue = if (showUnreadFilter) 1f else 0.6f,
            animationSpec = unreadSpring,
            label = "unreadScale"
        )
        val unreadTranslationY by animateFloatAsState(
            targetValue = if (showUnreadFilter) 0f else 40f,
            animationSpec = unreadSpring,
            label = "unreadTranslationY"
        )
        // Skip drawing the floating overlays as soon as the window loses focus,
        // so an OEM extended screenshot can't capture them.
        if (hasWindowFocus && (showUnreadFilter || unreadAlpha > 0f)) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .graphicsLayer {
                        alpha = unreadAlpha
                        scaleX = unreadScale
                        scaleY = unreadScale
                        translationY = unreadTranslationY
                    }
                    .padding(ShadowClipGuard)
            ) {
                BottomUnreadFilterButton(
                    state = unreadFilterState,
                    onFilterEnabled = onUnreadFilterEnabled,
                    onFilterDisabled = onUnreadFilterDisabled
                )
            }
        }

        // FAB / Toolbar morph – animates from bottom end (FAB) to center (toolbar)
        if (hasWindowFocus) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(ShadowClipGuard),
                contentAlignment = BiasAlignment(horizontalBias = horizontalBias, verticalBias = 0f)
            ) {
                Surface(
                    modifier = Modifier
                        .width(containerWidth)
                        .height(FabSize),
                    shape = RoundedCornerShape(percent = 50),
                    shadowElevation = ProtonDimens.ShadowElevation.Mini,
                    color = ProtonTheme.colors.interactionFabNorm
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.clickable(enabled = !isInSelectionMode) { onComposeClick() }
                    ) {
                        // FAB icon
                        Icon(
                            painter = painterResource(id = R.drawable.ic_proton_pen_square),
                            contentDescription = stringResource(
                                id = R.string.mailbox_fab_compose_button_content_description
                            ),
                            tint = ProtonTheme.colors.textNorm,
                            modifier = Modifier.graphicsLayer { alpha = fabAlpha }
                        )

                        // Toolbar actions – keep in composition while animating, remove once done
                        // so invisible IconButtons don't steal hits from the FAB.
                        val shownData = lastShownState.value
                        val isToolbarActive = isInSelectionMode || transition.currentState != transition.targetState
                        if (shownData != null && isToolbarActive) {
                            Row(
                                modifier = Modifier
                                    .graphicsLayer { alpha = toolbarAlpha }
                                    .fillMaxWidth()
                                    .padding(horizontal = ToolbarHorizontalPadding),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FloatingToolbarActionIcons(
                                    actions = shownData.actions,
                                    target = shownData.target,
                                    viewActionCallbacks = bottomBarActions
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private val FabSize = 56.dp
private val ToolbarHorizontalPadding = 12.dp
private const val ICON_BUTTON_SIZE = 48
private val ShadowClipGuard = 6.dp
