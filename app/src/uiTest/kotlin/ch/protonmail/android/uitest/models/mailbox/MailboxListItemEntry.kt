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

package ch.protonmail.android.uitest.models.mailbox

import ch.protonmail.android.uitest.models.avatar.AvatarInitial
import ch.protonmail.android.uitest.models.folders.MailFolderEntry
import ch.protonmail.android.uitest.models.folders.MailLabelEntry

internal data class MailboxListItemEntry(
    val index: Int,
    val avatarInitial: AvatarInitial,
    val participants: List<ParticipantEntry>,
    val locationIcons: List<MailFolderEntry>? = null,
    val labels: List<MailLabelEntry>? = null,
    val subject: String,
    val date: String,
    val count: String? = null
)
