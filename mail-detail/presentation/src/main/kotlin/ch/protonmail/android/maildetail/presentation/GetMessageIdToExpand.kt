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

import ch.protonmail.android.mailmessage.domain.model.MessageWithLabels
import javax.inject.Inject

class GetMessageIdToExpand @Inject constructor() {

    operator fun invoke(messages: List<MessageWithLabels>) = when {
        messages.isLatestMessageRead() -> messages.latestNonDraftMessageId()
        else -> messages.oldestNonDraftUnreadMessageId()
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
