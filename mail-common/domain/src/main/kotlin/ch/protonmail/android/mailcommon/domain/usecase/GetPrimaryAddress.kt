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
import ch.protonmail.android.mailcommon.domain.model.DataError
import kotlinx.coroutines.flow.first
import me.proton.core.domain.entity.UserId
import me.proton.core.user.domain.entity.UserAddress
import javax.inject.Inject

class GetPrimaryAddress @Inject constructor(
    private val observeUserAddresses: ObserveUserAddresses
) {

    suspend operator fun invoke(userId: UserId): Either<DataError, UserAddress> {
        val addresses = observeUserAddresses.invoke(userId).first()
        if (addresses.isEmpty()) {
            return DataError.Local.NoDataCached.left()
        }

        val primary = addresses.minBy { it.order }
        return primary.right()
    }

}
