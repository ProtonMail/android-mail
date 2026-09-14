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

package ch.protonmail.android.mailspotlight.presentation.model

import androidx.compose.runtime.Stable
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailspotlight.presentation.R
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Stable
internal data class HowItWorksStep(
    val title: TextUiModel,
    val description: TextUiModel
)

/**
 * What turning content search on does, in the order it happens.
 *
 * The list is the same wherever the page is shown, in the spotlight carousel or on its own, so it
 * lives here rather than on a view model.
 */
internal val contentSearchHowItWorksSteps: ImmutableList<HowItWorksStep> = persistentListOf(
    HowItWorksStep(
        title = TextUiModel.TextRes(R.string.spotlight_screen_content_search_how_it_works_download_title),
        description = TextUiModel.TextRes(R.string.spotlight_screen_content_search_how_it_works_download_subtitle)
    ),
    HowItWorksStep(
        title = TextUiModel.TextRes(R.string.spotlight_screen_content_search_how_it_works_index_title),
        description = TextUiModel.TextRes(R.string.spotlight_screen_content_search_how_it_works_index_subtitle)
    ),
    HowItWorksStep(
        title = TextUiModel.TextRes(R.string.spotlight_screen_content_search_how_it_works_search_now_title),
        description = TextUiModel.TextRes(R.string.spotlight_screen_content_search_how_it_works_search_now_subtitle)
    )
)
