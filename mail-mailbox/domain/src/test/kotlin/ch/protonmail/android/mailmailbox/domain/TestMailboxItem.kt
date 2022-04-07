/*
 * Copyright (c) 2021 Proton Technologies AG
 * This file is part of Proton Technologies AG and ProtonMail.
 *
 * ProtonMail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonMail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonMail.  If not, see <https://www.gnu.org/licenses/>.
 */

package ch.protonmail.android.mailmailbox.domain

import ch.protonmail.android.mailconversation.domain.entity.Recipient
import ch.protonmail.android.mailmailbox.domain.model.MailboxItem
import ch.protonmail.android.mailmailbox.domain.model.MailboxItemType
import me.proton.core.domain.entity.UserId

fun getMailboxItem(
    userId: UserId,
    id: String,
    time: Long,
    labelIds: List<String>,
    type: MailboxItemType,
) = MailboxItem(
    type = type,
    id = id,
    userId = userId,
    time = time,
    size = 1000,
    order = 1000,
    read = true,
    subject = "subject",
    senders = listOf(Recipient("address", "name")),
    recipients = emptyList(),
    labels = labelIds.map { getLabel(userId = userId, id = it) }
)
