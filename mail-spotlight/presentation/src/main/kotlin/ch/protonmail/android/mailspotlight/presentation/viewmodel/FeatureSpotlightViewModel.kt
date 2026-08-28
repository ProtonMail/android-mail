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

    private val _closeScreenEvent = MutableSharedFlow<Unit>()
    val closeScreenEvent = _closeScreenEvent.asSharedFlow()

    val overviewFeatures: ImmutableList<FeatureItem> = persistentListOf(
        FeatureItem(
            icon = R.drawable.ic_file_download,
            title = TextUiModel.TextRes(R.string.spotlight_screen_content_search_message_content_title),
            description = TextUiModel.TextRes(R.string.spotlight_screen_content_search_message_content_subtitle)
        ),
        FeatureItem(
            icon = R.drawable.ic_magnifier,
            title = TextUiModel.TextRes(R.string.spotlight_screen_content_search_bottom_bar_title),
            description = TextUiModel.TextRes(R.string.spotlight_screen_content_search_bottom_bar_subtitle)
        ),
        FeatureItem(
            icon = R.drawable.ic_mark_unread,
            title = TextUiModel.TextRes(R.string.spotlight_screen_content_search_recent_searches_title),
            description = TextUiModel.TextRes(R.string.spotlight_screen_content_search_recent_searches_subtitle)
        )
    )

    fun onGotIt() {
        viewModelScope.launch {
            markFeatureSpotlightSeen()
            _closeScreenEvent.emit(Unit)
        }
    }
}
