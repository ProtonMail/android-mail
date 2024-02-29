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

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ch.protonmail.android.mailcommon.presentation.ui.MailDivider
import ch.protonmail.android.mailsettings.presentation.accountsettings.identity.model.EditAddressIdentityState

@Composable
fun EditAddressIdentityScreenList(
    modifier: Modifier = Modifier,
    displayNameState: EditAddressIdentityState.DisplayNameState,
    signatureState: EditAddressIdentityState.SignatureState,
    mobileFooterState: EditAddressIdentityState.MobileFooterState,
    actions: EditAddressIdentityScreenList.Actions
) {
    LazyColumn(modifier = modifier) {
        item { MailDivider() }

        item {
            DisplayNameSettingItem(
                uiModel = displayNameState.displayNameUiModel,
                onDisplayNameChanged = actions.onDisplayNameChanged
            )
        }

        item { MailDivider() }

        item {
            SignatureDisplaySetting(
                uiModel = signatureState.addressSignatureUiModel,
                onSignatureChanged = actions.onSignatureValueChanged,
                onToggleSwitched = actions.onSignatureToggled
            )
        }

        item { MailDivider() }

        item {
            MobileFooterDisplaySetting(
                uiModel = mobileFooterState.mobileFooterUiModel,
                onMobileFooterChanged = actions.onMobileFooterValueChanged,
                onToggleSwitched = actions.onMobileFooterToggled
            )
        }
    }
}

object EditAddressIdentityScreenList {
    data class Actions(
        val onDisplayNameChanged: (String) -> Unit,
        val onSignatureValueChanged: (String) -> Unit,
        val onSignatureToggled: (Boolean) -> Unit,
        val onMobileFooterValueChanged: (String) -> Unit,
        val onMobileFooterToggled: (Boolean) -> Unit
    )
}
