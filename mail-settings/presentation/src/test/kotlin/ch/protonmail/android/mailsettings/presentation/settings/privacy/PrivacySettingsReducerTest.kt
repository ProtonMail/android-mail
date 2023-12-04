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

package ch.protonmail.android.mailsettings.presentation.settings.privacy

import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailsettings.domain.model.PrivacySettings
import ch.protonmail.android.mailsettings.presentation.settings.privacy.reducer.PrivacySettingsReducer
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
internal class PrivacySettingsReducerTest(
    private val testName: String,
    private val testInput: TestInput
) {

    private val privacySettingsReducer = PrivacySettingsReducer()

    @Test
    fun `should produce the expected new state`() = with(testInput) {
        val actualState = privacySettingsReducer.newStateFrom(currentState, event)
        assertEquals(expectedState, actualState, testName)
    }

    companion object {

        private val baseSettings = PrivacySettings(
            autoShowRemoteContent = false,
            autoShowEmbeddedImages = false,
            preventTakingScreenshots = false,
            requestLinkConfirmation = false,
            allowBackgroundSync = false
        )

        private val transitionFromLoadingState = listOf(
            TestInput(
                currentState = PrivacySettingsState.Loading,
                event = PrivacySettingsEvent.Data.ContentLoaded(baseSettings),
                expectedState = PrivacySettingsState.WithData(
                    settings = baseSettings,
                    updateSettingsError = Effect.empty()
                )
            ),
            TestInput(
                currentState = PrivacySettingsState.Loading,
                event = PrivacySettingsEvent.Error.LoadingError,
                expectedState = PrivacySettingsState.LoadingError
            )
        )

        private val transitionsFromLoadedState = listOf(
            TestInput(
                currentState = PrivacySettingsState.WithData(baseSettings, updateSettingsError = Effect.empty()),
                event = PrivacySettingsEvent.Error.UpdateError,
                expectedState = PrivacySettingsState.WithData(baseSettings, updateSettingsError = Effect.of(Unit))
            ),
            TestInput(
                currentState = PrivacySettingsState.WithData(baseSettings, updateSettingsError = Effect.empty()),
                event = PrivacySettingsEvent.Data.AutoLoadRemoteContentChanged(newValue = true),
                expectedState = PrivacySettingsState.WithData(
                    baseSettings.copy(autoShowRemoteContent = true),
                    updateSettingsError = Effect.empty()
                )
            ),
            TestInput(
                currentState = PrivacySettingsState.WithData(baseSettings, updateSettingsError = Effect.empty()),
                event = PrivacySettingsEvent.Data.AutoShowEmbeddedImagesChanged(newValue = true),
                expectedState = PrivacySettingsState.WithData(
                    baseSettings.copy(autoShowEmbeddedImages = true),
                    updateSettingsError = Effect.empty()
                )
            ),
            TestInput(
                currentState = PrivacySettingsState.WithData(baseSettings, updateSettingsError = Effect.empty()),
                event = PrivacySettingsEvent.Data.RequestLinkConfirmationChanged(newValue = true),
                expectedState = PrivacySettingsState.WithData(
                    baseSettings.copy(requestLinkConfirmation = true),
                    updateSettingsError = Effect.empty()
                )
            ),
            TestInput(
                currentState = PrivacySettingsState.WithData(baseSettings, updateSettingsError = Effect.empty()),
                event = PrivacySettingsEvent.Data.AllowBackgroundSyncChanged(newValue = true),
                expectedState = PrivacySettingsState.WithData(
                    baseSettings.copy(allowBackgroundSync = true),
                    updateSettingsError = Effect.empty()
                )
            )
        )

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data(): Collection<Array<Any>> = (transitionFromLoadingState + transitionsFromLoadedState)
            .map { testInput ->
                val testName = """
                    Current state: ${testInput.currentState}
                    Event: ${testInput.event}
                    Next state: ${testInput.expectedState}        
                """.trimIndent()
                arrayOf(testName, testInput)
            }
    }

    data class TestInput(
        val currentState: PrivacySettingsState,
        val event: PrivacySettingsEvent,
        val expectedState: PrivacySettingsState
    )
}
