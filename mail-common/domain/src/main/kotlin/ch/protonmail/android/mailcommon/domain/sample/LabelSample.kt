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

package ch.protonmail.android.mailcommon.domain.sample

import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.Label
import me.proton.core.label.domain.entity.LabelId
import me.proton.core.label.domain.entity.LabelType

object LabelSample {

    val Archive = build(
        labelId = LabelIdSample.Archive,
        type = LabelType.MessageFolder
    )
    val Document = build(
        labelId = LabelIdSample.Document
    )
    val Folder2021 = build(
        labelId = LabelIdSample.Folder2021,
        type = LabelType.MessageFolder
    )
    val Folder2022 = build(
        labelId = LabelIdSample.Folder2022,
        type = LabelType.MessageFolder
    )
    val Inbox = build(
        labelId = LabelIdSample.Inbox,
        type = LabelType.MessageFolder
    )
    val News = build(
        labelId = LabelIdSample.News
    )

    fun build(
        color: String = "#338AF3",
        isExpanded: Boolean? = null,
        labelId: LabelId = LabelIdSample.build(),
        name: String = labelId.id,
        order: Int = labelId.id.hashCode(),
        parentId: LabelId? = null,
        type: LabelType = LabelType.MessageLabel,
        userId: UserId = UserIdSample.Primary
    ) = Label(
        color = color,
        isExpanded = isExpanded,
        isNotified = null,
        isSticky = null,
        labelId = labelId,
        name = name,
        order = order,
        parentId = parentId,
        path = labelId.id,
        type = type,
        userId = userId
    )
}
