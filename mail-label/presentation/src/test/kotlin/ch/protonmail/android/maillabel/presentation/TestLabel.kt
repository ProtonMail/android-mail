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

package ch.protonmail.android.maillabel.presentation

import ch.protonmail.android.maillabel.domain.model.MailLabel
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.Label
import me.proton.core.label.domain.entity.LabelId
import me.proton.core.label.domain.entity.LabelType

fun getLabel(
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

fun getMailLabel(
    id: MailLabelId.Custom,
    name: String = id.labelId.id,
    color: Int = 0,
    parent: MailLabel.Custom? = null,
    isExpanded: Boolean = true,
    level: Int = 0,
    order: Int = 0,
    children: List<MailLabelId.Custom> = emptyList(),
) = MailLabel.Custom(
    id = id,
    text = name,
    color = color,
    parent = parent,
    isExpanded = isExpanded,
    level = level,
    order = order,
    children = children,
)

fun getMailLabel(
    id: String,
    name: String = id,
    color: Int = 0,
    parent: MailLabel.Custom? = null,
    isExpanded: Boolean = true,
    level: Int = 0,
    order: Int = 0,
    children: List<String> = emptyList(),
) = getMailLabel(
    id = MailLabelId.Custom.Label(LabelId(id)),
    name = name,
    color = color,
    parent = parent,
    isExpanded = isExpanded,
    level = level,
    order = order,
    children = children.map { MailLabelId.Custom.Label(LabelId(it)) },
)

fun getMailFolder(
    id: String,
    name: String = id,
    color: Int = 0,
    parent: MailLabel.Custom? = null,
    isExpanded: Boolean = true,
    level: Int = 0,
    order: Int = 0,
    children: List<String> = emptyList(),
) = getMailLabel(
    id = MailLabelId.Custom.Folder(LabelId(id)),
    name = name,
    color = color,
    parent = parent,
    isExpanded = isExpanded,
    level = level,
    order = order,
    children = children.map { MailLabelId.Custom.Folder(LabelId(it)) },
)
