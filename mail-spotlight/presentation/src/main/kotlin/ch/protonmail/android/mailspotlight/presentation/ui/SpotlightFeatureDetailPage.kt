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
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.mailcommon.presentation.AdaptivePreviews
import ch.protonmail.android.mailcommon.presentation.NO_CONTENT_DESCRIPTION
import ch.protonmail.android.mailspotlight.presentation.R
import ch.protonmail.android.mailspotlight.presentation.model.FeatureDetailPageContent
import ch.protonmail.android.mailspotlight.presentation.model.SpotlightActions

@Composable
internal fun FeatureDetailPage(
    content: FeatureDetailPageContent,
    actions: SpotlightActions,
    modifier: Modifier = Modifier,
    isLastPage: Boolean = false,
    showDismissButton: Boolean = false
) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (isLandscape) {
        LandscapeFeatureDetailPage(content, actions, modifier, isLastPage, showDismissButton)
    } else {
        PortraitFeatureDetailPage(content, modifier)
    }
}

@Composable
private fun PortraitFeatureDetailPage(content: FeatureDetailPageContent, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .padding(horizontal = ProtonDimens.Spacing.Large),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .weight(ILLUSTRATION_WEIGHT)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = content.illustrationRes),
                contentDescription = NO_CONTENT_DESCRIPTION,
                modifier = Modifier.padding(horizontal = ProtonDimens.Spacing.ExtraLarge)
            )
        }

        Spacer(modifier = Modifier.height(ProtonDimens.Spacing.Large))

        Text(
            text = stringResource(content.titleRes),
            style = ProtonTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(ProtonDimens.Spacing.Medium))

        Text(
            text = stringResource(content.subtitleRes),
            style = ProtonTheme.typography.bodyLarge,
            color = ProtonTheme.colors.textWeak,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = ProtonDimens.Spacing.Medium)
        )

        Spacer(modifier = Modifier.weight(TEXT_BOTTOM_WEIGHT))
    }
}

@Composable
private fun LandscapeFeatureDetailPage(
    content: FeatureDetailPageContent,
    actions: SpotlightActions,
    modifier: Modifier = Modifier,
    isLastPage: Boolean = false,
    showDismissButton: Boolean = false
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .padding(horizontal = ProtonDimens.Spacing.Medium)
            .padding(vertical = ProtonDimens.Spacing.ExtraLarge),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val maxImageHeight = LocalConfiguration.current.screenHeightDp.dp * LANDSCAPE_IMAGE_MAX_HEIGHT_FRACTION

        Box(
            modifier = Modifier
                .weight(LANDSCAPE_ILLUSTRATION_WEIGHT)
                .heightIn(max = maxImageHeight),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = content.illustrationRes),
                contentDescription = NO_CONTENT_DESCRIPTION,
                contentScale = ContentScale.Fit,
                modifier = Modifier.padding(horizontal = ProtonDimens.Spacing.Small)
            )
        }

        Column(
            modifier = Modifier
                .weight(LANDSCAPE_TEXT_WEIGHT)
                .padding(start = ProtonDimens.Spacing.Medium)
        ) {
            Text(
                text = stringResource(content.titleRes),
                style = ProtonTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
            )

            Spacer(modifier = Modifier.height(ProtonDimens.Spacing.Medium))

            Text(
                text = stringResource(content.subtitleRes),
                style = ProtonTheme.typography.bodyLarge,
                color = ProtonTheme.colors.textWeak
            )

            Spacer(modifier = Modifier.height(ProtonDimens.Spacing.ExtraLarge))

            if (isLastPage) {
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(ProtonDimens.Spacing.Medium),
                    verticalArrangement = Arrangement.spacedBy(ProtonDimens.Spacing.Small)
                ) {
                    PrimaryButton(
                        text = stringResource(R.string.spotlight_screen_category_view_button_try),
                        onClick = actions.onTryCategories
                    )
                    if (showDismissButton) {
                        SecondaryButton(
                            text = stringResource(R.string.spotlight_screen_category_view_button_dismiss),
                            onClick = actions.onDismissWithoutCategories
                        )
                    }
                }
            } else {
                PrimaryButton(
                    text = stringResource(R.string.spotlight_screen_category_view_button_continue),
                    onClick = actions.onContinue,
                    modifier = Modifier.width(IntrinsicSize.Max)
                )
            }
        }
    }
}

