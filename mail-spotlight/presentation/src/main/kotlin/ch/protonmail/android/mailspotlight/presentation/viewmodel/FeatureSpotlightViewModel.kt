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
import ch.protonmail.android.design.compose.viewmodel.stopTimeoutMillis
import ch.protonmail.android.mailcommon.domain.AppInformation
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailspotlight.domain.usecase.MarkFeatureSpotlightSeen
import ch.protonmail.android.mailspotlight.domain.usecase.ObserveIsBusinessUser
import ch.protonmail.android.mailspotlight.domain.usecase.UpdateCategoryView
import ch.protonmail.android.mailspotlight.presentation.R
import ch.protonmail.android.mailspotlight.presentation.model.AppVersionUiModel
import ch.protonmail.android.mailspotlight.presentation.model.FeatureItem
import ch.protonmail.android.mailspotlight.presentation.model.SpotlightUserType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class FeatureSpotlightViewModel @Inject constructor(
    appInformation: AppInformation,
    observeIsBusinessUser: ObserveIsBusinessUser,
    private val updateCategoryView: UpdateCategoryView,
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

    val userType: StateFlow<SpotlightUserType> = observeIsBusinessUser()
        .map { result ->
            result.fold(
                ifLeft = { DEFAULT_USER_TYPE },
                ifRight = { isBusiness -> if (isBusiness) SpotlightUserType.B2B else SpotlightUserType.B2C }
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis),
            initialValue = DEFAULT_USER_TYPE
        )

    val overviewFeatures = listOf(
        FeatureItem(
            icon = R.drawable.ic_proton_filing_cabinet,
            title = TextUiModel.TextRes(R.string.spotlight_screen_category_view_categories_title),
            description = TextUiModel.TextRes(R.string.spotlight_screen_category_view_categories_subtitle)
        ),
        FeatureItem(
            icon = R.drawable.ic_proton_lines_long_to_small,
            title = TextUiModel.TextRes(R.string.spotlight_screen_category_view_unread_filter_title),
            description = TextUiModel.TextRes(R.string.spotlight_screen_category_view_unread_filter_subtitle)
        ),
        FeatureItem(
            icon = R.drawable.ic_proton_paint_roller,
            title = TextUiModel.TextRes(R.string.spotlight_screen_category_view_ui_enhancements_title),
            description = TextUiModel.TextRes(R.string.spotlight_screen_category_view_ui_enhancements_subtitle)
        )
    ).toImmutableList()

    fun onTryCategories() {
        viewModelScope.launch {
            updateCategoryView(enabled = true)
            markFeatureSpotlightSeen()
            _closeScreenEvent.emit(Unit)
        }
    }

    fun onDismissWithoutCategories() {
        viewModelScope.launch {
            updateCategoryView(enabled = false)
            markFeatureSpotlightSeen()
            _closeScreenEvent.emit(Unit)
        }
    }

    companion object {

        val DEFAULT_USER_TYPE = SpotlightUserType.B2C
    }
}
