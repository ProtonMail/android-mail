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
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.Label
import me.proton.core.label.domain.repository.LabelRepository
import timber.log.Timber
import javax.inject.Inject

class UpdateLabel @Inject constructor(private val labelRepository: LabelRepository) {

    suspend operator fun invoke(userId: UserId, label: Label): Either<DataError, Unit> = Either.catch {
        labelRepository.updateLabel(userId, label)
    }.mapLeft {
        val error = when (it) {
            is UnknownHostException -> NetworkError.NoNetwork
            is SocketTimeoutException -> NetworkError.Unreachable
            else -> {
                Timber.e("Unknown error while updating label: $it")
                NetworkError.Unknown
            }
        }

        DataError.Remote.Http(error)
    }
}
