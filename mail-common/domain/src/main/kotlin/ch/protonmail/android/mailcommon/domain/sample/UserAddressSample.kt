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

package ch.protonmail.android.mailcommon.domain.sample

import me.proton.core.user.domain.entity.AddressId
import me.proton.core.user.domain.entity.AddressType
import me.proton.core.user.domain.entity.UserAddress

object UserAddressSample {

    val PrimaryAddress = build()

    val AliasAddress = build(
        addressId = AddressIdSample.Alias,
        addressType = AddressType.Alias,
        email = "alias@protonmail.ch",
        order = 1
    )

    val DisabledAddress = build(
        addressId = AddressIdSample.DisabledAddressId,
        addressType = AddressType.Alias,
        email = "disabled@protonmail.ch",
        order = 2,
        enabled = false
    )

    val ExternalAddressWithSend = build(
        addressId = AddressIdSample.ExternalAddressId,
        addressType = AddressType.External,
        email = "external@gmail.com",
        order = 2,
        canSend = true
    )

    val ExternalAddressWithoutSend = build(
        addressId = AddressIdSample.ExternalAddressId,
        addressType = AddressType.External,
        email = "external@gmail.com",
        order = 2,
        canSend = false
    )

    val PmMeAddressAlias = build(
        addressId = AddressIdSample.PmMeAlias,
        addressType = AddressType.Premium,
        email = "myaddress@pm.me",
        order = 3
    )

    fun build(
        addressId: AddressId = AddressIdSample.Primary,
        email: String = "primary-email@pm.me",
        order: Int = 0,
        enabled: Boolean = true,
        addressType: AddressType = AddressType.Original,
        canSend: Boolean = true
    ) = UserAddress(
        addressId = addressId,
        canReceive = true,
        canSend = canSend,
        displayName = "name",
        email = email,
        enabled = enabled,
        keys = emptyList(),
        type = addressType,
        order = order,
        signature = "signature",
        signedKeyList = null,
        userId = UserIdSample.Primary
    )
}
