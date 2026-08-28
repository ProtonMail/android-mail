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
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.mailspotlight.presentation.model.FeatureItem
import ch.protonmail.android.mailspotlight.presentation.viewmodel.FeatureSpotlightViewModel
import ch.protonmail.android.uicomponents.BottomNavigationBarSpacer
import ch.protonmail.android.uicomponents.TopNavigationBarSpacer
import kotlinx.collections.immutable.ImmutableList

@Composable
fun FeatureSpotlightScreen(onDismiss: () -> Unit) {
    val viewModel = hiltViewModel<FeatureSpotlightViewModel>()

    BackHandler(enabled = true) {
        // no-op, we just need to prevent dismissal via back button
    }

    LaunchedEffect(Unit) {
        viewModel.closeScreenEvent.collect { onDismiss() }
    }

    FeatureSpotlightScreen(
        featureItems = viewModel.overviewFeatures,
        onGotIt = viewModel::onGotIt
    )
}

@Composable
internal fun FeatureSpotlightScreen(
    featureItems: ImmutableList<FeatureItem>,
    onGotIt: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

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

            OverviewPage(
                featureItems = featureItems,
                modifier = Modifier.weight(1f),
                // In landscape the action sits next to the content, as there is no room for a bottom bar.
                onGotIt = if (isLandscape) onGotIt else null
            )
        }

        if (!isLandscape) {
            SpotlightBottomBar(onGotIt = onGotIt)
        }

        BottomNavigationBarSpacer()
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun FeatureSpotlightScreenPreview() {
    ProtonTheme {
        FeatureSpotlightScreen(
            featureItems = SpotlightPreviewData.previewFeatures,
            onGotIt = {}
        )
    }
}

@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    showBackground = true,
    device = "spec:width=891dp,height=411dp,orientation=landscape"
)
@Composable
private fun FeatureSpotlightScreenLandscapePreview() {
    ProtonTheme {
        FeatureSpotlightScreen(
            featureItems = SpotlightPreviewData.previewFeatures,
            onGotIt = {}
        )
    }
}
