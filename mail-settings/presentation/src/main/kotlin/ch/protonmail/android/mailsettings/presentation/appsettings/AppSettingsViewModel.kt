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

package ch.protonmail.android.mailsettings.presentation.appsettings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.Either
import arrow.core.left
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailfeatureflags.domain.annotation.IsCategoryViewEnabled
import ch.protonmail.android.mailfeatureflags.domain.model.FeatureFlag
import ch.protonmail.android.mailsession.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailsettings.domain.repository.AppSettingsRepository
import ch.protonmail.android.mailsettings.presentation.appsettings.usecase.GetAppIconDescription
import ch.protonmail.android.mailsettings.presentation.appsettings.usecase.GetNotificationsEnabled
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class AppSettingsViewModel @Inject constructor(
    val appSettingsRepository: AppSettingsRepository,
    val getNotificationsEnabled: GetNotificationsEnabled,
    val getAppIconDescription: GetAppIconDescription,
    val observePrimaryUserId: ObservePrimaryUserId,
    @IsCategoryViewEnabled private val isCategoryViewEnabled: FeatureFlag<Boolean>
) : ViewModel() {

    val state = appSettingsRepository.observeAppSettings().map { appSettings ->
        val notificationsEnabled = getNotificationsEnabled()
        val appIconDescription = getAppIconDescription()
        val uiModel = AppSettingsUiModelMapper.toUiModel(
            appSettings = appSettings,
            notificationsEnabled = notificationsEnabled,
            appIconDescription = appIconDescription,
            isEmailCategoriesEnabled = isCategoryViewEnabled.get()
        )
        AppSettingsState.Data(settings = uiModel)
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMs),
            initialValue = AppSettingsState.Loading
        )

    internal fun submit(intent: AppSettingsAction) {
        viewModelScope.launch {
            when (intent) {
                is ToggleAlternativeRouting -> updateAlternativeRouting(intent.value)
                is ToggleUseCombinedContacts -> updateUseCombinedContacts(intent.value)
                is ToggleSwipeToNextEmail -> updateSwipeToNextEmail(intent.value)
            }
        }
    }

    private suspend fun updateAlternativeRouting(value: Boolean) = appSettingsRepository.updateAlternativeRouting(value)
    private suspend fun updateUseCombinedContacts(value: Boolean) =
        appSettingsRepository.updateUseCombineContacts(value)

    private suspend fun updateSwipeToNextEmail(value: Boolean): Either<DataError, Unit> {
        val userId = observePrimaryUserId().firstOrNull() ?: return DataError.Local.NoUserSession.left()
        return appSettingsRepository.updateSwipeToNextEmail(userId, value)
    }

    companion object {

        // needs a stop timeout MS much shorter than our standard because we can change the notification settings
        // and return to the app in less than 2 seconds and our toggle value needs to update
        const val stopTimeoutMs = 1500L
    }
}
