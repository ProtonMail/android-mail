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

package ch.protonmail.android.mailmailbox.presentation.mailbox

import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import ch.protonmail.android.mailmailbox.presentation.R
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.design.compose.theme.bodyMediumWeak

@Composable
fun SelectDeselectAllButton(
    modifier: Modifier = Modifier,
    areAllItemsSelected: Boolean,
    actions: SelectDeselectAllButton.Actions
) {

    val buttonText = if (areAllItemsSelected) {
        stringResource(R.string.deselect_all)
    } else {
        stringResource(R.string.select_all)
    }

    FilterChip(
        modifier = modifier.testTag(SelectDeselectAllButtonTestTags.SelectDeselectAllChip),
        colors = chipColors().copy(
            containerColor = ProtonTheme.colors.backgroundNorm,
            selectedContainerColor = ProtonTheme.colors.interactionWeakPressed
        ),
        shape = ProtonTheme.shapes.huge,
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = false,
            borderWidth = ProtonDimens.OutlinedBorderSize,
            borderColor = ProtonTheme.colors.borderNorm
        ),
        selected = false,
        onClick = {
            if (areAllItemsSelected) {
                actions.onDeselectAllClicked()
            } else {
                actions.onSelectAllClicked()
            }
        },
        leadingIcon = {
            Icon(
                modifier = Modifier.size(ProtonDimens.IconSize.Small),
                painter = painterResource(R.drawable.ic_proton_checkmark_circle),
                contentDescription = null,
                tint = ProtonTheme.colors.iconNorm
            )
        },
        label = {
            Text(
                text = buttonText,
                style = ProtonTheme.typography.bodyMediumWeak
            )
        }
    )
}

@Composable
private fun chipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = ProtonTheme.colors.backgroundNorm,
    selectedLabelColor = ProtonTheme.colors.textWeak,
    containerColor = ProtonTheme.colors.backgroundNorm,
    labelColor = ProtonTheme.colors.textWeak
)

object SelectDeselectAllButton {
    data class Actions(
        val onSelectAllClicked: () -> Unit,
        val onDeselectAllClicked: () -> Unit
    )
}

object SelectDeselectAllButtonTestTags {

    const val SelectDeselectAllChip = "SelectDeselectAllChip"
}

@Preview
@Composable
fun SelectAllPreview() {
    SelectDeselectAllButton(
        areAllItemsSelected = false,
        actions = SelectDeselectAllButton.Actions(
            onSelectAllClicked = {},
            onDeselectAllClicked = {}
        )
    )
}

@Preview
@Composable
fun DeselectAllPreview() {
    SelectDeselectAllButton(
        areAllItemsSelected = true,
        actions = SelectDeselectAllButton.Actions(
            onSelectAllClicked = {},
            onDeselectAllClicked = {}
        )
    )
}
