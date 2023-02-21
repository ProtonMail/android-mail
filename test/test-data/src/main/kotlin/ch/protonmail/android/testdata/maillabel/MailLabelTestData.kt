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

package ch.protonmail.android.testdata.maillabel

import ch.protonmail.android.maillabel.domain.model.MailLabel
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import me.proton.core.label.domain.entity.LabelId

object MailLabelTestData {

    val customLabelOne = buildCustomLabel("customLabel1")
    val customLabelTwo = buildCustomLabel("customLabel2")
    val listOfCustomLabels = listOf(
        // See LabelIdSample
        buildCustomLabel("document"),
        buildCustomLabel("Label2021"),
        buildCustomLabel("Label2022")
    )

    fun buildCustomLabel(
        id: String,
        name: String = id,
        color: Int = 0,
        parent: MailLabel.Custom? = null,
        isExpanded: Boolean = true,
        level: Int = 0,
        order: Int = 0,
        children: List<String> = emptyList()
    ) = buildCustomLabel(
        id = MailLabelId.Custom.Label(LabelId(id)),
        name = name,
        color = color,
        parent = parent,
        isExpanded = isExpanded,
        level = level,
        order = order,
        children = children.map { MailLabelId.Custom.Label(LabelId(it)) }
    )

    fun buildCustomFolder(
        id: String,
        name: String = id,
        color: Int = 0,
        parent: MailLabel.Custom? = null,
        isExpanded: Boolean = true,
        level: Int = 0,
        order: Int = 0,
        children: List<String> = emptyList()
    ) = buildCustomLabel(
        id = MailLabelId.Custom.Folder(LabelId(id)),
        name = name,
        color = color,
        parent = parent,
        isExpanded = isExpanded,
        level = level,
        order = order,
        children = children.map { MailLabelId.Custom.Folder(LabelId(it)) }
    )

    private fun buildCustomLabel(
        id: MailLabelId.Custom,
        name: String = id.labelId.id,
        color: Int = 0,
        parent: MailLabel.Custom? = null,
        isExpanded: Boolean = true,
        level: Int = 0,
        order: Int = 0,
        children: List<MailLabelId.Custom> = emptyList()
    ) = MailLabel.Custom(
        id = id,
        text = name,
        color = color,
        parent = parent,
        isExpanded = isExpanded,
        level = level,
        order = order,
        children = children
    )
}
