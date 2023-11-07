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

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import ch.protonmail.android.mailcommon.presentation.ConsumableLaunchedEffect
import ch.protonmail.android.mailcommon.presentation.ui.MailDivider
import ch.protonmail.android.mailsettings.presentation.accountsettings.defaultaddress.model.DefaultAddressUiModel
import ch.protonmail.android.mailsettings.presentation.accountsettings.defaultaddress.model.EditDefaultAddressState
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultNorm

@Composable
fun ActiveAddressesList(
    modifier: Modifier = Modifier,
    state: EditDefaultAddressState.WithData.ActiveAddressesState,
    updateErrorState: EditDefaultAddressState.WithData.UpdateErrorState,
    actions: ActiveAddressesList.Actions
) {
    var selected by remember { mutableStateOf(state.addresses.first { it.isDefault() }.addressId) }
    var previouslySelected by remember { mutableStateOf(state.addresses.first { it.isDefault() }.addressId) }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(top = ProtonDimens.SmallSpacing)
    ) {

        fun onItemSelected(item: DefaultAddressUiModel.Active) {
            if (selected == item.addressId) return
            previouslySelected = selected
            selected = item.addressId
            actions.onAddressSelected(item.addressId)
        }

        items(state.addresses) {
            Row(
                modifier = Modifier
                    .selectable(selected == it.addressId, role = Role.RadioButton) { onItemSelected(it) }
                    .padding(ProtonDimens.DefaultSpacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = selected == it.addressId, onClick = null)
                Spacer(modifier = Modifier.width(ProtonDimens.DefaultSpacing))
                Text(
                    modifier = Modifier.weight(1f, fill = true),
                    text = it.address,
                    style = ProtonTheme.typography.defaultNorm
                )
            }
            MailDivider()
            ConsumableLaunchedEffect(updateErrorState.updateError) {
                selected = previouslySelected
                actions.showGenericUpdateError()
            }
            ConsumableLaunchedEffect(effect = updateErrorState.incompatibleSubscriptionError) {
                selected = previouslySelected
                actions.showSubscriptionUpdateError()
            }
        }
    }
}

object ActiveAddressesList {

    data class Actions(
        val onAddressSelected: (String) -> Unit,
        val showGenericUpdateError: suspend () -> Unit,
        val showSubscriptionUpdateError: suspend () -> Unit
    )
}
