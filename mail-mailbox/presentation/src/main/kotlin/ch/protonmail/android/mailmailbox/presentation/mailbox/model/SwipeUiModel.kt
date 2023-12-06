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

package ch.protonmail.android.mailmailbox.presentation.mailbox.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import me.proton.core.mailsettings.domain.entity.SwipeAction

data class SwipeUiModel(
    val swipeAction: SwipeAction,
    @DrawableRes val icon: Int,
    @StringRes val descriptionRes: Int,
    val getColor: @Composable () -> Color,
    val staysDismissed: Boolean
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SwipeUiModel) return false

        if (swipeAction != other.swipeAction) return false
        if (icon != other.icon) return false
        if (descriptionRes != other.descriptionRes) return false
        if (staysDismissed != other.staysDismissed) return false

        return true
    }
}
