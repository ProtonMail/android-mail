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
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import ch.protonmail.android.mailmailbox.presentation.mailbox.MailboxScreenTestTags
import ch.protonmail.android.uitest.models.mailbox.MailboxListItemEntry
import ch.protonmail.android.uitest.models.mailbox.MailboxListItemEntryModel
import ch.protonmail.android.uitest.util.awaitDisplayed
import kotlin.time.Duration.Companion.seconds

internal interface MailboxRobotInterface {

    val composeTestRule: ComposeContentTestRule get() = TODO("Override in subclass")

    fun clickMessageByPosition(position: Int) = apply {
        val model = MailboxListItemEntryModel(position)

        model.click()
    }

    fun scrollToBottom() = apply {
        composeTestRule.onNodeWithTag(MailboxScreenTestTags.List)
            .awaitDisplayed(composeTestRule, timeout = 5.seconds)
            .performTouchInput { swipeUp() }
    }

    interface Verify {

        fun listItemsAreShown(vararg mailboxItemEntries: MailboxListItemEntry) {
            for (entry in mailboxItemEntries) {
                val model = MailboxListItemEntryModel(entry.index)

                model.hasAvatar(entry.avatarInitial)
                    .hasParticipants(entry.participants)
                    .hasSubject(entry.subject)
                    .hasDate(entry.date)

                entry.locationIcons?.let { model.hasLocationIcons(it) } ?: model.hasNoLocationIcons()
                entry.count?.let { model.hasCount(it) } ?: model.hasNoCount()
            }
        }
    }
}
