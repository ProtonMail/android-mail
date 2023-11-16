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

package ch.protonmail.android.mailsettings.presentation.settings.notifications.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.getOrElse
import ch.protonmail.android.mailsettings.domain.usecase.notifications.GetExtendedNotificationsSetting
import ch.protonmail.android.mailsettings.domain.usecase.notifications.SetExtendedNotificationsSetting
import ch.protonmail.android.mailsettings.presentation.settings.notifications.model.PushNotificationSettingsEvent
import ch.protonmail.android.mailsettings.presentation.settings.notifications.model.PushNotificationSettingsViewAction
import ch.protonmail.android.mailsettings.presentation.settings.notifications.model.PushNotificationsSettingsOperation
import ch.protonmail.android.mailsettings.presentation.settings.notifications.model.PushNotificationsSettingsState
import ch.protonmail.android.mailsettings.presentation.settings.notifications.reducer.PushNotificationsSettingsReducer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PushNotificationsSettingsViewModel @Inject constructor(
    getNotificationExtendedPreference: GetExtendedNotificationsSetting,
    private val updateNotificationExtendedPreference: SetExtendedNotificationsSetting,
    private val reducer: PushNotificationsSettingsReducer
) : ViewModel() {

    private val mutableState =
        MutableStateFlow<PushNotificationsSettingsState>(PushNotificationsSettingsState.Loading)
    val state = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            val preference = getNotificationExtendedPreference().getOrElse {
                return@launch emitNewStateFrom(PushNotificationSettingsEvent.Error.LoadingError)
            }

            emitNewStateFrom(PushNotificationSettingsEvent.Data.Loaded(preference))
        }
    }

    internal fun submit(action: PushNotificationSettingsViewAction) {
        viewModelScope.launch {
            when (action) {
                is PushNotificationSettingsViewAction.ToggleExtendedNotifications ->
                    updateNotificationExtendedSettings(action.newValue)
            }
        }
    }

    private suspend fun updateNotificationExtendedSettings(value: Boolean) {
        updateNotificationExtendedPreference(value).onLeft {
            emitNewStateFrom(PushNotificationSettingsEvent.Error.UpdateError)
        }.onRight {
            emitNewStateFrom(PushNotificationSettingsViewAction.ToggleExtendedNotifications(value))
        }
    }

    private fun emitNewStateFrom(event: PushNotificationsSettingsOperation) = mutableState.update {
        reducer.newStateFrom(it, event)
    }
}
