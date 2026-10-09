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

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ch.protonmail.android.design.compose.component.ProtonWavyProgressIndicator
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.mailcontentsearch.presentation.R
import ch.protonmail.android.mailcontentsearch.presentation.model.IndexingProgressState
import ch.protonmail.android.vendored.material3.WavyProgressIndicatorDefaults

/**
 * Tells the user that this account's mail is still being indexed.
 *
 * Shown on the pages with nothing to read - the search history and "no results" - and never over a
 * list of results.
 */
@Composable
internal fun IndexingProgressBanner(
    state: IndexingProgressState.Ongoing,
    onSeeDetailsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isError = state is IndexingProgressState.Error
    val accentColor = if (isError) ProtonTheme.colors.notificationError else ProtonTheme.colors.textAccent
    val trackColor = if (isError) {
        ProtonTheme.colors.notificationError100
    } else {
        ProtonTheme.colors.interactionBrandWeakNorm
    }
    // One string per variant, rather than a label and a percentage glued together, so a translator
    // can move the two parts and pick their own separator. The spoken one drops the bullet, which
    // TalkBack would otherwise read out.
    val label = if (isError) {
        R.string.content_search_indexing_interrupted
    } else {
        R.string.content_search_indexing_progress
    }
    val spokenLabel = if (isError) {
        R.string.content_search_indexing_interrupted_content_description
    } else {
        R.string.content_search_indexing_progress_content_description
    }

    // Animated so each progress report slides the boundary along instead of snapping it.
    val progress by animateFloatAsState(
        targetValue = state.percentage / PERCENTAGE_SCALE,
        animationSpec = WavyProgressIndicatorDefaults.ProgressAnimationSpec,
        label = "indexingProgress"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = ProtonDimens.Spacing.ExtraLarge),
        horizontalAlignment = Alignment.Start
    ) {
        ProtonWavyProgressIndicator(
            progress = { progress },
            color = accentColor,
            trackColor = trackColor,
            waveSpeed = if (isError) 0.dp else WavyProgressIndicatorDefaults.LinearDeterminateWavelength,
            // The text below carries the percentage, so the bar itself is not worth announcing twice.
            modifier = Modifier
                .fillMaxWidth()
                .clearAndSetSemantics {}
        )

        Spacer(modifier = Modifier.height(ProtonDimens.Spacing.Medium))

        val spokenProgress = stringResource(spokenLabel, state.percentage)

        // The whole label opens the details, so there is no separate "See details" link.
        Text(
            text = stringResource(label, state.percentage),
            style = ProtonTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = accentColor,
            modifier = Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    role = Role.Button,
                    onClick = onSeeDetailsClick
                )
                .semantics { contentDescription = spokenProgress }
        )
    }
}

private const val PERCENTAGE_SCALE = 100f

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun IndexingProgressBannerPreview() {
    ProtonTheme {
        IndexingProgressBanner(
            state = IndexingProgressState.Visible(percentage = 30),
            onSeeDetailsClick = {}
        )
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun IndexingProgressBannerErrorPreview() {
    ProtonTheme {
        IndexingProgressBanner(
            state = IndexingProgressState.Error(percentage = 30),
            onSeeDetailsClick = {}
        )
    }
}
