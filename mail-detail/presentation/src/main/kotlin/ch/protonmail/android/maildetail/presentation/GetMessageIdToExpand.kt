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

package ch.protonmail.android.maildetail.presentation

import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.model.MessageWithLabels
import me.proton.core.label.domain.entity.LabelId
import javax.inject.Inject

class GetMessageIdToExpand @Inject constructor() {

    operator fun invoke(
        messages: List<MessageWithLabels>,
        filterByLocation: LabelId?,
        shouldHideMessagesBasedOnTrashFilter: Boolean
    ): MessageId? {
        val filteredMessages = if (filterByLocation != null) {
            messages.filterMessages(filterByLocation, shouldHideMessagesBasedOnTrashFilter)
        } else {
            messages
        }

        return when {
            filteredMessages.isLatestMessageRead() -> filteredMessages.latestNonDraftMessageId()
            else -> filteredMessages.oldestNonDraftUnreadMessageId()
        }
    }

    private fun List<MessageWithLabels>.filterMessages(
        filterByLocation: LabelId?,
        shouldHideMessagesBasedOnTrashFilter: Boolean
    ) = if (shouldHideMessagesBasedOnTrashFilter && filterByLocation != SystemLabelId.Trash.labelId) {
        filter {
            it.message.labelIds.contains(SystemLabelId.Trash.labelId).not() &&
                it.message.labelIds.contains(filterByLocation)
        }
    } else {
        filter { it.message.labelIds.contains(filterByLocation) }
    }

    private fun List<MessageWithLabels>.isLatestMessageRead() = filterNot { it.message.isDraft() }
        .sortedByDescending { it.message.order }
        .maxByOrNull { it.message.time }
        ?.message
        ?.read
        ?: false

    private fun List<MessageWithLabels>.oldestNonDraftUnreadMessageId() = this
        .filterNot { it.message.isDraft() }
        .filterNot { it.message.read }
        .sortedBy { it.message.order }
        .minByOrNull { it.message.time }
        ?.message
        ?.messageId

    private fun List<MessageWithLabels>.latestNonDraftMessageId() = this
        .filterNot { it.message.isDraft() }
        .sortedByDescending { it.message.order }
        .maxByOrNull { it.message.time }
        ?.message
        ?.messageId

}
