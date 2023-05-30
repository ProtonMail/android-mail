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

import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import ch.protonmail.android.mailsettings.presentation.settings.combinedcontacts.TEST_TAG_COMBINED_CONTACTS_TOGGLE_ITEM
import ch.protonmail.android.test.ksp.annotations.VerifiesOuter
import ch.protonmail.android.uitest.robot.ComposeRobot

internal class CombinedContactsRobot : ComposeRobot() {

    fun turnOnCombinedContacts(): CombinedContactsRobot {
        composeTestRule
            .onNodeWithTag(TEST_TAG_COMBINED_CONTACTS_TOGGLE_ITEM)
            .performClick()
        composeTestRule.waitUntil { combinedContactsSettingIsToggled() }
        return this
    }

    private fun combinedContactsSettingIsToggled(): Boolean {
        try {
            composeTestRule
                .onNodeWithTag(TEST_TAG_COMBINED_CONTACTS_TOGGLE_ITEM)
                .assertIsOn()
        } catch (ignored: AssertionError) {
            return false
        }
        return true
    }

    @VerifiesOuter
    inner class Verify {

        fun combinedContactsSettingIsToggled() {
            composeTestRule
                .onNodeWithTag(TEST_TAG_COMBINED_CONTACTS_TOGGLE_ITEM)
                .assertIsOn()
        }
    }
}
