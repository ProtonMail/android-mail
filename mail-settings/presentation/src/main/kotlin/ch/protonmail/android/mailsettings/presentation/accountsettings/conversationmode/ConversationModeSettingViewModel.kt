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

package ch.protonmail.android.mailsettings.presentation.accountsettings.conversationmode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.mailsettings.domain.usecase.ObserveMailSettings
import ch.protonmail.android.mailsettings.presentation.accountsettings.conversationmode.ConversationModeSettingState.Data
import ch.protonmail.android.mailsettings.presentation.accountsettings.conversationmode.ConversationModeSettingState.Loading
import ch.protonmail.android.mailsettings.presentation.accountsettings.conversationmode.ConversationModeSettingState.NotLoggedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.compose.viewmodel.stopTimeoutMillis
import me.proton.core.mailsettings.domain.entity.MailSettings
import me.proton.core.mailsettings.domain.entity.ViewMode.ConversationGrouping
import me.proton.core.mailsettings.domain.entity.ViewMode.NoConversationGrouping
import me.proton.core.mailsettings.domain.repository.MailSettingsRepository
import javax.inject.Inject

@HiltViewModel
class ConversationModeSettingViewModel @Inject constructor(
    private val accountManager: AccountManager,
    private val mailSettingsRepository: MailSettingsRepository,
    observeMailSettings: ObserveMailSettings
) : ViewModel() {

    val state: StateFlow<ConversationModeSettingState> =
        accountManager.getPrimaryUserId().flatMapLatest { userId ->
            if (userId == null) flowOf(NotLoggedIn)
            else observeMailSettings(userId).mapToState()
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(stopTimeoutMillis),
            Loading
        )

    fun onConversationToggled(isEnabled: Boolean) = accountManager
        .getPrimaryUserId()
        .filterNotNull()
        .mapLatest { userId -> mailSettingsRepository.updateViewMode(userId, viewModeFrom(isEnabled)) }
        .launchIn(viewModelScope)

    private fun viewModeFrom(isEnabled: Boolean) = if (isEnabled) {
        ConversationGrouping
    } else {
        NoConversationGrouping
    }

    private fun Flow<MailSettings?>.mapToState(): Flow<Data> =
        map { mailSettings -> Data(mailSettings?.viewMode?.enum?.let { it == ConversationGrouping }) }
}