private const val ILLUSTRATION_WEIGHT = 5f
private const val TEXT_BOTTOM_WEIGHT = 1f
private const val LANDSCAPE_ILLUSTRATION_WEIGHT = 2f
private const val LANDSCAPE_TEXT_WEIGHT = 3f
private const val LANDSCAPE_IMAGE_MAX_HEIGHT_FRACTION = 0.75f

@AdaptivePreviews
@Composable
private fun FeatureDetailPageB2CUnreadPreview() {
    ProtonTheme {
        SpotlightGradientBackground {
            FeatureDetailPage(
                content = FeatureDetailPageContent(
                    illustrationRes = R.drawable.category_view_spotlight_1,
                    titleRes = R.string.spotlight_screen_category_view_main_b2c_title,
                    subtitleRes = R.string.spotlight_screen_category_view_main_b2c_subtitle
                ),
                actions = SpotlightActions({}, {}, {})
            )
        }
    }
}

@AdaptivePreviews
@Composable
private fun FeatureDetailPageB2CCategoriesPreview() {
    ProtonTheme {
        SpotlightGradientBackground {
            FeatureDetailPage(
                content = FeatureDetailPageContent(
                    illustrationRes = R.drawable.category_view_spotlight_2,
                    titleRes = R.string.spotlight_screen_category_view_secondary_b2c_title,
                    subtitleRes = R.string.spotlight_screen_category_view_secondary_b2c_subtitle
                ),
                actions = SpotlightActions({}, {}, {})
            )
        }
    }
}

@AdaptivePreviews
@Composable
private fun FeatureDetailPageB2BUnreadPreview() {
    ProtonTheme {
        SpotlightGradientBackground {
            FeatureDetailPage(
                content = FeatureDetailPageContent(
                    illustrationRes = R.drawable.category_view_spotlight_1,
                    titleRes = R.string.spotlight_screen_category_view_main_b2b_title,
                    subtitleRes = R.string.spotlight_screen_category_view_main_b2b_subtitle
                ),
                actions = SpotlightActions({}, {}, {})
            )
        }
    }
}

@AdaptivePreviews
@Composable
private fun FeatureDetailPageB2BCategoriesPreview() {
    ProtonTheme {
        SpotlightGradientBackground {
            FeatureDetailPage(
                content = FeatureDetailPageContent(
                    illustrationRes = R.drawable.category_view_spotlight_2,
                    titleRes = R.string.spotlight_screen_category_view_secondary_b2b_title,
                    subtitleRes = R.string.spotlight_screen_category_view_secondary_b2b_subtitle
                ),
                actions = SpotlightActions({}, {}, {})
            )
        }
    }
}

@AdaptivePreviews
@Composable
private fun DetailPageLandscapeContinuePreview() {
    ProtonTheme {
        SpotlightGradientBackground {
            FeatureDetailPage(
                content = FeatureDetailPageContent(
                    illustrationRes = R.drawable.category_view_spotlight_1,
                    titleRes = R.string.spotlight_screen_category_view_main_b2c_title,
                    subtitleRes = R.string.spotlight_screen_category_view_main_b2c_subtitle
                ),
                actions = SpotlightActions({}, {}, {})
            )
        }
    }
}

@AdaptivePreviews
@Composable
private fun FeatureDetailPageLandscapeB2BLastPreview() {
    ProtonTheme {
        SpotlightGradientBackground {
            FeatureDetailPage(
                content = FeatureDetailPageContent(
                    illustrationRes = R.drawable.category_view_spotlight_2,
                    titleRes = R.string.spotlight_screen_category_view_secondary_b2b_title,
                    subtitleRes = R.string.spotlight_screen_category_view_secondary_b2b_subtitle
                ),
                actions = SpotlightActions({}, {}, {}),
                isLastPage = true,
                showDismissButton = true
            )
        }
    }
}
