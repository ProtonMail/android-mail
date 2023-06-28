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

package ch.protonmail.android.mailcomposer.presentation.usecase

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.usecase.IsPaidUser
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailcomposer.domain.usecase.ObserveUserAddresses
import kotlinx.coroutines.flow.first
import me.proton.core.user.domain.entity.UserAddress
import me.proton.core.user.domain.entity.isExternal
import javax.inject.Inject

class GetChangeSenderAddresses @Inject constructor(
    private val observePrimaryUserId: ObservePrimaryUserId,
    private val isPaidUser: IsPaidUser,
    private val observeUserAddresses: ObserveUserAddresses
) {

    suspend operator fun invoke(): Either<Error, List<UserAddress>> {
        val userId = observePrimaryUserId().first() ?: return Error.FailedGettingPrimaryUser.left()

        return isPaidUser(userId).fold(
            ifLeft = { Error.FailedDeterminingUserSubscription.left() },
            ifRight = { isPaid ->
                if (!isPaid) {
                    return Error.UpgradeToChangeSender.left()
                }

                observeUserAddresses(userId).first()
                    .filter { it.enabled }
                    .filterNot { it.isExternal() }
                    .right()
            }
        )
    }

    sealed interface Error {
        object UpgradeToChangeSender : Error
        object FailedDeterminingUserSubscription : Error
        object FailedGettingPrimaryUser : Error
    }
}
