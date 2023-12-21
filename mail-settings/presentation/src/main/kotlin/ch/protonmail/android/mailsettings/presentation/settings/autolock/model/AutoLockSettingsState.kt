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

package ch.protonmail.android.mailsettings.presentation.settings.autolock.model

import ch.protonmail.android.mailcommon.presentation.Effect

sealed interface AutoLockSettingsState {

    object Loading : AutoLockSettingsState

    data class DataLoaded(
        val autoLockEnabledState: AutoLockEnabledState,
        val autoLockIntervalsState: AutoLockIntervalState,
        val forceOpenPinCreation: Effect<Unit>,
        val pinLockChangeRequested: Effect<Unit>,
        val updateError: Effect<Unit>
    ) : AutoLockSettingsState {

        data class AutoLockEnabledState(val autoLockEnabledUiModel: AutoLockEnabledUiModel)
        data class AutoLockIntervalState(
            val autoLockIntervalsUiModel: AutoLockIntervalsUiModel,
            val dropdownExpanded: Boolean
        )
    }
}
