/*
 * Copyright (c) 2021 Proton Technologies AG
 * This file is part of Proton Technologies AG and ProtonMail.
 *
 * ProtonMail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonMail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonMail.  If not, see <https://www.gnu.org/licenses/>.
 */

package ch.protonmail.android.mailsettings.presentation.settings.swipeactions

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import ch.protonmail.android.mailsettings.domain.model.SwipeActionsPreference
import ch.protonmail.android.mailsettings.presentation.R
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
        imageRes = getImageRes(swipeAction),
        titleRes = getTitleRes(swipeAction),
        descriptionRes = getDescriptionRes(swipeAction),
        getColor = getColorGetter(swipeAction)
    )

    private fun getImageRes(swipeAction: SwipeAction) = when (swipeAction) {
        SwipeAction.Trash -> R.drawable.ic_proton_trash
        SwipeAction.Spam -> R.drawable.ic_proton_fire
        SwipeAction.Star -> R.drawable.ic_proton_star
        SwipeAction.Archive -> R.drawable.ic_proton_archive_box
        SwipeAction.MarkRead -> R.drawable.ic_proton_envelope_dot
    }

    private fun getTitleRes(swipeAction: SwipeAction) = when (swipeAction) {
        SwipeAction.Trash -> R.string.mail_settings_swipe_action_trash_title
        SwipeAction.Spam -> R.string.mail_settings_swipe_action_spam_title
        SwipeAction.Star -> R.string.mail_settings_swipe_action_star_title
        SwipeAction.Archive -> R.string.mail_settings_swipe_action_archive_title
        SwipeAction.MarkRead -> R.string.mail_settings_swipe_action_read_title
    }

    private fun getDescriptionRes(swipeAction: SwipeAction) = when (swipeAction) {
        SwipeAction.Trash -> R.string.mail_settings_swipe_action_trash_description
        SwipeAction.Spam -> R.string.mail_settings_swipe_action_spam_description
        SwipeAction.Star -> R.string.mail_settings_swipe_action_star_description
        SwipeAction.Archive -> R.string.mail_settings_swipe_action_archive_description
        SwipeAction.MarkRead -> R.string.mail_settings_swipe_action_read_description
    }

    private fun getColorGetter(swipeAction: SwipeAction): @Composable () -> Color = {
        when (swipeAction) {
            SwipeAction.Trash -> ProtonTheme.colors.notificationError
            SwipeAction.Spam -> ProtonTheme.colors.iconHint
            SwipeAction.Star -> ProtonTheme.colors.notificationWarning
            SwipeAction.Archive -> ProtonTheme.colors.iconHint
            SwipeAction.MarkRead -> ProtonTheme.colors.interactionNorm
        }
    }
}
