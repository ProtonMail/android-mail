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
import ch.protonmail.android.mailsettings.domain.annotations.CustomizeToolbarFeatureEnabled
import javax.inject.Inject

class GetMailboxBottomSheetActions @Inject constructor(
    @CustomizeToolbarFeatureEnabled private val showCustomizeToolbarAction: Boolean
) {

    operator fun invoke(isTrashOrSpam: Boolean) = listOfNotNull(
        Action.MarkRead,
        Action.MarkUnread,
        Action.Trash,
        Action.Delete.takeIf { isTrashOrSpam },
        Action.Move,
        Action.Label,
        Action.Spam,
        Action.Star,
        Action.Unstar,
        Action.Archive,
        Action.OpenCustomizeToolbar.takeIf { showCustomizeToolbarAction }
    )
}
