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
import ch.protonmail.android.mailcommon.domain.AppInformation
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailspotlight.domain.usecase.MarkFeatureSpotlightSeen
import ch.protonmail.android.mailspotlight.presentation.R
import ch.protonmail.android.mailspotlight.presentation.model.AppVersionUiModel
import ch.protonmail.android.mailspotlight.presentation.model.FeatureItem
import ch.protonmail.android.mailspotlight.presentation.model.SpotlightUserType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class FeatureSpotlightViewModel @Inject constructor(
    appInformation: AppInformation,
    private val markFeatureSpotlightSeen: MarkFeatureSpotlightSeen
) : ViewModel() {

    private val _closeScreenEvent = MutableSharedFlow<Unit>()
    val closeScreenEvent = _closeScreenEvent.asSharedFlow()

    val appVersion: AppVersionUiModel = AppVersionUiModel(
        text = TextUiModel.TextResWithArgs(
            value = R.string.spotlight_screen_version_text,
            formatArgs = listOf(appInformation.appVersionName)
        )
    )

    // ET-6044 - missing Rust wiring for B2B/B2C detection
    val userType: SpotlightUserType = SpotlightUserType.B2C

    val overviewFeatures = listOf(
        FeatureItem(
            icon = R.drawable.ic_proton_paint_roller,
            title = TextUiModel.TextRes(R.string.spotlight_screen_category_view_ui_enhancements_title),
            description = TextUiModel.TextRes(R.string.spotlight_screen_category_view_ui_enhancements_subtitle)
        ),
        FeatureItem(
            icon = R.drawable.ic_proton_lines_long_to_small,
            title = TextUiModel.TextRes(R.string.spotlight_screen_category_view_unread_filter_title),
            description = TextUiModel.TextRes(R.string.spotlight_screen_category_view_unread_filter_subtitle)
        ),
        FeatureItem(
            icon = R.drawable.ic_proton_filing_cabinet,
            title = TextUiModel.TextRes(R.string.spotlight_screen_category_view_categories_title),
            description = TextUiModel.TextRes(R.string.spotlight_screen_category_view_categories_subtitle)
        )
    ).toImmutableList()

    // ET-6044 missing Rust wiring for Categories toggling
    fun onTryCategories() {
        viewModelScope.launch {
            markFeatureSpotlightSeen()
            _closeScreenEvent.emit(Unit)
        }
    }

    // ET-6044 missing Rust wiring for Categories toggling
    fun onDismissWithoutCategories() {
        viewModelScope.launch {
            markFeatureSpotlightSeen()
            _closeScreenEvent.emit(Unit)
        }
    }
}
