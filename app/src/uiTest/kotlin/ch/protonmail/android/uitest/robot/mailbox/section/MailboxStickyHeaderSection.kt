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

import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import ch.protonmail.android.mailmailbox.presentation.mailbox.UnreadItemsFilterTestTags
import ch.protonmail.android.test.ksp.annotations.AttachTo
import ch.protonmail.android.test.ksp.annotations.VerifiesOuter
import ch.protonmail.android.uitest.robot.ComposeSectionRobot
import ch.protonmail.android.uitest.robot.mailbox.MailboxRobot
import ch.protonmail.android.uitest.util.awaitDisplayed

@AttachTo(targets = [MailboxRobot::class], identifier = "stickyHeaderSection")
internal class MailboxStickyHeaderSection : ComposeSectionRobot() {

    private val unreadFilterChip = composeTestRule
        .onNodeWithTag(UnreadItemsFilterTestTags.UnreadFilterChip)

    fun filterUnreadMessages() {
        unreadFilterChip
            .awaitDisplayed()
            .performClick()
    }

    @VerifiesOuter
    inner class Verify {

        fun unreadFilterIsDisplayed() {
            unreadFilterChip
                .awaitDisplayed()
                .assertIsNotSelected()
        }

        fun unreadFilterIsSelected() {
            unreadFilterChip
                .awaitDisplayed()
                .assertIsSelected()
        }
    }
}
