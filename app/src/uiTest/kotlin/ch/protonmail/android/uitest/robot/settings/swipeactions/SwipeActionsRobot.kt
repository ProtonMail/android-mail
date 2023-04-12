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

package ch.protonmail.android.uitest.robot.settings.swipeactions

import androidx.annotation.StringRes
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.performClick
import ch.protonmail.android.mailsettings.presentation.R.string
import ch.protonmail.android.uitest.util.assertions.assertTextContains
import ch.protonmail.android.uitest.util.awaitDisplayed
import ch.protonmail.android.uitest.util.onNodeWithText

class SwipeActionsRobot(val composeTestRule: ComposeContentTestRule) {

    fun openSwipeLeft(): EditSwipeActionRobot {
        composeTestRule
            .onNodeWithText(string.mail_settings_swipe_left_name)
            .awaitDisplayed(composeTestRule)
            .performClick()

        return EditSwipeActionRobot(composeTestRule)
    }

    fun openSwipeRight(): EditSwipeActionRobot {
        composeTestRule
            .onNodeWithText(string.mail_settings_swipe_right_name)
            .awaitDisplayed(composeTestRule)
            .performClick()

        return EditSwipeActionRobot(composeTestRule)
    }

    inline fun verify(block: Verify.() -> Unit): SwipeActionsRobot {
        Verify(composeTestRule).apply(block)
        return this
    }

    class Verify(val composeTestRule: ComposeContentTestRule) {

        inline fun swipeLeft(block: VerifySwipeAction.() -> Unit): VerifySwipeAction =
            VerifySwipeAction(composeTestRule, composeTestRule.onNodeWithText(string.mail_settings_swipe_left_name))
                .apply(block)

        inline fun swipeRight(block: VerifySwipeAction.() -> Unit): VerifySwipeAction =
            VerifySwipeAction(composeTestRule, composeTestRule.onNodeWithText(string.mail_settings_swipe_right_name))
                .apply(block)

        class VerifySwipeAction(
            private val composeTestRule: ComposeContentTestRule,
            private val interaction: SemanticsNodeInteraction
        ) {

            fun isArchive() {
                assertHasText(string.mail_settings_swipe_action_archive_title)
            }

            fun isMarkRead() {
                assertHasText(string.mail_settings_swipe_action_read_title)
            }

            private fun assertHasText(@StringRes textRes: Int) {
                interaction
                    .awaitDisplayed(composeTestRule)
                    .assertTextContains(textRes)
                    .assertIsDisplayed()
            }
        }
    }
}
