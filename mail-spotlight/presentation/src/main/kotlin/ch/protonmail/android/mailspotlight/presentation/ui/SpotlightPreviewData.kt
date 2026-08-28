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
import ch.protonmail.android.mailspotlight.presentation.model.FeatureItem
import kotlinx.collections.immutable.toImmutableList

internal object SpotlightPreviewData {

    val previewFeatures = listOf(
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
    ).toImmutableList()
}
