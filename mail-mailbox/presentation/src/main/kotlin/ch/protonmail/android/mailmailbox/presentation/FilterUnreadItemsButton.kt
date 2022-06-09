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

package ch.protonmail.android.mailmailbox.presentation

import androidx.compose.material.ChipDefaults
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FilterChip
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import ch.protonmail.android.mailmailbox.presentation.model.FilterUnreadState
import me.proton.core.compose.component.ProtonCenteredProgress
import me.proton.core.compose.theme.ProtonTheme

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun FilterUnreadItemsButton(
    modifier: Modifier = Modifier,
    state: FilterUnreadState,
    onFilterEnabled: () -> Unit,
    onFilterDisabled: () -> Unit
) {
    when (state) {
        is FilterUnreadState.Loading -> ProtonCenteredProgress(modifier)
        is FilterUnreadState.Data -> {
            FilterChip(
                modifier = modifier,
                colors = chipColors(),
                selected = state.isFilterEnabled,
                onClick = {
                    if (state.isFilterEnabled) {
                        onFilterDisabled()
                    } else {
                        onFilterEnabled()
                    }
                },
                selectedIcon = { Icon(imageVector = Icons.Filled.Close, contentDescription = null) }
            ) {
                Text(
                    text = stringResource(id = R.string.filter_unread_button_text, state.numUnread)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun chipColors() = ChipDefaults.filterChipColors(
    backgroundColor = ProtonTheme.colors.interactionWeakNorm,
    selectedBackgroundColor = ProtonTheme.colors.interactionNorm,
    contentColor = ProtonTheme.colors.interactionNorm,
    selectedContentColor = ProtonTheme.colors.textInverted
)

@Preview
@Composable
fun InactiveUnreadFilterButtonPreview() {
    FilterUnreadItemsButton(
        state = FilterUnreadState.Data(4, false),
        onFilterEnabled = {},
        onFilterDisabled = {}
    )
}

@Preview
@Composable
fun ActiveUnreadFilterButtonPreview() {
    FilterUnreadItemsButton(
        state = FilterUnreadState.Data(4, true),
        onFilterEnabled = {},
        onFilterDisabled = {}
    )
}
