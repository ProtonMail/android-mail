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

package ch.protonmail.android.mailsettings.domain.usecase

import java.util.LinkedList
import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.raise.either
import ch.protonmail.android.mailcommon.domain.usecase.IsPaidUser
import kotlinx.coroutines.flow.first
import me.proton.core.domain.entity.UserId
import me.proton.core.user.domain.UserAddressManager
import me.proton.core.user.domain.entity.AddressId
import me.proton.core.user.domain.entity.UserAddress
import me.proton.core.util.kotlin.takeIfNotEmpty
import javax.inject.Inject

class SetDefaultAddress @Inject constructor(
    private val userAddressManager: UserAddressManager,
    private val isPaidUser: IsPaidUser
) {

    suspend operator fun invoke(userId: UserId, addressId: AddressId): Either<Error, List<UserAddress>> = either {

        val isPaidUser = isPaidUser(userId).getOrElse { raise(Error.FailedDeterminingUserSubscription) }
        val addressesList = userAddressManager.observeAddresses(userId).first().takeIfNotEmpty()
            ?: raise(Error.AddressNotFound)
        val addressToDefault = addressesList.find { it.addressId == addressId }
            ?: raise(Error.AddressNotFound)

        if (!isPaidUser && addressToDefault.isPmAddress) {
            raise(Error.UpgradeRequired)
        }

        val updatedList = addressesList.map { address ->
            address.addressId
        }.moveAddressIdToTop(addressId.id)

        return either {
            runCatching {
                userAddressManager.updateOrder(userId, updatedList)
            }.getOrElse {
                raise(Error.UpdateFailed)
            }
        }
    }

    private fun List<AddressId>.moveAddressIdToTop(id: String): List<AddressId> {
        return LinkedList(this).apply {
            val addressId = AddressId(id)
            remove(addressId)
            addFirst(addressId)
        }.toList()
    }

    @Suppress("ClassOrdering")
    private val UserAddress.isPmAddress: Boolean
        get() = email.contains(PM_ME_SUFFIX)

    sealed interface Error {
        object FailedDeterminingUserSubscription : Error
        object AddressNotFound : Error
        object UpdateFailed : Error
        object UpgradeRequired : Error
    }

    private companion object {

        const val PM_ME_SUFFIX = "@pm.me"
    }
}
