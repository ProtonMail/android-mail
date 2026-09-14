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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.mailspotlight.presentation.model.HowItWorksStep
import ch.protonmail.android.mailspotlight.presentation.model.SpotlightActions
import ch.protonmail.android.mailspotlight.presentation.model.contentSearchHowItWorksSteps
import ch.protonmail.android.uicomponents.BottomNavigationBarSpacer
import ch.protonmail.android.uicomponents.TopNavigationBarSpacer
import kotlinx.collections.immutable.ImmutableList

/**
 * "How content search works" on its own, outside the spotlight carousel.
 *
 * There is no page indicator here, and [onTurnOn] is expected to turn content search on directly.
 * That is the whole difference from the carousel's second page. See [LocalSpotlightCarousel].
 */
@Composable
internal fun HowContentSearchWorksScreen(
    onTurnOn: () -> Unit,
    onNotNow: () -> Unit,
    modifier: Modifier = Modifier,
    steps: ImmutableList<HowItWorksStep> = contentSearchHowItWorksSteps
) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val actions = SpotlightActions(onContinue = {}, onTurnOn = onTurnOn, onNotNow = onNotNow)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ProtonTheme.colors.backgroundNorm)
    ) {
        SpotlightGradientBackground(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            TopNavigationBarSpacer()

            HowItWorksPage(
                steps = steps,
                modifier = Modifier.weight(1f),
                inlineActions = if (isLandscape) {
                    { SpotlightBottomBar(actions = actions) }
                } else {
                    null
                }
            )
        }

        if (!isLandscape) {
            SpotlightBottomBar(actions = actions)
        }

        BottomNavigationBarSpacer()
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun HowContentSearchWorksScreenPreview() {
    ProtonTheme {
        HowContentSearchWorksScreen(
            steps = SpotlightPreviewData.previewHowItWorksSteps,
            onTurnOn = {},
            onNotNow = {}
        )
    }
}
