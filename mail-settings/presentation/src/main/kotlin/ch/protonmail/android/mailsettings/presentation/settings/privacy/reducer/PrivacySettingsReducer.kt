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

package ch.protonmail.android.mailsettings.presentation.settings.privacy.reducer

import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailsettings.presentation.settings.privacy.PrivacySettingsEvent
import ch.protonmail.android.mailsettings.presentation.settings.privacy.PrivacySettingsState
import javax.inject.Inject

class PrivacySettingsReducer @Inject constructor() {

    fun newStateFrom(currentState: PrivacySettingsState, event: PrivacySettingsEvent) =
        currentState.toNewStateFromEvent(event)

    private fun PrivacySettingsState.toNewStateFromEvent(event: PrivacySettingsEvent): PrivacySettingsState {

        return when (this) {
            is PrivacySettingsState.WithData -> when (event) {
                is PrivacySettingsEvent.Data.ContentLoaded -> this
                is PrivacySettingsEvent.Data.AutoLoadRemoteContentChanged ->
                    copy(settings = settings.copy(autoShowRemoteContent = event.newValue))

                is PrivacySettingsEvent.Data.AutoShowEmbeddedImagesChanged ->
                    copy(settings = settings.copy(autoShowEmbeddedImages = event.newValue))

                is PrivacySettingsEvent.Data.PreventScreenshotsChanged ->
                    copy(settings = settings.copy(preventTakingScreenshots = event.newValue))

                is PrivacySettingsEvent.Data.RequestLinkConfirmationChanged ->
                    copy(settings = settings.copy(requestLinkConfirmation = event.newValue))

                is PrivacySettingsEvent.Data.AllowBackgroundSyncChanged ->
                    copy(settings = settings.copy(allowBackgroundSync = event.newValue))

                is PrivacySettingsEvent.Error ->
                    copy(updateSettingsError = Effect.of(Unit))
            }

            is PrivacySettingsState.Loading -> when (event) {
                is PrivacySettingsEvent.Data.ContentLoaded -> PrivacySettingsState.WithData(
                    settings = event.settings,
                    updateSettingsError = Effect.empty()
                )

                is PrivacySettingsEvent.Error.LoadingError -> PrivacySettingsState.LoadingError
                else -> this
            }

            else -> this
        }
    }
}
