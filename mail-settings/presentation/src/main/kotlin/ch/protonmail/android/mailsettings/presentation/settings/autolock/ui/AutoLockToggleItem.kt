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

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ch.protonmail.android.mailsettings.presentation.R
import ch.protonmail.android.mailsettings.presentation.settings.autolock.model.AutoLockEnabledUiModel
import ch.protonmail.android.uicomponents.settings.SettingsToggleItem

fun LazyListScope.AutoLockEnabledItem(uiModel: AutoLockEnabledUiModel, onClick: (Boolean) -> Unit) {
    item { Divider() }
    item { AutoLockToggleItem(uiModel = uiModel, onClick) }
}

@Composable
private fun AutoLockToggleItem(uiModel: AutoLockEnabledUiModel, onClick: (Boolean) -> Unit) {
    SettingsToggleItem(
        name = stringResource(id = R.string.mail_settings_auto_lock_item_title),
        hint = stringResource(id = R.string.mail_settings_auto_lock_item_description),
        value = uiModel.enabled,
        onToggle = onClick
    )
}
