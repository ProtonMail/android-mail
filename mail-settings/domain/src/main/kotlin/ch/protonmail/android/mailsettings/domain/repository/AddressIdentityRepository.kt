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

package ch.protonmail.android.mailsettings.domain.repository

import arrow.core.Either
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailsettings.domain.model.DisplayName
import ch.protonmail.android.mailsettings.domain.model.Signature
import ch.protonmail.android.mailsettings.domain.model.SignaturePreference
import ch.protonmail.android.mailsettings.domain.model.SignatureValue
import me.proton.core.domain.entity.UserId
import me.proton.core.user.domain.entity.AddressId

interface AddressIdentityRepository {

    suspend fun getDisplayName(addressId: AddressId): Either<DataError, DisplayName>

    suspend fun getSignatureValue(addressId: AddressId): Either<DataError, SignatureValue>

    suspend fun getSignatureEnabledPreferenceValue(addressId: AddressId): Either<DataError, SignaturePreference>

    suspend fun updateAddressIdentity(
        userId: UserId,
        addressId: AddressId,
        displayName: DisplayName,
        signature: Signature
    ): Either<DataError, Unit>
}
