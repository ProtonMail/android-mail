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
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.mailcommon.presentation.NO_CONTENT_DESCRIPTION
import ch.protonmail.android.mailcommon.presentation.model.string
import ch.protonmail.android.mailspotlight.presentation.R
import ch.protonmail.android.mailspotlight.presentation.model.FeatureItem
import ch.protonmail.android.mailspotlight.presentation.model.SpotlightActions
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun OverviewPage(
    featureItems: ImmutableList<FeatureItem>,
    modifier: Modifier = Modifier,
    inlineActions: (@Composable () -> Unit)? = null
) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (isLandscape) {
        LandscapeOverviewPage(featureItems, modifier, inlineActions)
    } else {
        PortraitOverviewPage(featureItems, modifier)
    }
}

@Composable
private fun PortraitOverviewPage(featureItems: ImmutableList<FeatureItem>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .padding(horizontal = ProtonDimens.Spacing.Large),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SpotlightPageHeader {
            Image(
                painter = painterResource(id = R.drawable.spotlight_celebration),
                contentDescription = NO_CONTENT_DESCRIPTION,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .heightIn(max = SpotlightIllustrationHeight)
                    .weight(1f, fill = false)
            )

            Spacer(modifier = Modifier.height(ProtonDimens.Spacing.Large))

            WhatsNewPill()

            Spacer(modifier = Modifier.height(ProtonDimens.Spacing.Standard))

            Text(
                text = stringResource(R.string.spotlight_screen_content_search_title),
                style = ProtonTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(ProtonDimens.Spacing.Huge))

        FeatureCard(featureItems)

        Spacer(modifier = Modifier.height(ProtonDimens.Spacing.Large))
    }
}

@Composable
private fun LandscapeOverviewPage(
    featureItems: ImmutableList<FeatureItem>,
    modifier: Modifier = Modifier,
    inlineActions: (@Composable () -> Unit)? = null
) {
    // The card is the only half that scrolls. The illustration, the title and the actions stay put,
    // so the actions cannot be scrolled out of reach.
    Row(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .padding(horizontal = ProtonDimens.Spacing.Large)
            .padding(vertical = ProtonDimens.Spacing.Medium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = ProtonDimens.Spacing.Large),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.spotlight_celebration),
                contentDescription = NO_CONTENT_DESCRIPTION,
                contentScale = ContentScale.Fit,
                modifier = Modifier.heightIn(max = LANDSCAPE_ILLUSTRATION_MAX_HEIGHT)
            )

            Spacer(modifier = Modifier.height(ProtonDimens.Spacing.Medium))

            WhatsNewPill()

            Spacer(modifier = Modifier.height(ProtonDimens.Spacing.Small))

            Text(
                text = stringResource(R.string.spotlight_screen_content_search_title),
                style = ProtonTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center
            )

            if (inlineActions != null) {
                Spacer(modifier = Modifier.height(ProtonDimens.Spacing.Large))

                inlineActions()
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
        ) {
            FeatureCard(featureItems)
        }
    }
}

@Composable
private fun FeatureCard(featureItems: ImmutableList<FeatureItem>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .spotlightCardBorder(),
        shape = spotlightCardShape,
        colors = CardDefaults.cardColors(
            containerColor = ProtonTheme.colors.backgroundNorm
        )
    ) {
        featureItems.forEachIndexed { index, item ->
            FeatureRow(item.icon, item.title.string(), item.description.string())
            if (index < featureItems.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = ProtonDimens.Spacing.Jumbo),
                    color = ProtonTheme.colors.borderNorm
                )
            }
        }
    }
}

@Composable
private fun FeatureRow(
    @DrawableRes icon: Int,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ProtonDimens.Spacing.Large)
            .padding(top = ProtonDimens.Spacing.ExtraLarge, bottom = ProtonDimens.Spacing.Large),
        horizontalArrangement = Arrangement.spacedBy(ProtonDimens.Spacing.Large),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(ProtonDimens.IconSize.SemiLarge)
                .background(
                    color = SpotlightColors.badgeBackground,
                    shape = ProtonTheme.shapes.large
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(id = icon),
                tint = ProtonTheme.colors.iconNorm,
                contentDescription = null,
                modifier = Modifier.size(ProtonDimens.IconSize.Small)
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(ProtonDimens.Spacing.Standard)) {
            Text(
                text = title,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = ProtonTheme.typography.titleMedium
            )
            Text(
                text = description,
                style = ProtonTheme.typography.bodyMedium
            )
        }
    }
}

private val LANDSCAPE_ILLUSTRATION_MAX_HEIGHT = 120.dp

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun OverviewPagePreview() {
    ProtonTheme {
        SpotlightGradientBackground {
            OverviewPage(
                featureItems = SpotlightPreviewData.previewFeatures
            )
        }
    }
}

@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    showBackground = true,
    device = "spec:width=891dp,height=411dp,orientation=landscape"
)
@Composable
private fun OverviewPageLandscapePreview() {
    ProtonTheme {
        SpotlightGradientBackground {
            OverviewPage(
                featureItems = SpotlightPreviewData.previewFeatures,
                inlineActions = { SpotlightBottomBar(actions = SpotlightActions({}, {}, {})) }
            )
        }
    }
}
