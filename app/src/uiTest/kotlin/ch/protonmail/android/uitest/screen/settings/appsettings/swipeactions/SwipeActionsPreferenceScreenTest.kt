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

import androidx.compose.ui.test.assertAny
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onParent
import ch.protonmail.android.mailsettings.presentation.settings.swipeactions.SwipeActionPreferenceUiModel
import ch.protonmail.android.mailsettings.presentation.settings.swipeactions.SwipeActionsPreferenceScreen
import ch.protonmail.android.mailsettings.presentation.settings.swipeactions.SwipeActionsPreferenceState
import ch.protonmail.android.mailsettings.presentation.settings.swipeactions.SwipeActionsPreferenceState.Loading
import ch.protonmail.android.mailsettings.presentation.settings.swipeactions.SwipeActionsPreferenceUiModel
import ch.protonmail.android.test.annotations.suite.RegressionTest
import ch.protonmail.android.uitest.util.HiltInstrumentedTest
import ch.protonmail.android.uitest.util.hasText
import ch.protonmail.android.uitest.util.onNodeWithText
import dagger.hilt.android.testing.HiltAndroidTest
import me.proton.core.compose.component.PROTON_PROGRESS_TEST_TAG
import me.proton.core.compose.theme.ProtonTheme
import kotlin.test.Test
import ch.protonmail.android.mailsettings.presentation.R as SettingsR
import me.proton.core.presentation.R as CoreR

@RegressionTest
@HiltAndroidTest
internal class SwipeActionsPreferenceScreenTest : HiltInstrumentedTest() {

    @Test
    fun progressIsShownWhileDataLoading() {
        setContentWithState(Loading)

        composeTestRule
            .onNodeWithTag(PROTON_PROGRESS_TEST_TAG)
            .assertIsDisplayed()
    }

    @Test
    fun correctActionsAreShown() {
        setContentWithState(swipeActionsData)

        composeTestRule
            .onNodeWithText(SettingsR.string.mail_settings_swipe_right_name)
            .onParent()
            .onChildren()
            .assertAny(hasText(swipeActionsData.model.right.titleRes))
            .assertAny(hasText(swipeActionsData.model.right.descriptionRes))

        composeTestRule
            .onNodeWithText(SettingsR.string.mail_settings_swipe_left_name)
            .onParent()
            .onChildren()
            .assertAny(hasText(swipeActionsData.model.left.titleRes))
            .assertAny(hasText(swipeActionsData.model.left.descriptionRes))
    }

    private fun setContentWithState(state: SwipeActionsPreferenceState) {
        composeTestRule.setContent {
            ProtonTheme {
                SwipeActionsPreferenceScreen(
                    state = state,
                    actions = SwipeActionsPreferenceScreen.Actions(
                        onBackClick = {},
                        onChangeSwipeRightClick = {},
                        onChangeSwipeLeftClick = {}
                    )
                )
            }
        }
    }

    private companion object TestData {

        private val swipeActionsData = SwipeActionsPreferenceState.Data(
            SwipeActionsPreferenceUiModel(
                left = SwipeActionPreferenceUiModel(
                    imageRes = CoreR.drawable.ic_proton_archive_box,
                    titleRes = SettingsR.string.mail_settings_swipe_action_archive_title,
                    descriptionRes = SettingsR.string.mail_settings_swipe_action_archive_description,
                    getColor = { ProtonTheme.colors.iconHint }
                ),
                right = SwipeActionPreferenceUiModel(
                    imageRes = CoreR.drawable.ic_proton_trash,
                    titleRes = SettingsR.string.mail_settings_swipe_action_trash_title,
                    descriptionRes = SettingsR.string.mail_settings_swipe_action_trash_description,
                    getColor = { ProtonTheme.colors.notificationError }
                )
            )
        )
    }
}
