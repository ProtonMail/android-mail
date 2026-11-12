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

package ch.protonmail.android.mailsettings.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.design.compose.viewmodel.stopTimeoutMillis
import ch.protonmail.android.mailbyoe.domain.model.ByoeSurface
import ch.protonmail.android.mailbyoe.domain.model.ByoeTelemetryEvent
import ch.protonmail.android.mailbyoe.domain.model.GmailConnectionSettingsAccess
import ch.protonmail.android.mailbyoe.domain.usecase.GetGmailConnectionSettingsAccess
import ch.protonmail.android.mailbyoe.domain.usecase.ReportByoeTelemetryEvent
import ch.protonmail.android.mailcommon.domain.AppInformation
import ch.protonmail.android.mailcontentsearch.domain.usecase.IsContentSearchFeatureEnabled
import ch.protonmail.android.mailsession.domain.usecase.ObservePrimaryAccount
import ch.protonmail.android.mailsession.presentation.mapper.AccountInformationMapper
import ch.protonmail.android.design.compose.model.VisibilityUiModel
import ch.protonmail.android.mailsettings.domain.usecase.ObserveStorageQuotaUseCase
import ch.protonmail.android.mailsettings.presentation.settings.SettingsState.Data
import ch.protonmail.android.mailsettings.presentation.settings.SettingsState.Loading
import ch.protonmail.android.mailsettings.presentation.settings.converter.toUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val appInformation: AppInformation,
    observePrimaryAccount: ObservePrimaryAccount,
    observeStorageQuotaUseCase: ObserveStorageQuotaUseCase,
    private val accountInformationMapper: AccountInformationMapper,
    private val isContentSearchFeatureEnabled: IsContentSearchFeatureEnabled,
    private val getGmailConnectionSettingsAccess: GetGmailConnectionSettingsAccess,
    private val reportByoeTelemetryEvent: ReportByoeTelemetryEvent
) : ViewModel() {

    val state = combine(observePrimaryAccount(), observeStorageQuotaUseCase()) { account, storageQuota ->
        Data(
            userId = account?.userId,
            accountInfoUiModel = account?.let { accountInformationMapper.toUiModel(it) },
            storageQuotaUiModel = storageQuota.getOrNull()?.let { quota ->
                VisibilityUiModel.Visible(quota.toUiModel())
            } ?: VisibilityUiModel.Hidden,
            appInformation = appInformation,
            isContentSearchEnabled = account != null && isContentSearchFeatureEnabled(account.userId),
            // ListOnly stays hidden until the connected-addresses list exists (DAWG-111).
            isConnectGmailVisible = account != null &&
                getGmailConnectionSettingsAccess(account.userId) == GmailConnectionSettingsAccess.Full
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(stopTimeoutMillis),
        Loading
    )

    /**
     * The settings show Connect Gmail: on each resume, and when the row appears on a resumed screen. This is the
     * Android counterpart of the SDK's BYOE settings page with Connect.
     */
    fun onConnectGmailShown() {
        val userId = (state.value as? Data)?.takeIf { it.isConnectGmailVisible }?.userId ?: return
        reportByoeTelemetryEvent(ByoeTelemetryEvent.SettingsShown, userId)
        reportByoeTelemetryEvent(ByoeTelemetryEvent.SettingsView, userId)
    }

    fun onConnectGmailClicked() {
        val userId = (state.value as? Data)?.userId ?: return
        reportByoeTelemetryEvent(ByoeTelemetryEvent.ExploreClicked(ByoeSurface.Settings), userId)
    }
}
