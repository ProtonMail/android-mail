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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.mailcategory.presentation.model.CategoryItemUiModel
import ch.protonmail.android.mailcategory.presentation.sample.CategoryItemUiModelSample

@Composable
fun CategoryViewMenu(
    items: List<CategoryItemUiModel>,
    modifier: Modifier = Modifier,
    onItemClick: (CategoryItemUiModel) -> Unit
) {
    Row(
        modifier = modifier
            .padding(
                vertical = ProtonDimens.Spacing.Small,
                horizontal = ProtonDimens.Spacing.ModeratelyLarge
            ),
        horizontalArrangement = Arrangement.spacedBy(ProtonDimens.Spacing.Small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { item ->
            CategoryPill(
                item = item,
                onClick = { onItemClick(item) }
            )
        }
    }
}

@Preview
@Composable
fun CategoryViewMenuPreview() {
    ProtonTheme {
        CategoryViewMenu(
            items = CategoryItemUiModelSample.all,
            onItemClick = {}
        )
    }
}
