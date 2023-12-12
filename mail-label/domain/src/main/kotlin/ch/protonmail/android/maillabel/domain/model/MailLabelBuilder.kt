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

package ch.protonmail.android.maillabel.domain.model

import android.graphics.Color
import ch.protonmail.android.maillabel.domain.extension.normalizeColorHex
import me.proton.core.label.domain.entity.Label
import me.proton.core.label.domain.entity.LabelId
import me.proton.core.label.domain.entity.LabelType

fun List<SystemLabelId>.toMailLabelSystem(): List<MailLabel.System> = map { it.toMailLabelSystem() }

fun List<Label>.toMailLabelCustom(): List<MailLabel.Custom> {
    val labelById = associateBy { it.labelId }
    val groupByParentId = groupBy { it.parentId }
    val mailLabels = mutableMapOf<LabelId, MailLabel.Custom>()
    fun getChildren(labelId: LabelId): List<Label> = groupByParentId[labelId].orEmpty().sortedBy { it.order }
    fun getMailLabel(labelId: LabelId): MailLabel.Custom = mailLabels.getOrPut(labelId) {
        val label = requireNotNull(labelById[labelId])
        when (label.type) {
            LabelType.MessageLabel -> label.toMailLabelCustom(::getMailLabel, ::getChildren)
            LabelType.MessageFolder -> label.toMailLabelCustom(::getMailLabel, ::getChildren)
            LabelType.ContactGroup -> throw UnsupportedOperationException()
            LabelType.SystemFolder -> throw UnsupportedOperationException()
        }
    }
    return mapNotNull { label ->
        if (label.parentId == null || this.any { label.parentId == it.labelId }) {
            getMailLabel(label.labelId)
        } else null
    }.sortedBy { it.order }.orderByParent()
}

private fun Label.toMailLabelCustom(
    getMailLabel: (LabelId) -> MailLabel.Custom,
    getChildren: (LabelId) -> List<Label>
): MailLabel.Custom {
    val parent = parentId?.let(getMailLabel)
    val level = parent?.level?.plus(1) ?: 0
    return MailLabel.Custom(
        id = labelId.toMailLabelId(type),
        text = name,
        color = Color.parseColor(color.normalizeColorHex()),
        parent = parent,
        isExpanded = parent?.isExpanded ?: true && isExpanded ?: true,
        level = level,
        order = order,
        children = getChildren(labelId).map { it.labelId.toMailLabelId(type) }
    )
}

private fun List<MailLabel.Custom>.orderByParent(): List<MailLabel.Custom> {
    val groupByParent = groupBy { it.parent }
    fun List<MailLabel.Custom>.itemsAndChildren(): List<MailLabel.Custom> = fold(emptyList()) { acc, item ->
        acc + item + groupByParent[item].orEmpty().itemsAndChildren()
    }
    return groupByParent[null].orEmpty().itemsAndChildren()
}

private fun LabelId.toMailLabelId(type: LabelType) = when (type) {
    LabelType.MessageLabel -> MailLabelId.Custom.Label(this)
    LabelType.MessageFolder -> MailLabelId.Custom.Folder(this)
    LabelType.ContactGroup -> throw UnsupportedOperationException()
    LabelType.SystemFolder -> throw UnsupportedOperationException()
}
