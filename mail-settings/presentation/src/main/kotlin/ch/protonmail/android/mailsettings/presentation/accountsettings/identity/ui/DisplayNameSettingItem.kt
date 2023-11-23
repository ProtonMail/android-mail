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
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ch.protonmail.android.mailsettings.presentation.R
import ch.protonmail.android.mailsettings.presentation.accountsettings.identity.model.DisplayNameUiModel
import ch.protonmail.android.mailsettings.presentation.accountsettings.identity.ui.DisplayNameSettingLimit.FieldLengthLimit
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultNorm

@Composable
fun DisplayNameSettingItem(
    modifier: Modifier = Modifier,
    uiModel: DisplayNameUiModel,
    onDisplayNameChanged: (String) -> Unit
) {

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .padding(
                    start = ProtonDimens.DefaultSpacing,
                    top = ProtonDimens.MediumSpacing,
                    bottom = ProtonDimens.SmallSpacing
                )
                .padding(
                    bottom = ProtonDimens.ExtraSmallSpacing
                )
        ) {
            Text(
                text = stringResource(id = R.string.mail_settings_identity_display_name),
                color = ProtonTheme.colors.textNorm,
                style = ProtonTheme.typography.defaultNorm
            )
        }

        AddressIdentityTextField(
            text = uiModel.textValue,
            placeholder = stringResource(id = R.string.mail_settings_identity_display_name_hint),
            multiLine = false,
            maxLength = FieldLengthLimit,
            onValueChanged = {
                onDisplayNameChanged(it)
            }
        )
    }
}

private object DisplayNameSettingLimit {

    const val FieldLengthLimit = 255
}
