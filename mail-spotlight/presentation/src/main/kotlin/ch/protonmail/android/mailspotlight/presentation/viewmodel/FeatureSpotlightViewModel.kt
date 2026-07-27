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

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.mailcategory.domain.model.CategorySpotlightType
import ch.protonmail.android.mailcategory.domain.usecase.MarkCategorySpotlightSeen
import ch.protonmail.android.mailcommon.domain.AppInformation
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailspotlight.domain.usecase.MarkFeatureSpotlightSeen
import ch.protonmail.android.mailspotlight.domain.usecase.UpdateCategoryView
import ch.protonmail.android.mailspotlight.presentation.R
import ch.protonmail.android.mailspotlight.presentation.model.AppVersionUiModel
import ch.protonmail.android.mailspotlight.presentation.model.FeatureItem
import ch.protonmail.android.mailspotlight.presentation.model.SpotlightUserType
import ch.protonmail.android.mailspotlight.presentation.ui.SPOTLIGHT_USER_TYPE_KEY
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class FeatureSpotlightViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    appInformation: AppInformation,
    private val updateCategoryView: UpdateCategoryView,
    private val markFeatureSpotlightSeen: MarkFeatureSpotlightSeen,
    private val markCategorySpotlightSeen: MarkCategorySpotlightSeen
) : ViewModel() {

    private val _closeScreenEvent = MutableSharedFlow<Unit>()
    val closeScreenEvent = _closeScreenEvent.asSharedFlow()

    val appVersion: AppVersionUiModel = AppVersionUiModel(
        text = TextUiModel.TextResWithArgs(
            value = R.string.spotlight_screen_version_text,
            formatArgs = listOf(appInformation.appVersionName)
        )
    )

    // The user type is resolved before navigation and passed as an argument, so it's known synchronously here.
    val userType: SpotlightUserType = savedStateHandle.get<String>(SPOTLIGHT_USER_TYPE_KEY)
        ?.let { runCatching { SpotlightUserType.valueOf(it) }.getOrNull() }
        ?: DEFAULT_USER_TYPE

    val overviewFeatures: ImmutableList<FeatureItem> = overviewFeaturesFor(userType)

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
            // The user opted out of categories, so the Personalise recategorise sheet is irrelevant.
            markCategorySpotlightSeen(CategorySpotlightType.Personalise)
            _closeScreenEvent.emit(Unit)
        }
    }

    private fun overviewFeaturesFor(userType: SpotlightUserType): ImmutableList<FeatureItem> {
        val categoriesSubtitle: Int
        val unreadFilterSubtitle: Int
        when (userType) {
            SpotlightUserType.B2C -> {
                categoriesSubtitle = R.string.spotlight_screen_category_view_categories_subtitle
                unreadFilterSubtitle = R.string.spotlight_screen_category_view_unread_filter_subtitle
            }

            SpotlightUserType.B2B -> {
                categoriesSubtitle = R.string.spotlight_screen_category_view_categories_subtitle_b2b
                unreadFilterSubtitle = R.string.spotlight_screen_category_view_unread_filter_subtitle_b2b
            }
        }
        return persistentListOf(
            FeatureItem(
                icon = R.drawable.ic_proton_filing_cabinet,
                title = TextUiModel.TextRes(R.string.spotlight_screen_category_view_categories_title),
                description = TextUiModel.TextRes(categoriesSubtitle)
            ),
            FeatureItem(
                icon = R.drawable.ic_proton_lines_long_to_small,
                title = TextUiModel.TextRes(R.string.spotlight_screen_category_view_unread_filter_title),
                description = TextUiModel.TextRes(unreadFilterSubtitle)
            ),
            FeatureItem(
                icon = R.drawable.ic_proton_paint_roller,
                title = TextUiModel.TextRes(R.string.spotlight_screen_category_view_ui_enhancements_title),
                description = TextUiModel.TextRes(R.string.spotlight_screen_category_view_ui_enhancements_subtitle)
            )
        )
    }

    companion object {

        val DEFAULT_USER_TYPE = SpotlightUserType.B2C
    }
}
