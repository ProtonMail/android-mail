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
import me.proton.core.user.domain.extension.hasSubscription
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetAvailableDriveStorage @Inject constructor(
    private val observePrimaryUser: ObservePrimaryUser
) {
    suspend operator fun invoke(): Either<GetAvailableDriveStorageError, AvailableDriveStorage> {
        val user = observePrimaryUser.invoke().first()
        if (user == null) return GetAvailableDriveStorageError.left()
        if (user.hasSubscription().not()) return AvailableDriveStorage.Free.right()

        return AvailableDriveStorage.Plus.right()
    }

    data object GetAvailableDriveStorageError
}

sealed interface AvailableDriveStorage {
    object Free : AvailableDriveStorage
    object Plus : AvailableDriveStorage
    object Unlimited : AvailableDriveStorage
}
