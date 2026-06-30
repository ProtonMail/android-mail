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
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.mailcommon.presentation.compose.MailDimens
import ch.protonmail.android.mailcommon.presentation.model.BottomBarState
import ch.protonmail.android.mailcommon.presentation.ui.BottomActionBar
import ch.protonmail.android.mailcommon.presentation.ui.FloatingToolbarActionIcons
import ch.protonmail.android.mailcommon.presentation.ui.protonFloatingButtonShadow
import ch.protonmail.android.mailcommon.presentation.ui.rememberWindowFocusState
import ch.protonmail.android.mailmailbox.presentation.R
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.UnreadFilterState

/**
 * Hosts the floating bottom controls and orchestrates which are visible:
 * - the unread filter pill (start), shown only while idle,
 * - the search FAB (end), shown only while idle,
 * - the compose FAB that morphs into the selection toolbar (center/end).
 *
 * Each control owns its own enter/exit animation; this function only decides visibility.
 */
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
    onSearchClick: () -> Unit,
    onUnreadFilterEnabled: () -> Unit,
    onUnreadFilterDisabled: () -> Unit,
    isSearchButtonVisible: Boolean = false,
    isSnackbarVisible: Boolean = false,
    modifier: Modifier = Modifier
) {
    val snackbarOffset by animateDpAsState(
        targetValue = if (isSnackbarVisible) MailDimens.SnackbarFabOffset else 0.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "snackbarOffset"
    )

    val hasWindowFocus by rememberWindowFocusState()

    // The unread filter and the search FAB are only relevant in the idle list,
    // i.e. neither selecting items nor searching.
    val isIdle = !isInSelectionMode && !isInSearch

    Box(
        modifier = modifier
            .padding(bottom = snackbarOffset)
            .fillMaxWidth()
    ) {
        UnreadFilterFab(
            visible = showBottomUnreadFilter && isIdle,
            hasWindowFocus = hasWindowFocus,
            state = unreadFilterState,
            onFilterEnabled = onUnreadFilterEnabled,
            onFilterDisabled = onUnreadFilterDisabled,
            modifier = Modifier.align(Alignment.CenterStart)
        )

        SearchFab(
            visible = isSearchButtonVisible && isIdle,
            enabled = !isInSelectionMode,
            hasWindowFocus = hasWindowFocus,
            onClick = onSearchClick,
            modifier = Modifier.align(Alignment.CenterEnd)
        )

        ComposeFabToolbarMorph(
            isInSelectionMode = isInSelectionMode,
            isInSearch = isInSearch,
            hasWindowFocus = hasWindowFocus,
            bottomBarState = bottomBarState,
            bottomBarActions = bottomBarActions,
            onComposeClick = onComposeClick,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun UnreadFilterFab(
    visible: Boolean,
    hasWindowFocus: Boolean,
    state: UnreadFilterState,
    onFilterEnabled: () -> Unit,
    onFilterDisabled: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Latch through a state so the control animates in on first appearance instead of snapping.
    var show by remember { mutableStateOf(false) }
    LaunchedEffect(visible) { show = visible }

    val alpha by animateFloatAsState(if (show) 1f else 0f, PopInSpring, label = "unreadAlpha")
    val scale by animateFloatAsState(if (show) 1f else POP_IN_HIDDEN_SCALE, PopInSpring, label = "unreadScale")
    val translationY by animateFloatAsState(
        if (show) 0f else POP_IN_HIDDEN_TRANSLATION_Y, PopInSpring, label = "unreadTranslationY"
    )

    // Skip drawing the overlay as soon as the window loses focus, so an OEM
    // extended screenshot can't capture it.
    if (hasWindowFocus && (show || alpha > 0f)) {
        Box(
            modifier = modifier
                .graphicsLayer {
                    this.alpha = alpha
                    scaleX = scale
                    scaleY = scale
                    this.translationY = translationY
                    // Fade without an offscreen buffer: ModulateAlpha clips the drop
                    // shadow drawn outside the layer bounds while alpha < 1.
                    compositingStrategy = CompositingStrategy.ModulateAlpha
                }
                .padding(ShadowClipGuard)
        ) {
            BottomUnreadFilterButton(
                state = state,
                onFilterEnabled = onFilterEnabled,
                onFilterDisabled = onFilterDisabled
            )
        }
    }
}

@Composable
private fun SearchFab(
    visible: Boolean,
    enabled: Boolean,
    hasWindowFocus: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var show by remember { mutableStateOf(false) }
    LaunchedEffect(visible) { show = visible }

    val alpha by animateFloatAsState(if (show) 1f else 0f, PopInSpring, label = "searchAlpha")
    val scale by animateFloatAsState(if (show) 1f else POP_IN_HIDDEN_SCALE, PopInSpring, label = "searchScale")
    val translationY by animateFloatAsState(
        if (show) 0f else POP_IN_HIDDEN_TRANSLATION_Y, PopInSpring, label = "searchTranslationY"
    )

    if (hasWindowFocus && (show || alpha > 0f)) {
        Box(
            modifier = modifier
                .padding(end = FabSize + SearchFabSpacing)
                .padding(ShadowClipGuard)
                .graphicsLayer {
                    this.alpha = alpha
                    scaleX = scale
                    scaleY = scale
                    this.translationY = translationY
                    // Fade without an offscreen buffer: ModulateAlpha clips the drop
                    // shadow drawn outside the layer bounds while alpha < 1.
                    compositingStrategy = CompositingStrategy.ModulateAlpha
                }
        ) {
            Surface(
                modifier = Modifier
                    .width(FabSize)
                    .height(FabSize)
                    .protonFloatingButtonShadow(),
                shape = RoundedCornerShape(percent = 50),
                color = ProtonTheme.colors.interactionFabNorm
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.clickable(enabled = enabled) { onClick() }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_proton_magnifier),
                        contentDescription = stringResource(
                            id = R.string.mailbox_toolbar_search_button_content_description
                        ),
                        tint = ProtonTheme.colors.textNorm
                    )
                }
            }
        }
    }
}

