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

package ch.protonmail.android.uitest.screen.common

import androidx.compose.ui.test.junit4.ComposeContentTestRule
import ch.protonmail.android.mailcommon.domain.model.Action
import ch.protonmail.android.mailcommon.presentation.model.BottomBarState
import ch.protonmail.android.mailcommon.presentation.ui.BottomActionBar
import ch.protonmail.android.test.annotations.suite.SmokeExtendedTest
import ch.protonmail.android.testdata.action.ActionUiModelTestData
import ch.protonmail.android.uitest.robot.common.BottomActionBarRobot
import ch.protonmail.android.uitest.robot.common.verify
import ch.protonmail.android.uitest.util.ComposeTestRuleHolder
import org.junit.Rule
import org.junit.Test

@SmokeExtendedTest
internal class BottomActionBarTest {

    @get:Rule
    val composeTestRule: ComposeContentTestRule = ComposeTestRuleHolder.createAndGetComposeRule()

    @Test
    fun whenBottomBarStateIsLoadingDisplayLoader() {
        // given
        val state = BottomBarState.Loading

        // when
        val robot = setupScreen(state = state)

        // then
        robot.verify { loaderIsDisplayed() }
    }

    @Test
    fun whenBottomBarStateIsFailedLoadingActionsDisplayError() {
        // given
        val state = BottomBarState.Error.FailedLoadingActions

        // when
        val robot = setupScreen(state = state)

        // then
        robot.verify { failedLoadingErrorIsDisplayed() }
    }

    @Test
    fun whenBottomBarStateIsDataUpToMaxActionsAreShowed() {
        // given
        val state = BottomBarState.Data(
            listOf(
                ActionUiModelTestData.reply,
                ActionUiModelTestData.forward,
                ActionUiModelTestData.archive,
                ActionUiModelTestData.move,
                ActionUiModelTestData.label,
                ActionUiModelTestData.markUnread,
                ActionUiModelTestData.reportPhishing
            )
        )

        // when
        val robot = setupScreen(state = state)

        // then
        robot.verify {
            errorAndLoaderHidden()

            actionIsDisplayed(Action.Reply)
            actionIsDisplayed(Action.Forward)
            actionIsDisplayed(Action.Archive)
            actionIsDisplayed(Action.Move)
            actionIsDisplayed(Action.Label)
            actionIsDisplayed(Action.MarkUnread)

            actionIsNotDisplayed(Action.ReportPhishing)
        }
    }

    private fun setupScreen(
        state: BottomBarState,
        actions: BottomActionBar.Actions = BottomActionBar.Actions.Empty
    ): BottomActionBarRobot = composeTestRule.BottomActionBarRobot {
        BottomActionBar(state = state, viewActionCallbacks = actions)
    }
}
