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

package ch.protonmail.android.mailcontentsearch.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import ch.protonmail.android.design.compose.modifier.drawHalo
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme

/**
 * The card the search screen's one-off messages sit in: outlined, on an even halo, over the page.
 */
@Composable
internal fun ContentSearchBannerCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = ProtonTheme.shapes.huge

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = ProtonDimens.Spacing.ModeratelyLarge)
            .drawHalo(
                color = ProtonTheme.colors.shadowWeak,
                blurRadiusPx = with(LocalDensity.current) { ShadowBlur.toPx() },
                shape = shape
            )
            .border(
                width = ProtonDimens.BorderSize.Default,
                color = ProtonTheme.colors.borderNorm,
                shape = shape
            )
            .background(color = ProtonTheme.colors.backgroundNorm, shape = shape)
            .padding(ProtonDimens.Spacing.ModeratelyLarge),
        content = content
    )
}

// A halo, not an elevation shadow: the design asks for 12dp of blur spreading evenly in every
// direction, which is what `box-shadow: 0 0 12px` does and an elevation shadow does not.
private val ShadowBlur = 12.dp
