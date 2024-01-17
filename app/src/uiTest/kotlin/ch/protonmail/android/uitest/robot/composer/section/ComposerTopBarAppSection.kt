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

package ch.protonmail.android.uitest.robot.composer.section

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import ch.protonmail.android.mailcomposer.presentation.ui.ComposerTestTags
import ch.protonmail.android.test.ksp.annotations.AttachTo
import ch.protonmail.android.test.ksp.annotations.VerifiesOuter
import ch.protonmail.android.uitest.robot.ComposeSectionRobot
import ch.protonmail.android.uitest.robot.composer.ComposerRobot
import ch.protonmail.android.uitest.util.awaitHidden
import ch.protonmail.android.uitest.util.child

@AttachTo(
    targets = [
        ComposerRobot::class
    ],
    identifier = "topAppBarSection"
)
internal class ComposerTopBarAppSection : ComposeSectionRobot() {

    private val rootItem = composeTestRule.onNodeWithTag(
        ComposerTestTags.TopAppBar
    )

    private val closeButton = rootItem.child {
        hasTestTag(ComposerTestTags.CloseButton)
    }

    private val attachmentsButton = rootItem.child {
        hasTestTag(ComposerTestTags.AttachmentsButton)
    }

    private val sendButton = rootItem.child {
        hasTestTag(ComposerTestTags.SendButton)
    }

    fun tapCloseButton() = apply {
        closeButton.performClick()
        rootItem.awaitHidden()
    }

    fun tapAttachmentsButton() = apply {
        attachmentsButton.performClick()
    }

    fun tapSendButton() = apply {
        sendButton.performClick()
    }

    @VerifiesOuter
    inner class Verify {

        fun isSendButtonEnabled() = apply {
            sendButton.assertIsEnabled()
        }

        fun isSendButtonDisabled() = apply {
            sendButton.assertIsNotEnabled()
        }
    }
}
