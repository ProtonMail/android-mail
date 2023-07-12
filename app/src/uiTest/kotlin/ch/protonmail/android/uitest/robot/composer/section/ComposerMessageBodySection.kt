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

import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import ch.protonmail.android.mailcomposer.presentation.ui.ComposerTestTags
import ch.protonmail.android.test.ksp.annotations.AttachTo
import ch.protonmail.android.test.ksp.annotations.VerifiesOuter
import ch.protonmail.android.uitest.robot.ComposeSectionRobot
import ch.protonmail.android.uitest.robot.composer.ComposerRobot
import ch.protonmail.android.uitest.util.getTestString
import ch.protonmail.android.test.R as testR

@AttachTo(targets = [ComposerRobot::class], identifier = "messageBodySection")
internal class ComposerMessageBodySection : ComposeSectionRobot() {

    private val messageBodyText = composeTestRule.onNodeWithTag(
        testTag = ComposerTestTags.MessageBody,
        useUnmergedTree = true
    )

    private val messageBodyPlaceholder = composeTestRule.onNodeWithTag(
        testTag = ComposerTestTags.MessageBodyPlaceholder,
        useUnmergedTree = true
    )

    fun typeMessageBody(value: String) = apply {
        messageBodyText.performTextInput(value)
    }

    @VerifiesOuter
    inner class Verify {

        fun hasPlaceholderText() = apply {
            messageBodyPlaceholder.assertTextEquals(getTestString(testR.string.test_composer_body_placeholder))
        }

        fun hasText(value: String) = apply {
            messageBodyPlaceholder.assertDoesNotExist()
            messageBodyText.assertTextEquals(value)
        }

        fun hasFocus() = apply {
            messageBodyText.assertIsFocused()
        }
    }
}
