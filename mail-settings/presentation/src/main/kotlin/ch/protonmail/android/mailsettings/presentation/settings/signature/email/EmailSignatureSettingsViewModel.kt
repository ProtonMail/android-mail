/*
 * Copyright (c) 2025 Proton Technologies AG
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

package ch.protonmail.android.mailsettings.presentation.settings.signature.email

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.mailsession.domain.usecase.ForkSession
import ch.protonmail.android.mailsession.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailsettings.domain.repository.AppSettingsRepository
import ch.protonmail.android.mailsettings.domain.usecase.HandleCloseWebSettings
import ch.protonmail.android.mailsettings.domain.usecase.ObserveWebSettingsConfig
import ch.protonmail.android.mailsettings.presentation.websettings.WebSettingsState
import ch.protonmail.android.mailsettings.presentation.websettings.model.WebSettingsAction
import ch.protonmail.android.mailsettings.presentation.websettings.model.WebSettingsOperation
import ch.protonmail.android.mailsettings.presentation.websettings.toEmailSignatureUrl
import ch.protonmail.android.mailupselling.presentation.model.UpsellingVisibility
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class EmailSignatureSettingsViewModel @Inject constructor(
    observePrimaryUserId: ObservePrimaryUserId,
    appSettingsRepository: AppSettingsRepository,
    observeWebSettingsConfig: ObserveWebSettingsConfig,
    private val forkSession: ForkSession,
    private val handleCloseWebSettings: HandleCloseWebSettings

) : ViewModel() {

    private val _state = MutableStateFlow<WebSettingsState>(WebSettingsState.Loading)
    val state: StateFlow<WebSettingsState> = _state.asStateFlow()

    init {
        combine(
            observePrimaryUserId().filterNotNull(),
            appSettingsRepository.observeTheme(),
            observeWebSettingsConfig()
        ) { userId, theme, webSettingsConfig ->

            forkSession(userId).fold(
                ifRight = { fork ->
                    WebSettingsState.Data(
                        webSettingsUrl = webSettingsConfig.toEmailSignatureUrl(fork.selector.value, theme),
                        theme = theme,
                        upsellingVisibility = UpsellingVisibility.Hidden, // Upselling flow not available here.
                        sessionId = fork.sessionId.value
                    )
                },
                ifLeft = { sessionError ->
                    Timber.e("web-settings: Forking session failed")
                    WebSettingsState.Error("Forking session failed due to $sessionError")
                }
            )
        }
            .onEach { newState ->
                _state.value = newState
            }
            .launchIn(viewModelScope)
    }

    internal fun submit(action: WebSettingsOperation) {
        viewModelScope.launch {
            when (action) {
                is WebSettingsAction.OnCloseWebSettings -> handleCloseWebSettings()
            }
        }
    }
}
