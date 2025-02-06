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

package ch.protonmail.android.maillabel.domain.usecase

import java.net.SocketTimeoutException
import java.net.UnknownHostException
import arrow.core.Either
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.model.NetworkError
import ch.protonmail.android.mailcommon.domain.usecase.ObserveUser
import ch.protonmail.android.maillabel.domain.model.isReservedSystemLabelId
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelType
import me.proton.core.user.domain.extension.hasSubscriptionForMail
import timber.log.Timber
import javax.inject.Inject

class IsLabelLimitReached @Inject constructor(
    private val getLabels: GetLabels,
    private val observeUser: ObserveUser
) {

    suspend operator fun invoke(userId: UserId, labelType: LabelType): Either<DataError, Boolean> = Either.catch {
        val hasSubscriptionForMail = observeUser(userId).filterNotNull().first().hasSubscriptionForMail()
        if (!hasSubscriptionForMail) {
            val customLabelList = getLabels(userId, labelType).getOrNull().orEmpty().filter {
                !it.labelId.isReservedSystemLabelId()
            }
            if (customLabelList.size >= FREE_USER_LABEL_LIMIT) return@catch true
        }
        return@catch false
    }.mapLeft {
        val error = when (it) {
            is UnknownHostException -> NetworkError.NoNetwork
            is SocketTimeoutException -> NetworkError.Unreachable
            else -> {
                Timber.e("Unknown error while checking labels count limit: $it")
                NetworkError.Unknown
            }
        }

        DataError.Remote.Http(error)
    }

    private companion object {

        const val FREE_USER_LABEL_LIMIT = 3
    }
}
