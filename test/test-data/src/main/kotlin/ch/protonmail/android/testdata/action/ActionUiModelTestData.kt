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

package ch.protonmail.android.testdata.action

import ch.protonmail.android.mailcommon.domain.model.Action
import ch.protonmail.android.mailcommon.presentation.model.ActionUiModel
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import me.proton.core.presentation.R
import ch.protonmail.android.mailcommon.R as commonRes

object ActionUiModelTestData {

    val star = ActionUiModel(
        Action.Star,
        R.drawable.ic_proton_star,
        TextUiModel(commonRes.string.action_star_description),
        TextUiModel(commonRes.string.action_star_content_description)
    )
    val delete = ActionUiModel(
        Action.Delete,
        R.drawable.ic_proton_trash,
        TextUiModel(commonRes.string.action_delete_description),
        TextUiModel(commonRes.string.action_delete_content_description)
    )
    val archive = ActionUiModel(
        Action.Archive,
        R.drawable.ic_proton_archive_box,
        TextUiModel(commonRes.string.action_archive_description),
        TextUiModel(commonRes.string.action_archive_content_description)
    )
    val markUnread = ActionUiModel(
        Action.MarkUnread,
        R.drawable.ic_proton_envelope_dot,
        TextUiModel(commonRes.string.action_mark_unread_description),
        TextUiModel(commonRes.string.action_mark_unread_content_description)
    )
    val move = ActionUiModel(
        Action.Move,
        R.drawable.ic_proton_folder_arrow_in,
        TextUiModel(commonRes.string.action_move_description),
        TextUiModel(commonRes.string.action_move_content_description)
    )
    val label = ActionUiModel(
        Action.Label,
        R.drawable.ic_proton_tag,
        TextUiModel(commonRes.string.action_label_description),
        TextUiModel(commonRes.string.action_label_content_description)
    )
    val reportPhishing = ActionUiModel(
        Action.ReportPhishing,
        R.drawable.ic_proton_hook,
        TextUiModel(commonRes.string.action_report_phishing_description),
        TextUiModel(commonRes.string.action_report_phishing_content_description)
    )
}
