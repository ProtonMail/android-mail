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

package ch.protonmail.android.mailcontentsearch.presentation.settings

import androidx.compose.runtime.Stable

@Stable
sealed interface ContentSearchSettingsState {

    data object Loading : ContentSearchSettingsState

    data object LoadingError : ContentSearchSettingsState

    data class Data(
        val isContentSearchEnabled: Boolean,
        val isAllowMobileDataEnabled: Boolean,
        val syncPercentage: Double?,
        val isIndexingActive: Boolean = false,
        val isBlockedByOtherUser: Boolean = false
    ) : ContentSearchSettingsState
}
