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
package ch.protonmail.android.uitest.robot.mailbox.drafts

import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onFirst
import ch.protonmail.android.maillabel.presentation.R
import ch.protonmail.android.uitest.util.awaitDisplayed
import ch.protonmail.android.uitest.util.onAllNodesWithText

class DraftsRobot {

    /**
     * Contains all the validations that can be performed by [DraftsRobot].
     */
    class Verify {

        fun draftsScreenDisplayed(composeRule: ComposeContentTestRule) {
            composeRule
                .onAllNodesWithText(R.string.label_title_drafts)
                .onFirst() // "Drafts" string has matches in both topbar and sidebar. Only topbar one is displayed.
                .awaitDisplayed(composeRule)
        }
    }

    inline fun verify(block: Verify.() -> Unit) = Verify().apply(block)
}
