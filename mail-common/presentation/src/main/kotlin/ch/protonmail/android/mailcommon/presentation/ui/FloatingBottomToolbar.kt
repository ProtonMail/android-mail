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

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.mailcommon.domain.model.Action
import ch.protonmail.android.mailcommon.presentation.model.ActionUiModel
import ch.protonmail.android.mailcommon.presentation.model.BottomBarState
import ch.protonmail.android.mailcommon.presentation.model.BottomBarTarget
import ch.protonmail.android.mailcommon.presentation.model.string
import kotlinx.collections.immutable.ImmutableList

@Composable
fun FloatingBottomToolbar(
    state: BottomBarState,
    viewActionCallbacks: BottomActionBar.Actions,
    modifier: Modifier = Modifier
) {
    val hasWindowFocus by rememberWindowFocusState()
    val isVisible = state is BottomBarState.Data.Shown

    val lastShownState = remember { mutableStateOf<BottomBarState.Data.Shown?>(null) }
    if (state is BottomBarState.Data.Shown) {
        lastShownState.value = state
    }

    val shownData = lastShownState.value ?: return

    val surfaceColor by animateColorAsState(
        targetValue = if (isVisible) ProtonTheme.colors.interactionFabNorm else Color.Transparent,
        animationSpec = tween(ANIMATION_DURATION),
        label = "surfaceColor"
    )
    val contentAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(ANIMATION_DURATION),
        label = "contentAlpha"
    )

    // Skip the whole surface while the window is unfocused so an OEM extended
    // screenshot can't capture the toolbar. A normal hide (e.g. selection cleared)
    // still goes through the existing fade-out via isVisible.
    if (!hasWindowFocus) return

    Surface(
        modifier = modifier
            .height(FloatingToolbarHeight)
            .protonFloatingButtonShadow(),
        shape = RoundedCornerShape(percent = 50),
        color = surfaceColor
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = ProtonDimens.Spacing.Standard)
                .graphicsLayer { alpha = contentAlpha },
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Only render action icons while visible or still fading out,
            // so invisible IconButtons don't steal taps from content behind.
            if (isVisible || contentAlpha > 0f) {
                FloatingToolbarActionIcons(
                    actions = shownData.actions,
                    target = shownData.target,
                    viewActionCallbacks = viewActionCallbacks
                )
            }
        }
    }
}

/**
 * Renders action icons inside the floating toolbar.
 * Uses horizontal three-dots icon for the "More" action.
 */
@Composable
fun FloatingToolbarActionIcons(
    actions: ImmutableList<ActionUiModel>,
    target: BottomBarTarget,
    viewActionCallbacks: BottomActionBar.Actions
) {
    actions.forEachIndexed { index, uiModel ->
        if (index > BottomActionBar.MAX_ACTIONS_COUNT) {
            return@forEachIndexed
        }
        IconButton(
            onClick = callbackForAction(uiModel.action, viewActionCallbacks, target)
        ) {
            if (uiModel.action == Action.More) {
                Icon(
                    imageVector = Icons.Default.MoreHoriz,
                    tint = ProtonTheme.colors.textNorm,
                    contentDescription = uiModel.contentDescription.string()
                )
            } else {
                Icon(
                    painter = painterResource(id = uiModel.icon),
                    tint = ProtonTheme.colors.textNorm,
                    contentDescription = uiModel.contentDescription.string()
                )
            }
        }
    }
}

private val FloatingToolbarHeight = 56.dp
private const val ANIMATION_DURATION = 250
