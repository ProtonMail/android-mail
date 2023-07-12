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

import ch.protonmail.android.mailcommon.presentation.model.AvatarUiModel
import ch.protonmail.android.mailcommon.presentation.usecase.GetInitial
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.mailmailbox.domain.model.MailboxItem
import ch.protonmail.android.mailmailbox.domain.model.MailboxItemType
import javax.inject.Inject

class MailboxAvatarUiModelMapper @Inject constructor(
    private val getInitial: GetInitial
) {

    operator fun invoke(mailboxItem: MailboxItem, participantsResolvedNames: List<String>): AvatarUiModel {
        return if (mailboxItem.isDraftInMessageMode()) {
            AvatarUiModel.DraftIcon
        } else {
            val firstResolvedName = participantsResolvedNames.firstOrNull() ?: UnknownParticipant
            val initial = getInitial(firstResolvedName) ?: UnknownParticipant
            AvatarUiModel.ParticipantInitial(initial)
        }
    }

    private fun MailboxItem.isDraftInMessageMode() = type == MailboxItemType.Message &&
        labelIds.any { it == SystemLabelId.AllDrafts.labelId }

    companion object {
        private const val UnknownParticipant = "?"
    }
}
