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

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.mailcategory.presentation.design.CategoryPillColors
import ch.protonmail.android.mailcategory.presentation.model.CategoryItemUiModel
import ch.protonmail.android.mailcategory.presentation.sample.CategoryItemUiModelSample
import ch.protonmail.android.mailcommon.presentation.compose.MailDimens

@Composable
fun CategoryPill(
    item: CategoryItemUiModel,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(999.dp)

    val activeContentColor = if (isSystemInDarkTheme()) {
        CategoryPillColors.ActiveContentDark
    } else {
        CategoryPillColors.ActiveContentLight
    }

    val targetContentColor =
        if (item.isActive) activeContentColor else ProtonTheme.colors.textWeak

    val targetBackgroundColor =
        if (item.isActive) item.activeColor else ProtonTheme.colors.interactionWeakNorm

    val contentColor by animateColorAsState(
        targetValue = targetContentColor,
        label = "CategoryPillContentColor"
    )

    val backgroundColor by animateColorAsState(
        targetValue = targetBackgroundColor,
        label = "CategoryPillBackgroundColor"
    )

    Row(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .height(MailDimens.CategoryView.ItemHeight)
            .padding(
                start = ProtonDimens.Spacing.ModeratelyLarge,
                top = ProtonDimens.Spacing.MediumLight,
                end = ProtonDimens.Spacing.ModeratelyLarge,
                bottom = ProtonDimens.Spacing.MediumLight
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ProtonDimens.Spacing.Compact)
    ) {
        Icon(
            painter = painterResource(id = item.iconRes),
            contentDescription = null,
            modifier = Modifier.size(MailDimens.CategoryView.IconSize),
            tint = contentColor
        )

        Text(
            text = stringResource(id = item.titleRes),
            style = ProtonTheme.typography.titleSmall.copy(color = contentColor),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Preview
@Composable
fun ActiveCategoryPillPreview() {
    ProtonTheme {
        CategoryPill(
            item = CategoryItemUiModelSample.primary,
            onClick = {}
        )
    }
}

@Preview
@Composable
fun InactiveCategoryPillPreview() {
    ProtonTheme {
        CategoryPill(
            item = CategoryItemUiModelSample.primary.copy(isActive = false),
            onClick = {}
        )
    }
}
