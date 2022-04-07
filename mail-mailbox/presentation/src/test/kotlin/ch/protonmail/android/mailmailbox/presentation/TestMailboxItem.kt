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

package ch.protonmail.android.mailmailbox.presentation

import ch.protonmail.android.mailconversation.domain.entity.Recipient
import ch.protonmail.android.mailmailbox.domain.model.MailboxItem
import ch.protonmail.android.mailmailbox.domain.model.MailboxItemType
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.Label
import me.proton.core.label.domain.entity.LabelId
import me.proton.core.label.domain.entity.LabelType

fun getMailboxItem(
    userId: UserId,
    id: String,
    type: MailboxItemType = MailboxItemType.Message,
    order: Long = 1000,
    time: Long = 1000,
    labels: List<Label> = listOf(getLabel(userId, LabelType.MessageLabel, "0")),
) = MailboxItem(
    type = type,
    id = id,
    userId = userId,
    time = time,
    size = 1000,
    order = order,
    read = false,
    labels = labels,
    subject = "subject",
    senders = listOf(Recipient("address", "name")),
    recipients = emptyList()
)

fun getLabel(
    userId: UserId,
    type: LabelType = LabelType.MessageLabel,
    id: String,
    name: String = id,
) = Label(
    userId = userId,
    labelId = LabelId(id),
    parentId = null,
    name = name,
    type = type,
    path = id,
    color = "color",
    order = 0,
    isNotified = null,
    isExpanded = null,
    isSticky = null
)
