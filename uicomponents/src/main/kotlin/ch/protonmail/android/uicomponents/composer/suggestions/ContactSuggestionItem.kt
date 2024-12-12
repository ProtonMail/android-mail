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

package ch.protonmail.android.uicomponents.composer.suggestions

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color

sealed interface ContactSuggestionItem {

    @Stable
    data class Contact(
        val initials: String,
        val header: String,
        val subheader: String,
        val email: String
    ) : ContactSuggestionItem

    @Stable
    data class Group(
        val header: String,
        val subheader: String,
        val emails: List<String>,
        val backgroundColor: Color
    ) : ContactSuggestionItem
}
