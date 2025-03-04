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

package ch.protonmail.android.mailmailbox.domain.usecase

import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUser
import ch.protonmail.android.mailmailbox.domain.model.UserAccountStorageStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.mapLatest
import javax.inject.Inject

class ObservePrimaryUserAccountStorageStatus @Inject constructor(
    private val observePrimaryUser: ObservePrimaryUser
) {

    operator fun invoke(): Flow<UserAccountStorageStatus> = observePrimaryUser()
        .filterNotNull()
        .mapLatest { user ->
            if (user.usedBaseSpace != null && user.maxBaseSpace != null) {
                UserAccountStorageStatus(user.usedBaseSpace!!, user.maxBaseSpace!!)
            } else {
                UserAccountStorageStatus(user.usedSpace, user.maxSpace)
            }
        }
        .distinctUntilChanged()
}
