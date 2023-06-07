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

package ch.protonmail.android.uitest.robot.mailbox.section

import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTouchInput
import androidx.test.espresso.action.ViewActions.swipeUp
import ch.protonmail.android.mailmailbox.presentation.mailbox.MailboxScreenTestTags
import ch.protonmail.android.test.ksp.annotations.AttachTo
import ch.protonmail.android.test.ksp.annotations.VerifiesOuter
import ch.protonmail.android.uitest.models.mailbox.MailboxListItemEntry
import ch.protonmail.android.uitest.models.mailbox.MailboxListItemEntryModel
import ch.protonmail.android.uitest.robot.ComposeSectionRobot
import ch.protonmail.android.uitest.robot.mailbox.MailboxRobot
import ch.protonmail.android.uitest.util.awaitDisplayed
import kotlin.time.Duration.Companion.seconds

@AttachTo(targets = [MailboxRobot::class], identifier = "listSection")
internal class MailboxListSection : ComposeSectionRobot() {

    private val messagesList = composeTestRule.onNodeWithTag(MailboxScreenTestTags.List)

    fun clickMessageByPosition(position: Int) = apply {
        val model = MailboxListItemEntryModel(position)

        model.click()
    }

    fun scrollToItemAtIndex(index: Int) {
        messagesList.performScrollToIndex(index)
    }

    fun scrollToBottom() = apply {
        messagesList.awaitDisplayed(composeTestRule, timeout = 5.seconds)
            .performTouchInput { swipeUp() }
    }

    @VerifiesOuter
    inner class Verify {

        fun listItemsAreShown(vararg mailboxItemEntries: MailboxListItemEntry) {
            for (entry in mailboxItemEntries) {
                val model = MailboxListItemEntryModel(entry.index)

                model.hasAvatar(entry.avatarInitial)
                    .hasParticipants(entry.participants)
                    .hasSubject(entry.subject)
                    .hasDate(entry.date)

                entry.locationIcons?.let { model.hasLocationIcons(it) } ?: model.hasNoLocationIcons()
                entry.labels?.let { model.hasLabels(it) } ?: model.hasNoLabels()
                entry.count?.let { model.hasCount(it) } ?: model.hasNoCount()
            }
        }

        fun unreadItemAtPosition(position: Int) {
            val model = MailboxListItemEntryModel(position)

            model.assertUnread()
        }

        fun readItemAtPosition(position: Int) {
            val model = MailboxListItemEntryModel(position)

            model.assertRead()
        }
    }
}
