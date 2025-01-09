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

package ch.protonmail.android.mailmessage.presentation.mapper

import ch.protonmail.android.mailcommon.domain.model.Action
import ch.protonmail.android.mailcommon.presentation.model.ActionUiModel
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailmessage.presentation.R
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.DetailMoreActionsBottomSheetState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import javax.inject.Inject

class DetailMoreActionsBottomSheetUiMapper @Inject constructor() {

    fun toHeaderUiModel(
        messageSender: String,
        messageSubject: String,
        messageId: String
    ) = DetailMoreActionsBottomSheetState.MessageDataUiModel(
        TextUiModel.Text(messageSubject),
        TextUiModel.TextResWithArgs(R.string.bottom_sheet_more_header_message_from, listOf(messageSender)),
        messageId
    )

    fun mapMoreActionUiModels(): ImmutableList<ActionUiModel> {
        return mutableListOf<ActionUiModel>().apply {
            add(ActionUiModel(Action.Reply))
            add(ActionUiModel(Action.ReplyAll))
            add(ActionUiModel(Action.Forward))
            add(ActionUiModel(Action.MarkUnread))
            add(ActionUiModel(Action.Label))
            add(ActionUiModel(Action.ViewInLightMode))
            add(ActionUiModel(Action.ViewInDarkMode))
            add(ActionUiModel(Action.Trash))
            add(ActionUiModel(Action.Archive))
            add(ActionUiModel(Action.Spam))
            add(ActionUiModel(Action.Move))
            add(ActionUiModel(Action.Print))
            add(ActionUiModel(Action.ReportPhishing))
        }.toImmutableList()
    }
}
