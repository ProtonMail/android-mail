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

package ch.protonmail.android.mailcomposer.domain.usecase

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.usecase.IsPaidMailUser
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailcommon.domain.usecase.ObserveUserAddresses
import ch.protonmail.android.mailcomposer.domain.annotation.IsExternalAddressSendingEnabled
import kotlinx.coroutines.flow.first
import me.proton.core.user.domain.entity.UserAddress
import me.proton.core.user.domain.entity.isExternal
import javax.inject.Inject

class GetComposerSenderAddresses @Inject constructor(
    private val observePrimaryUserId: ObservePrimaryUserId,
    private val isPaidUser: IsPaidMailUser,
    private val observeUserAddresses: ObserveUserAddresses,
    @IsExternalAddressSendingEnabled val externalSendingEnabled: Boolean
) {

    suspend operator fun invoke(): Either<Error, List<UserAddress>> {
        val userId = observePrimaryUserId().first() ?: return Error.FailedGettingPrimaryUser.left()

        val isPaidUser = isPaidUser.invoke(userId).getOrElse {
            return Error.FailedDeterminingUserSubscription.left()
        }

        val addresses = observeUserAddresses(userId).first()
            .filter { it.enabledAndCanSend() }

        return if (addresses.size < 2 && !isPaidUser) {
            Error.UpgradeToChangeSender.left()
        } else {
            addresses.right()
        }
    }

    private fun UserAddress.enabledAndCanSend() = enabled && canSend &&
        (externalSendingEnabled || isExternal().not())

    sealed interface Error {
        object UpgradeToChangeSender : Error
        object FailedDeterminingUserSubscription : Error
        object FailedGettingPrimaryUser : Error
    }
}
