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

package ch.protonmail.android.mailcategory.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.mailcategory.presentation.model.CategoryItemUiModel
import ch.protonmail.android.mailcategory.presentation.sample.CategoryItemUiModelSample
import ch.protonmail.android.mailcommon.presentation.ConsumableLaunchedEffect
import ch.protonmail.android.mailcommon.presentation.Effect

@Composable
fun CategoryViewMenu(
    items: List<CategoryItemUiModel>,
    modifier: Modifier = Modifier,
    lazyListState: LazyListState = rememberLazyListState(),
    resetScrollEffect: Effect<Unit> = Effect.empty(),
    onItemClick: (CategoryItemUiModel) -> Unit
) {
    val orientation = LocalConfiguration.current.orientation
    val activeIndex = items.indexOfFirst { it.isActive }

    var lastHandledOrientation by rememberSaveable { mutableIntStateOf(orientation) }

    // Extend the system-gesture exclusion beyond the category row
    val gestureExclusionPaddingPx = with(LocalDensity.current) { GestureExclusionVerticalPadding.toPx() }

    ConsumableLaunchedEffect(effect = resetScrollEffect) {
        lazyListState.animateScrollToItem(0)
    }

    LaunchedEffect(orientation) {
        if (orientation != lastHandledOrientation) {
            lastHandledOrientation = orientation

            if (activeIndex >= 0) {
                lazyListState.scrollToItem(activeIndex)
            }
        }
    }

    LazyRow(
        state = lazyListState,
        modifier = modifier
            .fillMaxWidth()
            .systemGestureExclusion { coordinates ->
                Rect(
                    left = 0f,
                    top = -gestureExclusionPaddingPx,
                    right = coordinates.size.width.toFloat(),
                    bottom = coordinates.size.height + gestureExclusionPaddingPx
                )
            },
        contentPadding = PaddingValues(horizontal = ProtonDimens.Spacing.ModeratelyLarge),
        horizontalArrangement = Arrangement.spacedBy(ProtonDimens.Spacing.Small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(items) { item ->
            CategoryPill(item = item, onClick = { onItemClick(item) })
        }
    }
}

// Vertical margin added above and below the category row to the gesture-exclusion area, so a swipe
// slightly outside the bar is still treated as a scroll rather than a back gesture.
// (Note: Platform gives 200dp-per-edge exclusion budget)
private val GestureExclusionVerticalPadding = 24.dp

@Preview
@Composable
fun CategoryViewMenuPreview() {
    ProtonTheme {
        CategoryViewMenu(
            items = CategoryItemUiModelSample.all,
            onItemClick = {}
        )
    }
}
