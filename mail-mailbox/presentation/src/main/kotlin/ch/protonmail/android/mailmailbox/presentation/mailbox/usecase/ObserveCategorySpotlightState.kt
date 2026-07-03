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

import arrow.core.getOrElse
import ch.protonmail.android.mailcategory.domain.model.CategorySpotlightType
import ch.protonmail.android.mailcategory.domain.usecase.ObserveCategorySpotlightSeen
import ch.protonmail.android.mailcategory.presentation.model.CategoryItemUiModel
import ch.protonmail.android.mailcategory.presentation.model.CategorySpotlightState
import ch.protonmail.android.mailonboarding.domain.usecase.ObserveOnboarding
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Decides which category-view onboarding banner (if any) should be shown.
 *
 * The unseen-dot banner has priority: when both are eligible, it is shown and the Personalise
 * banner surfaces on a later run once the unseen one is consumed
 */
class ObserveCategorySpotlightState @Inject constructor(
    private val observeCategorySpotlightSeen: ObserveCategorySpotlightSeen,
    private val observeOnboarding: ObserveOnboarding
) {

    operator fun invoke(
        categories: Flow<List<CategoryItemUiModel>?>,
        unseenDismissed: Flow<Boolean>,
        personaliseDismissed: Flow<Boolean>
    ): Flow<CategorySpotlightState> = combine(
        observeConsumed(CategorySpotlightType.UnseenCategory, unseenDismissed),
        observeConsumed(CategorySpotlightType.Personalise, personaliseDismissed),
        observeOnboardingCompleted(),
        categories.distinctUntilChanged()
    ) { unseenConsumed, personaliseConsumed, onboardingDone, availableCategories ->
        // Hold the spotlights back until the onboarding is finished and the category bar is available.
        if (!onboardingDone || availableCategories == null) return@combine CategorySpotlightState.Hidden

        val unseen = if (!unseenConsumed) {
            unseenStateFrom(availableCategories)
        } else {
            CategorySpotlightState.Hidden
        }
        when {
            unseen is CategorySpotlightState.Shown -> unseen
            !personaliseConsumed -> CategorySpotlightState.Shown.Personalise
            else -> CategorySpotlightState.Hidden
        }
    }.distinctUntilChanged()

    // A spotlight is "consumed" once it has been persisted as seen or dismissed in the current session.
    private fun observeConsumed(type: CategorySpotlightType, dismissed: Flow<Boolean>): Flow<Boolean> {
        return combine(
            observeCategorySpotlightSeen(type).map { either -> either.getOrElse { true } },
            dismissed
        ) { seen, wasDismissed -> seen || wasDismissed }
    }

    // Emits true only once we positively know onboarding is done.
    private fun observeOnboardingCompleted(): Flow<Boolean> = observeOnboarding()
        .map { either -> either.getOrNull()?.display == false }
        .distinctUntilChanged()

    private fun unseenStateFrom(categories: List<CategoryItemUiModel>): CategorySpotlightState {
        val unseenCategory = categories.firstOrNull { !it.isActive && it.hasUnseen }
            ?: return CategorySpotlightState.Hidden
        return CategorySpotlightState.Shown.UnseenCategory(unseenCategory)
    }
}
