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
import androidx.compose.material.ChipDefaults
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FilterChip
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.tooling.preview.Preview
import ch.protonmail.android.mailcommon.presentation.compose.MailDimens
import ch.protonmail.android.mailcommon.presentation.mapper.UnreadCountValueMapper
import ch.protonmail.android.mailmailbox.presentation.R
import ch.protonmail.android.mailmailbox.presentation.mailbox.PreviewData.DummyUnreadCount
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.UnreadFilterState
import me.proton.core.compose.component.ProtonCenteredProgress
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun UnreadItemsFilter(
    modifier: Modifier = Modifier,
    state: UnreadFilterState,
    onFilterEnabled: () -> Unit,
    onFilterDisabled: () -> Unit
) {
    when (state) {
        is UnreadFilterState.Loading -> {
            ProtonCenteredProgress(modifier = Modifier.size(MailDimens.ProgressDefaultSize))
        }

        is UnreadFilterState.Data -> {
            FilterChip(
                modifier = modifier.testTag(UnreadItemsFilterTestTags.UnreadFilterChip),
                colors = chipColors(state.isFilterEnabled),
                selected = state.isFilterEnabled,
                onClick = {
                    if (state.isFilterEnabled) {
                        onFilterDisabled()
                    } else {
                        onFilterEnabled()
                    }
                },
                trailingIcon = addCloseIconForEnabledState(state)
            ) {
                Text(
                    text = pluralStringResource(
                        id = R.plurals.filter_unread_button_text,
                        count = state.numUnread,
                        UnreadCountValueMapper.toCappedValue(state.numUnread)
                    )
                )
            }
        }
    }
}

@Composable
private fun addCloseIconForEnabledState(state: UnreadFilterState.Data): @Composable (() -> Unit)? {
    return if (state.isFilterEnabled) {
        {
            Icon(
                modifier = Modifier.size(ProtonDimens.SmallIconSize),
                imageVector = Icons.Filled.Close,
                contentDescription = null,
                tint = ProtonTheme.colors.iconInverted
            )
        }
    } else {
        null
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun chipColors(isSelected: Boolean) = if (isSelected) {
    ChipDefaults.filterChipColors(
        selectedBackgroundColor = ProtonTheme.colors.iconAccent,
        selectedContentColor = ProtonTheme.colors.textInverted
    )
} else {
    ChipDefaults.filterChipColors(
        backgroundColor = ProtonTheme.colors.backgroundSecondary,
        contentColor = ProtonTheme.colors.textAccent
    )
}

@Preview
@Composable
fun InactiveUnreadFilterButtonPreview() {
    UnreadItemsFilter(
        state = UnreadFilterState.Data(DummyUnreadCount, false),
        onFilterEnabled = {},
        onFilterDisabled = {}
    )
}

@Preview
@Composable
fun ActiveUnreadFilterButtonPreview() {
    UnreadItemsFilter(
        state = UnreadFilterState.Data(DummyUnreadCount, true),
        onFilterEnabled = {},
        onFilterDisabled = {}
    )
}

private object PreviewData {

    const val DummyUnreadCount = 4
}

object UnreadItemsFilterTestTags {

    const val UnreadFilterChip = "UnreadFilterChip"
}
