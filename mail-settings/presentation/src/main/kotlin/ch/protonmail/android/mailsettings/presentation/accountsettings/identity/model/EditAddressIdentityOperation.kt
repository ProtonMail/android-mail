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

package ch.protonmail.android.mailsettings.presentation.accountsettings.identity.model

import ch.protonmail.android.mailsettings.domain.model.DisplayName
import ch.protonmail.android.mailsettings.domain.model.MobileFooter
import ch.protonmail.android.mailsettings.domain.model.Signature

sealed interface EditAddressIdentityOperation

sealed interface EditAddressIdentityViewAction : EditAddressIdentityOperation {

    object Save : EditAddressIdentityViewAction

    sealed interface DisplayName : EditAddressIdentityViewAction {
        data class UpdateValue(val newValue: String) : EditAddressIdentityViewAction
    }

    sealed interface Signature : EditAddressIdentityViewAction {
        data class ToggleState(val enabled: Boolean) : Signature
        data class UpdateValue(val newValue: String) : Signature
    }

    sealed interface MobileFooter : EditAddressIdentityViewAction {
        data class ToggleState(val enabled: Boolean) : MobileFooter
        data class UpdateValue(val newValue: String) : MobileFooter
    }
}

sealed interface EditAddressIdentityEvent : EditAddressIdentityOperation {

    sealed interface Error : EditAddressIdentityEvent {
        object LoadingError : Error
        object UpdateError : Error
    }

    sealed interface Navigation : EditAddressIdentityEvent {
        object Close : Navigation
    }

    sealed class Data : EditAddressIdentityEvent {
        data class ContentLoaded(
            val displayName: DisplayName,
            val signature: Signature,
            val mobileFooter: MobileFooter
        ) : Data()
    }
}
