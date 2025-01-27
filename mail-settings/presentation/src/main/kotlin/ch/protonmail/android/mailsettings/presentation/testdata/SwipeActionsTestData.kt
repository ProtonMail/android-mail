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

package ch.protonmail.android.mailsettings.presentation.testdata

import ch.protonmail.android.mailsettings.presentation.settings.swipeactions.EditSwipeActionPreferenceItemUiModel
import ch.protonmail.android.mailsettings.presentation.settings.swipeactions.descriptionRes
import ch.protonmail.android.mailsettings.presentation.settings.swipeactions.imageRes
import ch.protonmail.android.mailsettings.presentation.settings.swipeactions.titleRes
import me.proton.core.mailsettings.domain.entity.SwipeAction
import ch.protonmail.android.mailsettings.presentation.settings.swipeactions.isSwipeActionAllowed

object SwipeActionsTestData {

    object Edit {

        fun buildAllItems(
            selected: SwipeAction?,
            additionalSwipeActions: Boolean = false
        ): List<EditSwipeActionPreferenceItemUiModel> =
            SwipeAction.entries.filter { it.isSwipeActionAllowed(additionalSwipeActions) }
                .map { buildItem(it, isSelected = it == selected) }

        fun buildItem(swipeAction: SwipeAction, isSelected: Boolean) = EditSwipeActionPreferenceItemUiModel(
            swipeAction = swipeAction,
            imageRes = swipeAction.imageRes,
            titleRes = swipeAction.titleRes,
            descriptionRes = swipeAction.descriptionRes,
            isSelected = isSelected
        )
    }
}
