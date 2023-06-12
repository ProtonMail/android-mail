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
import androidx.compose.ui.test.performClick
import ch.protonmail.android.mailmailbox.presentation.mailbox.MailboxTopAppBarTestTags
import ch.protonmail.android.test.ksp.annotations.AttachTo
import ch.protonmail.android.test.ksp.annotations.VerifiesOuter
import ch.protonmail.android.uitest.models.mailbox.MailboxType
import ch.protonmail.android.uitest.robot.ComposeSectionRobot
import ch.protonmail.android.uitest.robot.mailbox.MailboxRobot
import ch.protonmail.android.uitest.util.child

@AttachTo(targets = [MailboxRobot::class], identifier = "topAppBarSection")
internal class MailboxTopBarSection : ComposeSectionRobot() {

    private val rootItem = composeTestRule.onNodeWithTag(MailboxTopAppBarTestTags.RootItem, useUnmergedTree = true)

    private val locationLabel = rootItem.child {
        hasTestTag(MailboxTopAppBarTestTags.LocationLabel)
    }

    private val composerButton = rootItem.child {
        hasTestTag(MailboxTopAppBarTestTags.ComposerButton)
    }

    fun tapComposerIcon() = apply {
        composerButton.performClick()
    }

    @VerifiesOuter
    inner class Verify {

        fun isMailbox(type: MailboxType) {
            locationLabel.assertTextEquals(type.name)
        }
    }
}
