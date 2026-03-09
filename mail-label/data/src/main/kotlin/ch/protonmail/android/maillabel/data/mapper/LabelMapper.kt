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

package ch.protonmail.android.maillabel.data.mapper

import ch.protonmail.android.mailcommon.data.mapper.LocalLabelId
import ch.protonmail.android.mailcommon.data.mapper.LocalSystemLabel
import ch.protonmail.android.maillabel.domain.model.Label
import ch.protonmail.android.maillabel.domain.model.LabelId
import ch.protonmail.android.maillabel.domain.model.LabelType
import ch.protonmail.android.maillabel.domain.model.LabelWithSystemLabelId
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import timber.log.Timber
import uniffi.mail_uniffi.InlineCustomLabel
import uniffi.mail_uniffi.LabelDescription
import uniffi.mail_uniffi.MovableSystemFolder
import uniffi.mail_uniffi.SidebarCustomFolder
import uniffi.mail_uniffi.SidebarCustomLabel
import uniffi.mail_uniffi.SidebarSystemLabel

fun LabelId.toLocalLabelId(): LocalLabelId = LocalLabelId(this.id.toULong())
fun LocalLabelId.toLabelId(): LabelId = LabelId(this.value.toString())
fun LabelDescription.toLabelType(): LabelType {
    return when (this) {
        is LabelDescription.Label -> LabelType.MessageLabel
        is LabelDescription.System -> LabelType.SystemFolder
        is LabelDescription.ContactGroup -> LabelType.ContactGroup
        is LabelDescription.Folder -> LabelType.MessageFolder
    }
}

fun SidebarCustomFolder.toLabel(): Label {
    return Label(
        labelId = this.id.toLabelId(),
        parentId = this.parentId?.toLabelId(),
        name = this.name,
        type = this.description.toLabelType(),
        path = this.path ?: "",
        color = this.color?.value,
        order = this.displayOrder.toInt(),
        isNotified = this.notify,
        isExpanded = this.expanded,
        isSticky = this.sticky

    )
}
fun SidebarCustomLabel.toLabel(): Label {
    return Label(
        labelId = this.id.toLabelId(),
        parentId = null,
        name = this.name,
        type = this.description.toLabelType(),
        path = "",
        color = this.color.value,
        order = this.displayOrder.toInt(),
        isNotified = this.notify,
        isExpanded = null,
        isSticky = this.sticky

    )
}

fun SidebarSystemLabel.toLabelWithSystemLabelId(): LabelWithSystemLabelId {
    val systemLabelDescription = this.description
    if (systemLabelDescription !is LabelDescription.System) {
        Timber.w("rust-label: Mapping a non-system labelId to a system one. This is illegal.")
        throw IllegalStateException("Mapping a non-system label to system")
    }
    return LabelWithSystemLabelId(
        Label(
            labelId = this.id.toLabelId(),
            parentId = null,
            name = this.name,
            type = systemLabelDescription.toLabelType(),
            path = "",
            color = "",
            order = this.displayOrder.toInt(),
            isNotified = this.notify,
            isExpanded = null,
            isSticky = this.sticky
        ),
        systemLabelDescription.v1?.toSystemLabel() ?: SystemLabelId.AllMail
    )
}

fun LocalSystemLabel.toSystemLabel() = when (this) {
    LocalSystemLabel.INBOX -> SystemLabelId.Inbox
    LocalSystemLabel.ALL_DRAFTS -> SystemLabelId.AllDrafts
    LocalSystemLabel.ALL_SENT -> SystemLabelId.AllSent
    LocalSystemLabel.TRASH -> SystemLabelId.Trash
    LocalSystemLabel.SPAM -> SystemLabelId.Spam
    LocalSystemLabel.ALL_MAIL -> SystemLabelId.AllMail
    LocalSystemLabel.ARCHIVE -> SystemLabelId.Archive
    LocalSystemLabel.SENT -> SystemLabelId.Sent
    LocalSystemLabel.DRAFTS -> SystemLabelId.Drafts
    LocalSystemLabel.OUTBOX -> SystemLabelId.Outbox
    LocalSystemLabel.STARRED -> SystemLabelId.Starred
    LocalSystemLabel.SCHEDULED -> SystemLabelId.AllScheduled
    LocalSystemLabel.ALMOST_ALL_MAIL -> SystemLabelId.AlmostAllMail
    LocalSystemLabel.SNOOZED -> SystemLabelId.Snoozed
    LocalSystemLabel.CATEGORY_SOCIAL,
    LocalSystemLabel.CATEGORY_PROMOTIONS,
    LocalSystemLabel.CATERGORY_UPDATES,
    LocalSystemLabel.CATEGORY_FORUMS,
    LocalSystemLabel.BLOCKED,
    LocalSystemLabel.PINNED,
    LocalSystemLabel.CATEGORY_DEFAULT -> {
        Timber.w("rust-label: mapping from unknown system label ID $this. Fallback to all mail")
        SystemLabelId.AllMail
    }
}

fun SystemLabelId.toLocalSystemLabel() = when (this) {
    SystemLabelId.Inbox -> LocalSystemLabel.INBOX
    SystemLabelId.AllDrafts -> LocalSystemLabel.ALL_DRAFTS
    SystemLabelId.AllSent -> LocalSystemLabel.ALL_SENT
    SystemLabelId.Trash -> LocalSystemLabel.TRASH
    SystemLabelId.Spam -> LocalSystemLabel.SPAM
    SystemLabelId.AllMail -> LocalSystemLabel.ALL_MAIL
    SystemLabelId.Archive -> LocalSystemLabel.ARCHIVE
    SystemLabelId.Sent -> LocalSystemLabel.SENT
    SystemLabelId.Drafts -> LocalSystemLabel.DRAFTS
    SystemLabelId.Outbox -> LocalSystemLabel.OUTBOX
    SystemLabelId.Starred -> LocalSystemLabel.STARRED
    SystemLabelId.AllScheduled -> LocalSystemLabel.SCHEDULED
    SystemLabelId.AlmostAllMail -> LocalSystemLabel.ALMOST_ALL_MAIL
    SystemLabelId.Snoozed -> LocalSystemLabel.SNOOZED
}

fun MovableSystemFolder.toSystemLabel() = when (this) {
    MovableSystemFolder.INBOX -> SystemLabelId.Inbox
    MovableSystemFolder.TRASH -> SystemLabelId.Trash
    MovableSystemFolder.SPAM -> SystemLabelId.Spam
    MovableSystemFolder.ARCHIVE -> SystemLabelId.Archive
}

fun InlineCustomLabel.toLabel() = Label(
    labelId = this.id.toLabelId(),
    parentId = null,
    name = this.name,
    type = LabelType.MessageLabel,
    path = "",
    color = this.color.value,
    order = 0,
    isNotified = null,
    isExpanded = null,
    isSticky = null
)
