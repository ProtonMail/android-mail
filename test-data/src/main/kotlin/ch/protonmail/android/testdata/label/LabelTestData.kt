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

package ch.protonmail.android.testdata.label

import ch.protonmail.android.maillabel.domain.model.MailLabel
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.Label
import me.proton.core.label.domain.entity.LabelId
import me.proton.core.label.domain.entity.LabelType

object LabelTestData {

    val systemLabels = listOf(
        MailLabel.System(MailLabelId.System.Inbox),
        MailLabel.System(MailLabelId.System.AllDrafts),
        MailLabel.System(MailLabelId.System.AllSent),
        MailLabel.System(MailLabelId.System.Trash),
        MailLabel.System(MailLabelId.System.Spam),
        MailLabel.System(MailLabelId.System.AllMail),
        MailLabel.System(MailLabelId.System.Archive),
        MailLabel.System(MailLabelId.System.Sent),
        MailLabel.System(MailLabelId.System.Drafts),
        MailLabel.System(MailLabelId.System.Outbox),
        MailLabel.System(MailLabelId.System.Starred)
    )

    fun buildLabel(
        userId: UserId,
        type: LabelType = LabelType.MessageLabel,
        id: String,
        name: String = id,
        order: Int = 0,
        color: String = "#338AF3",
        parentId: String? = null,
        isExpanded: Boolean? = null
    ) = Label(
        userId = userId,
        labelId = LabelId(id),
        parentId = parentId?.let { LabelId(it) },
        name = name,
        type = type,
        path = id,
        color = color,
        order = order,
        isNotified = null,
        isExpanded = isExpanded,
        isSticky = null
    )
}
