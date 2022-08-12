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

package ch.protonmail.android.uitest.robot.settings

import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import ch.protonmail.android.mailsettings.presentation.settings.alternativerouting.TEST_TAG_ALTERNATIVE_ROUTING_TOGGLE_ITEM

class AlternativeRoutingRobot(
    val composeTestRule: ComposeContentTestRule
) {

    fun turnOffAlternativeRouting(): AlternativeRoutingRobot {
        composeTestRule
            .onNodeWithTag(TEST_TAG_ALTERNATIVE_ROUTING_TOGGLE_ITEM)
            .performClick()
        composeTestRule.waitUntil { AlternativeRoutingSettingIsToggled(composeTestRule) }
        return this
    }

    private fun AlternativeRoutingSettingIsToggled(
        composeTestRule: ComposeContentTestRule
    ): Boolean {
        try {
            composeTestRule
                .onNodeWithTag(TEST_TAG_ALTERNATIVE_ROUTING_TOGGLE_ITEM)
                .assertIsOff()
        } catch (ignored: AssertionError) {
            return false
        }
        return true
    }

    inline fun verify(block: Verify.() -> Unit): AlternativeRoutingRobot =
        also { Verify(composeTestRule).apply(block) }

    class Verify(private val composeTestRule: ComposeContentTestRule) {

        fun alternativeRoutingSettingIsToggled() {
            composeTestRule
                .onNodeWithTag(TEST_TAG_ALTERNATIVE_ROUTING_TOGGLE_ITEM)
                .assertIsOff()
        }
    }
}
