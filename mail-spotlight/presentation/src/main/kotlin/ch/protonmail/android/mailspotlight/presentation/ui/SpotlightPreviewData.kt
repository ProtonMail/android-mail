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

package ch.protonmail.android.mailspotlight.presentation.ui

import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailspotlight.presentation.R
import ch.protonmail.android.mailspotlight.presentation.model.AppVersionUiModel
import ch.protonmail.android.mailspotlight.presentation.model.FeatureItem
import kotlinx.collections.immutable.toImmutableList

internal object SpotlightPreviewData {

    val previewAppVersion = AppVersionUiModel(
        TextUiModel(value = R.string.spotlight_screen_version_text, formatArgs = arrayOf("1.11.2"))
    )

    val previewFeaturesB2C = listOf(
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

    val previewFeaturesB2B = listOf(
        FeatureItem(
            icon = R.drawable.ic_proton_filing_cabinet,
            title = TextUiModel.TextRes(R.string.spotlight_screen_category_view_categories_title),
            description = TextUiModel.TextRes(R.string.spotlight_screen_category_view_categories_subtitle_b2b)
        ),
        FeatureItem(
            icon = R.drawable.ic_proton_lines_long_to_small,
            title = TextUiModel.TextRes(R.string.spotlight_screen_category_view_unread_filter_title),
            description = TextUiModel.TextRes(R.string.spotlight_screen_category_view_unread_filter_subtitle_b2b)
        ),
        FeatureItem(
            icon = R.drawable.ic_proton_paint_roller,
            title = TextUiModel.TextRes(R.string.spotlight_screen_category_view_ui_enhancements_title),
            description = TextUiModel.TextRes(R.string.spotlight_screen_category_view_ui_enhancements_subtitle)
        )
    ).toImmutableList()
}

