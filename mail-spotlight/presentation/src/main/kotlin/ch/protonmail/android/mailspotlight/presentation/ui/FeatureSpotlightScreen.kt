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
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.mailspotlight.presentation.model.FeatureItem
import ch.protonmail.android.mailspotlight.presentation.model.HowItWorksStep
import ch.protonmail.android.mailspotlight.presentation.model.SpotlightActions
import ch.protonmail.android.mailspotlight.presentation.viewmodel.FeatureSpotlightViewAction
import ch.protonmail.android.mailspotlight.presentation.viewmodel.FeatureSpotlightViewModel
import ch.protonmail.android.uicomponents.BottomNavigationBarSpacer
import ch.protonmail.android.uicomponents.TopNavigationBarSpacer
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.launch

@Composable
fun FeatureSpotlightScreen(onDismiss: () -> Unit, onTurnOn: () -> Unit) {
    val viewModel = hiltViewModel<FeatureSpotlightViewModel>()

    BackHandler(enabled = true) {
        // no-op, we just need to prevent dismissal via back button
    }

    LaunchedEffect(Unit) {
        viewModel.dismissEvent.collect { action ->
            when (action) {
                FeatureSpotlightViewAction.TurnOn -> onTurnOn()
                FeatureSpotlightViewAction.NotNow -> onDismiss()
            }
        }
    }

    FeatureSpotlightScreen(
        featureItems = viewModel.overviewFeatures,
        howItWorksSteps = viewModel.howItWorksSteps,
        onTurnOn = { viewModel.submit(FeatureSpotlightViewAction.TurnOn) },
        onNotNow = { viewModel.submit(FeatureSpotlightViewAction.NotNow) }
    )
}

@Composable
internal fun FeatureSpotlightScreen(
    featureItems: ImmutableList<FeatureItem>,
    howItWorksSteps: ImmutableList<HowItWorksStep>,
    onTurnOn: () -> Unit,
    onNotNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(pageCount = { SpotlightScreenMetadata.VISIBLE_PAGE_COUNT })
    val scope = rememberCoroutineScope()
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    val bounce = spring<Float>(
        dampingRatio = SpotlightScreenMetadata.BOUNCE_DAMPING_RATIO,
        stiffness = SpotlightScreenMetadata.BOUNCE_STIFFNESS
    )

    val actions = SpotlightActions(
        onContinue = {
            scope.launch {
                pagerState.animateScrollToPage(pagerState.currentPage + 1, animationSpec = bounce)
            }
        },
        onTurnOn = onTurnOn,
        onNotNow = onNotNow
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ProtonTheme.colors.backgroundNorm)
    ) {
        CompositionLocalProvider(
            LocalSpotlightCarousel provides SpotlightCarousel(
                currentPage = pagerState.currentPage.coerceAtMost(SpotlightScreenMetadata.VISIBLE_PAGE_COUNT - 1),
                pageCount = SpotlightScreenMetadata.VISIBLE_PAGE_COUNT
            )
        ) {
            // In landscape the actions sit next to the content, as there is no room for a bottom bar.
            val inlineActions: (@Composable () -> Unit)? =
                if (isLandscape) {
                    { SpotlightBottomBar(actions = actions) }
                } else {
                    null
                }

            SpotlightGradientBackground(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                TopNavigationBarSpacer()

                HorizontalPager(
                    state = pagerState,
                    beyondViewportPageCount = pagerState.pageCount,
                    modifier = Modifier.weight(1f),
                    flingBehavior = PagerDefaults.flingBehavior(
                        state = pagerState,
                        pagerSnapDistance = PagerSnapDistance.atMost(1),
                        snapAnimationSpec = bounce
                    )
                ) { pageIndex ->
                    when (pageIndex) {
                        0 -> OverviewPage(
                            featureItems = featureItems,
                            inlineActions = inlineActions
                        )

                        else -> HowItWorksPage(
                            steps = howItWorksSteps,
                            inlineActions = inlineActions
                        )
                    }
                }
            }

            if (!isLandscape) {
                SpotlightBottomBar(actions = actions)
            }
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
            howItWorksSteps = SpotlightPreviewData.previewHowItWorksSteps,
            onTurnOn = {},
            onNotNow = {}
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
            howItWorksSteps = SpotlightPreviewData.previewHowItWorksSteps,
            onTurnOn = {},
            onNotNow = {}
        )
    }
}
