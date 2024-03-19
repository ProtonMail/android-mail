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

import ch.protonmail.android.mailcommon.domain.model.Action
import ch.protonmail.android.mailcommon.presentation.model.BottomBarState
import ch.protonmail.android.mailcommon.presentation.ui.BottomActionBar
import ch.protonmail.android.test.annotations.suite.RegressionTest
import ch.protonmail.android.uitest.util.HiltInstrumentedTest
import ch.protonmail.android.testdata.action.ActionUiModelTestData
import ch.protonmail.android.uitest.robot.common.BottomActionBarRobot
import ch.protonmail.android.uitest.robot.common.verify
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.collections.immutable.toImmutableList
import org.junit.Test

@RegressionTest
@HiltAndroidTest
internal class BottomActionBarTest : HiltInstrumentedTest() {

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
        val state = BottomBarState.Data.Shown(
            listOf(
                ActionUiModelTestData.star,
                ActionUiModelTestData.delete,
                ActionUiModelTestData.archive,
                ActionUiModelTestData.move,
                ActionUiModelTestData.label,
                ActionUiModelTestData.markUnread,
                ActionUiModelTestData.reportPhishing
            ).toImmutableList()
        )

        // when
        val robot = setupScreen(state = state)

        // then
        robot.verify {
            errorAndLoaderHidden()

            actionIsDisplayed(Action.Star)
            actionIsDisplayed(Action.Delete)
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
