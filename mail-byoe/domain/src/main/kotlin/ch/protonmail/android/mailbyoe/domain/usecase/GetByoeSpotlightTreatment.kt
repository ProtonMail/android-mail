/*
 * Copyright (c) 2026 Proton Technologies AG
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

package ch.protonmail.android.mailbyoe.domain.usecase

import ch.protonmail.android.mailbyoe.domain.model.ByoeTreatment
import ch.protonmail.android.mailbyoe.domain.repository.ByoeGuidanceRepository
import ch.protonmail.android.mailupselling.domain.usecase.HasInAppNotificationsEnabled
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import javax.inject.Inject

/**
 * The BYOE spotlight variant for an existing user, or null when it should not be shown. The spotlight is an
 * in-app notification, so it respects the user's preference for those.
 */
class GetByoeSpotlightTreatment @Inject constructor(
    private val byoeGuidanceRepository: ByoeGuidanceRepository,
    private val hasInAppNotificationsEnabled: HasInAppNotificationsEnabled
) {

    suspend operator fun invoke(userId: UserId): ByoeTreatment? {
        val inAppNotificationsEnabled = hasInAppNotificationsEnabled(userId).getOrNull() ?: false
        if (!inAppNotificationsEnabled) {
            Timber.d("byoe: no spotlight treatment, in-app notifications are turned off")
            return null
        }

        return byoeGuidanceRepository.getSpotlightTreatment(userId)
            .onLeft { Timber.e("byoe: unable to resolve the spotlight treatment: $it") }
            .getOrNull()
            .also { Timber.d("byoe: spotlight treatment = $it") }
    }
}
