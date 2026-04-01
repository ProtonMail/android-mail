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
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.mailspotlight.presentation.R
import ch.protonmail.android.mailspotlight.presentation.model.AppVersionUiModel
import ch.protonmail.android.mailspotlight.presentation.model.FeatureDetailPageContent
import ch.protonmail.android.mailspotlight.presentation.model.FeatureItem
import ch.protonmail.android.mailspotlight.presentation.model.SpotlightActions
import ch.protonmail.android.mailspotlight.presentation.model.SpotlightUserType
import ch.protonmail.android.mailspotlight.presentation.viewmodel.FeatureSpotlightViewModel
import ch.protonmail.android.uicomponents.BottomNavigationBarSpacer
import ch.protonmail.android.uicomponents.TopNavigationBarSpacer
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.launch

@Composable
fun FeatureSpotlightScreen(onDismiss: () -> Unit) {
    val viewModel = hiltViewModel<FeatureSpotlightViewModel>()

    LaunchedEffect(Unit) {
        viewModel.closeScreenEvent.collect { onDismiss() }
    }

    FeatureSpotlightScreen(
        appVersionUiModel = viewModel.appVersion,
        featureItems = viewModel.overviewFeatures,
        userType = viewModel.userType,
        onTryCategories = viewModel::onTryCategories,
        onDismissWithoutCategories = viewModel::onDismissWithoutCategories
    )
}

@Composable
internal fun FeatureSpotlightScreen(
    appVersionUiModel: AppVersionUiModel,
    featureItems: ImmutableList<FeatureItem>,
    userType: SpotlightUserType,
    onTryCategories: () -> Unit,
    onDismissWithoutCategories: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(pageCount = { SpotlightScreenMetadata.TOTAL_PAGE_COUNT })
    val scope = rememberCoroutineScope()
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    val onContinue: () -> Unit = {
        scope.launch {
            pagerState.animateScrollToPage(
                page = pagerState.currentPage + 1,
                animationSpec = spring(
                    dampingRatio = SpotlightScreenMetadata.BOUNCE_DAMPING_RATIO,
                    stiffness = SpotlightScreenMetadata.BOUNCE_STIFFNESS
                )
            )
        }
    }

    val detailPageActions = SpotlightActions(
        onContinue = onContinue,
        onTryCategories = onTryCategories,
        onDismissWithoutCategories = onDismissWithoutCategories
    )

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .collect { page ->
                if (page == SpotlightScreenMetadata.VISIBLE_PAGE_COUNT) {
                    onTryCategories()
                }
            }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ProtonTheme.colors.backgroundNorm)
    ) {
        TopNavigationBarSpacer()

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            SpotlightGradientBackground(
                modifier = Modifier.fillMaxSize()
            ) {
                HorizontalPager(
                    state = pagerState,
                    beyondViewportPageCount = pagerState.pageCount,
                    modifier = if (isLandscape) Modifier.fillMaxSize() else Modifier.weight(1f),
                    userScrollEnabled = pagerState.currentPage < SpotlightScreenMetadata.VISIBLE_PAGE_COUNT,
                    flingBehavior = PagerDefaults.flingBehavior(
                        state = pagerState,
                        pagerSnapDistance = PagerSnapDistance.atMost(1),
                        snapAnimationSpec = spring(
                            dampingRatio = SpotlightScreenMetadata.BOUNCE_DAMPING_RATIO,
                            stiffness = SpotlightScreenMetadata.BOUNCE_STIFFNESS
                        )
                    )
                ) { pageIndex ->
                    when (pageIndex) {
                        0 -> OverviewPage(
                            appVersionUiModel = appVersionUiModel,
                            featureItems = featureItems,
                            onContinue = if (isLandscape) onContinue else null
                        )

                        1 -> {
                            val content = detailPageContent(userType, isLastPage = false)
                            FeatureDetailPage(
                                content = content,
                                actions = detailPageActions
                            )
                        }

                        2 -> {
                            val content = detailPageContent(userType, isLastPage = true)
                            FeatureDetailPage(
                                content = content,
                                actions = detailPageActions,
                                isLastPage = true,
                                showDismissButton = userType == SpotlightUserType.B2B
                            )
                        }

                        else -> Spacer(modifier = Modifier.fillMaxSize())
                    }
                }

                if (!isLandscape) {
                    val currentPage = pagerState.currentPage
                        .coerceAtMost(SpotlightScreenMetadata.VISIBLE_PAGE_COUNT - 1)
                    SpotlightPageIndicator(
                        currentPage = currentPage,
                        pageCount = SpotlightScreenMetadata.VISIBLE_PAGE_COUNT,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = ProtonDimens.Spacing.Large)
                    )
                }
            }

            if (isLandscape) {
                SpotlightPageIndicator(
                    currentPage = pagerState.currentPage.coerceAtMost(SpotlightScreenMetadata.VISIBLE_PAGE_COUNT - 1),
                    pageCount = SpotlightScreenMetadata.VISIBLE_PAGE_COUNT,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(bottom = ProtonDimens.Spacing.Medium)
                )
            }
        }

        if (!isLandscape) {
            SpotlightBottomBar(
                pagerState = pagerState,
                userType = userType,
                actions = SpotlightActions(
                    onContinue = onContinue,
                    onTryCategories = onTryCategories,
                    onDismissWithoutCategories = onDismissWithoutCategories
                )
            )
        }

        BottomNavigationBarSpacer()
    }
}

private fun detailPageContent(userType: SpotlightUserType, isLastPage: Boolean): FeatureDetailPageContent =
    if (isLastPage) {
        FeatureDetailPageContent(
            illustrationRes = R.drawable.category_view_spotlight_2,
            titleRes = when (userType) {
                SpotlightUserType.B2C -> R.string.spotlight_screen_category_view_secondary_b2c_title
                SpotlightUserType.B2B -> R.string.spotlight_screen_category_view_secondary_b2b_title
            },
            subtitleRes = when (userType) {
                SpotlightUserType.B2C -> R.string.spotlight_screen_category_view_secondary_b2c_subtitle
                SpotlightUserType.B2B -> R.string.spotlight_screen_category_view_secondary_b2b_subtitle
            }
        )
    } else {
        FeatureDetailPageContent(
            illustrationRes = R.drawable.category_view_spotlight_1,
            titleRes = when (userType) {
                SpotlightUserType.B2C -> R.string.spotlight_screen_category_view_main_b2c_title
                SpotlightUserType.B2B -> R.string.spotlight_screen_category_view_main_b2b_title
            },
            subtitleRes = when (userType) {
                SpotlightUserType.B2C -> R.string.spotlight_screen_category_view_main_b2c_subtitle
                SpotlightUserType.B2B -> R.string.spotlight_screen_category_view_main_b2b_subtitle
            }
        )
    }

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun FeatureSpotlightScreenB2CPreview() {
    ProtonTheme {
        FeatureSpotlightScreen(
            appVersionUiModel = SpotlightPreviewData.previewAppVersion,
            featureItems = SpotlightPreviewData.previewFeatures,
            userType = SpotlightUserType.B2C,
            onTryCategories = {},
            onDismissWithoutCategories = {}
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Composable
private fun FeatureSpotlightScreenB2BPreview() {
    ProtonTheme {
        FeatureSpotlightScreen(
            appVersionUiModel = SpotlightPreviewData.previewAppVersion,
            featureItems = SpotlightPreviewData.previewFeatures,
            userType = SpotlightUserType.B2B,
            onTryCategories = {},
            onDismissWithoutCategories = {}
        )
    }
}
