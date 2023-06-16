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

package ch.protonmail.android.uitest.robot.detail

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithTag
import ch.protonmail.android.maildetail.presentation.ui.MessageDetailScreenTestTags
import ch.protonmail.android.test.ksp.annotations.AsDsl
import ch.protonmail.android.test.ksp.annotations.VerifiesOuter
import ch.protonmail.android.uitest.robot.ComposeRobot
import ch.protonmail.android.uitest.util.awaitDisplayed

@AsDsl
internal class MessageDetailRobot : ComposeRobot() {

    @VerifiesOuter
    inner class Verify {

        fun messageDetailScreenIsShown() {
            composeTestRule.onNodeWithTag(MessageDetailScreenTestTags.RootItem)
                .awaitDisplayed()
                .assertExists()
        }
    }
}

internal fun ComposeContentTestRule.MessageDetailRobot(content: @Composable () -> Unit): MessageDetailRobot {
    setContent(content)
    return MessageDetailRobot()
}
