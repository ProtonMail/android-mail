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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailsettings.domain.model.PrivacySettings
import ch.protonmail.android.mailsettings.domain.usecase.ObserveMailSettings
import ch.protonmail.android.mailsettings.domain.usecase.privacy.UpdateAutoShowEmbeddedImagesSetting
import ch.protonmail.android.mailsettings.domain.usecase.privacy.UpdateLinkConfirmationSetting
import ch.protonmail.android.mailsettings.domain.usecase.privacy.UpdateShowRemoteContentSetting
import ch.protonmail.android.mailsettings.presentation.settings.privacy.reducer.PrivacySettingsReducer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.proton.core.domain.type.IntEnum
import me.proton.core.mailsettings.domain.entity.MailSettings
import me.proton.core.mailsettings.domain.entity.ShowImage
import javax.inject.Inject

@HiltViewModel
class PrivacySettingsViewModel @Inject constructor(
    observePrimaryUserId: ObservePrimaryUserId,
    private val observeMailSettings: ObserveMailSettings,
    private val updateShowRemoteContentSetting: UpdateShowRemoteContentSetting,
    private val updateAutoShowEmbeddedImagesSetting: UpdateAutoShowEmbeddedImagesSetting,
    private val updateLinkConfirmationSetting: UpdateLinkConfirmationSetting,
    private val privacySettingsReducer: PrivacySettingsReducer
) : ViewModel() {

    private val mutableState = MutableStateFlow<PrivacySettingsState>(PrivacySettingsState.Loading)
    val state = mutableState.asStateFlow()

    init {
        observePrimaryUserId().mapLatest { userId ->
            userId ?: return@mapLatest emitNewStateFrom(PrivacySettingsEvent.Error.LoadingError)

            val settings = observeMailSettings(userId).firstOrNull()
                ?: return@mapLatest emitNewStateFrom(PrivacySettingsEvent.Error.LoadingError)

            emitNewStateFrom(PrivacySettingsEvent.Data.ContentLoaded(settings.toPrivacySettings()))
        }.launchIn(viewModelScope)
    }

    fun onAutoShowRemoteContentToggled(newValue: Boolean) {
        viewModelScope.launch {
            updateShowRemoteContentSetting(newValue)
                .onLeft { emitNewStateFrom(PrivacySettingsEvent.Error.UpdateError) }
                .onRight { emitNewStateFrom(PrivacySettingsEvent.Data.AutoLoadRemoteContentChanged(newValue)) }
        }
    }

    fun onAutoShowEmbeddedImagesToggled(newValue: Boolean) {
        viewModelScope.launch {
            updateAutoShowEmbeddedImagesSetting(newValue)
                .onLeft { emitNewStateFrom(PrivacySettingsEvent.Error.UpdateError) }
                .onRight { emitNewStateFrom(PrivacySettingsEvent.Data.AutoShowEmbeddedImagesChanged(newValue)) }
        }
    }

    fun onConfirmLinkToggled(newValue: Boolean) {
        viewModelScope.launch {
            updateLinkConfirmationSetting(newValue)
                .onLeft { emitNewStateFrom(PrivacySettingsEvent.Error.UpdateError) }
                .onRight { emitNewStateFrom(PrivacySettingsEvent.Data.RequestLinkConfirmationChanged(newValue)) }
        }
    }

    private fun emitNewStateFrom(event: PrivacySettingsEvent) = mutableState.update {
        privacySettingsReducer.newStateFrom(it, event)
    }
}

private fun MailSettings.toPrivacySettings(): PrivacySettings {
    return PrivacySettings(
        autoShowRemoteContent = showImages.isAutoShowRemoteContentEnabled,
        autoShowEmbeddedImages = showImages.isAutoShowEmbeddedImages,
        preventTakingScreenshots = false, // to be extracted (MAILANDR-1059)
        requestLinkConfirmation = confirmLink ?: false
    )
}

private val IntEnum<ShowImage>?.isAutoShowRemoteContentEnabled: Boolean
    get() = this?.enum == ShowImage.Remote || this?.enum == ShowImage.Both

private val IntEnum<ShowImage>?.isAutoShowEmbeddedImages: Boolean
    get() = this?.enum == ShowImage.Embedded || this?.enum == ShowImage.Both
