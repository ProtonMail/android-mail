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

package ch.protonmail.android.uitest.screen.snackbar

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.rememberScaffoldState
import androidx.compose.material.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import ch.protonmail.android.test.annotations.suite.SmokeTest
import ch.protonmail.android.uicomponents.snackbar.DismissableSnackbarHost
import ch.protonmail.android.uitest.util.HiltInstrumentedTest
import ch.protonmail.android.uitest.util.awaitDisplayed
import ch.protonmail.android.uitest.util.awaitHidden
import dagger.hilt.android.testing.HiltAndroidTest
import me.proton.core.compose.component.ProtonSnackbarHostState
import me.proton.core.compose.component.ProtonSnackbarType
import me.proton.core.compose.theme.ProtonTheme
import org.junit.Test

@SmokeTest
@HiltAndroidTest
internal class DismissableSnackbarHostTest : HiltInstrumentedTest() {

    private val snackbarHost = composeTestRule.onNodeWithTag(MainSnackbarTestTag)

    @Test
    fun snackbarCanBeDismissedRtl() {
        // Given
        prepareScreenWithSnackbarHost()
        snackbarHost.awaitDisplayed()

        // When
        snackbarHost.performTouchInput {
            // Override startX and endX otherwise it does not have enough space to perform the swipe action.
            swipeLeft(
                startX = snackbarHost.getBoundsInRoot().right.value,
                endX = snackbarHost.getBoundsInRoot().left.value
            )
        }

        // Then
        snackbarHost.awaitHidden()
    }

    @Test
    fun snackbarCanBeDismissedLtr() {
        // Given
        prepareScreenWithSnackbarHost()
        snackbarHost.awaitDisplayed()

        // When
        snackbarHost.performTouchInput {
            swipeRight()
        }

        // Then
        snackbarHost.awaitHidden()
    }

    private fun prepareScreenWithSnackbarHost() {
        composeTestRule.setContent {
            ProtonTheme {
                val protonSnackbarHostState = remember { ProtonSnackbarHostState() }

                LaunchedEffect(Unit) {
                    protonSnackbarHostState.showSnackbar(
                        ProtonSnackbarType.NORM,
                        "Hello hello hello",
                        duration = SnackbarDuration.Indefinite
                    )
                }

                Scaffold(
                    scaffoldState = rememberScaffoldState(),
                    snackbarHost = {
                        DismissableSnackbarHost(
                            modifier = Modifier.testTag(MainSnackbarTestTag),
                            protonSnackbarHostState = protonSnackbarHostState
                        )
                    }
                ) { paddingValues ->
                    Text(
                        modifier = Modifier.padding(paddingValues), text = "Text"
                    )
                }
            }
        }
    }

    private companion object {

        const val MainSnackbarTestTag = "MainSnackbar"
    }
}
