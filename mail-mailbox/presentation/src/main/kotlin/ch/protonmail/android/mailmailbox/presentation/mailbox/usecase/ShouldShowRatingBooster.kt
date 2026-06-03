/*
 * Copyright (c) 2025 Proton Technologies AG
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
package ch.protonmail.android.mailmailbox.presentation.mailbox.usecase

import ch.protonmail.android.mailfeatureflags.domain.annotation.IsRateOnUpsellEnabled
import ch.protonmail.android.mailfeatureflags.domain.annotation.IsShowRatingBoosterEnabled
import ch.protonmail.android.mailfeatureflags.domain.model.FeatureFlag
import ch.protonmail.android.mailmailbox.domain.repository.InMemoryMailboxRepository
import ch.protonmail.android.mailupselling.domain.repository.UpsellRatingTriggerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

class ShouldShowRatingBooster @Inject constructor(
    private val inMemoryMailboxRepository: InMemoryMailboxRepository,
    private val upsellRatingTriggerRepository: UpsellRatingTriggerRepository,
    @IsShowRatingBoosterEnabled val showRatingBooster: FeatureFlag<Boolean>,
    @IsRateOnUpsellEnabled val rateOnUpsell: FeatureFlag<Boolean>
) {

    operator fun invoke(userId: UserId): Flow<Boolean> {
        val screenCountPath = inMemoryMailboxRepository.observeScreenViewCount().map { screenViewCount ->
            showRatingBooster.get() && screenViewCount == SCREEN_VIEW_COUNT_THRESHOLD
        }.distinctUntilChanged()

        val upsellPath = upsellRatingTriggerRepository.observeUpsellSuccess()
            .map { rateOnUpsell.get() }
            .filter { it }

        return merge(screenCountPath, upsellPath)
    }

    companion object {

        const val SCREEN_VIEW_COUNT_THRESHOLD = 2
    }
}
