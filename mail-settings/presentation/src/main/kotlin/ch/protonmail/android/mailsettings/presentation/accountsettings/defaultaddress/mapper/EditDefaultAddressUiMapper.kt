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

package ch.protonmail.android.mailsettings.presentation.accountsettings.defaultaddress.mapper

import ch.protonmail.android.mailsettings.presentation.accountsettings.defaultaddress.model.DefaultAddressUiModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import me.proton.core.domain.arch.Mapper
import me.proton.core.user.domain.entity.UserAddress
import javax.inject.Inject

class EditDefaultAddressUiMapper @Inject constructor() : Mapper<UserAddress, DefaultAddressUiModel> {

    fun toActiveAddressUiModel(addresses: List<UserAddress>): ImmutableList<DefaultAddressUiModel.Active> {
        return addresses.map {
            DefaultAddressUiModel.Active(
                isDefault = it.order == 1,
                addressId = it.addressId.id,
                address = it.email
            )
        }.toImmutableList()
    }

    fun toInactiveAddressUiModel(addresses: List<UserAddress>): ImmutableList<DefaultAddressUiModel.Inactive> {
        return addresses.map {
            DefaultAddressUiModel.Inactive(address = it.email)
        }.toImmutableList()
    }
}
