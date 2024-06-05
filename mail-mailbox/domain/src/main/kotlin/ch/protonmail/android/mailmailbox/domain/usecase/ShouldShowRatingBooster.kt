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

import ch.protonmail.android.mailcommon.domain.MailFeatureId
import ch.protonmail.android.mailcommon.domain.usecase.ObserveMailFeature
import ch.protonmail.android.mailmailbox.domain.repository.InMemoryMailboxRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

class ShouldShowRatingBooster @Inject constructor(
    private val inMemoryMailboxRepository: InMemoryMailboxRepository,
    private val observeMailFeature: ObserveMailFeature
) {

    operator fun invoke(userId: UserId): Flow<Boolean> {
        return combine(
            observeMailFeature(userId, MailFeatureId.RatingBooster),
            inMemoryMailboxRepository.observeScreenViewCount()
        ) { ratingBoosterFeatureFlag, screenViewCount ->
            ratingBoosterFeatureFlag.value && screenViewCount >= SCREEN_VIEW_COUNT_THRESHOLD
        }
    }

    companion object {
        const val SCREEN_VIEW_COUNT_THRESHOLD = 2
    }
}
