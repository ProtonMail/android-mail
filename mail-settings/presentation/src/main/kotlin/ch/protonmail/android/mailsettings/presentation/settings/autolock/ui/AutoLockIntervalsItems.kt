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


package ch.protonmail.android.mailsettings.presentation.settings.autolock.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.Text
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import ch.protonmail.android.mailsettings.domain.model.autolock.AutoLockInsertionMode
import ch.protonmail.android.mailsettings.domain.model.autolock.AutoLockInterval
import ch.protonmail.android.mailsettings.presentation.R
import ch.protonmail.android.mailsettings.presentation.settings.autolock.model.AutoLockSettingsState
import me.proton.core.compose.component.ProtonSettingsItem
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultNorm

fun LazyListScope.AutoLockIntervalsSection(
    state: AutoLockSettingsState.DataLoaded.AutoLockIntervalState,
    onIntervalSelected: (AutoLockInterval) -> Unit,
    onPinScreenNavigation: (AutoLockInsertionMode) -> Unit,
    onIntervalItemClick: (toggleExpanded: Boolean) -> Unit
) {
    item { Divider() }
    item {
        ProtonSettingsItem(
            name = stringResource(id = R.string.mail_settings_auto_lock_change_pin_description),
            onClick = { onPinScreenNavigation(AutoLockInsertionMode.ChangePin) }
        )
    }
    item { Divider() }
    item {
        ProtonSettingsItem(
            name = stringResource(id = R.string.mail_settings_auto_lock_item_timer_title),
            hint = stringResource(id = state.autoLockIntervalsUiModel.selectedInterval.description),
            onClick = { onIntervalItemClick(!state.dropdownExpanded) }
        )
    }
    item {
        AutoLockIntervalDropDownMenu(
            state,
            onIntervalSelected = onIntervalSelected,
            onIntervalItemClick = onIntervalItemClick
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AutoLockIntervalDropDownMenu(
    state: AutoLockSettingsState.DataLoaded.AutoLockIntervalState,
    onIntervalSelected: (AutoLockInterval) -> Unit,
    onIntervalItemClick: (toggleExpanded: Boolean) -> Unit
) {
    DropdownMenu(
        modifier = Modifier.background(ProtonTheme.colors.backgroundNorm),
        properties = PopupProperties(focusable = false),
        offset = DpOffset(x = ProtonDimens.DefaultSpacing, y = 0.dp),
        expanded = state.dropdownExpanded,
        onDismissRequest = { onIntervalItemClick(false) }
    ) {
        state.autoLockIntervalsUiModel.list.forEach { item ->
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(id = item.description),
                        color = ProtonTheme.colors.textNorm,
                        style = ProtonTheme.typography.defaultNorm
                    )
                },
                onClick = { onIntervalSelected(item.autoLockInterval) },
                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
            )
        }
    }
}
