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
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ch.protonmail.android.mailsettings.presentation.R
import ch.protonmail.android.mailsettings.presentation.accountsettings.identity.model.MobileFooterUiModel
import ch.protonmail.android.mailupselling.presentation.ui.UpsellingIcon
import ch.protonmail.android.uicomponents.settings.SettingsToggleItem
import me.proton.core.compose.theme.ProtonDimens

@Composable
fun MobileFooterDisplaySetting(
    modifier: Modifier = Modifier,
    uiModel: MobileFooterUiModel,
    onMobileFooterChanged: (String) -> Unit,
    onToggleSwitched: (Boolean) -> Unit
) {
    Column(modifier = modifier) {
        Row {
            SettingsToggleItem(
                name = stringResource(R.string.mail_settings_identity_mobile_footer),
                value = uiModel.enabled,
                isFieldEnabled = uiModel.isToggleEnabled,
                onToggle = {
                    onToggleSwitched(it)
                },
                hint = stringResource(id = R.string.mail_settings_identity_mobile_footer_subtext),
                upsellingIcon = {
                    if (uiModel.isUpsellingVisible) {
                        UpsellingIcon(modifier = Modifier.padding(horizontal = ProtonDimens.SmallSpacing))
                    }
                }
            )
        }

        if (uiModel.enabled) {
            AddressIdentityTextField(
                text = uiModel.textValue,
                enabled = uiModel.isFieldEnabled,
                placeholder = stringResource(id = R.string.mail_settings_identity_mobile_footer_hint),
                multiLine = true,
                onValueChanged = {
                    onMobileFooterChanged(it)
                }
            )
        }
    }
}
