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

package ch.protonmail.android.mailcategory.presentation

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.design.compose.theme.bodyMediumWeak
import ch.protonmail.android.design.compose.theme.titleSmallNorm
import ch.protonmail.android.mailcategory.presentation.model.CategoryItemUiModel
import ch.protonmail.android.mailcategory.presentation.sample.CategoryItemUiModelSample
import ch.protonmail.android.mailcommon.presentation.ui.protonTwoLayerShadow

@Composable
fun CategorySpotlightBanner(
    category: CategoryItemUiModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(ProtonDimens.CornerRadius.Huge)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .protonTwoLayerShadow(shape = shape)
            .clip(shape)
            // Consume taps on the banner body so they don't fall through to the list item underneath.
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {}
            .background(ProtonTheme.colors.backgroundNorm)
            .border(BorderSize, ProtonTheme.colors.borderLight, shape)
            .padding(
                start = ProtonDimens.Spacing.ModeratelyLarge,
                top = ProtonDimens.Spacing.ModeratelyLarge,
                end = ProtonDimens.Spacing.Medium,
                bottom = ProtonDimens.Spacing.ModeratelyLarge
            ),
        verticalAlignment = Alignment.Top
    ) {
        SpotlightIllustration(iconRes = category.iconRes)

        Spacer(modifier = Modifier.width(ProtonDimens.Spacing.Standard))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(ProtonDimens.Spacing.Tiny)
        ) {
            Text(
                text = stringResource(
                    id = R.string.category_spotlight_unseen_title,
                    stringResource(id = category.titleRes)
                ),
                style = ProtonTheme.typography.titleSmallNorm
            )
            Text(
                text = stringResource(id = R.string.category_spotlight_unseen_description),
                style = ProtonTheme.typography.bodyMediumWeak
            )
        }

        Spacer(modifier = Modifier.width(ProtonDimens.Spacing.Large))

        Icon(
            painter = painterResource(id = R.drawable.ic_proton_cross),
            contentDescription = stringResource(id = R.string.category_spotlight_close_content_description),
            tint = ProtonTheme.colors.textWeak,
            modifier = Modifier
                .size(CloseButtonSize)
                .clickable(onClick = onClose)
                .background(Color.Transparent)
        )
    }
}

@Composable
private fun SpotlightIllustration(iconRes: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(IllustrationSize)
            .clip(RoundedCornerShape(ProtonDimens.CornerRadius.Large))
            .background(ProtonTheme.colors.borderLight),
        contentAlignment = Alignment.Center
    ) {
        CategoryIcon(
            iconRes = iconRes,
            tint = ProtonTheme.colors.iconNorm,
            showUnseenBadge = true
        )
    }
}

private val BorderSize = 1.dp
private val IllustrationSize = 36.dp
private val CloseButtonSize = 20.dp

@Preview(name = "Light", uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun CategorySpotlightBannerPreview() {
    ProtonTheme {
        CategorySpotlightBanner(
            category = CategoryItemUiModelSample.social,
            onClose = {},
            modifier = Modifier.padding(ProtonDimens.Spacing.Large)
        )
    }
}
