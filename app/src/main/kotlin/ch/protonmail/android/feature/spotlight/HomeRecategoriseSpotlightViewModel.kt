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

package ch.protonmail.android.feature.spotlight

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.getOrElse
import ch.protonmail.android.mailcategory.domain.model.CategorySpotlightType
import ch.protonmail.android.mailcategory.domain.usecase.MarkCategorySpotlightSeen
import ch.protonmail.android.mailcategory.domain.usecase.ObserveCategorySpotlightSeen
import ch.protonmail.android.mailsession.domain.usecase.IsCategoryViewEnabled
import ch.protonmail.android.mailsession.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailspotlight.domain.usecase.ObserveFeatureSpotlightDisplay
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives the "Recategorise" onboarding bottom sheet as a Home interstitial.
 *
 * It becomes eligible only once the category-view feature spotlight has been consumed and the
 * Personalise spotlight has not been seen yet. Priority ordering (see [resolveHomeInterstitialPriority])
 * guarantees it never surfaces while any other interstitial is being shown.
 */
@HiltViewModel
class HomeRecategoriseSpotlightViewModel @Inject constructor(
    observeFeatureSpotlightDisplay: ObserveFeatureSpotlightDisplay,
    private val observeCategorySpotlightSeen: ObserveCategorySpotlightSeen,
    private val isCategoryViewEnabled: IsCategoryViewEnabled,
    private val observePrimaryUserId: ObservePrimaryUserId,
    private val markCategorySpotlightSeen: MarkCategorySpotlightSeen
) : ViewModel() {

    val state: StateFlow<RecategoriseSpotlightState> = flow {
        val userId = observePrimaryUserId().filterNotNull().first()
        if (!isCategoryViewEnabled(userId)) {
            emit(RecategoriseSpotlightState.Hide)
        } else {
            emitAll(
                combine(
                    observeFeatureSpotlightDisplay(),
                    observeCategorySpotlightSeen(CategorySpotlightType.Personalise)
                ) { displayEither, personaliseSeenEither ->
                    val featureSpotlightSeen = displayEither.getOrNull()?.show == false
                    // On error, treat the spotlight as already seen so we never surface it accidentally.
                    val personaliseSeen = personaliseSeenEither.getOrElse { true }
                    if (featureSpotlightSeen && !personaliseSeen) {
                        RecategoriseSpotlightState.Show
                    } else {
                        RecategoriseSpotlightState.Hide
                    }
                }
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = RecategoriseSpotlightState.Loading
    )

    fun markPersonaliseSeen() {
        viewModelScope.launch { markCategorySpotlightSeen(CategorySpotlightType.Personalise) }
    }
}
