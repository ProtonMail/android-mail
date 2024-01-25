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

package ch.protonmail.android.mailsettings.domain.usecase.autolock.biometric

import ch.protonmail.android.mailsettings.domain.extension.toAutoLockBiometricsState
import ch.protonmail.android.mailsettings.domain.model.autolock.biometric.AutoLockBiometricsState
import ch.protonmail.android.mailsettings.domain.repository.AutoLockRepository
import ch.protonmail.android.mailsettings.domain.repository.BiometricsSystemStateRepository
import javax.inject.Inject

class GetCurrentAutoLockBiometricState @Inject constructor(
    private val autoLockRepository: AutoLockRepository,
    private val biometricsSystemStateRepository: BiometricsSystemStateRepository
) {

    suspend operator fun invoke(): AutoLockBiometricsState {
        val biometricEnabledPreference = autoLockRepository.getCurrentAutoLockBiometricsPreference()
        val biometricSystemState = biometricsSystemStateRepository.getCurrentState()
        return biometricSystemState.toAutoLockBiometricsState(biometricEnabledPreference)
    }
}
