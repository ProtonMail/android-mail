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

package ch.protonmail.android.mailsettings.presentation.accountsettings.defaultaddress.previewdata

import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailsettings.presentation.accountsettings.defaultaddress.model.DefaultAddressUiModel
import ch.protonmail.android.mailsettings.presentation.accountsettings.defaultaddress.model.EditDefaultAddressState
import kotlinx.collections.immutable.toImmutableList

object EditDefaultAddressPreviewData {

    val NoErrorState = EditDefaultAddressState.WithData.UpdateErrorState(Effect.empty(), Effect.empty())
    val ActiveAddressesState = EditDefaultAddressState.WithData.ActiveAddressesState(
        listOf(
            DefaultAddressUiModel.Active(true, "id1", "address1@proton.me"),
            DefaultAddressUiModel.Active(false, "id2", "address2@proton.me"),
            DefaultAddressUiModel.Active(false, "id3", "address3@proton.me")
        ).toImmutableList()
    )
    val InactiveAddressesState = EditDefaultAddressState.WithData.InactiveAddressesState(
        listOf(
            DefaultAddressUiModel.Inactive("inactive@proton.me"),
            DefaultAddressUiModel.Inactive("inactive2@proton.me")
        ).toImmutableList()
    )
    val InactiveAddressesEmptyState = EditDefaultAddressState.WithData.InactiveAddressesState(
        mutableListOf<DefaultAddressUiModel.Inactive>().toImmutableList()
    )
}
