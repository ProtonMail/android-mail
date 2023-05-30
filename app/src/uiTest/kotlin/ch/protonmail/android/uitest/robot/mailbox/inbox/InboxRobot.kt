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

package ch.protonmail.android.uitest.robot.mailbox.inbox

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import ch.protonmail.android.maillabel.R
import ch.protonmail.android.mailmailbox.presentation.mailbox.MailboxScreenTestTags
import ch.protonmail.android.mailmailbox.presentation.mailbox.UnreadItemsFilterTestTags
import ch.protonmail.android.test.ksp.annotations.AsDsl
import ch.protonmail.android.test.ksp.annotations.VerifiesOuter
import ch.protonmail.android.uitest.models.mailbox.MailboxListItemEntryModel
import ch.protonmail.android.uitest.robot.ComposeRobot
import ch.protonmail.android.uitest.util.onAllNodesWithText

@AsDsl
internal class InboxRobot : ComposeRobot() {

    private val unreadFilterChip = composeTestRule
        .onNodeWithTag(UnreadItemsFilterTestTags.UnreadFilterChip)

    fun filterUnreadMessages() = apply {
        unreadFilterChip.performClick()
    }

    @VerifiesOuter
    inner class Verify {

        fun mailboxScreenDisplayed() {
            composeTestRule.waitUntil(timeoutMillis = 60_000) {
                composeTestRule.onAllNodesWithText(R.string.label_title_inbox)
                    .fetchSemanticsNodes(false)
                    .isNotEmpty()
            }

            composeTestRule.onNodeWithTag(MailboxScreenTestTags.Root).assertIsDisplayed()
        }

        fun unreadFilterIsDisplayed() {
            unreadFilterChip
                .assertIsDisplayed()
                .assertIsNotSelected()
        }

        fun unreadFilterIsSelected() {
            unreadFilterChip
                .assertIsDisplayed()
                .assertIsSelected()
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
