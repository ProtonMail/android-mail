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
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.usecase.GetPrimaryAddress
import ch.protonmail.android.mailsettings.domain.model.Signature
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

class GetPrimaryAddressSignature @Inject constructor(
    private val getPrimaryAddress: GetPrimaryAddress,
    private val getAddressSignature: GetAddressSignature
) {

    suspend operator fun invoke(userId: UserId): Either<DataError, Signature> = either {
        val address = getPrimaryAddress(userId).getOrElse {
            raise(DataError.AddressNotFound)
        }

        getAddressSignature(userId, address.email).bind()
    }
}
