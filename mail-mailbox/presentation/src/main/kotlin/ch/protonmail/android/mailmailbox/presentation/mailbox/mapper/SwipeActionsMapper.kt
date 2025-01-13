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

package ch.protonmail.android.mailmailbox.presentation.mailbox.mapper

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.mailmailbox.presentation.R
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.SwipeActionsUiModel
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.SwipeUiModel
import ch.protonmail.android.mailsettings.domain.model.SwipeActionsPreference
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.label.domain.entity.LabelId
import me.proton.core.mailsettings.domain.entity.SwipeAction
import javax.inject.Inject

class SwipeActionsMapper @Inject constructor() {

    operator fun invoke(
        currentMailLabel: LabelId,
        swipeActionsPreference: SwipeActionsPreference
    ): SwipeActionsUiModel = SwipeActionsUiModel(
        start = toUiModel(currentMailLabel, swipeActionsPreference.swipeRight),
        end = toUiModel(currentMailLabel, swipeActionsPreference.swipeLeft)
    )


    @Suppress("LongMethod")
    private fun toUiModel(currentMailLabel: LabelId, swipeAction: SwipeAction): SwipeUiModel {
        return when (swipeAction) {
            SwipeAction.None -> SwipeUiModel(
                swipeAction = swipeAction,
                icon = R.drawable.ic_proton_cross_circle,
                descriptionRes = R.string.mail_settings_swipe_action_none_description,
                getColor = getColorForSwipeAction(swipeAction),
                staysDismissed = dismissible(swipeAction, currentMailLabel)
            )

            SwipeAction.Trash -> SwipeUiModel(
                swipeAction = swipeAction,
                icon = R.drawable.ic_proton_trash,
                descriptionRes = R.string.mail_settings_swipe_action_trash_description,
                getColor = getColorForSwipeAction(swipeAction),
                staysDismissed = dismissible(swipeAction, currentMailLabel)
            )

            SwipeAction.Spam -> SwipeUiModel(
                swipeAction = swipeAction,
                icon = R.drawable.ic_proton_fire,
                descriptionRes = R.string.mail_settings_swipe_action_spam_description,
                getColor = getColorForSwipeAction(swipeAction),
                staysDismissed = dismissible(swipeAction, currentMailLabel)
            )

            SwipeAction.Star -> SwipeUiModel(
                swipeAction = swipeAction,
                icon = R.drawable.ic_proton_star,
                descriptionRes = R.string.mail_settings_swipe_action_star_description,
                getColor = getColorForSwipeAction(swipeAction),
                staysDismissed = dismissible(swipeAction, currentMailLabel)
            )

            SwipeAction.Archive -> SwipeUiModel(
                swipeAction = swipeAction,
                icon = R.drawable.ic_proton_archive_box,
                descriptionRes = R.string.mail_settings_swipe_action_archive_description,
                getColor = getColorForSwipeAction(swipeAction),
                staysDismissed = dismissible(swipeAction, currentMailLabel)
            )

            SwipeAction.MarkRead -> SwipeUiModel(
                swipeAction = swipeAction,
                icon = R.drawable.ic_proton_envelope_dot,
                descriptionRes = R.string.mail_settings_swipe_action_read_description,
                getColor = getColorForSwipeAction(swipeAction),
                staysDismissed = dismissible(swipeAction, currentMailLabel)
            )

            SwipeAction.LabelAs -> SwipeUiModel(
                swipeAction = swipeAction,
                icon = R.drawable.ic_proton_tag,
                descriptionRes = R.string.mail_settings_swipe_action_label_as_description,
                getColor = getColorForSwipeAction(swipeAction),
                staysDismissed = dismissible(swipeAction, currentMailLabel)
            )

            SwipeAction.MoveTo -> SwipeUiModel(
                swipeAction = swipeAction,
                icon = R.drawable.ic_proton_folder_arrow_in,
                descriptionRes = R.string.mail_settings_swipe_action_move_to_description,
                getColor = getColorForSwipeAction(swipeAction),
                staysDismissed = dismissible(swipeAction, currentMailLabel)
            )
        }
    }

    private fun getColorForSwipeAction(swipeAction: SwipeAction): @Composable () -> Color = {
        when (swipeAction) {
            SwipeAction.None -> ProtonTheme.colors.notificationNorm
            SwipeAction.Trash -> ProtonTheme.colors.notificationError
            SwipeAction.Spam -> ProtonTheme.colors.notificationNorm
            SwipeAction.Star -> ProtonTheme.colors.notificationWarning
            SwipeAction.Archive -> ProtonTheme.colors.notificationNorm
            SwipeAction.MarkRead -> ProtonTheme.colors.brandNorm
            SwipeAction.LabelAs -> ProtonTheme.colors.notificationNorm
            SwipeAction.MoveTo -> ProtonTheme.colors.notificationNorm
        }
    }

    private fun dismissible(swipeAction: SwipeAction, labelId: LabelId): Boolean {
        return when (swipeAction) {
            SwipeAction.None -> false
            SwipeAction.Trash -> labelId != SystemLabelId.Trash.labelId
            SwipeAction.Spam -> labelId != SystemLabelId.Spam.labelId
            SwipeAction.Star -> false
            SwipeAction.Archive -> labelId != SystemLabelId.Archive.labelId
            SwipeAction.MarkRead -> false
            SwipeAction.LabelAs -> false
            SwipeAction.MoveTo -> false
        }
    }
}
