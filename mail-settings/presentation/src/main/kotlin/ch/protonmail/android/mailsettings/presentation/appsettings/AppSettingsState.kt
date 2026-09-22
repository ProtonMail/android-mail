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

import ch.protonmail.android.mailcommon.presentation.model.TextUiModel

internal sealed class AppSettingsState {
    data class Data(val settings: AppSettingsUiModel) : AppSettingsState()
    data object Loading : AppSettingsState()
}

internal data class AppSettingsUiModel(
    val autoLockEnabled: Boolean,
    val alternativeRoutingEnabled: Boolean,
    val customLanguage: String?,
    val theme: TextUiModel,
    val deviceContactsEnabled: Boolean,
    val notificationsEnabledStatus: TextUiModel,
    val swipeNextEnabled: Boolean,
    val appIconName: TextUiModel
)

internal sealed interface AppSettingsAction
data class ToggleAlternativeRouting(val value: Boolean) : AppSettingsAction
data class ToggleUseCombinedContacts(val value: Boolean) : AppSettingsAction
data class ToggleSwipeToNextEmail(val value: Boolean) : AppSettingsAction
