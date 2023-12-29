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

package ch.protonmail.android.mailsettings.presentation.settings.autolock.usecase

import ch.protonmail.android.mailsettings.domain.model.autolock.AutoLockPin
import ch.protonmail.android.mailsettings.domain.usecase.autolock.SaveAutoLockPin
import ch.protonmail.android.mailsettings.domain.usecase.autolock.ToggleAutoLockAttemptPendingStatus
import ch.protonmail.android.mailsettings.domain.usecase.autolock.ToggleAutoLockEnabled
import ch.protonmail.android.mailsettings.domain.usecase.autolock.UpdateRemainingAutoLockAttempts
import javax.inject.Inject

class ResetAutoLockDefaults @Inject constructor(
    private val toggleAutoLockEnabled: ToggleAutoLockEnabled,
    private val updateRemainingAutoLockAttempts: UpdateRemainingAutoLockAttempts,
    private val saveAutoLockPin: SaveAutoLockPin,
    private val toggleAutoLockAttemptStatus: ToggleAutoLockAttemptPendingStatus
) {

    suspend operator fun invoke() {
        toggleAutoLockEnabled(Defaults.AutoLockPreference)
        saveAutoLockPin(Defaults.AutoLockPin)
        updateRemainingAutoLockAttempts(Defaults.AutoLockRemainingAttempts)
        toggleAutoLockAttemptStatus(Defaults.AutoLockPendingStatus)
    }

    private object Defaults {

        const val AutoLockPreference = false
        val AutoLockPin = AutoLockPin("")
        const val AutoLockRemainingAttempts = 10
        const val AutoLockPendingStatus = false
    }
}
