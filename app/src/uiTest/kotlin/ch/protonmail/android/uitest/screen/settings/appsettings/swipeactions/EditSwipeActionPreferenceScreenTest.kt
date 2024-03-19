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

package ch.protonmail.android.uitest.screen.settings.appsettings.swipeactions

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import ch.protonmail.android.mailsettings.domain.model.SwipeActionDirection
import ch.protonmail.android.mailsettings.presentation.settings.swipeactions.EditSwipeActionPreferenceScreen
import ch.protonmail.android.mailsettings.presentation.settings.swipeactions.EditSwipeActionPreferenceState
import ch.protonmail.android.mailsettings.presentation.testdata.SwipeActionsTestData
import ch.protonmail.android.test.annotations.suite.RegressionTest
import ch.protonmail.android.uitest.util.HiltInstrumentedTest
import ch.protonmail.android.uitest.util.onNodeWithText
import dagger.hilt.android.testing.HiltAndroidTest
import me.proton.core.compose.component.PROTON_PROGRESS_TEST_TAG
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.mailsettings.domain.entity.SwipeAction
import org.junit.Test
import ch.protonmail.android.mailsettings.presentation.R.string as settingsString

@RegressionTest
@HiltAndroidTest
internal class EditSwipeActionPreferenceScreenTest : HiltInstrumentedTest() {

    @Test
    fun whenRightSwipeIsSelectedCorrectTitleIsShown() {
        // when
        setContentWithState(EditSwipeActionPreferenceState.Loading, direction = SwipeActionDirection.RIGHT)

        // then
        composeTestRule.onNodeWithText(settingsString.mail_settings_swipe_right_name)
            .assertIsDisplayed()
    }

    @Test
    fun whenLeftSwipeIsSelectedCorrectTitleIsShown() {
        // when
        setContentWithState(EditSwipeActionPreferenceState.Loading, direction = SwipeActionDirection.LEFT)

        // then
        composeTestRule.onNodeWithText(settingsString.mail_settings_swipe_left_name)
            .assertIsDisplayed()
    }

    @Test
    fun whileDataIsLoadingProgressIsShown() {
        // when
        setContentWithState(EditSwipeActionPreferenceState.Loading, direction = SwipeActionDirection.LEFT)

        // then
        composeTestRule.onNodeWithTag(PROTON_PROGRESS_TEST_TAG)
            .assertIsDisplayed()
    }

    @Test
    fun whenDataIsReadyCorrectItemIsSelected() {
        // when
        val items = SwipeActionsTestData.Edit.buildAllItems(selected = SwipeAction.Star)
        setContentWithState(EditSwipeActionPreferenceState.Data(items), direction = SwipeActionDirection.LEFT)

        // then
        composeTestRule.onArchive()
            .assertIsDisplayed()
            .assertIsNotSelected()

        composeTestRule.onRead()
            .assertIsDisplayed()
            .assertIsNotSelected()

        composeTestRule.onSpam()
            .assertIsDisplayed()
            .assertIsNotSelected()

        composeTestRule.onStar()
            .assertIsDisplayed()
            .assertIsSelected()

        composeTestRule.onTrash()
            .assertIsDisplayed()
            .assertIsNotSelected()
    }

    private fun setContentWithState(state: EditSwipeActionPreferenceState, direction: SwipeActionDirection) {
        composeTestRule.setContent {
            ProtonTheme {
                EditSwipeActionPreferenceScreen(
                    state = state,
                    direction = direction,
                    onBack = {},
                    onSwipeActionSelect = {}
                )
            }
        }
    }

    private fun ComposeTestRule.onArchive(): SemanticsNodeInteraction =
        onNodeWithText(settingsString.mail_settings_swipe_action_archive_description)

    private fun ComposeTestRule.onRead(): SemanticsNodeInteraction =
        onNodeWithText(settingsString.mail_settings_swipe_action_read_description)

    private fun ComposeTestRule.onSpam(): SemanticsNodeInteraction =
        onNodeWithText(settingsString.mail_settings_swipe_action_spam_description)

    private fun ComposeTestRule.onStar(): SemanticsNodeInteraction =
        onNodeWithText(settingsString.mail_settings_swipe_action_star_description)

    private fun ComposeTestRule.onTrash(): SemanticsNodeInteraction =
        onNodeWithText(settingsString.mail_settings_swipe_action_trash_description)
}
