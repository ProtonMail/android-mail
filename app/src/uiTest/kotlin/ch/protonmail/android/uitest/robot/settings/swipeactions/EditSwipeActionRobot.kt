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

import androidx.compose.ui.test.performClick
import ch.protonmail.android.mailsettings.presentation.R.string
import ch.protonmail.android.uitest.robot.ComposeRobot
import ch.protonmail.android.uitest.util.onAllNodesWithText
import ch.protonmail.android.uitest.util.onNodeWithContentDescription
import me.proton.core.presentation.compose.R.string as coreString

internal class EditSwipeActionRobot : ComposeRobot() {

    fun navigateUpToSwipeActions(): SwipeActionsRobot {
        composeTestRule
            .onNodeWithContentDescription(coreString.presentation_back)
            .performClick()

        return SwipeActionsRobot()
    }

    fun selectArchive(): EditSwipeActionRobot {
        composeTestRule
            .onAllNodesWithText(string.mail_settings_swipe_action_archive_description)[0]
            .performClick()

        return this
    }

    fun selectMarkRead(): EditSwipeActionRobot {
        composeTestRule
            .onAllNodesWithText(string.mail_settings_swipe_action_read_description)[0]
            .performClick()

        return this
    }
}
