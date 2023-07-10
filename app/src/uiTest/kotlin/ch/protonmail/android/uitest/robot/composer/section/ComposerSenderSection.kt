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

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import ch.protonmail.android.mailcomposer.presentation.ui.ComposerTestTags
import ch.protonmail.android.mailcomposer.presentation.ui.PrefixedEmailSelectorTestTags
import ch.protonmail.android.test.ksp.annotations.AttachTo
import ch.protonmail.android.test.ksp.annotations.VerifiesOuter
import ch.protonmail.android.uitest.robot.ComposeSectionRobot
import ch.protonmail.android.uitest.robot.composer.ComposerRobot
import ch.protonmail.android.uitest.robot.composer.model.ComposerFieldPrefixes
import ch.protonmail.android.uitest.util.assertions.assertEditableTextEquals
import ch.protonmail.android.uitest.util.assertions.assertNotEditableTextEquals
import ch.protonmail.android.uitest.util.child

@AttachTo(targets = [ComposerRobot::class], identifier = "senderSection")
internal class ComposerSenderSection : ComposeSectionRobot() {

    private val parent = composeTestRule.onNodeWithTag(ComposerTestTags.FromSender)
    private val text = parent.child {
        hasTestTag(PrefixedEmailSelectorTestTags.TextField)
    }
    private val changeSenderButton = parent.child {
        hasTestTag(ComposerTestTags.ChangeSenderButton)
    }

    fun tapChangeSender() {
        changeSenderButton.performClick()
    }

    @VerifiesOuter
    inner class Verify {

        fun hasValue(value: String) = text.run {
            assertNotEditableTextEquals(ComposerFieldPrefixes.From.value)
            assertEditableTextEquals(value)
        }
    }
}
