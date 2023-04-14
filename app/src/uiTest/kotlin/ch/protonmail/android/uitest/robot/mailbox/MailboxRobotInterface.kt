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
package ch.protonmail.android.uitest.robot.mailbox

import androidx.compose.ui.test.junit4.ComposeContentTestRule
import ch.protonmail.android.uitest.models.mailbox.MailboxListItemEntry
import ch.protonmail.android.uitest.models.mailbox.MailboxListItemEntryModel

interface MailboxRobotInterface {

    val composeTestRule: ComposeContentTestRule get() = TODO("Override in subclass")

    fun clickMessageByPosition(position: Int) = apply {
        val model = MailboxListItemEntryModel(position)

        model.click()
    }

    interface Verify {

        fun listItemsAreShown(vararg inboxEntries: MailboxListItemEntry) {
            for (entry in inboxEntries) {
                val model = MailboxListItemEntryModel(entry.index)

                model.hasAvatar(entry.avatarInitial)
                    .hasParticipants(entry.participants)
                    .hasSubject(entry.subject)
                    .hasDate(entry.date)

                entry.count?.let { model.hasCount(it) } ?: model.hasNoCount()
            }
        }
    }
}
