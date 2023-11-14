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

package ch.protonmail.android.mailsettings.presentation.accountsettings.defaultaddress.model

import me.proton.core.user.domain.entity.UserAddress

sealed interface EditDefaultAddressOperation

sealed interface EditDefaultAddressEvent : EditDefaultAddressOperation {

    sealed interface Error : EditDefaultAddressEvent {

        object LoadingError : Error

        sealed interface Update : Error {

            object Generic :
                Update

            sealed class Revertable(val previouslySelectedAddressId: String) : Update {

                class Generic(previouslySelectedAddressId: String) : Revertable(previouslySelectedAddressId)
                class UpgradeRequired(previouslySelectedAddressId: String) : Revertable(previouslySelectedAddressId)
            }
        }
    }

    data class Update(val newAddressId: String) : EditDefaultAddressEvent

    sealed class Data(val addresses: List<UserAddress>) : EditDefaultAddressEvent {

        data class ContentLoaded(val loadedAddresses: List<UserAddress>) : Data(loadedAddresses)
        data class ContentUpdated(val updatedAddresses: List<UserAddress>) : Data(updatedAddresses)
    }
}
