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

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import ch.protonmail.android.mailmailbox.presentation.mailbox.MailboxTopAppBarTestTags
import ch.protonmail.android.mailmailbox.presentation.mailbox.MailboxTopAppBarTestTags.NavigationButton
import ch.protonmail.android.test.ksp.annotations.AttachTo
import ch.protonmail.android.test.ksp.annotations.VerifiesOuter
import ch.protonmail.android.uitest.models.mailbox.MailboxType
import ch.protonmail.android.uitest.robot.ComposeSectionRobot
import ch.protonmail.android.uitest.robot.mailbox.MailboxRobot
import ch.protonmail.android.uitest.util.awaitDisplayed
import ch.protonmail.android.uitest.util.child
import ch.protonmail.android.uitest.util.getTestString
import ch.protonmail.android.test.R as testR

@AttachTo(targets = [MailboxRobot::class], identifier = "topAppBarSection")
internal class MailboxTopBarSection : ComposeSectionRobot() {

    private val rootItem = composeTestRule.onNodeWithTag(MailboxTopAppBarTestTags.RootItem, useUnmergedTree = true)

    private val navigationButton = rootItem.child {
        hasTestTag(NavigationButton)
    }

    private val hamburgerMenuButton = navigationButton.child {
        hasContentDescription(
            getTestString(testR.string.test_mailbox_toolbar_menu_button_content_description)
        )
    }

    private val exitSelectionButton = navigationButton.child {
        hasContentDescription(
            getTestString(testR.string.test_mailbox_toolbar_exit_selection_mode_button_content_description)
        )
    }

    private val locationLabel = rootItem.child {
        hasTestTag(MailboxTopAppBarTestTags.LocationLabel)
    }

    private val composerButton = rootItem.child {
        hasTestTag(MailboxTopAppBarTestTags.ComposerButton)
    }

    fun tapComposerIcon() {
        composerButton.awaitDisplayed().performClick()
    }

    fun tapExitSelectionMode() {
        exitSelectionButton.performClick()
    }

    @VerifiesOuter
    inner class Verify {

        /**
         * Verifies that the current Mailbox is of the given [MailboxType].
         *
         * A [timeout] is provided as switching Mailbox does not trigger any loaders or idling resources.
         * By waiting for the condition to be fulfilled, we prevent the automation from performing the check
         * before the Mailbox is effectively switched, avoiding unnecessary test flakiness.
         *
         * @param type the Mailbox type (Inbox, Drafts, Sent...)
         * @param timeout the max timeout for the check to be successful
         *
         */
        fun isMailbox(type: MailboxType, timeout: Long = 2_000) {
            composeTestRule.waitUntil(timeoutMillis = timeout) {
                runCatching { locationLabel.assertTextEquals(type.name) }.isSuccess
            }

            hamburgerMenuButton.awaitDisplayed()
        }

        fun isInSelectionMode(numSelected: Int, timeout: Long = 2_000) {
            composeTestRule.waitUntil(timeoutMillis = timeout) {
                runCatching { locationLabel.assertTextEquals("$numSelected Selected") }.isSuccess
            }

            hamburgerMenuButton.assertDoesNotExist()
            exitSelectionButton.awaitDisplayed()
        }
    }
}
