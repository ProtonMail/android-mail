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

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import ch.protonmail.android.mailmailbox.presentation.mailbox.MailboxScreenTestTags
import ch.protonmail.android.test.ksp.annotations.AsDsl
import ch.protonmail.android.test.ksp.annotations.VerifiesOuter
import ch.protonmail.android.uitest.robot.ComposeRobot
import ch.protonmail.android.uitest.util.awaitDisplayed

@AsDsl
internal class MailboxRobot : ComposeRobot() {

    private val rootItem = composeTestRule.onNodeWithTag(MailboxScreenTestTags.Root)

    @VerifiesOuter
    inner class Verify {

        fun isShown() {
            rootItem.awaitDisplayed(composeTestRule).assertIsDisplayed()
        }

        // TODO remove as part of MAILANDR-578
        fun itemLabelIsDisplayed(label: String) {
            composeTestRule.onNodeWithText(label)
                .awaitDisplayed(composeTestRule)
                .assertIsDisplayed()
        }

        // TODO remove as part of MAILANDR-578
        fun itemWithSubjectIsDisplayed(subject: String) {
            composeTestRule.onNodeWithText(subject)
                .awaitDisplayed(composeTestRule)
                .assertIsDisplayed()
        }
    }
}
