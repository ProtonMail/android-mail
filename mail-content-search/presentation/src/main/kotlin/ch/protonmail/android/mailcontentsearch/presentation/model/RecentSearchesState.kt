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

package ch.protonmail.android.mailcontentsearch.presentation.model

import androidx.compose.runtime.Stable
import kotlinx.collections.immutable.ImmutableList

/**
 * Search history shown in place of the generic "Search your mail" empty state.
 *
 * [Loading] and [Empty] are kept apart so the screen can hold off on the generic empty state until
 * the first read comes back, instead of flashing it and then swapping in the history.
 */
@Stable
sealed interface RecentSearchesState {

    data object Loading : RecentSearchesState

    /** No history to show: either there never was any, or the user dismissed all of it. */
    data object Empty : RecentSearchesState

    data class Data(
        val terms: ImmutableList<String>,
        val foundItems: ImmutableList<RecentFoundItemUiModel>
    ) : RecentSearchesState
}