@Composable
private fun ComposeFabToolbarMorph(
    isInSelectionMode: Boolean,
    isInSearch: Boolean,
    hasWindowFocus: Boolean,
    bottomBarState: BottomBarState,
    bottomBarActions: BottomActionBar.Actions,
    onComposeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lastShownState = remember { mutableStateOf<BottomBarState.Data.Shown?>(null) }
    if (bottomBarState is BottomBarState.Data.Shown) {
        lastShownState.value = bottomBarState
    }

    // --- selection morph: FAB (bottom-end) <-> toolbar (centered) ---
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

    // --- search fade: hide the bottom bar while searching, but never while selecting ---
    val fadeForSearch = isInSearch && !isInSelectionMode
    val searchAlpha by animateFloatAsState(if (fadeForSearch) 0f else 1f, PopInSpring, label = "composeAlpha")
    val searchScale by animateFloatAsState(
        if (fadeForSearch) POP_IN_HIDDEN_SCALE else 1f, PopInSpring, label = "composeScale"
    )
    val searchTranslationY by animateFloatAsState(
        if (fadeForSearch) POP_IN_HIDDEN_TRANSLATION_Y else 0f, PopInSpring, label = "composeTranslationY"
    )

    if (hasWindowFocus && (!fadeForSearch || searchAlpha > 0f)) {
        Box(
            modifier = modifier
                .padding(ShadowClipGuard)
                .graphicsLayer {
                    alpha = searchAlpha
                    scaleX = searchScale
                    scaleY = searchScale
                    translationY = searchTranslationY
                    // Fade without an offscreen buffer: ModulateAlpha clips the drop
                    // shadow drawn outside the layer bounds while alpha < 1.
                    compositingStrategy = CompositingStrategy.ModulateAlpha
                },
            contentAlignment = BiasAlignment(horizontalBias = horizontalBias, verticalBias = 0f)
        ) {
            Surface(
                modifier = Modifier
                    .width(containerWidth)
                    .height(FabSize)
                    .protonFloatingButtonShadow(),
                shape = RoundedCornerShape(percent = 50),
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

private val FabSize = 56.dp
private val SearchFabSpacing = 12.dp
private val ToolbarHorizontalPadding = 12.dp
private const val ICON_BUTTON_SIZE = 48
private val ShadowClipGuard = 6.dp

// Shared "pop in" enter/exit animation used by the floating controls.
private val PopInSpring = spring<Float>(
    dampingRatio = Spring.DampingRatioLowBouncy,
    stiffness = Spring.StiffnessMediumLow
)
private const val POP_IN_HIDDEN_SCALE = 0.6f
private const val POP_IN_HIDDEN_TRANSLATION_Y = 40f
