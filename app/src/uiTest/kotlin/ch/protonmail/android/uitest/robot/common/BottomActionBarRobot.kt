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

package ch.protonmail.android.uitest.robot.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithTag
import ch.protonmail.android.mailcommon.domain.model.Action
import ch.protonmail.android.mailcommon.presentation.R
import ch.protonmail.android.mailcommon.presentation.model.contentDescriptionRes
import ch.protonmail.android.mailcommon.presentation.model.descriptionRes
import ch.protonmail.android.test.ksp.annotations.VerifiesOuter
import ch.protonmail.android.uitest.robot.ComposeRobot
import ch.protonmail.android.uitest.util.onNodeWithContentDescription
import ch.protonmail.android.uitest.util.onNodeWithText
import me.proton.core.compose.component.PROTON_PROGRESS_TEST_TAG

internal class BottomActionBarRobot : ComposeRobot() {

    @VerifiesOuter
    inner class Verify {

        fun loaderIsDisplayed() {
            onLoaderNode().assertIsDisplayed()
        }

        fun failedLoadingErrorIsDisplayed() {
            onErrorMessageNode().assertIsDisplayed()
        }

        fun errorAndLoaderHidden() {
            onLoaderNode().assertDoesNotExist()
            onErrorMessageNode().assertDoesNotExist()
        }

        fun actionIsDisplayed(action: Action) {
            composeTestRule.onNodeWithContentDescription(action.descriptionRes)
                .assertIsDisplayed()
        }

        fun actionIsNotDisplayed(action: Action) {
            composeTestRule.onNodeWithContentDescription(action.contentDescriptionRes)
                .assertDoesNotExist()
        }

        private fun onErrorMessageNode() = composeTestRule.onNodeWithText(R.string.common_error_loading_actions)

        private fun onLoaderNode() = composeTestRule.onNodeWithTag(PROTON_PROGRESS_TEST_TAG, useUnmergedTree = true)
    }
}

internal fun ComposeContentTestRule.BottomActionBarRobot(content: @Composable () -> Unit): BottomActionBarRobot {
    setContent(content)
    return BottomActionBarRobot()
}
