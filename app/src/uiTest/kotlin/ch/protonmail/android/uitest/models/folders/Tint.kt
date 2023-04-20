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

package ch.protonmail.android.uitest.models.folders

import androidx.compose.ui.graphics.Color

internal sealed class Tint {
    object NoColor : Tint()

    sealed class WithColor(val value: Color) : Tint() {
        object Carrot : WithColor(Color(0xFFF78400))
        object Fern : WithColor(Color(0xFF3CBB3A))
        object PurpleBase : WithColor(Color(0xFF8080FF))
        object Bridge : WithColor(Color(0xFFFF6666))
        class Custom(hex: Long) : WithColor(Color(hex))
    }
}
