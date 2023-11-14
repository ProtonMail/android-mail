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

package ch.protonmail.android.mailsettings.presentation.accountsettings.defaultaddress.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailsettings.domain.usecase.SetDefaultAddress
import ch.protonmail.android.mailsettings.presentation.accountsettings.defaultaddress.model.EditDefaultAddressEvent
import ch.protonmail.android.mailsettings.presentation.accountsettings.defaultaddress.model.EditDefaultAddressEvent.Data
import ch.protonmail.android.mailsettings.presentation.accountsettings.defaultaddress.model.EditDefaultAddressEvent.Error
import ch.protonmail.android.mailsettings.presentation.accountsettings.defaultaddress.model.EditDefaultAddressState
import ch.protonmail.android.mailsettings.presentation.accountsettings.defaultaddress.reducer.EditDefaultAddressReducer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import me.proton.core.domain.entity.UserId
import me.proton.core.user.domain.UserAddressManager
import me.proton.core.user.domain.entity.AddressId
import javax.inject.Inject

@HiltViewModel
class EditDefaultAddressViewModel @Inject constructor(
    private val observePrimaryUserId: ObservePrimaryUserId,
    private val userAddressManager: UserAddressManager,
    private val setDefaultEmailAddress: SetDefaultAddress,
    private val reducer: EditDefaultAddressReducer
) : ViewModel() {

    private val mutableState =
        MutableStateFlow<EditDefaultAddressState>(EditDefaultAddressState.Loading)
    val state = mutableState.asStateFlow()
    private var updateJob: Job? = null

    init {
        observePrimaryUserId().mapLatest { userId ->
            userId ?: return@mapLatest emitNewStateFrom(Error.LoadingError)

            val userAddresses = userAddressManager.observeAddresses(userId).first()
            if (userAddresses.isEmpty()) return@mapLatest emitNewStateFrom(Error.LoadingError)

            emitNewStateFrom(Data.ContentLoaded(userAddresses))
        }.launchIn(viewModelScope)
    }

    fun setPrimaryAddress(addressId: String) {
        updateJob?.cancel()
        updateJob = viewModelScope.launch {
            val userId = observePrimaryUserId().first()
                ?: return@launch emitNewStateFrom(Error.Update.Generic)

            emitNewStateFrom(EditDefaultAddressEvent.Update(addressId))

            val previouslySelectedItem = getCurrentDefaultAddressId(userId).also {
                if (it == addressId) return@launch
            } ?: return@launch emitNewStateFrom(Error.Update.Generic)

            setDefaultEmailAddress(userId, AddressId(addressId))
                .onLeft {
                    // Do not propagate errors if the job has been cancelled.
                    if (!isActive) return@launch

                    val event = when (it) {
                        is SetDefaultAddress.Error.UpgradeRequired ->
                            Error.Update.Revertable.UpgradeRequired(previouslySelectedItem)

                        else -> Error.Update.Revertable.Generic(previouslySelectedItem)
                    }

                    emitNewStateFrom(event)
                }
                .onRight { emitNewStateFrom(Data.ContentUpdated(it)) }
        }
    }

    private suspend fun getCurrentDefaultAddressId(userId: UserId): String? =
        userAddressManager.observeAddresses(userId).first().find { it.order == 1 }?.addressId?.id

    private fun emitNewStateFrom(event: EditDefaultAddressEvent) = mutableState.update {
        reducer.newStateFrom(it, event)
    }
}
