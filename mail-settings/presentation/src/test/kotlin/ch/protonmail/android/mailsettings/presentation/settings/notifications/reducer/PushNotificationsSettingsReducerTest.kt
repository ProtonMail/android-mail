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

package ch.protonmail.android.mailsettings.presentation.settings.notifications.reducer

import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailsettings.domain.model.ExtendedNotificationPreference
import ch.protonmail.android.mailsettings.presentation.settings.notifications.model.ExtendedNotificationsSettingUiModel
import ch.protonmail.android.mailsettings.presentation.settings.notifications.model.PushNotificationSettingsEvent
import ch.protonmail.android.mailsettings.presentation.settings.notifications.model.PushNotificationSettingsViewAction
import ch.protonmail.android.mailsettings.presentation.settings.notifications.model.PushNotificationsSettingsOperation
import ch.protonmail.android.mailsettings.presentation.settings.notifications.model.PushNotificationsSettingsState
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
internal class PushNotificationsSettingsReducerTest(
    private val testName: String,
    private val testInput: TestInput
) {

    private val reducer = PushNotificationsSettingsReducer()

    @Test
    fun `should produce the expected new state`() = with(testInput) {
        val actualState = reducer.newStateFrom(currentState, operation)
        assertEquals(expectedState, actualState, testName)
    }

    companion object {

        private val notificationExtendedPreference = ExtendedNotificationPreference(false)
        private val baseNotificationExtendedState = PushNotificationsSettingsState.ExtendedNotificationState(
            ExtendedNotificationsSettingUiModel(notificationExtendedPreference.enabled)
        )
        private val baseUpdateErrorState = PushNotificationsSettingsState.UpdateErrorState(Effect.empty())
        private val baseState = PushNotificationsSettingsState.DataLoaded(
            extendedNotificationState = baseNotificationExtendedState,
            updateErrorState = baseUpdateErrorState
        )

        private val transitionFromLoadingState = listOf(
            TestInput(
                currentState = PushNotificationsSettingsState.Loading,
                operation = PushNotificationSettingsEvent.Data.Loaded(notificationExtendedPreference),
                expectedState = baseState
            ),
            TestInput(
                currentState = PushNotificationsSettingsState.Loading,
                operation = PushNotificationSettingsEvent.Error.LoadingError,
                expectedState = PushNotificationsSettingsState.LoadingError
            )
        )

        private val transitionsFromLoadedState = listOf(
            TestInput(
                currentState = baseState,
                operation = PushNotificationSettingsEvent.Error.UpdateError,
                expectedState = baseState.copy(
                    updateErrorState = PushNotificationsSettingsState.UpdateErrorState(Effect.of(Unit))
                )
            ),
            TestInput(
                currentState = baseState,
                operation = PushNotificationSettingsViewAction.ToggleExtendedNotifications(newValue = true),
                expectedState = baseState.copy(
                    extendedNotificationState = baseNotificationExtendedState.copy(
                        ExtendedNotificationsSettingUiModel(true)
                    )
                )
            )
        )

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data(): Collection<Array<Any>> = (transitionFromLoadingState + transitionsFromLoadedState)
            .map { testInput ->
                val testName = """
                    Current state: ${testInput.currentState}
                    Operation: ${testInput.operation}
                    Next state: ${testInput.expectedState}        
                """.trimIndent()
                arrayOf(testName, testInput)
            }
    }

    data class TestInput(
        val currentState: PushNotificationsSettingsState,
        val operation: PushNotificationsSettingsOperation,
        val expectedState: PushNotificationsSettingsState
    )
}
