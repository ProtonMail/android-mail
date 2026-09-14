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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.mailcommon.presentation.model.string
import ch.protonmail.android.mailspotlight.presentation.R
import ch.protonmail.android.mailspotlight.presentation.model.HowItWorksStep
import ch.protonmail.android.mailspotlight.presentation.model.SpotlightActions
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun HowItWorksPage(
    steps: ImmutableList<HowItWorksStep>,
    modifier: Modifier = Modifier,
    inlineActions: (@Composable () -> Unit)? = null
) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (isLandscape) {
        LandscapeHowItWorksPage(steps, modifier, inlineActions)
    } else {
        PortraitHowItWorksPage(steps, modifier)
    }
}

@Composable
private fun PortraitHowItWorksPage(steps: ImmutableList<HowItWorksStep>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .padding(horizontal = ProtonDimens.Spacing.Large),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SpotlightPageHeader {
            IllustrationPlaceholder(
                modifier = Modifier
                    .height(SpotlightIllustrationHeight)
                    .weight(1f, fill = false)
            )

            Spacer(modifier = Modifier.height(ProtonDimens.Spacing.Large))

            Title()
        }

        Spacer(modifier = Modifier.height(ProtonDimens.Spacing.Huge))

        StepsCard(steps)

        Spacer(modifier = Modifier.height(ProtonDimens.Spacing.Large))
    }
}

@Composable
private fun LandscapeHowItWorksPage(
    steps: ImmutableList<HowItWorksStep>,
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
            IllustrationPlaceholder(modifier = Modifier.height(LANDSCAPE_ILLUSTRATION_HEIGHT))

            Spacer(modifier = Modifier.height(ProtonDimens.Spacing.Medium))

            Title()

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
            StepsCard(steps)
        }
    }
}

@Composable
private fun Title() {
    Text(
        text = stringResource(R.string.spotlight_screen_content_search_how_it_works_title),
        style = ProtonTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
        textAlign = TextAlign.Center
    )
}

@Composable
private fun IllustrationPlaceholder(modifier: Modifier = Modifier) {
    // Missing asset, to be included
    Spacer(modifier = modifier)
}

@Composable
private fun StepsCard(steps: ImmutableList<HowItWorksStep>, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .spotlightCardBorder(),
        shape = spotlightCardShape,
        colors = CardDefaults.cardColors(
            containerColor = ProtonTheme.colors.backgroundNorm
        )
    ) {
        steps.forEachIndexed { index, step ->
            StepRow(
                position = index + 1,
                title = step.title.string(),
                description = step.description.string()
            )
            if (index < steps.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = ProtonDimens.Spacing.Jumbo),
                    color = ProtonTheme.colors.separatorNorm
                )
            }
        }
    }
}

@Composable
private fun StepRow(
    position: Int,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(ProtonDimens.Spacing.Large),
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
            Text(
                text = "$position",
                style = ProtonTheme.typography.titleMedium,
                color = ProtonTheme.colors.textNorm,
                textAlign = TextAlign.Center
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(ProtonDimens.Spacing.Standard)) {
            Text(
                text = title,
                style = ProtonTheme.typography.titleMedium
            )
            Text(
                text = description,
                style = ProtonTheme.typography.bodyMedium,
                color = ProtonTheme.colors.textWeak
            )
        }
    }
}

private val LANDSCAPE_ILLUSTRATION_HEIGHT = 120.dp

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun HowItWorksPagePreview() {
    ProtonTheme {
        SpotlightGradientBackground {
            HowItWorksPage(steps = SpotlightPreviewData.previewHowItWorksSteps)
        }
    }
}

@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    showBackground = true,
    device = "spec:width=891dp,height=411dp,orientation=landscape"
)
@Composable
private fun HowItWorksPageLandscapePreview() {
    ProtonTheme {
        SpotlightGradientBackground {
            HowItWorksPage(
                steps = SpotlightPreviewData.previewHowItWorksSteps,
                inlineActions = { SpotlightBottomBar(actions = SpotlightActions({}, {}, {})) }
            )
        }
    }
}
