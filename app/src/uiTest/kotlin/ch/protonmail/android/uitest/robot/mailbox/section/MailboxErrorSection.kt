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
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import ch.protonmail.android.mailmailbox.presentation.mailbox.MailboxScreenTestTags
import ch.protonmail.android.test.ksp.annotations.AttachTo
import ch.protonmail.android.test.ksp.annotations.VerifiesOuter
import ch.protonmail.android.uitest.robot.ComposeSectionRobot
import ch.protonmail.android.uitest.robot.mailbox.MailboxRobot
import ch.protonmail.android.uitest.util.awaitDisplayed
import ch.protonmail.android.uitest.util.child
import ch.protonmail.android.uitest.util.getTestString
import ch.protonmail.android.test.R as testR

@AttachTo(targets = [MailboxRobot::class], identifier = "fullScreenErrorSection")
internal class MailboxErrorSection : ComposeSectionRobot(), RefreshableSection {

    private val rootItem = composeTestRule.onNodeWithTag(MailboxScreenTestTags.MailboxError)
    private val errorLabel = rootItem.child { hasTestTag(MailboxScreenTestTags.MailboxErrorMessage) }

    override fun pullDownToRefresh() {
        rootItem.performTouchInput { swipeDown() }
    }

    @VerifiesOuter
    inner class Verify {

        fun isShown() {
            errorLabel
                .awaitDisplayed()
                .assertTextEquals(getTestString(testR.string.test_mailbox_error_message_generic))
        }
    }
}
