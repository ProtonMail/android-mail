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

package ch.protonmail.android.mailspotlight.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailspotlight.domain.usecase.MarkFeatureSpotlightSeen
import ch.protonmail.android.mailspotlight.presentation.R
import ch.protonmail.android.mailspotlight.presentation.model.FeatureItem
import ch.protonmail.android.mailspotlight.presentation.model.HowItWorksStep
import ch.protonmail.android.mailspotlight.presentation.model.contentSearchHowItWorksSteps
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class FeatureSpotlightViewModel @Inject constructor(
    private val markFeatureSpotlightSeen: MarkFeatureSpotlightSeen
) : ViewModel() {

    private val mutableDismissEvent = MutableSharedFlow<FeatureSpotlightViewAction>()
    val dismissEvent = mutableDismissEvent.asSharedFlow()

    val overviewFeatures: ImmutableList<FeatureItem> = persistentListOf(
        FeatureItem(
            icon = R.drawable.ic_magnifier,
            title = TextUiModel.TextRes(R.string.spotlight_screen_content_search_message_content_title),
            description = TextUiModel.TextRes(R.string.spotlight_screen_content_search_message_content_subtitle)
        ),
        FeatureItem(
            icon = R.drawable.ic_lock,
            title = TextUiModel.TextRes(R.string.spotlight_screen_content_search_encryption_title),
            description = TextUiModel.TextRes(R.string.spotlight_screen_content_search_encryption_subtitle)
        ),
        FeatureItem(
            icon = R.drawable.ic_letter,
            title = TextUiModel.TextRes(R.string.spotlight_screen_content_search_recent_searches_title),
            description = TextUiModel.TextRes(R.string.spotlight_screen_content_search_recent_searches_subtitle)
        )
    )

    val howItWorksSteps: ImmutableList<HowItWorksStep> = contentSearchHowItWorksSteps

    /**
     * Marks the spotlight seen and reports which action closed it.
     *
     * Note: Content search is never turned on here. [FeatureSpotlightViewAction.TurnOn] only hands over
     * to the enabling bottomsheet.
     */
    fun submit(action: FeatureSpotlightViewAction) {
        viewModelScope.launch {
            markFeatureSpotlightSeen()
            mutableDismissEvent.emit(action)
        }
    }
}
