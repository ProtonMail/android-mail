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
import ch.protonmail.android.mailsettings.presentation.settings.notifications.model.ExtendedNotificationsSettingUiModel
import ch.protonmail.android.mailsettings.presentation.settings.notifications.model.PushNotificationSettingsEvent
import ch.protonmail.android.mailsettings.presentation.settings.notifications.model.PushNotificationSettingsViewAction
import ch.protonmail.android.mailsettings.presentation.settings.notifications.model.PushNotificationsSettingsOperation
import ch.protonmail.android.mailsettings.presentation.settings.notifications.model.PushNotificationsSettingsState
import javax.inject.Inject

class PushNotificationsSettingsReducer @Inject constructor() {

    fun newStateFrom(currentState: PushNotificationsSettingsState, operation: PushNotificationsSettingsOperation) =
        when (operation) {
            is PushNotificationSettingsViewAction -> operation.newStateForAction(currentState)
            is PushNotificationSettingsEvent -> currentState.toNewStateFromEvent(operation)
        }

    private fun PushNotificationsSettingsState.toNewStateFromEvent(
        event: PushNotificationSettingsEvent
    ): PushNotificationsSettingsState {

        return when (this) {
            is PushNotificationsSettingsState.Loading -> when (event) {
                is PushNotificationSettingsEvent.Data.Loaded -> event.toDataState()
                is PushNotificationSettingsEvent.Error -> PushNotificationsSettingsState.LoadingError
            }

            is PushNotificationsSettingsState.DataLoaded -> when (event) {
                is PushNotificationSettingsEvent.Error.UpdateError -> this.copy(
                    updateErrorState = PushNotificationsSettingsState.UpdateErrorState(Effect.of(Unit))
                )

                else -> this
            }

            else -> this
        }
    }

    private fun PushNotificationSettingsViewAction.newStateForAction(
        currentState: PushNotificationsSettingsState
    ): PushNotificationsSettingsState {
        return when (currentState) {
            is PushNotificationsSettingsState.DataLoaded -> when (this) {
                is PushNotificationSettingsViewAction.ToggleExtendedNotifications ->
                    updateExtendedNotificationValue(currentState, newValue)
            }

            else -> currentState
        }
    }

    private fun updateExtendedNotificationValue(
        currentState: PushNotificationsSettingsState.DataLoaded,
        value: Boolean
    ): PushNotificationsSettingsState.DataLoaded {
        val updatedUiModel = currentState.extendedNotificationState.model.copy(enabled = value)
        return currentState.copy(
            extendedNotificationState = PushNotificationsSettingsState.ExtendedNotificationState(updatedUiModel)
        )
    }

    private fun PushNotificationSettingsEvent.Data.Loaded.toDataState(): PushNotificationsSettingsState.DataLoaded {
        val notificationsExtendedUiModel = ExtendedNotificationsSettingUiModel(notificationExtended.enabled)
        val updateErrorState = PushNotificationsSettingsState.UpdateErrorState(Effect.empty())

        return PushNotificationsSettingsState.DataLoaded(
            PushNotificationsSettingsState.ExtendedNotificationState(notificationsExtendedUiModel),
            updateErrorState
        )
    }
}
