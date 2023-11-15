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

package ch.protonmail.android.mailsettings.domain.usecase.identity

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.raise.either
import ch.protonmail.android.mailcommon.domain.usecase.GetPrimaryAddress
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailsettings.domain.model.DisplayName
import ch.protonmail.android.mailsettings.domain.model.Signature
import ch.protonmail.android.mailsettings.domain.repository.AddressIdentityRepository
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

class UpdatePrimaryAddressIdentity @Inject constructor(
    private val observePrimaryUserId: ObservePrimaryUserId,
    private val getPrimaryAddress: GetPrimaryAddress,
    private val addressIdentityRepository: AddressIdentityRepository
) {

    suspend operator fun invoke(displayName: DisplayName, signatureValue: Signature): Either<Error, Unit> = either {
        val userId = observePrimaryUserId().firstOrNull() ?: raise(Error.UserIdNotFound)
        val addressId = getPrimaryAddress(userId).getOrElse { raise(Error.PrimaryAddressNotFound) }

        addressIdentityRepository.updateAddressIdentity(userId, addressId.addressId, displayName, signatureValue)
            .onLeft { raise(Error.UpdateFailure) }
    }

    sealed interface Error {
        object UserIdNotFound : Error
        object PrimaryAddressNotFound : Error
        object UpdateFailure : Error
    }
}
