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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import ch.protonmail.android.uicomponents.thenIf
import me.proton.core.compose.component.ProtonRawListItem
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultNorm
import me.proton.core.compose.theme.defaultSmallUnspecified
import me.proton.core.compose.theme.textNorm

@Composable
fun SettingsToggleItem(
    modifier: Modifier = Modifier,
    name: String,
    value: Boolean,
    hint: String? = null,
    isFieldEnabled: Boolean = true,
    upsellingIcon: @Composable () -> Unit = {},
    onToggle: (Boolean) -> Unit = {}
) {
    Column(
        modifier = modifier
            .thenIf(isFieldEnabled) {
                toggleable(
                    value = value,
                    enabled = isFieldEnabled,
                    role = Role.Switch
                ) { onToggle(!value) }
            }
            .padding(horizontal = ProtonDimens.DefaultSpacing),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        ProtonRawListItem(
            modifier = Modifier
                .sizeIn(minHeight = ProtonDimens.ListItemHeight)
                .padding(vertical = ProtonDimens.DefaultSpacing),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    modifier = Modifier.weight(weight = 1f, fill = false),
                    color = ProtonTheme.colors.textNorm(),
                    style = ProtonTheme.typography.defaultNorm
                )
                upsellingIcon()
            }
            Switch(
                checked = value,
                onCheckedChange = null,
                enabled = isFieldEnabled
            )
        }
        hint?.let {
            Text(
                modifier = Modifier.offset(y = toggleItemNegativeOffset),
                text = hint,
                color = ProtonTheme.colors.textHint,
                style = ProtonTheme.typography.defaultSmallUnspecified
            )
        }
    }
}

private val toggleItemNegativeOffset = (-10).dp
