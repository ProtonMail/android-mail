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

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ch.protonmail.android.design.compose.component.ProtonTextButton
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.mailspotlight.presentation.R
import ch.protonmail.android.mailspotlight.presentation.model.SpotlightActions
import ch.protonmail.android.mailspotlight.presentation.model.SpotlightUserType

@Composable
internal fun SpotlightBottomBar(
    pagerState: PagerState,
    userType: SpotlightUserType,
    actions: SpotlightActions,
    modifier: Modifier = Modifier
) {
    val isLastPage by remember {
        derivedStateOf { pagerState.currentPage == SpotlightScreenMetadata.VISIBLE_PAGE_COUNT - 1 }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .padding(horizontal = ProtonDimens.Spacing.Large)
            .padding(bottom = ProtonDimens.Spacing.Large),
        verticalArrangement = Arrangement.spacedBy(ProtonDimens.Spacing.Small)
    ) {
        if (isLastPage) {
            PrimaryButton(
                text = stringResource(R.string.spotlight_screen_category_view_button_try),
                onClick = actions.onTryCategories,
                modifier = Modifier.fillMaxWidth()
            )

            if (userType == SpotlightUserType.B2B) {
                SecondaryButton(
                    text = stringResource(R.string.spotlight_screen_category_view_button_dismiss),
                    onClick = actions.onDismissWithoutCategories,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            PrimaryButton(
                text = stringResource(R.string.spotlight_screen_category_view_button_continue),
                onClick = actions.onContinue,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
internal fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ProtonTextButton(
        modifier = modifier
            .background(
                color = ProtonTheme.colors.brandNorm,
                shape = ProtonTheme.shapes.massive
            ),
        onClick = onClick
    ) {
        Text(
            text = text,
            style = ProtonTheme.typography.titleMedium,
            color = ProtonTheme.colors.textInverted,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = ProtonDimens.Spacing.Large)
        )
    }
}

@Composable
internal fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ProtonTextButton(
        modifier = modifier
            .border(
                width = SECONDARY_BUTTON_BORDER_WIDTH,
                color = ProtonTheme.colors.separatorNorm,
                shape = ProtonTheme.shapes.massive
            )
            .background(
                color = ProtonTheme.colors.backgroundNorm,
                shape = ProtonTheme.shapes.massive
            ),
        onClick = onClick
    ) {
        Text(
            text = text,
            style = ProtonTheme.typography.titleMedium,
            color = ProtonTheme.colors.textNorm,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = ProtonDimens.Spacing.Large)
        )
    }
}

private val SECONDARY_BUTTON_BORDER_WIDTH = 1.dp
