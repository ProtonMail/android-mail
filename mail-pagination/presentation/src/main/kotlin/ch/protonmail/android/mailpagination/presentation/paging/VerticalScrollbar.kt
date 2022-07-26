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

package ch.protonmail.android.mailpagination.presentation.paging

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.proton.core.compose.theme.ProtonTheme

@Composable
fun Modifier.verticalScrollbar(
    state: LazyListState,
    width: Dp = 2.dp,
    color: Color = ProtonTheme.colors.interactionStrongNorm
): Modifier {
    val targetAlpha = if (state.isScrollInProgress) 1f else 0f
    val duration = if (state.isScrollInProgress) 150 else 500
    val alpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(durationMillis = duration)
    )
    return drawWithContent {
        drawContent()
        val firstVisibleItemIndex = state.layoutInfo.visibleItemsInfo.firstOrNull()?.index
        val isDrawScrollbarNeeded = state.isScrollInProgress || alpha > 0.0f
        if (isDrawScrollbarNeeded && firstVisibleItemIndex != null) {
            val itemHeight = this.size.height / state.layoutInfo.totalItemsCount
            val scrollbarY = firstVisibleItemIndex * itemHeight
            val scrollbarHeight = state.layoutInfo.visibleItemsInfo.size * itemHeight
            drawRect(
                color = color,
                topLeft = Offset(this.size.width - width.toPx(), scrollbarY),
                size = Size(width.toPx(), scrollbarHeight),
                alpha = alpha
            )
        }
    }
}
