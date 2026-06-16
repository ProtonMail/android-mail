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
import ch.protonmail.android.mailcommon.domain.AppInformation
import ch.protonmail.android.mailfeatureflags.domain.annotation.IsContentSearchEnabled
import ch.protonmail.android.mailfeatureflags.domain.model.FeatureFlag
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
    @IsContentSearchEnabled private val contentSearchEnabled: FeatureFlag<Boolean>
) : ViewModel() {

    val state = combine(observePrimaryAccount(), observeStorageQuotaUseCase()) { account, storageQuota ->
        Data(
            userId = account?.userId,
            accountInfoUiModel = account?.let { accountInformationMapper.toUiModel(it) },
            storageQuotaUiModel = storageQuota.getOrNull()?.let { quota ->
                VisibilityUiModel.Visible(quota.toUiModel())
            } ?: VisibilityUiModel.Hidden,
            appInformation = appInformation,
            isContentSearchEnabled = contentSearchEnabled.get()
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(stopTimeoutMillis),
        Loading
    )
}

