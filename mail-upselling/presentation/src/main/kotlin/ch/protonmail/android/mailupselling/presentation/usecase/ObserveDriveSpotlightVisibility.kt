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

package ch.protonmail.android.mailupselling.presentation.usecase

import arrow.core.getOrElse
import ch.protonmail.android.mailupselling.domain.annotations.DriveSpotlightEnabled
import ch.protonmail.android.mailupselling.domain.repository.DriveSpotlightVisibilityRepository
import ch.protonmail.android.mailupselling.domain.usecase.GetAccountAgeInDays
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapLatest
import me.proton.core.user.domain.entity.User
import javax.inject.Inject

class ObserveDriveSpotlightVisibility @Inject constructor(
    private val repo: DriveSpotlightVisibilityRepository,
    private val getAccountAgeInDays: GetAccountAgeInDays,
    @DriveSpotlightEnabled private val driveSpotlightEnabled: Boolean
) {

    operator fun invoke(user: User): Flow<Boolean> {
        if (driveSpotlightEnabled.not()) return flowOf(false)
        return repo.observe().mapLatest {
            val lastSeen = it.getOrElse { null }?.seenTimestamp
            lastSeen == null && user.isAbove30DaysOld()
        }
    }

    private fun User.isAbove30DaysOld(): Boolean = getAccountAgeInDays
        .invoke(this)
        .days >= ACCOUNT_AGE_DAYS_THRESHOLD
}

private const val ACCOUNT_AGE_DAYS_THRESHOLD = 30
