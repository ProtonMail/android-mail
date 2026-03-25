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

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.maillabel.domain.extension.isOutbox
import ch.protonmail.android.maillabel.domain.model.LabelId
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.mailmailbox.presentation.R
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.SwipeActionsUiModel
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.SwipeIconProvider
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.SwipeUiModel
import ch.protonmail.android.mailsettings.domain.model.SwipeActionsPreference
import me.proton.core.mailsettings.domain.entity.SwipeAction
import javax.inject.Inject

class SwipeActionsMapper @Inject constructor() {

    private val markReadIconProvider = SwipeIconProvider { item ->
        if (item.isRead) {
            R.drawable.ic_proton_envelope_dot
        } else {
            R.drawable.ic_proton_envelope_open
        }
    }

    private val starIconProvider = SwipeIconProvider { item ->
        if (item.isStarred) {
            R.drawable.ic_proton_star_slash
        } else {
            R.drawable.ic_proton_star
        }
    }

    operator fun invoke(
        currentMailLabel: LabelId,
        swipeActionsPreference: SwipeActionsPreference
    ): SwipeActionsUiModel = SwipeActionsUiModel(
        start = toUiModel(currentMailLabel, swipeActionsPreference.swipeRight),
        end = toUiModel(currentMailLabel, swipeActionsPreference.swipeLeft)
    )

    private fun toUiModel(currentMailLabel: LabelId, swipeAction: SwipeAction): SwipeUiModel {
        val actionConfig = getActionConfig(swipeAction)
        return SwipeUiModel(
            swipeAction = swipeAction,
            iconProvider = actionConfig.iconProvider,
            descriptionRes = actionConfig.descriptionRes,
            getColor = { actionConfig.color() },
            staysDismissed = isDismissible(swipeAction, currentMailLabel),
            isEnabled = isEnabled(swipeAction, currentMailLabel)
        )
    }

    private fun getActionConfig(swipeAction: SwipeAction): ActionConfig = when (swipeAction) {
        SwipeAction.None -> ActionConfig(
            iconProvider = fixedIcon(R.drawable.ic_proton_cross_circle),
            descriptionRes = R.string.mail_settings_swipe_action_none_description,
            color = { ProtonTheme.colors.notificationNorm }
        )

        SwipeAction.Trash -> ActionConfig(
            iconProvider = fixedIcon(R.drawable.ic_proton_trash),
            descriptionRes = R.string.mail_settings_swipe_action_trash_description,
            color = { ProtonTheme.colors.notificationError }
        )

        SwipeAction.Spam -> ActionConfig(
            iconProvider = fixedIcon(R.drawable.ic_proton_fire),
            descriptionRes = R.string.mail_settings_swipe_action_spam_description,
            color = { ProtonTheme.colors.notificationWarning }
        )

        SwipeAction.Star -> ActionConfig(
            iconProvider = starIconProvider,
            descriptionRes = R.string.mail_settings_swipe_action_star_description,
            color = { ProtonTheme.colors.starSelected }
        )

        SwipeAction.Archive -> ActionConfig(
            iconProvider = fixedIcon(R.drawable.ic_proton_archive_box),
            descriptionRes = R.string.mail_settings_swipe_action_archive_description,
            color = { ProtonTheme.colors.notificationSuccess }
        )

        SwipeAction.MarkRead -> ActionConfig(
            iconProvider = markReadIconProvider,
            descriptionRes = R.string.mail_settings_swipe_action_read_description,
            color = { ProtonTheme.colors.brandMinus10 }
        )

        SwipeAction.LabelAs -> ActionConfig(
            iconProvider = fixedIcon(R.drawable.ic_proton_tag),
            descriptionRes = R.string.mail_settings_swipe_action_label_as_description,
            color = { ProtonTheme.colors.brandMinus10 }
        )

        SwipeAction.MoveTo -> ActionConfig(
            iconProvider = fixedIcon(R.drawable.ic_proton_folder_arrow_in),
            descriptionRes = R.string.mail_settings_swipe_action_move_to_description,
            color = { ProtonTheme.colors.brandMinus10 }
        )
    }

    private fun isDismissible(swipeAction: SwipeAction, labelId: LabelId): Boolean = when (swipeAction) {
        SwipeAction.None -> false
        SwipeAction.Trash -> labelId != SystemLabelId.Trash.labelId
        SwipeAction.Spam -> labelId != SystemLabelId.Spam.labelId
        SwipeAction.Star -> false
        SwipeAction.Archive -> labelId != SystemLabelId.Archive.labelId
        SwipeAction.MarkRead, SwipeAction.LabelAs, SwipeAction.MoveTo -> false
    }

    private fun isEnabled(swipeAction: SwipeAction, labelId: LabelId) = when (swipeAction) {
        SwipeAction.None -> false
        SwipeAction.Trash -> labelId != SystemLabelId.Trash.labelId
        SwipeAction.Spam -> labelId != SystemLabelId.Spam.labelId
        SwipeAction.Archive -> labelId != SystemLabelId.Archive.labelId
        else -> true
    } && labelId.isOutbox().not()

    private data class ActionConfig(
        val iconProvider: SwipeIconProvider,
        val descriptionRes: Int,
        val color: @Composable () -> Color
    )

    private fun fixedIcon(@DrawableRes iconRes: Int): SwipeIconProvider = SwipeIconProvider { iconRes }

}
