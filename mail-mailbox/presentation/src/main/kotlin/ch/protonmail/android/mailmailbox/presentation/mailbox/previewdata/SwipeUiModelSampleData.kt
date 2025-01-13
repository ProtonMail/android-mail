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

package ch.protonmail.android.mailmailbox.presentation.mailbox.previewdata

import androidx.compose.ui.graphics.Color
import ch.protonmail.android.mailmailbox.presentation.R
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.SwipeUiModel
import me.proton.core.mailsettings.domain.entity.SwipeAction

object SwipeUiModelSampleData {

    val None = SwipeUiModel(
        swipeAction = SwipeAction.None,
        icon = R.drawable.ic_proton_cross_circle,
        descriptionRes = R.string.mail_settings_swipe_action_none_description,
        getColor = { Color.Gray },
        staysDismissed = false
    )

    val Trash = SwipeUiModel(
        swipeAction = SwipeAction.Trash,
        icon = R.drawable.ic_proton_trash,
        descriptionRes = R.string.mail_settings_swipe_action_trash_description,
        getColor = { Color.Red },
        staysDismissed = true
    )

    val Archive = SwipeUiModel(
        swipeAction = SwipeAction.Archive,
        icon = R.drawable.ic_proton_archive_box,
        descriptionRes = R.string.mail_settings_swipe_action_archive_description,
        getColor = { Color.Blue },
        staysDismissed = true
    )

    val MarkRead = SwipeUiModel(
        swipeAction = SwipeAction.MarkRead,
        icon = R.drawable.ic_proton_envelope_dot,
        descriptionRes = R.string.mail_settings_swipe_action_read_description,
        getColor = { Color.Green },
        staysDismissed = false
    )

}
