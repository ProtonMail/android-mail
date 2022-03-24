/*
 * Copyright (c) 2021 Proton Technologies AG
 * This file is part of Proton Technologies AG and ProtonMail.
 *
 * ProtonMail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonMail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonMail.  If not, see <https://www.gnu.org/licenses/>.
 */
package ch.protonmail.android.uitest.robot.settings.account

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithText

/**
 * Class represents Change Conversation Mode view.
 */
@Suppress("unused")
class ConversationModeRobot(
    private val composeTestRule: ComposeContentTestRule? = null
) {

    /**
     * Contains all the validations that can be performed by [ConversationModeRobot].
     */
    class Verify {

        fun conversationModeToggleShown(composeRule: ComposeContentTestRule) {
            composeRule
                .onNodeWithText("Conversation mode")
                .assertTextContains("Group emails in the same conversation together in your inbox or display them separately")
                .assertIsDisplayed()
                .assertIsEnabled()
        }
    }

    inline fun verify(block: Verify.() -> Unit) =
        Verify().apply(block)
}
