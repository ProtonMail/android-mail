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

package ch.protonmail.android.mailmailbox.presentation

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import ch.protonmail.android.mailconversation.domain.Conversation
import ch.protonmail.android.mailmessage.domain.model.MailLocation

@Stable
class MailboxState(
    val loading: Boolean = false,
    val currentLocations: Set<MailLocation> = emptySet(),
    val currentLocationsItems: List<Conversation> = emptyList(),
    val inboxUnreadCount: Int? = 1,
    val draftsUnreadCount: Int? = null,
    val sentUnreadCount: Int? = null,
    val starredUnreadCount: Int? = 1,
    val archiveUnreadCount: Int? = null,
    val spamUnreadCount: Int? = null,
    val trashUnreadCount: Int? = null,
    val allMailUnreadCount: Int? = 1,
    val folders: List<Folder> = listOf(
        Folder("1", "Folder 1", Color.Red)
    ),
    val labels: List<Label> = listOf(
        Label("1", "Label 1", Color.Cyan),
        Label("2", "Label 2", Color.Yellow)
    ),
) {
    fun isLocationSelected(location: MailLocation) = currentLocations.contains(location)
}

data class Folder(
    val id: String,
    val text: String,
    val color: Color,
)

data class Label(
    val id: String,
    val text: String,
    val color: Color,
)
