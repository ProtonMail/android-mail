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

package ch.protonmail.android.mailmailbox.presentation.mailbox.usecase

import ch.protonmail.android.mailfeatureflags.domain.annotation.IsNpsFeedbackEnabled
import ch.protonmail.android.mailfeatureflags.domain.annotation.IsShowRatingBoosterEnabled
import ch.protonmail.android.mailfeatureflags.domain.model.FeatureFlag
import ch.protonmail.android.mailmailbox.domain.repository.InMemoryMailboxRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Whether to offer the NPS survey: once the user is back on the mailbox from the message reader.
 * The rating booster keeps the visit it is due on, so the two never show at the same time.
 */
class ShouldShowNps @Inject constructor(
    private val inMemoryMailboxRepository: InMemoryMailboxRepository,
    @IsNpsFeedbackEnabled private val isNpsFeedbackEnabled: FeatureFlag<Boolean>,
    @IsShowRatingBoosterEnabled private val showRatingBooster: FeatureFlag<Boolean>
) {

    operator fun invoke(): Flow<Boolean> = inMemoryMailboxRepository.observeScreenViewCount()
        .map { screenViewCount ->
            screenViewCount >= SCREEN_VIEW_COUNT_THRESHOLD &&
                !isRatingBoosterDue(screenViewCount) &&
                isNpsFeedbackEnabled.get()
        }
        .distinctUntilChanged()

    private suspend fun isRatingBoosterDue(screenViewCount: Int) =
        screenViewCount == ShouldShowRatingBooster.SCREEN_VIEW_COUNT_THRESHOLD && showRatingBooster.get()

    companion object {

        const val SCREEN_VIEW_COUNT_THRESHOLD = 1
    }
}
