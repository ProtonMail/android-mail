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

package ch.protonmail.android.uitest.screen.settings.appsettings.alternativerouting

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailsettings.presentation.settings.alternativerouting.AlternativeRoutingSettingScreen
import ch.protonmail.android.mailsettings.presentation.settings.alternativerouting.AlternativeRoutingSettingState
import ch.protonmail.android.mailsettings.presentation.settings.alternativerouting.TEST_TAG_ALTERNATIVE_ROUTING_SNACKBAR
import ch.protonmail.android.mailsettings.presentation.settings.alternativerouting.TEST_TAG_ALTERNATIVE_ROUTING_TOGGLE_ITEM
import ch.protonmail.android.test.annotations.suite.RegressionTest
import ch.protonmail.android.uitest.util.HiltInstrumentedTest
import dagger.hilt.android.testing.HiltAndroidTest
import me.proton.core.compose.component.PROTON_PROGRESS_TEST_TAG
import me.proton.core.compose.component.ProtonCenteredProgress
import me.proton.core.compose.theme.ProtonTheme
import org.junit.Test
import kotlin.test.assertEquals

@RegressionTest
@HiltAndroidTest
internal class AlternativeRoutingSettingScreenTest : HiltInstrumentedTest() {

    @Test
    fun testProgressIsShownWhenStateIsLoading() {
        setupScreenWithState(AlternativeRoutingSettingState.Loading)

        composeTestRule
            .onNodeWithTag(PROTON_PROGRESS_TEST_TAG)
            .assertIsDisplayed()
    }

    @Test
    fun testSwitchIsCheckedIfAlternativeRoutingSettingIsEnabled() {
        setupScreenWithState(
            AlternativeRoutingSettingState.Data(isEnabled = true, alternativeRoutingSettingErrorEffect = Effect.empty())
        )

        composeTestRule
            .onNodeWithTag(TEST_TAG_ALTERNATIVE_ROUTING_TOGGLE_ITEM)
            .assertIsOn()
    }

    @Test
    fun testSwitchIsNotCheckedIfAlternativeRoutingSettingIsNotEnabled() {
        setupScreenWithState(
            AlternativeRoutingSettingState.Data(
                isEnabled = false,
                alternativeRoutingSettingErrorEffect = Effect.empty()
            )
        )

        composeTestRule
            .onNodeWithTag(TEST_TAG_ALTERNATIVE_ROUTING_TOGGLE_ITEM)
            .assertIsOff()
    }

    @Test
    fun testCallbackIsInvokedWhenSwitchIsToggled() {
        var isEnabled = false
        setupScreenWithState(
            state = AlternativeRoutingSettingState.Data(
                isEnabled = false,
                alternativeRoutingSettingErrorEffect = Effect.empty()
            ),
            onToggle = { isEnabled = !isEnabled }
        )

        composeTestRule
            .onNodeWithTag(TEST_TAG_ALTERNATIVE_ROUTING_TOGGLE_ITEM)
            .performClick()

        assertEquals(true, isEnabled)
    }

    @Test
    fun testErrorSnackbarIsShownWhenStateContainsThrowableEffect() {
        setupScreenWithState(
            AlternativeRoutingSettingState.Data(
                isEnabled = false,
                alternativeRoutingSettingErrorEffect = Effect.of(Unit)
            )
        )

        composeTestRule
            .onNodeWithTag(TEST_TAG_ALTERNATIVE_ROUTING_SNACKBAR)
            .assertIsDisplayed()
    }

    @Test
    fun testSwitchIsOffAndSnackbarIsShownWhenSwitchStateIsNull() {
        setupScreenWithState(
            AlternativeRoutingSettingState.Data(
                isEnabled = null,
                alternativeRoutingSettingErrorEffect = Effect.of(Unit)
            )
        )

        composeTestRule
            .onNodeWithTag(TEST_TAG_ALTERNATIVE_ROUTING_TOGGLE_ITEM)
            .assertIsDisplayed()
            .assertIsOff()
        composeTestRule
            .onNodeWithTag(TEST_TAG_ALTERNATIVE_ROUTING_SNACKBAR)
            .assertIsDisplayed()
    }

    private fun setupScreenWithState(
        state: AlternativeRoutingSettingState,
        onBackClick: () -> Unit = {},
        onToggle: (Boolean) -> Unit = {}
    ) {
        composeTestRule.setContent {
            ProtonTheme {
                when (state) {
                    is AlternativeRoutingSettingState.Data -> {
                        AlternativeRoutingSettingScreen(
                            onBackClick = onBackClick,
                            onToggle = onToggle,
                            state = state
                        )
                    }
                    is AlternativeRoutingSettingState.Loading -> {
                        ProtonCenteredProgress(Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}
