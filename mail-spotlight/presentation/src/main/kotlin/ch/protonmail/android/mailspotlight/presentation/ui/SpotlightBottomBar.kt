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

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ch.protonmail.android.design.compose.component.ProtonSecondaryButton
import ch.protonmail.android.design.compose.component.ProtonSolidButton
import ch.protonmail.android.design.compose.component.protonSecondaryButtonColors
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.mailspotlight.presentation.R
import ch.protonmail.android.mailspotlight.presentation.model.SpotlightActions

/**
 * The page indicator and the actions of the current page.
 */
@Composable
internal fun SpotlightBottomBar(actions: SpotlightActions, modifier: Modifier = Modifier) {
    val carousel = LocalSpotlightCarousel.current
    val isLastPage = carousel == null || carousel.currentPage == carousel.pageCount - 1
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    Column(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .padding(horizontal = ProtonDimens.Spacing.Large)
            .padding(bottom = ProtonDimens.Spacing.Large),
        verticalArrangement = Arrangement.spacedBy(ProtonDimens.Spacing.Medium)
    ) {
        if (carousel != null) {
            SpotlightPageIndicator(
                currentPage = carousel.currentPage,
                pageCount = carousel.pageCount,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = ProtonDimens.Spacing.Small)
            )
        }

        if (isLastPage) {
            LastPageActions(actions = actions, isLandscape = isLandscape)
        } else {
            PrimaryButton(
                text = stringResource(R.string.spotlight_screen_content_search_button_continue),
                onClick = actions.onContinue,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun LastPageActions(actions: SpotlightActions, isLandscape: Boolean) {
    val turnOn: @Composable (Modifier) -> Unit = { buttonModifier ->
        PrimaryButton(
            text = stringResource(R.string.spotlight_screen_content_search_button_turn_on),
            onClick = actions.onTurnOn,
            modifier = buttonModifier
        )
    }
    val notNow: @Composable (Modifier) -> Unit = { buttonModifier ->
        SecondaryButton(
            text = stringResource(R.string.spotlight_screen_content_search_button_not_now),
            onClick = actions.onNotNow,
            modifier = buttonModifier
        )
    }

    if (isLandscape) {
        Row(horizontalArrangement = Arrangement.spacedBy(ProtonDimens.Spacing.Medium)) {
            notNow(Modifier.weight(1f))
            turnOn(Modifier.weight(1f))
        }
    } else {
        turnOn(Modifier.fillMaxWidth())
        notNow(Modifier.fillMaxWidth())
    }
}

@Composable
internal fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ProtonSolidButton(
        modifier = modifier.heightIn(min = ButtonMinHeight),
        onClick = onClick,
        shape = ProtonTheme.shapes.massive
    ) {
        ButtonLabel(text)
    }
}

@Composable
internal fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ProtonSecondaryButton(
        modifier = modifier.heightIn(min = ButtonMinHeight),
        onClick = onClick,
        shape = ProtonTheme.shapes.massive,
        colors = ButtonDefaults.protonSecondaryButtonColors(
            backgroundColor = ProtonTheme.colors.interactionBrandWeakNorm,
            contentColor = ProtonTheme.colors.textAccent
        )
    ) {
        ButtonLabel(text)
    }
}

@Composable
private fun ButtonLabel(text: String) {
    Text(
        text = text,
        style = ProtonTheme.typography.titleMedium,
        textAlign = TextAlign.Center,
        maxLines = 1
    )
}

private val ButtonMinHeight = 56.dp

@Preview(showBackground = true)
@Composable
private fun SpotlightBottomBarFirstPagePreview() {
    ProtonTheme {
        CompositionLocalProvider(
            LocalSpotlightCarousel provides SpotlightCarousel(currentPage = 0, pageCount = 2)
        ) {
            SpotlightBottomBar(actions = SpotlightActions({}, {}, {}))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SpotlightBottomBarLastPagePreview() {
    ProtonTheme {
        CompositionLocalProvider(
            LocalSpotlightCarousel provides SpotlightCarousel(currentPage = 1, pageCount = 2)
        ) {
            SpotlightBottomBar(actions = SpotlightActions({}, {}, {}))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SpotlightBottomBarStandalonePreview() {
    ProtonTheme {
        SpotlightBottomBar(actions = SpotlightActions({}, {}, {}))
    }
}
