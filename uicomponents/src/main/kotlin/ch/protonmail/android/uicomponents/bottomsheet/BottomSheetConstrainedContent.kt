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

package ch.protonmail.android.uicomponents.bottomsheet

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Sets the height threshold of the inner bottom sheet content.
 *
 * @param heightThreshold the height threshold.
 */
@Composable
fun bottomSheetHeightConstrainedContent(
    heightThreshold: Float = DefaultBottomSheetThreshold,
    content: @Composable () -> Unit
): @Composable ColumnScope.() -> Unit = {
    BoxWithConstraints {
        Box(Modifier.heightIn(max = maxHeight * heightThreshold)) {
            content()
        }
    }
}

@Suppress("TopLevelPropertyNaming")
private const val DefaultBottomSheetThreshold = 0.9f
