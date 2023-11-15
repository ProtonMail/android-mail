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

package ch.protonmail.android.mailsettings.presentation.accountsettings.identity.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import ch.protonmail.android.mailsettings.presentation.R
import ch.protonmail.android.mailsettings.presentation.accountsettings.identity.model.MobileFooterUiModel

@Composable
fun MobileFooterDisplaySetting(
    uiModel: MobileFooterUiModel,
    onMobileFooterChanged: (String) -> Unit,
    onToggleSwitched: (Boolean) -> Unit
) {
    var text by rememberSaveable { mutableStateOf(uiModel.textValue) }
    var toggled by rememberSaveable { mutableStateOf(uiModel.enabled) }
    val isFieldEnabled by remember { mutableStateOf(uiModel.isFieldEnabled) }

    Column {
        Row {
            AddressIdentitySettingToggleItem(
                name = stringResource(R.string.mail_settings_identity_mobile_footer),
                value = toggled,
                isFieldEnabled = isFieldEnabled,
                onToggle = {
                    toggled = it
                    onToggleSwitched(it)
                },
                hint = stringResource(id = R.string.mail_settings_identity_mobile_footer_subtext)
            )
        }

        if (toggled) {
            AddressIdentityTextField(
                text,
                placeholder = stringResource(id = R.string.mail_settings_identity_mobile_footer_hint),
                enabled = isFieldEnabled,
                onValueChanged = {
                    text = it
                    onMobileFooterChanged(it)
                }
            )
        }
    }
}
