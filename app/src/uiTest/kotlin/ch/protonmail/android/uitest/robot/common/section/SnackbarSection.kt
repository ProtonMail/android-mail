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

package ch.protonmail.android.uitest.robot.common.section

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import ch.protonmail.android.mailcommon.presentation.ui.CommonTestTags
import ch.protonmail.android.test.ksp.annotations.AttachTo
import ch.protonmail.android.test.ksp.annotations.VerifiesOuter
import ch.protonmail.android.uitest.models.snackbar.SnackbarTextEntry
import ch.protonmail.android.uitest.robot.ComposeSectionRobot
import ch.protonmail.android.uitest.robot.detail.ConversationDetailRobot
import ch.protonmail.android.uitest.robot.mailbox.MailboxRobot
import ch.protonmail.android.uitest.util.assertions.hasAnyChildWith

@AttachTo(targets = [MailboxRobot::class, ConversationDetailRobot::class])
internal class SnackbarSection : ComposeSectionRobot() {

    private val snackbarHost = composeTestRule.onNodeWithTag(CommonTestTags.SnackbarHost)

    @VerifiesOuter
    inner class Verify {

        fun hasMessage(text: SnackbarTextEntry) = apply {
            // The actual text node is not an immediate child, so the hierarchy needs to be traversed.
            snackbarHost.hasAnyChildWith(hasText(text.value))
        }
    }
}
