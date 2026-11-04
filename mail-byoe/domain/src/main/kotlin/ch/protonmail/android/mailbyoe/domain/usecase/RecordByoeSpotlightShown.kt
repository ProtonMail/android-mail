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

import ch.protonmail.android.mailbyoe.domain.repository.ByoeGuidanceRepository
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import javax.inject.Inject

/**
 * Records that the BYOE spotlight appeared. It never shows again on this install, and the SDK starts the
 * sidebar entry's window from it.
 */
class RecordByoeSpotlightShown @Inject constructor(
    private val byoeGuidanceRepository: ByoeGuidanceRepository
) {

    suspend operator fun invoke(userId: UserId) {
        byoeGuidanceRepository.markSpotlightShown(userId)
            .onLeft { Timber.e("byoe: unable to record the spotlight as shown: $it") }
            .onRight { Timber.d("byoe: spotlight recorded as shown") }
    }
}
