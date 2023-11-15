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

package ch.protonmail.android.mailcommon.domain.usecase

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import kotlinx.coroutines.flow.first
import me.proton.core.domain.entity.UserId
import me.proton.core.user.domain.entity.AddressId
import me.proton.core.user.domain.entity.UserAddress
import timber.log.Timber
import javax.inject.Inject

class ResolveUserAddress @Inject constructor(
    private val observeUserAddresses: ObserveUserAddresses
) {

    suspend operator fun invoke(userId: UserId, email: String): Either<Error, UserAddress> {
        val userAddress = observeUserAddresses(userId).first().find { it.email == email }
        if (userAddress == null) {
            Timber.e("Could not resolve user address for email: $email")
            return Error.UserAddressNotFound.left()
        }

        return userAddress.right()
    }

    suspend operator fun invoke(userId: UserId, addressId: AddressId): Either<Error, UserAddress> {
        val userAddress = observeUserAddresses(userId).first().find { it.addressId == addressId }
        if (userAddress == null) {
            Timber.e("Could not resolve user address for address ID: ${addressId.id}")
            return Error.UserAddressNotFound.left()
        }

        return userAddress.right()
    }

    sealed interface Error {
        object UserAddressNotFound : Error
    }
}
