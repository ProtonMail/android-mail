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

package ch.protonmail.android.mailsettings.presentation.settings.swipeactions

import ch.protonmail.android.mailsettings.domain.model.SwipeActionDirection
import ch.protonmail.android.mailsettings.domain.model.SwipeActionsPreference
import me.proton.core.domain.arch.Mapper
import me.proton.core.mailsettings.domain.entity.SwipeAction
import javax.inject.Inject

class EditSwipeActionPreferenceUiModelMapper @Inject constructor() :
    Mapper<SwipeActionsPreference, SwipeActionsPreferenceUiModel> {

    /**
     * @param swipeActionsPreference will be `null` in case we're still loading the preferences, but we want to display
     *  the list meanwhile, without any item selected
     */
    fun toUiModels(
        swipeActionsPreference: SwipeActionsPreference?,
        swipeActionDirection: SwipeActionDirection,
        areAdditionalSwipeActionsEnabled: Boolean
    ): List<EditSwipeActionPreferenceItemUiModel> = SwipeAction.entries
        .filter { it.isSwipeActionAllowed(areAdditionalSwipeActionsEnabled) }
        .map { swipeAction ->
            toUiModel(
                swipeAction = swipeAction,
                swipeActionsPreference = swipeActionsPreference,
                swipeActionDirection = swipeActionDirection
            )
        }

    private fun toUiModel(
        swipeAction: SwipeAction,
        swipeActionsPreference: SwipeActionsPreference?,
        swipeActionDirection: SwipeActionDirection
    ): EditSwipeActionPreferenceItemUiModel {
        val isSelected = run {
            if (swipeActionsPreference == null) {
                return@run false
            }
            val currentSwipeAction = when (swipeActionDirection) {
                SwipeActionDirection.RIGHT -> swipeActionsPreference.swipeRight
                SwipeActionDirection.LEFT -> swipeActionsPreference.swipeLeft
            }
            swipeAction == currentSwipeAction
        }
        return toUiModel(
            swipeAction = swipeAction,
            isSelected = isSelected
        )
    }

    private fun toUiModel(swipeAction: SwipeAction, isSelected: Boolean) = EditSwipeActionPreferenceItemUiModel(
        swipeAction = swipeAction,
        imageRes = swipeAction.imageRes,
        titleRes = swipeAction.titleRes,
        descriptionRes = swipeAction.descriptionRes,
        isSelected = isSelected
    )
}

fun SwipeAction.isSwipeActionAllowed(areAdditionalSwipeActionsEnabled: Boolean) =
    areAdditionalSwipeActionsEnabled || this != SwipeAction.LabelAs &&
        this != SwipeAction.MoveTo && this != SwipeAction.None
