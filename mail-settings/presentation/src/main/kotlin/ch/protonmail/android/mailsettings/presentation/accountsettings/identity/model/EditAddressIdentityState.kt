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

import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailupselling.presentation.model.BottomSheetVisibilityEffect

sealed interface EditAddressIdentityState {

    data object Loading : EditAddressIdentityState
    data object LoadingError : EditAddressIdentityState

    data class DataLoaded(
        val displayNameState: DisplayNameState,
        val signatureState: SignatureState,
        val mobileFooterState: MobileFooterState,
        val updateError: Effect<Unit>,
        val close: Effect<Unit>,
        val upsellingVisibility: Effect<BottomSheetVisibilityEffect>,
        val upsellingInProgress: Effect<TextUiModel>
    ) : EditAddressIdentityState

    data class DisplayNameState(val displayNameUiModel: DisplayNameUiModel)
    data class SignatureState(val addressSignatureUiModel: AddressSignatureUiModel)
    data class MobileFooterState(val mobileFooterUiModel: MobileFooterUiModel)
}
