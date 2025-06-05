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

package ch.protonmail.android.mailsettings.presentation.accountsettings.defaultaddress.reducer

import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailsettings.presentation.accountsettings.defaultaddress.mapper.EditDefaultAddressUiMapper
import ch.protonmail.android.mailsettings.presentation.accountsettings.defaultaddress.model.DefaultAddressUiModel
import ch.protonmail.android.mailsettings.presentation.accountsettings.defaultaddress.model.EditDefaultAddressEvent
import ch.protonmail.android.mailsettings.presentation.accountsettings.defaultaddress.model.EditDefaultAddressOperation
import ch.protonmail.android.mailsettings.presentation.accountsettings.defaultaddress.model.EditDefaultAddressState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import me.proton.core.user.domain.entity.UserAddress
import me.proton.core.user.domain.entity.isInternal
import javax.inject.Inject

class EditDefaultAddressReducer @Inject constructor(
    private val mapper: EditDefaultAddressUiMapper
) {

    fun newStateFrom(currentState: EditDefaultAddressState, operation: EditDefaultAddressOperation) = when (operation) {
        is EditDefaultAddressEvent -> currentState.toNewStateFromEvent(operation)
    }

    private fun EditDefaultAddressState.toNewStateFromEvent(event: EditDefaultAddressEvent): EditDefaultAddressState {

        return when (this) {
            is EditDefaultAddressState.WithData -> when (event) {
                is EditDefaultAddressEvent.Data.ContentLoaded -> this
                is EditDefaultAddressEvent.Data.ContentUpdated -> event.toDataState()
                is EditDefaultAddressEvent.Update -> updateSelectedAddress(event.newAddressId)

                is EditDefaultAddressEvent.Error.Update.Revertable.UpgradeRequired ->
                    revertDefaultAddressSelection(
                        addressId = event.previouslySelectedAddressId,
                        subscriptionError = Effect.of(Unit)
                    )

                is EditDefaultAddressEvent.Error.Update.Revertable.Generic -> revertDefaultAddressSelection(
                    addressId = event.previouslySelectedAddressId,
                    error = Effect.of(Unit)
                )

                is EditDefaultAddressEvent.Error -> copy(
                    updateErrorState = EditDefaultAddressState.WithData.UpdateErrorState(
                        updateError = Effect.of(Unit),
                        incompatibleSubscriptionError = Effect.empty()
                    ),
                    showOverlayLoader = false
                )
            }

            is EditDefaultAddressState.Loading -> when (event) {
                is EditDefaultAddressEvent.Data.ContentLoaded -> event.toDataState()
                is EditDefaultAddressEvent.Data.ContentUpdated -> event.toDataState()
                is EditDefaultAddressEvent.Error -> EditDefaultAddressState.LoadingError
                else -> this
            }

            else -> this
        }
    }

    private fun EditDefaultAddressState.WithData.updateSelectedAddress(
        addressId: String
    ): EditDefaultAddressState.WithData {
        val addresses = getActiveAddressesListWithNewDefault(addressId)

        return copy(
            activeAddressesState = EditDefaultAddressState.WithData.ActiveAddressesState(addresses = addresses),
            showOverlayLoader = true
        )
    }

    private fun EditDefaultAddressState.WithData.revertDefaultAddressSelection(
        addressId: String,
        error: Effect<Unit> = Effect.empty(),
        subscriptionError: Effect<Unit> = Effect.empty()
    ): EditDefaultAddressState.WithData {
        val addresses = getActiveAddressesListWithNewDefault(addressId)

        return copy(
            activeAddressesState = EditDefaultAddressState.WithData.ActiveAddressesState(addresses = addresses),
            updateErrorState = EditDefaultAddressState.WithData.UpdateErrorState(error, subscriptionError),
            showOverlayLoader = false
        )
    }

    private fun EditDefaultAddressState.WithData.getActiveAddressesListWithNewDefault(
        addressId: String
    ): ImmutableList<DefaultAddressUiModel.Active> {
        return activeAddressesState.addresses.map {
            it.copy(isDefault = it.addressId == addressId)
        }.toImmutableList()
    }

    private fun EditDefaultAddressEvent.Data.toDataState(): EditDefaultAddressState.WithData {
        val (activeAddresses, inactiveAddresses) = addresses.splitAddresses()
        val activeUiAddresses = mapper.toActiveAddressUiModel(activeAddresses)
        val inactiveUiAddresses = mapper.toInactiveAddressUiModel(inactiveAddresses)

        return EditDefaultAddressState.WithData(
            activeAddressesState = EditDefaultAddressState.WithData.ActiveAddressesState(activeUiAddresses),
            inactiveAddressesState = EditDefaultAddressState.WithData.InactiveAddressesState(inactiveUiAddresses),
            updateErrorState = EditDefaultAddressState.WithData.UpdateErrorState(Effect.empty(), Effect.empty()),
            showOverlayLoader = false
        )
    }

    private fun List<UserAddress>.splitAddresses(): Pair<List<UserAddress>, List<UserAddress>> =
        filter { it.isInternal() }.partition { it.enabled }
}
