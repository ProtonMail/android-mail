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

package ch.protonmail.android.testdata.label.rust

import ch.protonmail.android.mailcommon.data.mapper.LocalLabelId
import ch.protonmail.android.mailcommon.data.mapper.LocalSystemLabel
import uniffi.mail_uniffi.LabelColor
import uniffi.mail_uniffi.LabelDescription
import uniffi.mail_uniffi.SidebarCustomFolder
import uniffi.mail_uniffi.SidebarCustomLabel
import uniffi.mail_uniffi.SidebarSystemLabel

object LocalLabelTestData {
    val localSystemLabelWithCount = SidebarSystemLabel(
        id = LocalLabelId(1uL),
        name = "Inbox",
        description = LabelDescription.System(LocalSystemLabel.INBOX),
        displayOrder = 1.toUInt(),
        display = false,
        notify = false,
        sticky = false,
        count = 0.toULong()
    )

    val localMessageLabelWithCount = SidebarCustomLabel(
        id = LocalLabelId(100uL),
        name = "CustomMessageLabel",
        color = LabelColor("color"),
        description = LabelDescription.Label,
        displayOrder = 1.toUInt(),
        display = false,
        notify = false,
        sticky = false,
        total = 2.toULong(),
        unread = 0.toULong()
    )

    val localMessageFolderWithCount = SidebarCustomFolder(
        id = LocalLabelId(200uL),
        name = "CustomMessageFolder",
        path = "path",
        color = LabelColor("color"),
        description = LabelDescription.Folder,
        displayOrder = 1.toUInt(),
        parentId = LocalLabelId(3uL),
        display = false,
        expanded = false,
        notify = false,
        sticky = false,
        total = 2.toULong(),
        unread = 7.toULong(),
        children = emptyList()
    )

    fun buildSystem(localSystemLabel: LocalSystemLabel) = SidebarSystemLabel(
        id = LocalLabelId(1000.toULong()),
        name = "SomeSystemFolder",
        description = LabelDescription.System(localSystemLabel),
        displayOrder = 1.toUInt(),
        display = false,
        notify = false,
        sticky = false,
        count = 7.toULong()
    )
}
