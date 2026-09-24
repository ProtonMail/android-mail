/*
 * Copyright (c) 2025 Proton Technologies AG
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

package ch.protonmail.android.mailspotlight.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import ch.protonmail.android.design.compose.theme.ProtonDimens

/**
 * The illustration, badge and title above a spotlight card.
 */
@Composable
internal fun SpotlightPageHeader(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val headerHeight = LocalConfiguration.current.screenHeightDp.dp * HEADER_HEIGHT_FRACTION

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(headerHeight),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
        content = content
    )
}

/**
 * The left half of a landscape spotlight page: the header content on top, the actions pinned below.
 *
 * The actions are measured first, so they always stay on screen and tappable. The header takes the
 * remaining height and scrolls when large font sizes make it taller than that.
 *
 * The illustration is dropped at large font scales, to leave the room to the text.
 */
@Composable
internal fun LandscapeSpotlightHeader(
    illustration: @Composable () -> Unit,
    inlineActions: (@Composable () -> Unit)?,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val showIllustration = LocalDensity.current.fontScale < LANDSCAPE_ILLUSTRATION_MAX_FONT_SCALE

    Column(
        modifier = modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (showIllustration) {
                illustration()

                Spacer(modifier = Modifier.height(ProtonDimens.Spacing.Medium))
            }

            content()
        }

        if (inlineActions != null) {
            Spacer(modifier = Modifier.height(ProtonDimens.Spacing.Large))

            inlineActions()
        }
    }
}

/**
 * The illustration height both spotlight pages share.
 */
internal val SpotlightIllustrationHeight = 144.dp

private const val HEADER_HEIGHT_FRACTION = 0.29f
private const val LANDSCAPE_ILLUSTRATION_MAX_FONT_SCALE = 1.5f
