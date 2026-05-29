/*
 * Copyright (c) 2026 Proton Technologies AG
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

package ch.protonmail.android.mailmessage.data.mapper

import ch.protonmail.android.maillabel.data.mapper.toLabelId
import ch.protonmail.android.maillabel.domain.model.CategorySystemLabelId
import ch.protonmail.android.maillabel.domain.model.MailLabel
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import android.graphics.Color
import timber.log.Timber
import uniffi.mail_uniffi.CategoryDestination
import uniffi.mail_uniffi.CustomFolderDestination
import uniffi.mail_uniffi.Id
import uniffi.mail_uniffi.InboxDestination
import uniffi.mail_uniffi.LabelColor
import uniffi.mail_uniffi.MovableCategoryFolder
import uniffi.mail_uniffi.MovableSystemFolder
import uniffi.mail_uniffi.MoveDestination
import uniffi.mail_uniffi.SystemFolderDestination
import javax.inject.Inject

class MoveDestinationMapper @Inject constructor() {

    operator fun invoke(destinations: List<MoveDestination>): List<MailLabel> {
        val systemLabels = destinations.flatMap { moveDestination ->
            when (moveDestination) {
                is MoveDestination.SystemFolder -> listOfNotNull(moveDestination.v1.toSystemMailLabelOrNull())
                is MoveDestination.Inbox -> moveDestination.v1.toSystemMailLabels()
                is MoveDestination.CustomFolder -> emptyList()
            }
        }

        val customFolderActions = destinations.filterIsInstance<MoveDestination.CustomFolder>().map { it.v1 }
        fun toFolderId(localId: Id) = MailLabelId.Custom.Folder(localId.toLabelId())

        fun buildCustomFolders(
            folder: CustomFolderDestination,
            parent: MailLabel.Custom?,
            level: Int,
            order: Int,
            acc: MutableList<MailLabel.Custom>,
            visited: MutableSet<Id>
        ) {
            if (!visited.add(folder.localId)) return

            val current = MailLabel.Custom(
                id = toFolderId(folder.localId),
                parent = parent,
                text = folder.name,
                color = folder.color.toColorIntOrNull(),
                isExpanded = true,
                level = level,
                order = order,
                children = folder.children.map { child -> toFolderId(child.localId) }
            )
            acc += current
            folder.children.forEachIndexed { index, child ->
                buildCustomFolders(child, current, level + 1, index, acc, visited)
            }
        }

        val customLabels = mutableListOf<MailLabel.Custom>()
        val visited = mutableSetOf<Id>()
        customFolderActions.forEachIndexed { index, root ->
            buildCustomFolders(root, parent = null, level = 0, order = index, customLabels, visited)
        }

        return (systemLabels + customLabels).distinctBy { it.id }
    }
}

private fun InboxDestination.toSystemMailLabels(): List<MailLabel.System> = listOf(
    MailLabel.System(
        id = MailLabelId.System(localId.toLabelId()),
        systemLabelId = SystemLabelId.Inbox,
        order = 0,
        categories = categories.mapNotNull { it.toCategoryMailLabelOrNull() }
            .takeIf { it.isNotEmpty() }
    )
)

private fun SystemFolderDestination.toSystemMailLabelOrNull(): MailLabel.System? {
    val systemLabelId = name.toSystemLabelIdOrNull() ?: return null
    return MailLabel.System(
        id = MailLabelId.System(localId.toLabelId()),
        systemLabelId = systemLabelId,
        order = 0
    )
}

private fun CategoryDestination.toCategoryMailLabelOrNull(): MailLabel.Category? {
    val categorySystemLabelId = name.toCategorySystemLabelIdOrNull() ?: return null
    return MailLabel.Category(
        id = MailLabelId.Category(localId.toLabelId()),
        categorySystemLabelId = categorySystemLabelId,
        order = 0
    )
}

private fun MovableCategoryFolder.toCategorySystemLabelIdOrNull(): CategorySystemLabelId? = when (this) {
    MovableCategoryFolder.CATEGORY_SOCIAL -> CategorySystemLabelId.Social
    MovableCategoryFolder.CATEGORY_PROMOTIONS -> CategorySystemLabelId.Promotions
    MovableCategoryFolder.CATEGORY_UPDATES -> CategorySystemLabelId.Updates
    MovableCategoryFolder.CATEGORY_DEFAULT -> CategorySystemLabelId.Primary
    MovableCategoryFolder.CATEGORY_NEWSLETTER -> CategorySystemLabelId.Newsletter
    MovableCategoryFolder.CATEGORY_TRANSACTIONS -> CategorySystemLabelId.Transactions
}

private fun MovableSystemFolder.toSystemLabelIdOrNull(): SystemLabelId? = when (this) {
    MovableSystemFolder.INBOX -> SystemLabelId.Inbox
    MovableSystemFolder.TRASH -> SystemLabelId.Trash
    MovableSystemFolder.SPAM -> SystemLabelId.Spam
    MovableSystemFolder.ARCHIVE -> SystemLabelId.Archive
}

private fun LabelColor?.toColorIntOrNull(): Int? = this?.value?.let { rawColor ->
    runCatching { Color.parseColor(rawColor.normalizeColorHex()) }
        .onFailure { Timber.w("rust-actions-mapper: invalid custom folder color '$rawColor'") }
        .getOrNull()
}

private fun String.normalizeColorHex(): String {
    val shortHexLength = 4 // e.g. #fff
    return if (length == shortHexLength) {
        val r = this[1]
        val g = this[2]
        val b = this[3]
        "#$r$r$g$g$b$b"
    } else {
        this
    }
}

