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

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.design.compose.theme.titleMediumNorm
import ch.protonmail.android.mailcategory.presentation.design.activeCategoryColor
import ch.protonmail.android.mailcommon.presentation.model.CappedNumberUiModel
import ch.protonmail.android.mailcommon.presentation.model.asDisplayText
import ch.protonmail.android.mailcommon.presentation.model.isEmpty
import ch.protonmail.android.mailcommon.presentation.ui.protonFloatingButtonShadow
import ch.protonmail.android.maillabel.domain.model.CategorySystemLabelId
import ch.protonmail.android.mailmailbox.presentation.R
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.UnreadFilterState

@Composable
internal fun BottomUnreadFilterButton(
    state: UnreadFilterState,
    onFilterEnabled: () -> Unit,
    onFilterDisabled: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (state !is UnreadFilterState.Data) return

    val isActive = state.isFilterEnabled
    val activeCategoryColor = state.activeCategory?.activeCategoryColor()
    val activeBackgroundColor = activeCategoryColor ?: ProtonTheme.colors.brandNorm
    val inactiveBackgroundColor = ProtonTheme.colors.interactionFabNorm

    val backgroundColor by animateColorAsState(
        targetValue = if (isActive) activeBackgroundColor else inactiveBackgroundColor,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "unreadBg"
    )

    val contentColor = if (isActive) Color.White else ProtonTheme.colors.textNorm
    val shouldShowUnreadCount = !isActive && !state.unreadCount.isEmpty()
    val shouldShowCategoryUnreadCircle = shouldShowUnreadCount && activeCategoryColor != null

    Surface(
        modifier = modifier
            .height(UnreadHeight)
            .protonFloatingButtonShadow(),
        shape = RoundedCornerShape(percent = 50),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier
                .clickable {
                    if (isActive) onFilterDisabled() else onFilterEnabled()
                }
                .padding(
                    start = ProtonDimens.Spacing.ModeratelyLarge,
                    end = ProtonDimens.Spacing.ModeratelyLarge
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.filter_unread_button_text),
                style = ProtonTheme.typography.titleMediumNorm,
                color = contentColor
            )

            // Count collapses towards its trailing edge as the Close icon expands from its
            // leading edge, so the two swap in place instead of the pill shrinking then growing.
            AnimatedVisibility(
                visible = shouldShowUnreadCount && !shouldShowCategoryUnreadCircle,
                enter = pillItemEnter(Alignment.End),
                exit = pillItemExit(Alignment.End)
            ) {
                AnimatedContent(
                    targetState = state.unreadCount.asDisplayText(),
                    transitionSpec = { unreadCountTransition() },
                    contentAlignment = Alignment.Center,
                    label = "unreadCountInline"
                ) { countText ->
                    Text(
                        text = " $countText",
                        style = ProtonTheme.typography.titleMediumNorm,
                        color = contentColor
                    )
                }
            }

            AnimatedVisibility(
                visible = shouldShowCategoryUnreadCircle,
                enter = pillItemEnter(Alignment.End),
                exit = pillItemExit(Alignment.End)
            ) {
                Surface(
                    modifier = Modifier
                        .padding(start = ProtonDimens.Spacing.Standard)
                        .height(CategoryUnreadBadgeHeight)
                        .defaultMinSize(minWidth = CategoryUnreadBadgeMinWidth),
                    shape = ProtonTheme.shapes.massive,
                    color = activeCategoryColor ?: ProtonTheme.colors.interactionWeakNorm
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = ProtonDimens.Spacing.Standard)
                    ) {
                        AnimatedContent(
                            targetState = state.unreadCount.asDisplayText(),
                            transitionSpec = { unreadCountTransition() },
                            contentAlignment = Alignment.Center,
                            label = "unreadCountBadge"
                        ) { countText ->
                            Text(
                                text = countText,
                                style = ProtonTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color.White
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = isActive,
                enter = pillItemEnter(Alignment.Start),
                exit = pillItemExit(Alignment.Start)
            ) {
                Icon(
                    modifier = Modifier
                        .padding(start = ProtonDimens.Spacing.Small)
                        .size(ProtonDimens.IconSize.Medium),
                    imageVector = Icons.Filled.Close,
                    contentDescription = null,
                    tint = contentColor
                )
            }
        }
    }
}

private fun unreadCountTransition() = ContentTransform(
    targetContentEnter = fadeIn(animationSpec = tween(durationMillis = PILL_ANIMATION_DURATION_MS)),
    initialContentExit = fadeOut(animationSpec = tween(durationMillis = PILL_ANIMATION_DURATION_MS)),
    // Grow/shrink the badge in lock-step with the fade (same tween) and centred, so the reveal
    // clip only ever falls on faint, fading text instead of cutting off the visible number.
    sizeTransform = SizeTransform { _, _ -> tween(durationMillis = PILL_ANIMATION_DURATION_MS) }
)

private fun pillItemEnter(expandFrom: Alignment.Horizontal) =
    fadeIn(animationSpec = tween(durationMillis = PILL_ANIMATION_DURATION_MS)) +
        expandHorizontally(
            animationSpec = tween(durationMillis = PILL_ANIMATION_DURATION_MS),
            expandFrom = expandFrom
        )

private fun pillItemExit(shrinkTowards: Alignment.Horizontal) =
    fadeOut(animationSpec = tween(durationMillis = PILL_ANIMATION_DURATION_MS)) +
        shrinkHorizontally(
            animationSpec = tween(durationMillis = PILL_ANIMATION_DURATION_MS),
            shrinkTowards = shrinkTowards
        )

private const val PILL_ANIMATION_DURATION_MS = 200
private val UnreadHeight = 56.dp
private val CategoryUnreadBadgeHeight = 30.dp
private val CategoryUnreadBadgeMinWidth = 30.dp

@Preview(showBackground = true)
@Composable
@Suppress("MagicNumber")
private fun BottomUnreadFilterButtonPreviewVariants() {
    Column(
        verticalArrangement = Arrangement.spacedBy(ProtonDimens.Spacing.Small),
        modifier = Modifier.padding(ProtonDimens.Spacing.Small)
    ) {
        BottomUnreadFilterButton(
            state = UnreadFilterState.Data(unreadCount = CappedNumberUiModel.Exact(12), isFilterEnabled = false),
            onFilterEnabled = {},
            onFilterDisabled = {}
        )

        BottomUnreadFilterButton(
            state = UnreadFilterState.Data(unreadCount = CappedNumberUiModel.Exact(12), isFilterEnabled = true),
            onFilterEnabled = {},
            onFilterDisabled = {}
        )

        BottomUnreadFilterButton(
            state = UnreadFilterState.Data(
                unreadCount = CappedNumberUiModel.Exact(7),
                isFilterEnabled = false,
                activeCategory = CategorySystemLabelId.Primary
            ),
            onFilterEnabled = {},
            onFilterDisabled = {}
        )

        BottomUnreadFilterButton(
            state = UnreadFilterState.Data(
                unreadCount = CappedNumberUiModel.Capped(999),
                isFilterEnabled = false,
                activeCategory = CategorySystemLabelId.Primary
            ),
            onFilterEnabled = {},
            onFilterDisabled = {}
        )
    }
}
