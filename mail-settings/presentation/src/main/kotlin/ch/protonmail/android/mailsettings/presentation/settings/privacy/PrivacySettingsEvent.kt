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

import ch.protonmail.android.mailsettings.domain.model.PrivacySettings

sealed interface PrivacySettingsEvent {

    sealed interface Error : PrivacySettingsEvent {
        object UpdateError : Error
        object LoadingError : Error
    }

    sealed interface Data : PrivacySettingsEvent {
        data class ContentLoaded(val settings: PrivacySettings) : PrivacySettingsEvent
        data class AutoLoadRemoteContentChanged(val newValue: Boolean) : PrivacySettingsEvent
        data class AutoShowEmbeddedImagesChanged(val newValue: Boolean) : PrivacySettingsEvent
        data class RequestLinkConfirmationChanged(val newValue: Boolean) : PrivacySettingsEvent
        data class PreventScreenshotsChanged(val newValue: Boolean) : PrivacySettingsEvent
        data class AllowBackgroundSyncChanged(val newValue: Boolean) : PrivacySettingsEvent
    }
}
