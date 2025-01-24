/*
 * Copyright (c) 2022 Proton Technologies AG
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

package ch.protonmail.android.mailsettings.presentation.settings.customizetoolbar

import androidx.annotation.DrawableRes
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel

sealed class CustomizeToolbarState {

    data class Data(
        val pages: List<Page>,
        val tabs: List<TextUiModel>,
        val selectedTabIdx: Int
    ) : CustomizeToolbarState() {

        data class Page(
            val disclaimer: TextUiModel,
            val selectedActions: List<ToolbarActionUiModel>,
            val remainingActions: List<ToolbarActionUiModel>
        )
    }

    data object Loading : CustomizeToolbarState()

    data object NotLoggedIn : CustomizeToolbarState()
}

data class ToolbarActionUiModel(
    val id: String,
    val enabled: Boolean,
    @DrawableRes val icon: Int,
    val description: TextUiModel
)
