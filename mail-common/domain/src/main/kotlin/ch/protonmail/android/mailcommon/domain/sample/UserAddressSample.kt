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
import me.proton.core.user.domain.entity.UserAddress

object UserAddressSample {

    val PrimaryAddress = build()

    val AliasAddress = build(
        addressId = AddressIdSample.Alias,
        email = "alias@protonmail.ch",
        order = 1
    )

    fun build(
        addressId: AddressId = AddressIdSample.Primary,
        email: String = "primary-email@pm.me",
        order: Int = 0
    ) = UserAddress(
        addressId = addressId,
        canReceive = true,
        canSend = true,
        displayName = "name",
        email = email,
        enabled = true,
        keys = emptyList(),
        order = order,
        signature = "signature",
        signedKeyList = null,
        userId = UserIdSample.Primary
    )
}
