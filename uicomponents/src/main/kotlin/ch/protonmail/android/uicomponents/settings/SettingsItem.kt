/*
 * Copyright (c) 2022 Proton Technologies AG
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

package ch.protonmail.android.uicomponents.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import me.proton.core.compose.component.ProtonRawListItem
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme

@Composable
fun SettingsItem(
    modifier: Modifier = Modifier,
    name: String,
    hint: String? = null,
    isClickable: Boolean = true,
    onClick: () -> Unit = {},
    upsellingIcon: @Composable () -> Unit = {}
) {
    ProtonRawListItem(
        modifier = modifier
            .clickable(isClickable, onClick = onClick)
            .padding(
                vertical = ProtonDimens.ListItemTextStartPadding,
                horizontal = ProtonDimens.DefaultSpacing
            )
    ) {
        Column(
            modifier = Modifier
                .semantics(mergeDescendants = true) {}
                .fillMaxWidth()
        ) {
            Row {
                Text(
                    modifier = Modifier,
                    text = name,
                    color = ProtonTheme.colors.textNorm,
                    style = ProtonTheme.typography.body1Regular
                )
                upsellingIcon()
            }
            hint?.let {
                Text(
                    modifier = Modifier.padding(top = ProtonDimens.ExtraSmallSpacing),
                    text = hint,
                    color = ProtonTheme.colors.textHint,
                    style = ProtonTheme.typography.body2Regular
                )
            }
        }
    }
}
