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

package ch.protonmail.android.mailsettings.data.repository

import arrow.core.Either
import arrow.core.raise.either
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailsettings.data.repository.local.AddressIdentityLocalDataSource
import ch.protonmail.android.mailsettings.data.repository.remote.AddressIdentityRemoteDataSource
import ch.protonmail.android.mailsettings.domain.model.DisplayName
import ch.protonmail.android.mailsettings.domain.model.Signature
import ch.protonmail.android.mailsettings.domain.model.SignaturePreference
import ch.protonmail.android.mailsettings.domain.model.SignatureValue
import ch.protonmail.android.mailsettings.domain.repository.AddressIdentityRepository
import kotlinx.coroutines.flow.firstOrNull
import me.proton.core.domain.entity.UserId
import me.proton.core.user.domain.entity.AddressId
import javax.inject.Inject

class AddressIdentityRepositoryImpl @Inject constructor(
    private val addressIdentityLocalDataSource: AddressIdentityLocalDataSource,
    private val addressIdentityRemoteDataSource: AddressIdentityRemoteDataSource
) : AddressIdentityRepository {

    override suspend fun getDisplayName(addressId: AddressId): Either<DataError, DisplayName> = either {
        addressIdentityLocalDataSource.observeDisplayName(addressId).firstOrNull()
            ?.getOrNull()
            ?: raise(DataError.Local.NoDataCached)
    }

    override suspend fun getSignatureValue(addressId: AddressId): Either<DataError, SignatureValue> = either {
        addressIdentityLocalDataSource.observeSignatureValue(addressId).firstOrNull()
            ?.getOrNull()
            ?: raise(DataError.Local.NoDataCached)
    }

    override suspend fun updateAddressIdentity(
        userId: UserId,
        addressId: AddressId,
        displayName: DisplayName,
        signature: Signature
    ): Either<DataError, Unit> = either {
        addressIdentityLocalDataSource.updateSignatureEnabledState(addressId, signature.enabled).onLeft {
            raise(DataError.Local.Unknown)
        }

        addressIdentityLocalDataSource.updateAddressIdentity(addressId, displayName, signature).onLeft {
            raise(DataError.Local.Unknown)
        }.onRight {
            addressIdentityRemoteDataSource.updateAddressIdentity(userId, addressId, displayName, signature.value)
        }
    }

    override suspend fun getSignatureEnabled(addressId: AddressId): Either<DataError, SignaturePreference> = either {
        addressIdentityLocalDataSource.observeSignaturePreference(addressId)
            .firstOrNull()
            ?.getOrNull()
            ?: raise(DataError.Local.NoDataCached)
    }
}
