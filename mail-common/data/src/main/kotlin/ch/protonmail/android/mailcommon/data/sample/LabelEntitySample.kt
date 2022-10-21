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

package ch.protonmail.android.mailcommon.data.sample

import ch.protonmail.android.mailcommon.domain.sample.LabelSample
import me.proton.core.label.data.local.LabelEntity
import me.proton.core.label.domain.entity.Label

object LabelEntitySample {

    val Archive = build(LabelSample.Archive)
    val Document = build(LabelSample.Document)

    fun build(label: Label = LabelSample.build()) = LabelEntity(
        color = label.color,
        isExpanded = label.isExpanded,
        isNotified = label.isNotified,
        isSticky = label.isSticky,
        labelId = label.labelId,
        name = label.name,
        order = label.order,
        parentId = label.parentId?.id,
        path = label.path,
        type = label.type.value,
        userId = label.userId
    )
}
