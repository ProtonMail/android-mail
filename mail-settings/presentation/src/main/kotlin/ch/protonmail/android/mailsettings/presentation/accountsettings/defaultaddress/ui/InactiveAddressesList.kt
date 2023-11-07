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

package ch.protonmail.android.mailsettings.presentation.accountsettings.defaultaddress.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ch.protonmail.android.mailsettings.presentation.R
import ch.protonmail.android.mailsettings.presentation.accountsettings.defaultaddress.model.DefaultAddressUiModel
import ch.protonmail.android.mailsettings.presentation.accountsettings.defaultaddress.model.EditDefaultAddressState
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultHint
import me.proton.core.compose.theme.defaultNorm

@Composable
fun InactiveAddressesList(
    modifier: Modifier = Modifier,
    state: EditDefaultAddressState.WithData.InactiveAddressesState
) {
    LazyColumn(modifier = modifier) {
        if (state.addresses.isNotEmpty()) {
            items(state.addresses) {
                InactiveAddressesListItem(it)
            }
        } else {
            item { NoInactiveAddressesItem() }
        }
    }
}

@Composable
fun InactiveAddressesListItem(item: DefaultAddressUiModel.Inactive) {
    Row(
        modifier = Modifier.padding(ProtonDimens.DefaultSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier.weight(1f, fill = true),
            text = item.address,
            style = ProtonTheme.typography.defaultHint
        )
    }
    Divider()
}

@Composable
fun NoInactiveAddressesItem(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(ProtonDimens.DefaultSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier.weight(1f, fill = true),
            text = stringResource(id = R.string.mail_settings_default_email_address_no_inactive_addresses),
            style = ProtonTheme.typography.defaultNorm
        )
    }
}
