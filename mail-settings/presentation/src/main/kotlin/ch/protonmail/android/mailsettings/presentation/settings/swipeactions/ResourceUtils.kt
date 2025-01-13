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

import ch.protonmail.android.mailsettings.presentation.R
import me.proton.core.mailsettings.domain.entity.SwipeAction

internal val SwipeAction.imageRes
    get() = when (this) {
        SwipeAction.None -> R.drawable.ic_proton_cross_circle
        SwipeAction.Trash -> R.drawable.ic_proton_trash
        SwipeAction.Spam -> R.drawable.ic_proton_fire
        SwipeAction.Star -> R.drawable.ic_proton_star
        SwipeAction.Archive -> R.drawable.ic_proton_archive_box
        SwipeAction.MarkRead -> R.drawable.ic_proton_envelope_dot
        SwipeAction.LabelAs -> R.drawable.ic_proton_tag
        SwipeAction.MoveTo -> R.drawable.ic_proton_folder_arrow_in
    }

internal val SwipeAction.titleRes
    get() = when (this) {
        SwipeAction.None -> R.string.mail_settings_swipe_action_none_title
        SwipeAction.Trash -> R.string.mail_settings_swipe_action_trash_title
        SwipeAction.Spam -> R.string.mail_settings_swipe_action_spam_title
        SwipeAction.Star -> R.string.mail_settings_swipe_action_star_title
        SwipeAction.Archive -> R.string.mail_settings_swipe_action_archive_title
        SwipeAction.MarkRead -> R.string.mail_settings_swipe_action_read_title
        SwipeAction.LabelAs -> R.string.mail_settings_swipe_action_label_as_title
        SwipeAction.MoveTo -> R.string.mail_settings_swipe_action_move_to_title
    }

internal val SwipeAction.descriptionRes
    get() = when (this) {
        SwipeAction.None -> R.string.mail_settings_swipe_action_none_description
        SwipeAction.Trash -> R.string.mail_settings_swipe_action_trash_description
        SwipeAction.Spam -> R.string.mail_settings_swipe_action_spam_description
        SwipeAction.Star -> R.string.mail_settings_swipe_action_star_description
        SwipeAction.Archive -> R.string.mail_settings_swipe_action_archive_description
        SwipeAction.MarkRead -> R.string.mail_settings_swipe_action_read_description
        SwipeAction.LabelAs -> R.string.mail_settings_swipe_action_label_as_description
        SwipeAction.MoveTo -> R.string.mail_settings_swipe_action_move_to_description
    }
