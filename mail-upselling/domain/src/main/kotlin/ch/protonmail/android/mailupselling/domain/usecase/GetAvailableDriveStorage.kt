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

package ch.protonmail.android.mailupselling.domain.usecase

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUser
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetAvailableDriveStorage @Inject constructor(
    private val observePrimaryUser: ObservePrimaryUser
) {
    suspend operator fun invoke(): Either<GetAvailableDriveStorageError, AvailableDriveStorage> {
        val user = observePrimaryUser.invoke().first()
        if (user == null) return GetAvailableDriveStorageError.left()

        val maxSplitDriveSpace = user.maxDriveSpace
        val usedSplitDriveSpace = user.usedDriveSpace
        val isSplit = maxSplitDriveSpace != null && usedSplitDriveSpace != null
        val availableDriveStorageBytes = if (isSplit) {
            maxSplitDriveSpace - usedSplitDriveSpace
        } else if (user.maxSpace > 0) {
            user.maxSpace - user.usedSpace
        } else {
            null
        }
        val driveStorageGB = availableDriveStorageBytes?.takeIf { it > 0 }?.let {
            it / (1024F * 1024 * 1024)
        }

        return AvailableDriveStorage(driveStorageGB).right()
    }

    data object GetAvailableDriveStorageError
}

data class AvailableDriveStorage(val totalDriveStorageGB: Float?)
