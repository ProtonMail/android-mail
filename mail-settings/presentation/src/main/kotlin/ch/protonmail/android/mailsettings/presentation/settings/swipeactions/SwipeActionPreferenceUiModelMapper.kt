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

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import ch.protonmail.android.mailsettings.domain.model.SwipeActionsPreference
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.domain.arch.Mapper
import me.proton.core.mailsettings.domain.entity.SwipeAction
import javax.inject.Inject

class SwipeActionPreferenceUiModelMapper @Inject constructor() :
    Mapper<SwipeActionsPreference, SwipeActionsPreferenceUiModel> {

    fun toUiModel(swipeActionsPreference: SwipeActionsPreference) = SwipeActionsPreferenceUiModel(
        left = toUiModel(swipeActionsPreference.swipeLeft),
        right = toUiModel(swipeActionsPreference.swipeRight)
    )

    private fun toUiModel(swipeAction: SwipeAction) = SwipeActionPreferenceUiModel(
        imageRes = swipeAction.imageRes,
        titleRes = swipeAction.titleRes,
        descriptionRes = swipeAction.descriptionRes,
        getColor = getColorGetter(swipeAction)
    )

    private fun getColorGetter(swipeAction: SwipeAction): @Composable () -> Color = {
        when (swipeAction) {
            SwipeAction.None -> ProtonTheme.colors.iconHint
            SwipeAction.Trash -> ProtonTheme.colors.notificationError
            SwipeAction.Spam -> ProtonTheme.colors.iconHint
            SwipeAction.Star -> ProtonTheme.colors.notificationWarning
            SwipeAction.Archive -> ProtonTheme.colors.iconHint
            SwipeAction.MarkRead -> ProtonTheme.colors.interactionNorm
            SwipeAction.LabelAs -> ProtonTheme.colors.iconHint
            SwipeAction.MoveTo -> ProtonTheme.colors.iconHint
        }
    }
}
