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

package ch.protonmail.android.mailsettings.presentation.accountsettings.identity.previewdata

import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailsettings.presentation.accountsettings.identity.model.AddressSignatureUiModel
import ch.protonmail.android.mailsettings.presentation.accountsettings.identity.model.DisplayNameUiModel
import ch.protonmail.android.mailsettings.presentation.accountsettings.identity.model.EditAddressIdentityState
import ch.protonmail.android.mailsettings.presentation.accountsettings.identity.model.MobileFooterUiModel
import ch.protonmail.android.mailsettings.presentation.accountsettings.identity.ui.EditAddressIdentityScreenList

internal object EditAddressIdentityScreenPreviewData {

    val state = EditAddressIdentityState.DataLoaded(
        displayNameState = EditAddressIdentityState.DisplayNameState(DisplayNameUiModel("Display name")),
        signatureState = EditAddressIdentityState.SignatureState(
            AddressSignatureUiModel("", enabled = true)
        ),
        mobileFooterState = EditAddressIdentityState.MobileFooterState(
            MobileFooterUiModel(
                "Sent with Proton Mail for Android",
                enabled = true,
                isFieldEnabled = true,
                isToggleEnabled = true,
                isUpsellingVisible = true
            )
        ),
        updateError = Effect.empty(),
        close = Effect.empty(),
        upsellingVisibility = Effect.empty(),
        upsellingInProgress = Effect.empty()
    )

    val listActions = EditAddressIdentityScreenList.Actions(
        onDisplayNameChanged = {},
        onSignatureValueChanged = {},
        onSignatureToggled = {},
        onMobileFooterValueChanged = {},
        onMobileFooterToggled = {}
    )
}
