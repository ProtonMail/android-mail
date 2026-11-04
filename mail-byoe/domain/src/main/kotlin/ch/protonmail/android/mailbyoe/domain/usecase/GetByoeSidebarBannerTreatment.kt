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
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import javax.inject.Inject

/**
 * The variant of the temporary BYOE entry in the sidebar, or null when it should not be shown. The SDK decides;
 * ask again each time the sidebar opens.
 */
class GetByoeSidebarBannerTreatment @Inject constructor(
    private val byoeGuidanceRepository: ByoeGuidanceRepository
) {

    suspend operator fun invoke(userId: UserId): ByoeTreatment? {
        return byoeGuidanceRepository.getSidebarBannerTreatment(userId)
            .onLeft { Timber.e("byoe: unable to resolve the sidebar banner: $it") }
            .getOrNull()
            .also { Timber.d("byoe: sidebar banner treatment = $it") }
    }
}
