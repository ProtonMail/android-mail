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

package ch.protonmail.android.mailmailbox.domain.usecase

import ch.protonmail.android.mailcommon.domain.model.Action
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import me.proton.core.label.domain.entity.LabelId
import javax.inject.Inject

class GetMailboxBottomSheetActions @Inject constructor() {

    operator fun invoke(currentMailLabel: LabelId): List<Action> {
        val isTrash = currentMailLabel == SystemLabelId.Trash.labelId
        val isSpam = currentMailLabel == SystemLabelId.Spam.labelId
        return listOfNotNull(
            Action.MarkRead,
            Action.MarkUnread,
            Action.Trash.takeIf { !isTrash },
            Action.Delete.takeIf { isTrash || isSpam },
            Action.Move,
            Action.Label,
            Action.Spam.takeIf { !isSpam },
            Action.Star,
            Action.Unstar,
            Action.Archive,
            Action.OpenCustomizeToolbar
        )
    }
}
