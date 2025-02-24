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

package ch.protonmail.android.mailcommon.presentation.sample

import ch.protonmail.android.mailcommon.domain.model.Action
import ch.protonmail.android.mailcommon.presentation.model.ActionUiModel
import ch.protonmail.android.mailcommon.presentation.model.contentDescription
import ch.protonmail.android.mailcommon.presentation.model.description
import ch.protonmail.android.mailcommon.presentation.model.iconDrawable

object ActionUiModelSample {

    val Reply: ActionUiModel =
        build(Action.Reply)

    val ReplyAll: ActionUiModel =
        build(Action.Reply)

    val Forward: ActionUiModel =
        build(Action.Reply)

    val Archive: ActionUiModel =
        build(Action.Archive)

    val MarkUnread: ActionUiModel =
        build(Action.MarkUnread)

    val CustomizeToolbar: ActionUiModel =
        build(Action.OpenCustomizeToolbar)

    val Trash: ActionUiModel =
        build(Action.Trash)

    val ReportPhishing: ActionUiModel =
        build(Action.ReportPhishing)

    fun build(action: Action) = ActionUiModel(
        action = action,
        icon = action.iconDrawable(),
        description = action.description(),
        contentDescription = action.contentDescription()
    )
}
