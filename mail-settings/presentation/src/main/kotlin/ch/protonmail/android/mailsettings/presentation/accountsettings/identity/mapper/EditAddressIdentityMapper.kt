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

package ch.protonmail.android.mailsettings.presentation.accountsettings.identity.mapper

import ch.protonmail.android.mailsettings.domain.model.DisplayName
import ch.protonmail.android.mailsettings.domain.model.MobileFooter
import ch.protonmail.android.mailsettings.domain.model.Signature
import ch.protonmail.android.mailsettings.presentation.accountsettings.identity.model.AddressSignatureUiModel
import ch.protonmail.android.mailsettings.presentation.accountsettings.identity.model.DisplayNameUiModel
import ch.protonmail.android.mailsettings.presentation.accountsettings.identity.model.MobileFooterUiModel
import javax.inject.Inject

class EditAddressIdentityMapper @Inject constructor() {

    fun toDisplayNameUiModel(displayName: DisplayName) = DisplayNameUiModel(textValue = displayName.value)

    fun toSignatureUiModel(signature: Signature) =
        AddressSignatureUiModel(textValue = signature.value.text, enabled = signature.enabled)

    fun toMobileFooterUiModel(mobileFooter: MobileFooter, isUpgradePending: Boolean = false): MobileFooterUiModel =
        MobileFooterUiModel(
            textValue = mobileFooter.value,
            enabled = mobileFooter.enabled,
            isFieldEnabled = mobileFooter.editable && !isUpgradePending,
            isToggleEnabled = mobileFooter.toggleable,
            isUpsellingVisible = !mobileFooter.editable && mobileFooter.toggleable
        )
}
